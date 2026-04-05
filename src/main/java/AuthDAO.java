import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory DAO for demo purposes. Not for production.
 */
public class AuthDAO {
    private static final Map<String, User> users = new ConcurrentHashMap<>();
    private static final Map<String, Admin> admins = new ConcurrentHashMap<>();

    // User methods
    public static boolean registerUser(User u) {
        if (u == null || u.getUsername() == null) return false;
        String key = u.getUsername().toLowerCase();
        if (users.containsKey(key)) return false;
        users.put(key, u);
        return true;
    }

    public static User loginUser(String username, String password) {
        if (username == null) return null;
        User u = users.get(username.toLowerCase());
        if (u == null) return null;
        if (u.getPassword().equals(password)) return u;
        return null;
    }

    // Admin methods
    public static boolean registerAdmin(Admin a) {
        if (a == null || a.getUsername() == null) return false;
        String key = a.getUsername().toLowerCase();
        if (admins.containsKey(key)) return false;
        admins.put(key, a);
        return true;
    }

    public static Admin loginAdmin(String username, String password) {
        if (username == null) return null;
        Admin a = admins.get(username.toLowerCase());
        if (a == null) return null;
        if (a.getPassword().equals(password)) return a;
        return null;
    }

    // For testing / debug
    public static int countUsers() { return users.size(); }
    public static int countAdmins() { return admins.size(); }
}

