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

@WebServlet(name = "AdminChangePassword", urlPatterns = {"/api/admin/change-password"})
public class AdminChangePassword extends HttpServlet {
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
		String token = null;
		if (auth != null && auth.toLowerCase().startsWith("bearer ")) token = auth.substring(7).trim();
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

		// parse JSON body
		Type mapType = new TypeToken<Map<String, String>>() {}.getType();
		Map<String, String> body;
		try (BufferedReader reader = req.getReader()) {
			body = gson.fromJson(reader, mapType);
		}

		if (body == null) {
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			out.print(gson.toJson(Map.of("success", false, "message", "invalid body")));
			return;
		}

		// If targetUserId is provided, allow admin to change another user's password without current password.
		String targetUserIdStr = body.get("targetUserId");
		String newPassword = body.get("newPassword");

		if (targetUserIdStr != null && !targetUserIdStr.isEmpty()) {
			if (newPassword == null || newPassword.isEmpty()) {
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				out.print(gson.toJson(Map.of("success", false, "message", "newPassword required")));
				return;
			}
			int targetId;
			try { targetId = Integer.parseInt(targetUserIdStr); } catch (NumberFormatException e) {
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				out.print(gson.toJson(Map.of("success", false, "message", "invalid targetUserId")));
				return;
			}
			boolean ok = JdbcAuthDAO.changePasswordAsAdmin(callerId, targetId, newPassword);
			if (!ok) {
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				out.print(gson.toJson(Map.of("success", false, "message", "update failed")));
				return;
			}
			resp.setStatus(HttpServletResponse.SC_OK);
			out.print(gson.toJson(Map.of("success", true, "message", "password changed")));
			return;
		}

		// otherwise admin changing their own password: require currentPassword and newPassword
		String current = body.get("currentPassword");
		if (current == null || newPassword == null || current.isEmpty() || newPassword.isEmpty()) {
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			out.print(gson.toJson(Map.of("success", false, "message", "currentPassword and newPassword required")));
			return;
		}

		boolean ok = JdbcAuthDAO.changePassword(callerId, current, newPassword);
		if (!ok) {
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			out.print(gson.toJson(Map.of("success", false, "message", "invalid current password or update failed")));
			return;
		}

		resp.setStatus(HttpServletResponse.SC_OK);
		out.print(gson.toJson(Map.of("success", true, "message", "password changed")));
	}

	private void setCorsHeaders(HttpServletResponse resp) {
		resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
		resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
	}

}
