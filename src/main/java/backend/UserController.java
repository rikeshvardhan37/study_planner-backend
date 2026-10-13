package backend;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = { "http://localhost:5173",
                "https://study-planner-frontend-ashen.vercel.app" }, allowCredentials = "true")
public class UserController {

        private final UserRepository userRepository;
        private final PasswordResetTokenRepository tokenRepository;
        private final PasswordEncoder passwordEncoder;

        public UserController(
                        UserRepository userRepository,
                        PasswordResetTokenRepository tokenRepository,
                        PasswordEncoder passwordEncoder) {

                this.userRepository = userRepository;
                this.tokenRepository = tokenRepository;
                this.passwordEncoder = passwordEncoder;
        }

        // =========================================================
        // REGISTER
        // =========================================================

        @PostMapping("/register")
        public ResponseEntity<?> register(@RequestBody User user) {

                if (user.getName() == null || user.getName().trim().isEmpty()) {
                        return ResponseEntity.badRequest()
                                        .body("Name is required.");
                }

                if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                        return ResponseEntity.badRequest()
                                        .body("Email is required.");
                }

                if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                        return ResponseEntity.badRequest()
                                        .body("Password is required.");
                }

                String email = user.getEmail().trim().toLowerCase();

                if (userRepository.findByEmail(email).isPresent()) {
                        return ResponseEntity.badRequest()
                                        .body("Email is already registered.");
                }

                user.setEmail(email);

                // Never store plain-text password
                user.setPassword(
                                passwordEncoder.encode(user.getPassword()));

                User savedUser = userRepository.save(user);

                savedUser.setPassword(null);

                return ResponseEntity.ok(savedUser);
        }

        // =========================================================
        // LOGIN
        // =========================================================

        @PostMapping("/login")
        public ResponseEntity<?> login(
                        @RequestBody User loginUser,
                        HttpSession session) {

                if (loginUser.getEmail() == null ||
                                loginUser.getPassword() == null) {

                        return ResponseEntity.badRequest()
                                        .body("Email and password are required.");
                }

                String email = loginUser.getEmail().trim().toLowerCase();

                User existingUser = userRepository.findByEmail(email).orElse(null);

                if (existingUser == null) {
                        return ResponseEntity.status(401)
                                        .body("Invalid email or password.");
                }

                boolean passwordMatches = passwordEncoder.matches(
                                loginUser.getPassword(),
                                existingUser.getPassword());

                if (!passwordMatches) {
                        return ResponseEntity.status(401)
                                        .body("Invalid email or password.");
                }

                // Store logged-in user's ID in server session
                session.setAttribute("userId", existingUser.getId());

                // Do not send password to frontend
                existingUser.setPassword(null);

                return ResponseEntity.ok(existingUser);
        }

        // =========================================================
        // CHECK CURRENT LOGIN
        // =========================================================

        @GetMapping("/me")
        public ResponseEntity<?> getCurrentUser(
                        HttpSession session) {

                Object userIdObject = session.getAttribute("userId");

                if (userIdObject == null) {
                        return ResponseEntity.status(401)
                                        .body("Not logged in.");
                }

                Long userId = (Long) userIdObject;

                User user = userRepository.findById(userId).orElse(null);

                if (user == null) {
                        session.invalidate();

                        return ResponseEntity.status(401)
                                        .body("User not found.");
                }

                // Do not send password
                user.setPassword(null);

                return ResponseEntity.ok(user);
        }

        // =========================================================
        // LOGOUT
        // =========================================================

        @PostMapping("/logout")
        public ResponseEntity<?> logout(
                        HttpSession session) {

                session.invalidate();

                return ResponseEntity.ok(
                                "Logged out successfully.");
        }

        // =========================================================
        // FORGOT PASSWORD
        // =========================================================

        @PostMapping("/forgot-password")
        public ResponseEntity<?> forgotPassword(
                        @RequestBody ForgotPasswordRequest request) {

                if (request.getEmail() == null ||
                                request.getEmail().trim().isEmpty()) {

                        return ResponseEntity.badRequest()
                                        .body("Email is required.");
                }

                String email = request.getEmail().trim().toLowerCase();

                User user = userRepository.findByEmail(email).orElse(null);

                /*
                 * We return the same message even when the email
                 * does not exist. This prevents someone from
                 * checking which emails are registered.
                 */

                if (user == null) {

                        return ResponseEntity.ok(
                                        "If the email is registered, a password reset token has been generated.");
                }

                // Generate secure random token
                SecureRandom secureRandom = new SecureRandom();

                byte[] randomBytes = new byte[32];

                secureRandom.nextBytes(randomBytes);

                String token = Base64.getUrlEncoder()
                                .withoutPadding()
                                .encodeToString(randomBytes);

                // Save token
                PasswordResetToken resetToken = new PasswordResetToken();

                resetToken.setUserId(user.getId());

                resetToken.setToken(token);

                // Token valid for 15 minutes
                resetToken.setExpiryTime(
                                LocalDateTime.now().plusMinutes(15));

                resetToken.setUsed(false);

                tokenRepository.save(resetToken);

                /*
                 * DEVELOPMENT ONLY
                 *
                 * Later this token should be sent through
                 * email instead of printing it.
                 */

                System.out.println("----------------------------------");
                System.out.println("PASSWORD RESET TOKEN");
                System.out.println("User Email: " + email);
                System.out.println("Token: " + token);
                System.out.println("Valid for: 15 minutes");
                System.out.println("----------------------------------");

                return ResponseEntity.ok(
                                "If the email is registered, a password reset token has been generated.");
        }

        // =========================================================
        // RESET PASSWORD
        // =========================================================

        @PostMapping("/reset-password")
        public ResponseEntity<?> resetPassword(
                        @RequestBody ResetPasswordRequest request) {

                if (request.getToken() == null ||
                                request.getToken().trim().isEmpty()) {

                        return ResponseEntity.badRequest()
                                        .body("Reset token is required.");
                }

                if (request.getNewPassword() == null ||
                                request.getNewPassword().trim().isEmpty()) {

                        return ResponseEntity.badRequest()
                                        .body("New password is required.");
                }

                if (request.getNewPassword().length() < 6) {

                        return ResponseEntity.badRequest()
                                        .body("Password must contain at least 6 characters.");
                }

                PasswordResetToken resetToken = tokenRepository
                                .findByToken(request.getToken())
                                .orElse(null);

                if (resetToken == null) {

                        return ResponseEntity.badRequest()
                                        .body("Invalid reset token.");
                }

                if (resetToken.isUsed()) {

                        return ResponseEntity.badRequest()
                                        .body("This reset token has already been used.");
                }

                if (resetToken.getExpiryTime()
                                .isBefore(LocalDateTime.now())) {

                        return ResponseEntity.badRequest()
                                        .body("Reset token has expired.");
                }

                User user = userRepository
                                .findById(resetToken.getUserId())
                                .orElse(null);

                if (user == null) {

                        return ResponseEntity.badRequest()
                                        .body("User not found.");
                }

                // Hash the new password
                user.setPassword(
                                passwordEncoder.encode(
                                                request.getNewPassword()));

                userRepository.save(user);

                // Make token unusable
                resetToken.setUsed(true);

                tokenRepository.save(resetToken);

                return ResponseEntity.ok(
                                "Password reset successfully.");
        }
}