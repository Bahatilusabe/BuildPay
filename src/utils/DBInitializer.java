package utils;

import config.DatabaseConfig;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DBInitializer {

    public static boolean runSqlFile(Connection conn, String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            String[] statements = sb.toString().split(";\\s*\\n");
            try (Statement st = conn.createStatement()) {
                for (String stmt : statements) {
                    String t = stmt.trim();
                    if (t.isEmpty()) continue;
                    st.execute(t);
                }
            }
            return true;
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean initDatabase() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            boolean a = runSqlFile(conn, "database/schema.sql");
            boolean b = runSqlFile(conn, "database/seed_data.sql");
            return a && b;
        } catch (SQLException e) {
            // JDBC driver may be missing (sqlite-jdbc). Try sqlite3 CLI fallback.
            System.err.println("JDBC failed: " + e.getMessage() + " — attempting sqlite3 CLI fallback.");
            try {
                Process p1 = new ProcessBuilder("sh", "-c", "sqlite3 database/payroll.db < database/schema.sql").inheritIO().start();
                int r1 = p1.waitFor();
                Process p2 = new ProcessBuilder("sh", "-c", "sqlite3 database/payroll.db < database/seed_data.sql").inheritIO().start();
                int r2 = p2.waitFor();
                return r1 == 0 && r2 == 0;
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
                return false;
            }
        }
    }
}
