

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;




@WebServlet("/")
public class PropertyServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public PropertyServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	response.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
    	response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    	response.setContentType("application/json");
        // 1. Set CORS headers so React (port 3000) can access this (port 8080)
       // response.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // 2. Mock Data (Later you will pull this from a Database)
        List<Property> list = new ArrayList<>();
        list.add(new Property(1, "Modern Villa", 500000, "Addis Ababa"));
        list.add(new Property(2, "Luxury Apartment", 250000, "Bole"));

        // 3. Convert List to JSON using Gson
        String json = new Gson().toJson(list);
        // 4. Send response
        PrintWriter out = response.getWriter();
       out.print(json);
        out.flush();
    }
    
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
   
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}
}



