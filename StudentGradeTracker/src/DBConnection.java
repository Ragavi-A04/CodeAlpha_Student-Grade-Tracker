import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection("jdbc:sqlite:student.db");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
