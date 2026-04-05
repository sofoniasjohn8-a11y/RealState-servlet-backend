import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "AdminRegisterServlet", urlPatterns = {"/api/admin/register"})
public class AdminRegisterServlet extends HttpServlet {
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

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String email = req.getParameter("email");

        if ((username == null || password == null) && "application/json".equalsIgnoreCase(req.getContentType())) {
            String body = readBody(req);
            username = extractJsonValue(body, "username", username);
            password = extractJsonValue(body, "password", password);
            email = extractJsonValue(body, "email", email);
        }

        PrintWriter out = resp.getWriter();
        if (username == null || password == null) {
            out.print("{\"success\":false,\"message\":\"username and password required\"}");
            return;
        }

        Admin a = new Admin(username, password, email == null ? "" : email);
        boolean ok = JdbcAuthDAO.registerAdmin(a);
        if (!ok) {
            out.print("{\"success\":false,\"message\":\"admin username already exists\"}");
            return;
        }

        out.print("{\"success\":true,\"message\":\"admin registered\",\"adminId\":" + a.getId() + "}");
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
}
