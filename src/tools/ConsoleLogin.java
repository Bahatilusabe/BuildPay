package tools;

import models.User;
import services.AuthenticationService;

import java.util.Scanner;

public class ConsoleLogin {
    public static void main(String[] args) {
        AuthenticationService auth = new AuthenticationService();
        Scanner sc = new Scanner(System.in);
        System.out.print("Username: ");
        String u = sc.nextLine();
        System.out.print("Password: ");
        String p = sc.nextLine();

        User user = auth.login(u, p);
        if (user != null) {
            String session = auth.createSession(user.getUsername());
            System.out.println("Login successful. session=" + session + " user=" + user.getUsername());
        } else {
            System.out.println("Invalid credentials");
        }
    }
}
