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
        String role = null;
        String licenseNumber = null;

        // 1. Parse JSON body into a simple map
        try (BufferedReader reader = req.getReader()) {
            Map<String, String> data = gson.fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
            if (data != null) {
                username = data.get("username");
                password = data.get("password");
                email = data.get("email");
                role = data.get("role");
                licenseNumber = data.get("licenseNumber");
            }
        }

        // 2. Validation
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Map.of("success", false, "message", "username and password required")));
            return;
        }

        // 3. Database Operation
        User u = new User(username, password, email == null ? "" : email);

        // If role is AGENT, validate license before attempting registration so we can return 400
        if (role != null && "AGENT".equalsIgnoreCase(role)) {
            if (licenseNumber == null || licenseNumber.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(Map.of("success", false, "message", "invalid or missing licenseNumber")));
                return;
            }
            if (!JdbcAuthDAO.licenseExists(licenseNumber)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(Map.of("success", false, "message", "invalid licenseNumber")));
                return;
            }
        }

        boolean ok = JdbcAuthDAO.registerUserWithRole(u, role, licenseNumber);

        if (ok) {
            resp.setStatus(HttpServletResponse.SC_OK);
            String roleOut = (role == null || role.isEmpty()) ? "customer" : role.toLowerCase();
            out.print(gson.toJson(Map.of(
                "success", true,
                "message", "User registered",
                "userId", (u.getId() != null ? u.getId() : "N/A"),
                "role", roleOut
            )));
        } else {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            out.print(gson.toJson(Map.of("success", false, "message", "User already exists or license invalid")));
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

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Handle CORS preflight
        setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private void setCorsHeaders(HttpServletResponse resp) {
        // adjust CORS policy as appropriate for your app
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}
