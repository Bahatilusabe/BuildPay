package services;

import models.User;
import repositories.UserRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthenticationService {
    private final UserRepository userRepository = new UserRepository();

    // Simple in-memory session store: sessionId -> username
    private final Map<String, String> sessions = new HashMap<>();

    public AuthenticationService() {}

    public User login(String username, String password) {
        if (username == null || password == null) return null;
        User user = userRepository.findByUsername(username);
        if (user == null) return null;

        // NOTE: In production, compare password hashes with a secure algorithm.
        if (password.equals(user.getPasswordHash())) {
            return user;
        }
        return null;
    }

    public String createSession(String username) {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, username);
        return sessionId;
    }

    public void logout(String sessionId) {
        sessions.remove(sessionId);
    }

    public boolean isAuthenticated(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    // Forgot password stub: generate a reset token and return it. Integrate email service to send in prod.
    public String forgotPassword(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) return null;
        // generate token (should be stored and have expiry in a real implementation)
        return "reset-" + UUID.randomUUID().toString();
    }
}
