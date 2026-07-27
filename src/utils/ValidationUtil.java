package utils;

public class ValidationUtil {
    public static boolean isEmail(String email) {
        return email != null && email.contains("@");
    }
}
