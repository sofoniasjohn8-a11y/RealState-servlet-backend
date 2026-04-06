import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.Map;

@WebServlet(name = "AddPropertyServlet", urlPatterns = {"/api/admin/properties"})
public class AddPropertyServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Gson gson = new Gson();

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

        // parse and echo back for now; implement property DAO later
        Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> body;
        try (BufferedReader reader = req.getReader()) {
            body = gson.fromJson(reader, mapType);
        }
        if (body == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Map.of("success", false, "message", "invalid body")));
            return;
        }

        // TODO: call JdbcAuthDAO.addProperty(...) when property schema and DAO are defined.
        resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
        out.print(gson.toJson(Map.of("success", false, "message", "not implemented - implement property DAO")));
    }

    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}
