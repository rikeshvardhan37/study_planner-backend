package backend;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyTaskRepository
        extends JpaRepository<StudyTask, Long> {

    List<StudyTask> findByUserId(Long userId);
}