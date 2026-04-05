import java.io.Serializable;

public class Admin implements Serializable {
    private static final long serialVersionUID = 1L;
    private static int counter = 1;

    private int id;
    private String username;
    private String password;
    private String email;

    public Admin(String username, String password, String email) {
        this.id = counter++;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public int getId() {
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
}
