import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
        // Include the role column so the returned User object has its role populated
        String sql = "SELECT id, username, password, email, role FROM users WHERE username = ? LIMIT 1";
        try (Connection c = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String dbPass = rs.getString("password");
                if (!password.equals(dbPass)) return null;
                User u = new User(rs.getString("username"), dbPass, rs.getString("email"));
                u.setId(rs.getInt("id"));
                // set role if present in the result set (may be null)
                try {
                    String role = rs.getString("role");
                    u.setRole(role);
                } catch (SQLException ex) {
                    // If the column doesn't exist for some reason, ignore and continue
                }
                return u;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // change password: verifies current password and updates to new password
    public static boolean changePassword(int userId, String current, String next) {
        if (current == null || next == null) return false;
        init();
        String sel = "SELECT password FROM users WHERE id = ? LIMIT 1";
        String upd = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection c = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = c.prepareStatement(sel)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                String dbPass = rs.getString("password");
                if (!current.equals(dbPass)) return false;
            }
            try (PreparedStatement pu = c.prepareStatement(upd)) {
                pu.setString(1, next);
                pu.setInt(2, userId);
                return pu.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // Admin override: change another user's password without knowing current password
    public static boolean changePasswordAsAdmin(int adminId, int targetUserId, String newPassword) {
        if (newPassword == null) return false;
        init();
        String upd = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection c = DriverManager.getConnection(url, user, pass);
             PreparedStatement pu = c.prepareStatement(upd)) {
            pu.setString(1, newPassword);
            pu.setInt(2, targetUserId);
            return pu.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Change a password by matching the existing password value (no user id used).
     * This will update a single row that matches the old password.
     */
    public static boolean changePasswordByOld(String oldPassword, String newPassword) {
        if (oldPassword == null || newPassword == null) return false;
        init();
        String upd = "UPDATE users SET password = ? WHERE password = ? LIMIT 1";
        try (Connection c = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = c.prepareStatement(upd)) {
            ps.setString(1, newPassword);
            ps.setString(2, oldPassword);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Update the single-row license table's license_num value by matching the old license.
     * Returns true if a row was updated.
     */
    public static boolean updateGlobalLicense(String oldLicense, String newLicense) {
        if (oldLicense == null || newLicense == null) return false;
        init();
        // Update the single-row license_list table only. Column is expected to be `license_num`.
        String upd = "UPDATE license_list SET license_num = ? WHERE license_num = ?";
        try (Connection c = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = c.prepareStatement(upd)) {
            ps.setString(1, newLicense);
            ps.setString(2, oldLicense);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean licenseExists(String licenseNumber) {
        if (licenseNumber == null) return false;
        init();
        String sql1 = "SELECT 1 FROM licenses WHERE license_number = ? LIMIT 1";
        String sql2 = "SELECT 1 FROM users WHERE license_number = ? LIMIT 1";
        try (Connection c = DriverManager.getConnection(url, user, pass)) {
            try (PreparedStatement ps = c.prepareStatement(sql1)) {
                ps.setString(1, licenseNumber);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return true;
                }
            } catch (SQLException ex) {
                // licenses table might not exist; fall through to check users table
            }
            try (PreparedStatement ps2 = c.prepareStatement(sql2)) {
                ps2.setString(1, licenseNumber);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    return rs2.next();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean registerUserWithRole(User u, String role, String licenseNumber) {
        if (u == null || u.getUsername() == null) return false;
        init();
        String sql = "INSERT INTO users (username, password, email, role, license_number) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPassword());
            ps.setString(3, u.getEmail());
            ps.setString(4, role);
            ps.setString(5, licenseNumber);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) u.setId(rs.getInt(1));
            }
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) return false;
            throw new RuntimeException(e);
        }
    }

    public static boolean updateLicense(int userId, String licenseNumber) {
        if (licenseNumber == null) return false;
        init();
        String upd = "UPDATE users SET license_number = ? WHERE id = ?";
        try (Connection c = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = c.prepareStatement(upd)) {
            ps.setString(1, licenseNumber);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static User getUserById(int id) {
        init();
        String sql = "SELECT id, username, password, email, role FROM users WHERE id = ? LIMIT 1";
        try (Connection c = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                User u = new User(rs.getString("username"), rs.getString("password"), rs.getString("email"));
                u.setId(rs.getInt("id"));
                try { u.setRole(rs.getString("role")); } catch (SQLException ex) {}
                return u;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<User> getUsersByRole(String role) {
        init();
        String sql = "SELECT id, username, email, role FROM users WHERE role = ?";
        List<User> res = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User u = new User(rs.getString("username"), "", rs.getString("email"));
                    u.setId(rs.getInt("id"));
                    try { u.setRole(rs.getString("role")); } catch (SQLException ex) {}
                    res.add(u);
                }
            }
            return res;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
