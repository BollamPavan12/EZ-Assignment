package EZShare;

/**
 * This class is used for subscribing and unsubscribing functions on EZShare System.
 *
 * @author: Jiacheng Chen and Jiahuan He
 * @date: May, 2017
 */


import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.lang.RandomStringUtils;
import javax.net.ssl.SSLSocketFactory;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import static java.lang.Thread.sleep;


public class Subscribe {

    /**
     * Initiate the subscription
     *
     * @param cmd JSON command
     * @return the response
     */
    static HashMap<String, Boolean> relayFlag = new HashMap<>();

    public static void init(JSONObject cmd, Socket clientSocket, HashMap<Integer, Resource> resourceList,
                            boolean secure, Logger logr_debug, String ip,
                            int port, boolean debug, JSONArray serverList, HashMap<Socket, JSONObject> relayList)
            throws IOException, InterruptedException {
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        JSONObject response = new JSONObject();
        ArrayList<JSONObject> resTempList = new ArrayList<>();
        boolean relay = false;

        //must contain resource template
        if (cmd.containsKey("resourceTemplate")) {
            //must contain an id
            if (cmd.containsKey("id")) {
                response.put("response", "success");
                response.put("id", cmd.get("id"));
                send(out, logr_debug, response, ip, port, debug);
                resTempList.add((JSONObject) cmd.get("resourceTemplate"));
                sleep(2000);
                if (cmd.containsKey("relay")) {
                    if (cmd.get("relay").equals("true")) {
                        relay = true;
                        relayFlag.put(cmd.get("id").toString(), true);
                    }
                }
                HashMap<String, ArrayList<JSONObject>> sub = new HashMap<>();
                sub.put((String) cmd.get("id"), resTempList);
                Server.updateSubList(clientSocket, sub);
                subscribe(cmd,clientSocket, resourceList, secure, logr_debug, resTempList,
                            relay, ip, port, debug, serverList, relayList);
            } else {
                response.put("response", "error");
                response.put("errorMessage", "missing ID");
                send(out, logr_debug, response, ip, port, debug);
            }
        } else {
            response.put("response", "error");
            response.put("errorMessage", "missing resourceTemplate");
            send(out, logr_debug, response, ip, port, debug);
        }
    }


    private static void subscribe(JSONObject cmd, Socket clientSocket, HashMap<Integer, Resource> resourceList,
                                 boolean secure, Logger logr_debug, ArrayList<JSONObject> resTempList,
                                 boolean relay, String ip, int port, boolean debug, JSONArray serverList,
                                  HashMap<Socket, JSONObject> relayList) {
        boolean flag = true;

        try {
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            JSONArray sendMsg = new JSONArray();

            // create new threads for relay connection
            if (relay) {
                relayList.put(clientSocket, cmd);
                Thread startRelay = new Thread(() -> {
                    try {
                        relay(cmd, serverList, secure, logr_debug, ip, port, debug);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                startRelay.start();
            }

            clientSocket.setSoTimeout(500);
            while (flag) {
                //check if unsubscribe
                String recv = "";
                try {
                    recv = in.readUTF();
                } catch(SocketTimeoutException e) {
                }
                if (recv.length() != 0) {

                    if (recv.contains("UNSUBSCRIBE")) {
                        JSONObject clientUnsub = JSONObject.fromObject(recv);
                        logr_debug.fine("RECEIVED: " + recv);
                        if (debug) {
                            Subscribe.printDebugLog(ip, port);
                        }
                        JSONObject unsubmsg = new JSONObject();
                        unsubmsg.put("resultSize", Server.getCounter(clientSocket));
                        sendMsg.clear();
                        sendMsg.add(unsubmsg);
                        send(out, logr_debug, sendMsg, ip, port, debug);
                        if (clientUnsub.has("id")) {
                            relayFlag.put(clientUnsub.get("id").toString(), false);
                        }
                        flag = false;
                    }

                    //debug message
                    if (debug) {
                        printDebugLog(ip, port);
                    }
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void send(DataOutputStream out, Logger logr_debug, JSONArray sendMsg, String ip, int port, Boolean debug) {

        try {
            for (int i = 0; i < sendMsg.size(); i++) {
                out.writeUTF(sendMsg.getJSONObject(i).toString());
                logr_debug.fine("SENT: " + sendMsg.getJSONObject(i).toString());
                if (debug) {
                    Subscribe.printDebugLog(ip, port);
                }
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void send(DataOutputStream out, Logger logr_debug, JSONObject sendMsg, String ip, int port, Boolean debug) {

        try {
            out.writeUTF(sendMsg.toString());
            logr_debug.fine("SENT: " + sendMsg.toString());
            if (debug) {
                Subscribe.printDebugLog(ip, port);
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * check if the resource matches resource template
     * @param src resource to be check
     * @param resTempList resource templates
     * @return the resource if match or with a null tag if unmatch
     */
    public static JSONObject checkTemplate(Resource src, ArrayList<JSONObject> resTempList) {

        for (JSONObject resTemp : resTempList) {
            JSONArray cmdTagsJson = resTemp.getJSONArray("tags");
            String[] cmdTags = cmdTagsJson.toString().substring(1,
                    cmdTagsJson.toString().length() - 1).split(",");
            String cmdName = resTemp.get("name").toString();
            String cmdDescription = resTemp.get("description").toString();


            Boolean channel = true, owner = true, tags = true, uri = true;
            Boolean name = false, description = false, nameDescription = false;

            if (!resTemp.get("channel").equals("") && !resTemp.get("channel").equals(src.getChannel())) {
                channel = false;
            } else if (resTemp.get("channel").equals("") && !src.getChannel().equals("")) {
                channel = false;
            }
            if (!resTemp.get("owner").equals("") && !resTemp.get("owner").equals(src.getOwner())) {
                owner = false;
            }
            if (cmdTags.length != 0 && !cmdTags[0].equals("")) {
                ArrayList<String> srcTags = src.getTags();
                for(int j = 0; j < cmdTags.length; j++) {
                    tags = false;
                    for (int k = 0; k < srcTags.size(); k++) {
                        if (cmdTags[j].substring(1, cmdTags[j].length() - 1).equals(srcTags.get(k))) {
                            tags = true;
                            break;
                        }
                    }
                }
            }
            if (!resTemp.get("uri").equals("") && !resTemp.get("uri").equals(src.getUri())) {
                uri = false;
            }
            if ((!cmdName.equals("")) && src.getName().contains(cmdName)) {
                name = true;
            }
            if ((!cmdDescription.equals("")) && src.getDescription().contains(cmdDescription)) {
                description = true;
            }
            if (cmdName.equals("") && cmdDescription.equals("")) {
                nameDescription = true;
            }
            if (channel && owner && tags && uri && (name || description || nameDescription)) {
                src.setOwner("*");
                return src.toJSON();
            }
        }
        JSONObject nul = new JSONObject();
        nul.put("null", "true");
        return nul;
    }

    public static void relay(JSONObject cmd, JSONArray serverList, boolean secure, Logger logr_debug, String ip, int port, Boolean debug) throws InterruptedException{

        try{
            if (cmd.containsKey("relay")){
                cmd.put("relay", "false");
            }

            for (int i = 1; i < serverList.size(); i++) {
                Socket toServer;
                String host = serverList.getJSONObject(i).get("hostname").toString();
                int newPort = Integer.parseInt(serverList.getJSONObject(i).get("port").toString());

                if (secure) {
                    SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                    toServer = sslsocketfactory.createSocket(host, newPort);
                } else {
                    toServer = new Socket(host, newPort);
                }

                Thread tRelay = new Thread(() -> {
                    try {
                        relayThread(cmd, toServer, logr_debug, ip, port, debug);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                tRelay.start();

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


	public static void relayThread(JSONObject cmd, Socket toServer, Logger logr_debug, String ip, int port, Boolean debug) throws IOException {

        try {
        	String sendData = cmd.toString();
            String receiveData = "";
            String id = "";
            toServer.setSoTimeout(500);

            if (cmd.has("id")) {
                id = cmd.getString("id");
            }

            DataInputStream in = new DataInputStream(toServer.getInputStream());
            DataOutputStream out = new DataOutputStream(toServer.getOutputStream());
            out.writeUTF(sendData);
            logr_debug.fine("SEND:" + sendData);
            if (debug) {
                printDebugLog(ip, port);
            }
            out.flush();

            while (relayFlag.get(id)) {

                String read = "";
                try {
                    read = in.readUTF();
                }catch (SocketTimeoutException e){
                }

                //debug message
                if (debug) {
                    printDebugLog(ip, port);
                }

                if (read.length()!=0){
                    receiveData += read + ",";
                    logr_debug.fine("RECEIVED:" + read);
                }

                //debug message
                if (debug) {
                    printDebugLog(ip, port);
                }

            	if (!receiveData.equals("")) {
                    receiveData = "[" + receiveData.substring(0, receiveData.length() - 1) + "]";
                    JSONArray recv = (JSONArray) JSONSerializer.toJSON(receiveData);
                    JSONObject resp = recv.getJSONObject(0);
                    Resource src;
                    if (resp.has("owner") && resp.has("uri") && resp.has("channel")) {
                        src = new Resource(resp);
                        Server.notifySubsRelay(src, id);
                    }

                    receiveData = "";
                    recv.clear();
            	}

                //debug message
                if (debug) {
                    printDebugLog(ip, port);
                }
            }

            JSONObject unsub = new JSONObject();
            unsub.put("id", id);
            unsub.put("command", "UNSUBSCRIBE");
            out.writeUTF(unsub.toString());
            logr_debug.fine("SENT:" + unsub.toString());
            toServer.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (SocketTimeoutException e) {
            toServer.close();
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
		}
	}

	public static void printDebugLog (String ip, int port) throws IOException {
        FileReader file = new FileReader("./debug_" + ip + "_" + port +".log");
        BufferedReader br = new BufferedReader(file);
        String dCurrentLine;
        while ((dCurrentLine = br.readLine()) != null) {
            System.out.println(dCurrentLine);
        }
        Server.setupDebug();
        br.close();
        file.close();
    }

    public static void notifyNewServerRelay(Boolean secureFlag, JSONObject servers, HashMap<Socket, JSONObject> relayList,
                                            Logger logr_debug, String ip, int InitPort, Boolean debug) throws IOException {

        String host = servers.get("hostname").toString();
        int port = Integer.parseInt(servers.get("port").toString());
        int counter = 0;
        ArrayList<Thread> threadExchangeRelay = new ArrayList<>();
        for(Socket relaySocket : relayList.keySet()) {
            Socket newRelay;
            if (secureFlag) {
                SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                newRelay = sslsocketfactory.createSocket(host, port);
            } else {
                newRelay = new Socket(host, port);
            }
            JSONObject cmd = relayList.get(relaySocket);
            if (cmd.has("relay")) {
                cmd.put("relay", false);
            }
            threadExchangeRelay.add(new Thread(() -> {
                try {
                    relayThread(cmd, newRelay, logr_debug, ip, InitPort, debug);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
            threadExchangeRelay.get(counter).start();
            counter++;
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
