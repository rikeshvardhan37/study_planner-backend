package backend;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class StudyTaskController {

        private final StudyTaskRepository taskRepository;
        private final StudySessionRepository sessionRepository;

        public StudyTaskController(
                        StudyTaskRepository taskRepository,
                        StudySessionRepository sessionRepository) {

                this.taskRepository = taskRepository;
                this.sessionRepository = sessionRepository;
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
        // GET TASKS
        // ============================================================

        @GetMapping
        public List<StudyTask> getUserTasks(
                        HttpSession session) {

                return taskRepository.findByUserId(
                                getLoggedInUserId(session));
        }

        // ============================================================
        // ADD TASK
        // ============================================================

        @PostMapping
        public StudyTask addTask(
                        @RequestBody StudyTask request,
                        HttpSession session) {

                Long userId = getLoggedInUserId(session);

                if (request.getSubject() == null
                                || request.getSubject().isBlank()) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Subject is required.");
                }

                if (request.getTopic() == null
                                || request.getTopic().isBlank()) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Topic is required.");
                }

                if (request.getDate() == null
                                || request.getDate().isBlank()) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Date is required.");
                }

                StudyTask task = new StudyTask();

                task.setUserId(userId);
                task.setSubject(request.getSubject().trim());
                task.setTopic(request.getTopic().trim());
                task.setDate(request.getDate());

                task.setPriority(
                                request.getPriority() == null
                                                || request.getPriority().isBlank()
                                                                ? "Medium"
                                                                : request.getPriority());

                task.setStatus("Pending");

                return taskRepository.save(task);
        }

        // ============================================================
        // UPDATE TASK DETAILS
        // ============================================================

        @PutMapping("/{id}")
        public StudyTask updateTask(
                        @PathVariable Long id,
                        @RequestBody StudyTask request,
                        HttpSession session) {

                Long userId = getLoggedInUserId(session);

                StudyTask existingTask = taskRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Study task not found."));

                if (!userId.equals(existingTask.getUserId())) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "You cannot update this study task.");
                }

                // A task cannot be marked completed through this endpoint.
                if (request.getStatus() != null
                                && "Completed".equalsIgnoreCase(
                                                request.getStatus())
                                && !"Completed".equalsIgnoreCase(
                                                existingTask.getStatus())) {

                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "A task can only be completed after its study timer finishes.");
                }

                if (request.getSubject() != null
                                && !request.getSubject().isBlank()) {
                        existingTask.setSubject(
                                        request.getSubject().trim());
                }

                if (request.getTopic() != null
                                && !request.getTopic().isBlank()) {
                        existingTask.setTopic(
                                        request.getTopic().trim());
                }

                if (request.getDate() != null
                                && !request.getDate().isBlank()) {
                        existingTask.setDate(request.getDate());
                }

                if (request.getPriority() != null
                                && !request.getPriority().isBlank()) {
                        existingTask.setPriority(
                                        request.getPriority());
                }

                if (request.getStatus() != null
                                && !request.getStatus().isBlank()
                                && !"Completed".equalsIgnoreCase(
                                                request.getStatus())) {
                        existingTask.setStatus(
                                        request.getStatus());
                }

                return taskRepository.save(existingTask);
        }

        // ============================================================
        // DELETE TASK
        // ============================================================

        @DeleteMapping("/{id}")
        public void deleteTask(
                        @PathVariable Long id,
                        HttpSession session) {

                Long userId = getLoggedInUserId(session);

                StudyTask existingTask = taskRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Study task not found."));

                if (!userId.equals(existingTask.getUserId())) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "You cannot delete this study task.");
                }

                if (sessionRepository
                                .findByTaskIdAndActiveTrue(id)
                                .isPresent()) {

                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "You cannot delete a task while its timer is running.");
                }

                taskRepository.deleteById(id);
        }
}