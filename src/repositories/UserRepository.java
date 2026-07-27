package repositories;

import models.User;

public class UserRepository {
    public User findByUsername(String username) {
        // Mock: return a test user for demo
        if ("admin".equals(username)) {
            User user = new User();
            user.setUsername("admin");
            user.setPasswordHash("admin123");
            return user;
        }
        return null;
    }
}
