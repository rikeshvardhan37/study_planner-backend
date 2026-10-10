package backend;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SyllabusRepository extends JpaRepository<Syllabus, Long> {

    List<Syllabus> findByUserId(Long userId);
}