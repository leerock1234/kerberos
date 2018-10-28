package http;


import net.sourceforge.spnego.SpnegoHttpURLConnection;
import sun.misc.IOUtils;

import java.io.*;
import java.net.URL;

public class Test {
    public static void main(final String[] args) throws Exception {
        final String username = "client2"; // ex. dfelix
        final String password = "Clicli@1"; // ex. myp@s5
        final String url = "http://dc1.leerockso.com:8080/hello_spnego.jsp"; // ex. http://medusa:8080/hello_jsp.jsp
        final String module = "client"; // ex. spnego-client
        System.setProperty("sun.security.krb5.debug", "true");
        String path = "C:\\Users\\q1062\\IdeaProjects\\kerberos\\src\\main\\resources\\";
        System.setProperty("java.security.auth.login.config",path+"jaas-krb5-spnego.conf");
        System.setProperty("java.security.krb5.conf",path+"krb5-spnego.conf");
        SpnegoHttpURLConnection spnego = null;
        BufferedReader rr=null;
        try {
            spnego = new SpnegoHttpURLConnection(module, username, password);
            spnego.connect(new URL(url));

            System.out.println("\nHTTP_STATUS_CODE=" + spnego.getResponseCode());
            rr = new BufferedReader(new InputStreamReader(spnego.getInputStream()));

            String row;
            while((row = rr.readLine()) != null){
                System.out.println(row);
            }

        } finally {
            if (null != spnego) {
                rr.close();
                spnego.disconnect();
            }
        }
    }
}
