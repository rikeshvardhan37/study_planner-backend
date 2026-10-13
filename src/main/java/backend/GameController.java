package backend;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(
        origins = {"http://localhost:5173", "https://study-planner-frontend-ashen.vercel.app"}
        allowCredentials = "true"
)
public class GameController {

    private final GameAccessRepository gameAccessRepository;
    private final StudyTaskRepository studyTaskRepository;

    private static final int REQUIRED_TASKS = 3;
    private static final int GAME_DURATION_MINUTES = 10;

    public GameController(
            GameAccessRepository gameAccessRepository,
            StudyTaskRepository studyTaskRepository) {

        this.gameAccessRepository = gameAccessRepository;
        this.studyTaskRepository = studyTaskRepository;
    }

    // =========================================================
    // GET GAME STATUS
    // =========================================================

    @GetMapping("/status")
    public ResponseEntity<?> getGameStatus(
            HttpSession session) {

        Object userIdObject =
                session.getAttribute("userId");

        if (userIdObject == null) {
            return ResponseEntity.status(401)
                    .body("Not logged in.");
        }

        Long userId = (Long) userIdObject;

        int completedTasks =
                studyTaskRepository
                        .findByUserId(userId)
                        .stream()
                        .filter(task ->
                                "Completed".equals(
                                        task.getStatus()
                                )
                        )
                        .mapToInt(task -> 1)
                        .sum();

        GameAccess gameAccess =
                gameAccessRepository
                        .findByUserId(userId)
                        .orElse(null);

        boolean unlocked =
                completedTasks >= REQUIRED_TASKS;

        boolean active = false;

        long remainingSeconds = 0;

        if (gameAccess != null &&
                gameAccess.getExpiresAt() != null) {

            LocalDateTime now =
                    LocalDateTime.now();

            if (gameAccess.getExpiresAt()
                    .isAfter(now)) {

                active = true;

                remainingSeconds =
                        Duration.between(
                                now,
                                gameAccess.getExpiresAt()
                        ).getSeconds();
            }
        }

        Map<String, Object> result =
                new HashMap<>();

        result.put(
                "requiredTasks",
                REQUIRED_TASKS
        );

        result.put(
                "completedTasks",
                completedTasks
        );

        result.put(
                "unlocked",
                unlocked
        );

        result.put(
                "active",
                active
        );

        result.put(
                "remainingSeconds",
                remainingSeconds
        );

        return ResponseEntity.ok(result);
    }

    // =========================================================
    // START GAME
    // =========================================================

    @PostMapping("/start")
    public ResponseEntity<?> startGame(
            HttpSession session) {

        Object userIdObject =
                session.getAttribute("userId");

        if (userIdObject == null) {
            return ResponseEntity.status(401)
                    .body("Not logged in.");
        }

        Long userId = (Long) userIdObject;

        int completedTasks =
                studyTaskRepository
                        .findByUserId(userId)
                        .stream()
                        .filter(task ->
                                "Completed".equals(
                                        task.getStatus()
                                )
                        )
                        .mapToInt(task -> 1)
                        .sum();

        if (completedTasks < REQUIRED_TASKS) {

            return ResponseEntity.status(403)
                    .body(
                            "Complete 3 study tasks to unlock the game."
                    );
        }

        GameAccess gameAccess =
                gameAccessRepository
                        .findByUserId(userId)
                        .orElse(null);

        LocalDateTime now =
                LocalDateTime.now();

        // Do not restart an already active game
        if (gameAccess != null &&
                gameAccess.getExpiresAt() != null &&
                gameAccess.getExpiresAt()
                        .isAfter(now)) {

            long remaining =
                    Duration.between(
                            now,
                            gameAccess.getExpiresAt()
                    ).getSeconds();

            Map<String, Object> result =
                    new HashMap<>();

            result.put("active", true);
            result.put(
                    "remainingSeconds",
                    remaining
            );

            return ResponseEntity.ok(result);
        }

        if (gameAccess == null) {
            gameAccess = new GameAccess();
        }

        gameAccess.setUserId(userId);
        gameAccess.setStartedAt(now);
        gameAccess.setExpiresAt(
                now.plusMinutes(
                        GAME_DURATION_MINUTES
                )
        );

        gameAccessRepository.save(gameAccess);

        Map<String, Object> result =
                new HashMap<>();

        result.put("active", true);
        result.put(
                "remainingSeconds",
                GAME_DURATION_MINUTES * 60L
        );

        return ResponseEntity.ok(result);
    }
}