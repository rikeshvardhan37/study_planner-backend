package backend;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/syllabus")
@CrossOrigin(
        origins = "http://localhost:5173",
        allowCredentials = "true"
)
public class SyllabusController {

    private final SyllabusRepository syllabusRepository;

    public SyllabusController(SyllabusRepository syllabusRepository) {
        this.syllabusRepository = syllabusRepository;
    }

    @GetMapping
    public ResponseEntity<?> getSyllabus(HttpSession session) {

        Object userIdObject = session.getAttribute("userId");

        if (userIdObject == null) {
            return ResponseEntity.status(401)
                    .body("Not logged in.");
        }

        Long userId = (Long) userIdObject;

        List<Syllabus> syllabus =
                syllabusRepository.findByUserId(userId);

        return ResponseEntity.ok(syllabus);
    }

    @PostMapping
    public ResponseEntity<?> addSyllabus(
            @RequestBody Syllabus syllabus,
            HttpSession session) {

        Object userIdObject = session.getAttribute("userId");

        if (userIdObject == null) {
            return ResponseEntity.status(401)
                    .body("Not logged in.");
        }

        Long userId = (Long) userIdObject;

        syllabus.setUserId(userId);

        Syllabus saved =
                syllabusRepository.save(syllabus);

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSyllabus(
            @PathVariable Long id,
            @RequestBody Syllabus updatedSyllabus,
            HttpSession session) {

        Object userIdObject = session.getAttribute("userId");

        if (userIdObject == null) {
            return ResponseEntity.status(401)
                    .body("Not logged in.");
        }

        Long userId = (Long) userIdObject;

        Syllabus existing =
                syllabusRepository.findById(id).orElse(null);

        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        if (!userId.equals(existing.getUserId())) {
            return ResponseEntity.status(403)
                    .body("You cannot modify this syllabus.");
        }

        existing.setSubject(updatedSyllabus.getSubject());
        existing.setUnit(updatedSyllabus.getUnit());
        existing.setTopic(updatedSyllabus.getTopic());
        existing.setContent(updatedSyllabus.getContent());

        Syllabus saved =
                syllabusRepository.save(existing);

        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSyllabus(
            @PathVariable Long id,
            HttpSession session) {

        Object userIdObject = session.getAttribute("userId");

        if (userIdObject == null) {
            return ResponseEntity.status(401)
                    .body("Not logged in.");
        }

        Long userId = (Long) userIdObject;

        Syllabus existing =
                syllabusRepository.findById(id).orElse(null);

        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        if (!userId.equals(existing.getUserId())) {
            return ResponseEntity.status(403)
                    .body("You cannot delete this syllabus.");
        }

        syllabusRepository.delete(existing);

        return ResponseEntity.ok(
                "Syllabus deleted successfully."
        );
    }
}