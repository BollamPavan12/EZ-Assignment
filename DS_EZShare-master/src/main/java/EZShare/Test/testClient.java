package EZShare.Test;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class testClient {
    public static void main(String[] arstring) {
        Socket connection = null;
        try {
            connection = new Socket("localhost", 8080);
        } catch (IOException e) {
            e.printStackTrace();
        }
        DataInputStream in = null;
        try {
            in = new DataInputStream(connection.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(connection.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        try {
            connection.setSoTimeout(500);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        Boolean unSub = false;

        while (!unSub) {
            String read = "";
            try {
                read = in.readUTF();
            } catch (Exception e) {
            }
//                    System.out.println(read);
            try {
                if (console.ready()) {
                    try {
                        if (console.read() == '\n') {
                            String unsubMsg = "unsubscribe";
                            out.writeUTF(unsubMsg);
                            System.out.println("inside the unsub procedure, the socket will be closed");
                            unSub = true;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
