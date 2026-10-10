package backend;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudySessionRepository
        extends JpaRepository<StudySession, Long> {

    List<StudySession> findByUserId(Long userId);

    Optional<StudySession> findByUserIdAndActiveTrue(
            Long userId);

    Optional<StudySession> findByTaskIdAndActiveTrue(
            Long taskId);
}