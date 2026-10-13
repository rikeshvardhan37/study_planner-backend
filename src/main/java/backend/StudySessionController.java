package backend;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/study-sessions")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class StudySessionController {

        private static final int MIN_DURATION_MINUTES = 20;

        private final StudySessionRepository sessionRepository;
        private final StudyTaskRepository taskRepository;

        public StudySessionController(
                        StudySessionRepository sessionRepository,
                        StudyTaskRepository taskRepository) {

                this.sessionRepository = sessionRepository;
                this.taskRepository = taskRepository;
        }

        private Long getLoggedInUserId(HttpSession session) {

                Object userIdObject = session.getAttribute("userId");

                if (userIdObject == null) {
                        throw new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Please login first.");
                }

                return (Long) userIdObject;
        }

        // ============================================================
        // GET ALL STUDY SESSIONS FOR THE LOGGED-IN USER
        // ============================================================

        @GetMapping
        public List<StudySession> getUserSessions(
                        HttpSession session) {

                Long userId = getLoggedInUserId(session);

                return sessionRepository.findByUserId(userId);
        }

        // ============================================================
        // START TIMER FOR AN EXISTING STUDY TASK
        // ============================================================

        @PostMapping("/start")
        @Transactional
        public StudySession startSession(
                        @RequestBody StudySession request,
                        HttpSession session) {

                Long userId = getLoggedInUserId(session);

                if (request.getTaskId() == null) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Please select a study task.");
                }

                int duration = request.getPlannedDurationMinutes();

                if (duration < MIN_DURATION_MINUTES) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Minimum timer duration is 20 minutes. Received: "
                                                        + duration);
                }

                // Allow only one active timer per user.
                if (sessionRepository
                                .findByUserIdAndActiveTrue(userId)
                                .isPresent()) {

                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "You already have an active study session.");
                }

                // Find the selected task.
                StudyTask task = taskRepository
                                .findById(request.getTaskId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Study task not found."));

                // Ensure this task belongs to the logged-in user.
                if (!userId.equals(task.getUserId())) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "You cannot start a timer for another user's task.");
                }

                if ("Completed".equalsIgnoreCase(task.getStatus())) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "This study task is already completed.");
                }

                LocalDateTime startedAt = LocalDateTime.now();

                StudySession studySession = new StudySession();

                studySession.setUserId(userId);
                studySession.setTaskId(task.getId());

                // Use details from the existing Study Task.
                studySession.setSubject(task.getSubject());
                studySession.setTopic(task.getTopic());
                studySession.setStudyDate(task.getDate());

                studySession.setPlannedDurationMinutes(duration);
                studySession.setDurationMinutes(0);

                studySession.setStartedAt(startedAt);
                studySession.setEndedAt(null);
                studySession.setActive(true);

                return sessionRepository.save(studySession);
        }

        // ============================================================
        // COMPLETE SESSION AND ORIGINAL STUDY TASK
        // ============================================================

        @PostMapping("/{id}/complete")
        @Transactional
        public StudySession completeSession(
                        @PathVariable Long id,
                        HttpSession session) {

                Long userId = getLoggedInUserId(session);

                StudySession studySession = sessionRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Study session not found."));

                if (!userId.equals(studySession.getUserId())) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "You cannot complete this study session.");
                }

                if (!studySession.isActive()) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "This study session is already completed.");
                }

                if (studySession.getStartedAt() == null) {
                        throw new ResponseStatusException(
                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                        "Study session start time is missing.");
                }

                int plannedMinutes = studySession.getPlannedDurationMinutes();

                if (plannedMinutes < MIN_DURATION_MINUTES) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "This session has an invalid timer duration. "
                                                        + "Please start a new session.");
                }

                LocalDateTime now = LocalDateTime.now();

                long elapsedSeconds = Duration.between(
                                studySession.getStartedAt(),
                                now).getSeconds();

                long requiredSeconds = plannedMinutes * 60L;

                // The server rejects early completion.
                if (elapsedSeconds < requiredSeconds) {

                        long remaining = requiredSeconds - elapsedSeconds;

                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Timer is still running. "
                                                        + (remaining / 60)
                                                        + ":"
                                                        + String.format(
                                                                        "%02d",
                                                                        remaining % 60)
                                                        + " remaining.");
                }

                if (studySession.getTaskId() == null) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "This session is not linked to a study task.");
                }

                StudyTask task = taskRepository.findById(
                                studySession.getTaskId()).orElseThrow(
                                                () -> new ResponseStatusException(
                                                                HttpStatus.NOT_FOUND,
                                                                "Linked study task not found."));

                if (!userId.equals(task.getUserId())) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "You cannot complete this study task.");
                }

                if ("Completed".equalsIgnoreCase(task.getStatus())) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "This study task is already completed.");
                }

                // Mark the original task as completed.
                task.setStatus("Completed");
                taskRepository.save(task);

                // Record the completed study session.
                studySession.setEndedAt(now);
                studySession.setDurationMinutes(
                                (int) Math.max(0, elapsedSeconds / 60));
                studySession.setActive(false);

                return sessionRepository.save(studySession);
        }

        // ============================================================
        // DELETE A FINISHED STUDY SESSION
        // ============================================================

        @DeleteMapping("/{id}")
        public void deleteSession(
                        @PathVariable Long id,
                        HttpSession session) {

                Long userId = getLoggedInUserId(session);

                StudySession studySession = sessionRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Study session not found."));

                if (!userId.equals(studySession.getUserId())) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "You cannot delete this study session.");
                }

                if (studySession.isActive()) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "You cannot delete an active study session.");
                }

                sessionRepository.deleteById(id);
        }
}