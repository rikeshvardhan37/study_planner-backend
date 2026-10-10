package backend;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameAccessRepository
        extends JpaRepository<GameAccess, Long> {

    Optional<GameAccess> findByUserId(Long userId);
}