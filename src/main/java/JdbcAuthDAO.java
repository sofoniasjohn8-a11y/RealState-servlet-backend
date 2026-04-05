import java.sql.*;

public class JdbcAuthDAO {
    // CHANGE 1: Verify your port. XAMPP usually uses 3306. 
    // If you haven't specifically changed it to 3307, change this to 3306.
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3307/realstate?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = ""; 

    private static final String url = System.getProperty("db.url", System.getenv().getOrDefault("DB_URL", DEFAULT_URL));
    private static final String user = System.getProperty("db.user", System.getenv().getOrDefault("DB_USER", DEFAULT_USER));
    private static final String pass = System.getProperty("db.pass", System.getenv().getOrDefault("DB_PASS", DEFAULT_PASS));

    private static volatile boolean initialized = false;

    // CHANGE 2: Add a static block to force the Driver to load.
    // This solves the "No suitable driver found" error in most Servlet containers.
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found! Ensure the jar is in WEB-INF/lib");
            e.printStackTrace();
        }
    }

    private static void init() {
        if (initialized) return;
        synchronized (JdbcAuthDAO.class) {
            if (initialized) return;
            try (Connection c = DriverManager.getConnection(url, user, pass); Statement s = c.createStatement()) {
                s.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "username VARCHAR(100) NOT NULL UNIQUE, " +
                        "password VARCHAR(255) NOT NULL, " +
                        "email VARCHAR(255) DEFAULT ''" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                s.executeUpdate("CREATE TABLE IF NOT EXISTS admins (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "username VARCHAR(100) NOT NULL UNIQUE, " +
                        "password VARCHAR(255) NOT NULL, " +
                        "email VARCHAR(255) DEFAULT ''" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            } catch (SQLException e) {
                throw new RuntimeException("Failed to initialize database schema: " + e.getMessage(), e);
            }
            initialized = true;
        }
    }

    public static boolean registerUser(User u) {
        if (u == null || u.getUsername() == null) return false;
        init();
        String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        try (Connection c = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPassword());
            ps.setString(3, u.getEmail());
            ps.executeUpdate();
            
            // CHANGE 3: Update the User object's ID so the Servlet can return it
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    u.setId(rs.getInt(1)); 
                }
            }
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) return false;
            throw new RuntimeException(e);
        }
    }
    
    // Basic login implementation that checks username and password against DB.
    // NOTE: Passwords are stored in plaintext in this demo. For production, use hashing!
    public static User loginUser(String username, String password) {
        if (username == null || password == null) return null;
        init();
        String sql = "SELECT id, username, password, email FROM users WHERE username = ? LIMIT 1";
        try (Connection c = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String dbPass = rs.getString("password");
                if (!password.equals(dbPass)) return null;
                User u = new User(rs.getString("username"), dbPass, rs.getString("email"));
                u.setId(rs.getInt("id"));
                return u;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ... rest of your methods (registerAdmin, etc.)
}