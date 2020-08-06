package rpc;

import java.util.List;
import java.util.Set;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;

import db.MySQLConnection;
import entity.Item;
import external.GitHubClient;
/**
 * Servlet implementation class SearchItem
 */
public class SearchItem extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SearchItem() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    // update response --> fix the format, put it as an input rather than return
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		// response content type is json (can also be html ...)
		/*
		response.setContentType("application/json");
		PrintWriter writer = response.getWriter();
		// get front end info from the web: http://localhost:8080/jupiter/search?username=ricksun
		// key: value pair, request.getPara returns the value
		if (request.getParameter("username") != null) {
			JSONObject obj = new JSONObject();
			String username = request.getParameter("username");
			obj.put("username", username);
			writer.print(obj);
		}
		
		JSONArray array = new JSONArray();
		array.put(new JSONObject().put("username", "abcd"));
		array.put(new JSONObject().put("username", "1234"));
		RpcHelper.writeJsonArray(response, array);
		*/
		HttpSession session = request.getSession(false);
		if (session == null) {
			response.setStatus(403);
			return;
		}
		String userId = request.getParameter("user_id");
		double lat = Double.parseDouble(request.getParameter("lat"));
		double lon = Double.parseDouble(request.getParameter("lon"));

		GitHubClient client = new GitHubClient();
		List<Item> items = client.search(lat, lon, null);
		MySQLConnection connection = new MySQLConnection();
		Set<String> favoritedItemIds = connection.getFavoriteItemIds(userId);
		connection.close();
		JSONArray array = new JSONArray();
		for (Item item : items) {
			JSONObject obj = item.toJSONObject();
			obj.put("favorite", favoritedItemIds.contains(item.getItemId()));
			array.put(obj);
		}
		RpcHelper.writeJsonArray(response, array);
	
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
