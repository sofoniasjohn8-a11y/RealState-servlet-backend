import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    // id is nullable until the database assigns a value
    private Integer id;
    private String username;
    private String password;
    private String email;
    // role of the user (e.g., customer, agent, admin). May be null until set.
    private String role;

    public User(String username, String password, String email) {
        // Do not pre-assign an id here; the database will generate it.
        this.id = null;
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = null;
    }

    public Integer getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    // Overload for callers that pass a primitive int
    public void setId(int id) {
        this.id = Integer.valueOf(id);
    }
}
