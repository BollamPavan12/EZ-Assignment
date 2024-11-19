package EZShare;

/**
 * public and share functions
 * created by Jiacheng Chen
 */

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;


public class PublishNShare {

    /**
     * publish resource in the server
     * @param obj
     * json object contain the resource
     * @param resourceList
     * the list that stores resources
     * @param keys
     * the list that stores key of the resources
     * @param address
     * ip address of the server
     * @param port
     * port of the server
     */
    public synchronized static JSONObject publish(JSONObject obj, HashMap<Integer, Resource> resourceList,
                                                  KeyList keys, String address, int port) {
        //json object contains command respond
        //JSONArray r = new JSONArray();
        JSONObject response = new JSONObject();

        //check if json has a resource
        if (obj.containsKey("resource")) {
            //get the resources json
            JSONObject resJSON = (JSONObject) obj.get("resource");
            if (resJSON.containsKey("uri")) {
                String uri = (String) resJSON.get("uri");
                String owner = "";
                if (resJSON.containsKey("owner")) {
                    owner = (String) resJSON.get("owner");
                }

                //check if resource is valid
                if (uri.equals("") || owner.equals("*")) {
                    response.put("response", "error");
                    response.put("errorMessage", "invalid resource");
                } else if (!(uri.startsWith("http://") || uri.startsWith("ftp://")
                        || uri.startsWith("https://") || uri.startsWith("jar://"))) {
                    response.put("response", "error");
                    response.put("errorMessage", "cannot publish resource");
                } else {
                    //create a resource and add to the resource list
                    Resource res = getResource(resJSON, address, port);
                    int index = keys.put(res);
                    /*
                        index -1: resource has same channel and uri but different user
                        index >0: otherwise
                    */
                    if (index != -1) {
                        //resourceList.remove(index);
                        resourceList.put(index, res);
                        Server.notifySubs(res);
                        response.put("response", "success");
                    } else {
                        response.put("response", "error");
                        response.put("errorMessage", "invalid resource");
                    }
                }
            } else {
                response.put("response", "error");
                response.put("errorMessage", "invalid resource");
            }
        } else {
            response.put("response", "error");
            response.put("errorMessage", "missing resource");
        }
        //respond to command
        return response;
    }

    /**
     * publish resource with a file type uri in the server
     *
     * @param obj
     * json object contain the resource
     * @param resourceList
     * the list that stores resources
     * @param keys
     * the list that stores key of the resources
     * @param secret
     * secret of the server
     * @param address
     * ip address of the server
     * @param port
     * port of the server
     */
    public synchronized static JSONObject share(JSONObject obj, HashMap<Integer, Resource> resourceList,
                                                KeyList keys, String secret, String address, int port) {
        //json object contains command respond
        JSONObject response = new JSONObject();

        //check if json contains a secret
        if (obj.containsKey("secret") || obj.get("secret").equals("")) {
            //check if the secret is valid
            if (obj.get("secret").equals(secret)) {
                //check if there is a resource
                if (obj.containsKey("resource")) {
                    //get the resources json
                    JSONObject resJSON = (JSONObject) obj.get("resource");
                    if (resJSON.containsKey("uri")) {
                        String uri = (String) resJSON.get("uri");
                        File f = new File(uri);
                        String owner = "";
                        if (resJSON.containsKey("owner")) {
                            owner = (String) resJSON.get("owner");
                        }

                        //check if resource is valid
                        if (uri.equals("") || owner.equals("*")) {
                            response.put("response", "error");
                            response.put("errorMessage", "invalid resource");
                        } else if (!f.exists()) {
                            response.put("response", "error");
                            response.put("errorMessage", "missing resource and/or secret");
                        } else {
                            //create a resource and add to the resource list
                            Resource res = getResource(resJSON, address, port);
                            int index = keys.put(res);

                            /*
                            index -1: resource has same channel and uri but different user
                            index >0: otherwise
                             */
                            if (index != -1) {
                                resourceList.put(index, res);
                                Server.notifySubs(res);
                                response.put("response", "success");
                            } else {
                                response.put("response", "error");
                                response.put("errorMessage", "invalid resource");
                            }
                        }
                    } else {
                        response.put("response", "error");
                        response.put("errorMessage", "invalid resource");
                    }
                } else {
                    response.put("response", "error");
                    response.put("errorMessage", "missing resource and\\/or secret");
                }
            } else {
                response.put("response", "error");
                response.put("errorMessage", "incorrect secret");
            }
        } else {
            response.put("response", "error");
            response.put("errorMessage", "missing resource and\\/or secret");
        }

        return response;
    }

    /**
     * parse json object into a resource object
     *
     * @param obj
     * json object contain the resource
     * @param address
     * ip address of the server
     * @param port
     * port of the server
     */
    private static Resource getResource(JSONObject obj, String address, int port) {
        //get resource parameters
        String uri = (String)obj.get("uri");

        String channel = "";
        if (obj.containsKey("channel")) {
            channel = (String) obj.get("channel");
        }

        String owner = "";
        if (obj.containsKey("owner")) {
            owner = (String) obj.get("owner");
        }

        String name = "";
        if (obj.containsKey("name")) {
            name = (String) obj.get("name");
        }

        String des = "";
        if (obj.containsKey("description")) {
            des = (String) obj.get("description");
        }

        ArrayList<String> tags = new ArrayList<String>();
        if (obj.containsKey("tags")) {
            JSONArray arr = (JSONArray) obj.get("tags");
            for(int i = 0; i < arr.size(); i++) {
                tags.add((String) arr.get(i));
            }
        }

        String server = address + ":" + port;

        //new resource
        Resource res = new Resource(uri, channel, owner, name, des, tags, server);

        //add resource to resource list
        return res;
    }

}
