package backend;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@CrossOrigin(origins = { "http://localhost:5173",
                "https://study-planner-frontend-ashen.vercel.app" }, allowCredentials = "true")
public class SubjectController {

        private final SubjectRepository subjectRepository;

        public SubjectController(
                        SubjectRepository subjectRepository) {
                this.subjectRepository = subjectRepository;
        }

        // =========================================================
        // GET LOGGED-IN USER ID
        // =========================================================

        private Long getLoggedInUserId(
                        HttpSession session) {

                Object userIdObject = session.getAttribute("userId");

                if (userIdObject == null) {
                        throw new RuntimeException(
                                        "User is not logged in.");
                }

                return (Long) userIdObject;
        }

        // =========================================================
        // GET SUBJECTS
        // =========================================================

        @GetMapping
        public ResponseEntity<?> getSubjects(
                        HttpSession session) {

                try {

                        Long userId = getLoggedInUserId(session);

                        List<Subject> subjects = subjectRepository.findByUserId(userId);

                        return ResponseEntity.ok(subjects);

                } catch (Exception e) {

                        return ResponseEntity.status(401)
                                        .body("User is not logged in.");
                }
        }

        // =========================================================
        // ADD SUBJECT
        // =========================================================

        @PostMapping
        public ResponseEntity<?> addSubject(
                        @RequestBody Subject subject,
                        HttpSession session) {

                try {

                        Long userId = getLoggedInUserId(session);

                        if (subject.getName() == null ||
                                        subject.getName().trim().isEmpty()) {

                                return ResponseEntity.badRequest()
                                                .body("Subject name is required.");
                        }

                        subject.setUserId(userId);

                        Subject savedSubject = subjectRepository.save(subject);

                        return ResponseEntity.ok(savedSubject);

                } catch (Exception e) {

                        return ResponseEntity.status(401)
                                        .body("User is not logged in.");
                }
        }

        // =========================================================
        // UPDATE SUBJECT
        // =========================================================

        @PutMapping("/{id}")
        public ResponseEntity<?> updateSubject(
                        @PathVariable Long id,
                        @RequestBody Subject updatedSubject,
                        HttpSession session) {

                try {

                        Long userId = getLoggedInUserId(session);

                        Subject existingSubject = subjectRepository
                                        .findById(id)
                                        .orElse(null);

                        if (existingSubject == null) {

                                return ResponseEntity.notFound()
                                                .build();
                        }

                        if (!userId.equals(
                                        existingSubject.getUserId())) {

                                return ResponseEntity.status(403)
                                                .body(
                                                                "You cannot update this subject.");
                        }

                        existingSubject.setName(
                                        updatedSubject.getName());

                        existingSubject.setDescription(
                                        updatedSubject.getDescription());

                        Subject savedSubject = subjectRepository.save(
                                        existingSubject);

                        return ResponseEntity.ok(savedSubject);

                } catch (Exception e) {

                        return ResponseEntity.status(401)
                                        .body("User is not logged in.");
                }
        }

        // =========================================================
        // DELETE SUBJECT
        // =========================================================

        @DeleteMapping("/{id}")
        public ResponseEntity<?> deleteSubject(
                        @PathVariable Long id,
                        HttpSession session) {

                try {

                        Long userId = getLoggedInUserId(session);

                        Subject existingSubject = subjectRepository
                                        .findById(id)
                                        .orElse(null);

                        if (existingSubject == null) {

                                return ResponseEntity.notFound()
                                                .build();
                        }

                        if (!userId.equals(
                                        existingSubject.getUserId())) {

                                return ResponseEntity.status(403)
                                                .body(
                                                                "You cannot delete this subject.");
                        }

                        subjectRepository.delete(
                                        existingSubject);

                        return ResponseEntity.ok(
                                        "Subject deleted successfully.");

                } catch (Exception e) {

                        return ResponseEntity.status(401)
                                        .body("User is not logged in.");
                }
        }
}