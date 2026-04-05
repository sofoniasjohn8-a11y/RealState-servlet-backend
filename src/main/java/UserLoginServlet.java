import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
// ...existing imports...

@WebServlet(name = "UserLoginServlet", urlPatterns = {"/api/user/login"})
public class UserLoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        // require only `username` (do not accept `email` as a username fallback)
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // If parameters are missing, try to read JSON body. Some clients may not set
        // Content-Type exactly, so we'll attempt to parse the body when params are absent
        // and the body looks like JSON.
        if (username == null || password == null) {
            String body = readBody(req);
            if (body != null) {
                String trimmed = body.trim();
                if (trimmed.startsWith("{")) {
                    username = extractJsonValue(body, "username", username);
                    // do NOT accept "email" as a substitute for username per request
                    password = extractJsonValue(body, "password", password);
                }
            }
        }

        PrintWriter out = resp.getWriter();
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"username and password required\"}");
            return;
        }
        // Use the JDBC-backed DAO so logins match registered users in the database.
        // (Previously used AuthDAO in-memory store which is empty when users are stored in DB.)
        User u = JdbcAuthDAO.loginUser(username, password);
        if (u == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\":false,\"message\":\"invalid credentials\"}");
            return;
        }

        // create token and return it. If user id is null (in-memory users), use 0 as id.
        int uid = (u.getId() == null) ? 0 : u.getId();
        String token = TokenUtil.createToken(uid, u.getUsername(), 24 * 60 * 60); // 1 day
        resp.setStatus(HttpServletResponse.SC_OK);
        out.print("{\"status\":200,\"success\":true,\"message\":\"login successful\",\"userId\":" + uid + ",\"username\":\"" + escape(u.getUsername()) + "\",\"token\":\"" + escape(token) + "\"}");
    }

    private static void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader rd = req.getReader();
        String line;
        while ((line = rd.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    private static String extractJsonValue(String body, String key, String fallback) {
        if (body == null) return fallback;
        String search = '"' + key + '"';
        int idx = body.indexOf(search);
        if (idx == -1) return fallback;
        int colon = body.indexOf(':', idx);
        if (colon == -1) return fallback;
        int start = body.indexOf('"', colon);
        if (start == -1) return fallback;
        int end = body.indexOf('"', start + 1);
        if (end == -1) return fallback;
        return body.substring(start + 1, end);
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
