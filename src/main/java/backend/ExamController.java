package backend;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
@CrossOrigin(
        origins = "http://localhost:5173",
        allowCredentials = "true"
)
public class ExamController {

    private final ExamRepository examRepository;

    public ExamController(
            ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    // ============================================================
    // GET ALL EXAMS FOR LOGGED-IN USER
    // ============================================================

    @GetMapping
    public ResponseEntity<?> getExams(
            HttpSession session) {

        Object userIdObject =
                session.getAttribute("userId");

        if (userIdObject == null) {
            return ResponseEntity
                    .status(401)
                    .body("Not logged in.");
        }

        Long userId =
                (Long) userIdObject;

        List<Exam> exams =
                examRepository.findByUserId(
                        userId
                );

        return ResponseEntity.ok(exams);
    }

    // ============================================================
    // ADD EXAM
    // ============================================================

    @PostMapping
    public ResponseEntity<?> addExam(
            @RequestBody Exam exam,
            HttpSession session) {

        Object userIdObject =
                session.getAttribute("userId");

        if (userIdObject == null) {
            return ResponseEntity
                    .status(401)
                    .body("Not logged in.");
        }

        Long userId =
                (Long) userIdObject;

        exam.setUserId(userId);

        Exam savedExam =
                examRepository.save(exam);

        return ResponseEntity.ok(
                savedExam
        );
    }

    // ============================================================
    // UPDATE EXAM
    // ============================================================

    @PutMapping("/{id}")
    public ResponseEntity<?> updateExam(
            @PathVariable Long id,
            @RequestBody Exam updatedExam,
            HttpSession session) {

        Object userIdObject =
                session.getAttribute("userId");

        if (userIdObject == null) {
            return ResponseEntity
                    .status(401)
                    .body("Not logged in.");
        }

        Long userId =
                (Long) userIdObject;

        Exam existingExam =
                examRepository
                        .findById(id)
                        .orElse(null);

        if (existingExam == null) {
            return ResponseEntity
                    .notFound()
                    .build();
        }

        if (!userId.equals(
                existingExam.getUserId())) {

            return ResponseEntity
                    .status(403)
                    .body("You cannot modify this exam.");
        }

        existingExam.setExamName(
                updatedExam.getExamName()
        );

        existingExam.setSubject(
                updatedExam.getSubject()
        );

        existingExam.setExamDate(
                updatedExam.getExamDate()
        );

        existingExam.setDescription(
                updatedExam.getDescription()
        );

        Exam savedExam =
                examRepository.save(
                        existingExam
                );

        return ResponseEntity.ok(
                savedExam
        );
    }

    // ============================================================
    // DELETE EXAM
    // ============================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExam(
            @PathVariable Long id,
            HttpSession session) {

        Object userIdObject =
                session.getAttribute("userId");

        if (userIdObject == null) {
            return ResponseEntity
                    .status(401)
                    .body("Not logged in.");
        }

        Long userId =
                (Long) userIdObject;

        Exam existingExam =
                examRepository
                        .findById(id)
                        .orElse(null);

        if (existingExam == null) {
            return ResponseEntity
                    .notFound()
                    .build();
        }

        if (!userId.equals(
                existingExam.getUserId())) {

            return ResponseEntity
                    .status(403)
                    .body("You cannot delete this exam.");
        }

        examRepository.delete(
                existingExam
        );

        return ResponseEntity.ok(
                "Exam deleted successfully."
        );
    }
}