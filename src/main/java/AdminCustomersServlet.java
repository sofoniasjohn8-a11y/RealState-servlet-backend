import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@WebServlet(name = "AdminCustomersServlet", urlPatterns = {"/api/admin/customers"})
public class AdminCustomersServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Gson gson = new Gson();

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String auth = req.getHeader("Authorization");
        String token = (auth != null && auth.toLowerCase().startsWith("bearer ")) ? auth.substring(7).trim() : null;
        if (token == null || !TokenUtil.verifyToken(token)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(gson.toJson(Map.of("success", false, "message", "unauthorized")));
            return;
        }
        Integer callerId = TokenUtil.getUserIdFromToken(token);
        if (callerId == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(gson.toJson(Map.of("success", false, "message", "invalid token")));
            return;
        }
        User caller = JdbcAuthDAO.getUserById(callerId);
        if (caller == null || caller.getRole() == null || !caller.getRole().equalsIgnoreCase("admin")) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.print(gson.toJson(Map.of("success", false, "message", "forbidden")));
            return;
        }

        List<User> customers = JdbcAuthDAO.getUsersByRole("customer");
        resp.setStatus(HttpServletResponse.SC_OK);
        out.print(gson.toJson(Map.of("success", true, "data", customers)));
    }

    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}
