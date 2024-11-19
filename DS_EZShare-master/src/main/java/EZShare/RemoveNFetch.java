package EZShare;

/**
 * This class is used for removing and fetching resources on EZShare System.
 * @author: Jiahuan He and Jiayu Wang
 * @date: April 6, 2017
 */

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import java.io.*;
import java.util.HashMap;


public class RemoveNFetch {
	/**
	 * This function is used for removing resource.
	 * @param command
	 * @param resourceList
	 * @param keys
	 * @return JSONObject
	 */
	public synchronized static JSONObject remove(JSONObject command, HashMap<Integer, Resource> resourceList, KeyList keys) {
		JSONObject response = new JSONObject();

		if (!command.containsKey("resource")) {
			response.put("response", "error");
			response.put("errorMessage", "missing resource");
		} else if (!command.getJSONObject("resource").containsKey("uri")) {
			response.put("response", "error");
			response.put("errorMessage", "invalid resource");
		} else {
			String cmdUri = command.getJSONObject("resource").getString("uri");
			String cmdOwner = command.getJSONObject("resource").getString("owner");
			String cmdChannel = command.getJSONObject("resource").getString("channel");
			for (Resource src : resourceList.values()) {
				src.getChannel().equals(cmdChannel);
				src.getOwner();
				src.getUri();
				if (cmdChannel.equals(src.getChannel()) && cmdOwner.equals(src.getOwner()) && cmdUri.equals(src.getUri())) {
					while (resourceList.values().remove(src)) ;
					keys.remove(cmdChannel, cmdUri, cmdOwner);
					response.put("response", "success");
					return response;
				}
			}
			response.put("response", "error");
			response.put("errorMessage", "invalid resource");
		}
		return response;
	}

	/**
	 * This function is used for judging whether the fetched resource existing on server.
	 * @param command
	 * @param resourceList
	 * @return JSONArray
	 */
	public synchronized static JSONArray fetch(JSONObject command, HashMap<Integer, Resource> resourceList) {
		JSONArray response = new JSONArray();
		JSONObject msg = new JSONObject();
		JSONObject newSrc;

		if (!command.containsKey("resourceTemplate")) {
			msg.put("response", "error");
			msg.put("errorMessage", "missing resourceTemplate");
			response.add(msg);
		} else if (!command.getJSONObject("resourceTemplate").containsKey("uri")) {
			msg.put("response", "error");
			msg.put("errorMessage", "invalid resourceTemplate");
			response.add(msg);
		} else {
			String cmdUri = command.getJSONObject("resourceTemplate").getString("uri");
            String cmdChannel = "";
            if (command.getJSONObject("resourceTemplate").containsKey("channel")) {
                cmdChannel = command.getJSONObject("resourceTemplate").getString("channel");
            }
			for (Resource src : resourceList.values()) {
				if (src.getChannel().equals(cmdChannel) && src.getUri().equals(cmdUri)) {
					File f = new File(cmdUri);
					if (f.exists()) {
						msg.put("response", "success");
						newSrc = src.toJSON();
						newSrc.put("resourceSize", f.length());
						response.add(msg);
						response.add(newSrc);
						return response;
					}
				}
			}
			msg.put("response", "error");
			msg.put("errorMessage", "invalid resourceTemplate");
			response.add(msg);
		}
		return response;
	}
}