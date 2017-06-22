package umlwikigen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Scanner;

/**
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 6/22/2017
 */
public class SharePointRest {

    /**
     * I have no clue what's happening here, I'm just copying stuff and
     * hoping for thebest
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        String urlStr = "https://larced.spstg.jsc.nasa.gov/sites/EDM/seemb/sandbox";
        String domain = ""; // May also be referred as realm
        String userName = "kkabdolh";
        //Scanner scan = new Scanner(System.in);
        System.out.print("HELLO>!");
        String password = "password";//scan.next();

        String responseText = getAuthenticatedResponse(urlStr, domain, userName, password);

        System.out.println("response: " + responseText);
    }

    private static String getAuthenticatedResponse(final String urlStr, final String domain, final String userName, final String password) throws IOException {
        System.out.print("HELLO>!");
        StringBuilder response = new StringBuilder();
        System.out.print("HELLO>!");
        Authenticator.setDefault(new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(domain + "\\" + userName, password.toCharArray());
            }
        });
        System.out.print("HELLO>!");
        URL urlRequest = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) urlRequest.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("GET");
        System.out.print("HELLO>!");
        InputStream stream = conn.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        String str = "";
        int count = 0;
        while ((str = in.readLine()) != null) {
            System.out.println("Line: " + count++);
            response.append("\n" + str);
        }
        in.close();
        return response.toString();
    }
}
