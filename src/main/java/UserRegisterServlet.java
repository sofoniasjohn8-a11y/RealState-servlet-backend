 // set the correct package

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@WebServlet("/register") // or configure in web.xml
public class UserRegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final Gson gson = new Gson();

    @Override

protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    setCorsHeaders(resp);
    resp.setContentType("application/json;charset=UTF-8");
    
    // Use a single PrintWriter and handle errors explicitly
    PrintWriter out = resp.getWriter();

    try {
        String username = null;
        String password = null;
        String email = null;

        // 1. Parse JSON
        try (BufferedReader reader = req.getReader()) {
            Map<String, String> data = gson.fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
            if (data != null) {
                username = data.get("username");
                password = data.get("password");
                email = data.get("email");
            }
        }

        // 2. Validation
        if (username == null || password == null || username.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Map.of("success", false, "message", "Missing credentials")));
            return;
        }

        // 3. Database Operation
        User u = new User(username, password, email == null ? "" : email);
        boolean ok = JdbcAuthDAO.registerUser(u);

        if (ok) {
            resp.setStatus(HttpServletResponse.SC_OK);
            // Be careful: Map.of fails if u.getId() is null!
            out.print(gson.toJson(Map.of(
                "success", true, 
                "message", "User registered", 
                "userId", (u.getId() != 0 ? u.getId() : "N/A")
            )));
        } else {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            out.print(gson.toJson(Map.of("success", false, "message", "User already exists")));
        }

    } catch (Exception e) {
        e.printStackTrace(); // Check your server console/logs!
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        out.print(gson.toJson(Map.of("success", false, "message", "Server Error: " + e.getMessage())));
    } finally {
        out.flush();
        out.close();
    }
}

    private void setCorsHeaders(HttpServletResponse resp) {
        // adjust CORS policy as appropriate for your app
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}
