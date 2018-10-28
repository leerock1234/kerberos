package jaas;

import org.ietf.jgss.*;
import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.security.*;
import java.util.Date;

public class GssServer {
    private static final int PORT = 4567;
    private static final boolean verbose = false;
    private static final int LOOP_LIMIT = 1;
    private static int loopCount = 0;

    public static void main(String[] args) throws Exception {
        String path = "C:\\Users\\q1062\\IdeaProjects\\kerberos\\src\\main\\resources\\";
        System.setProperty("java.security.auth.login.config",path+"jaas-krb5.conf");
        System.setProperty("java.security.krb5.conf",path+"krb5.conf");
        System.setProperty("sun.security.krb5.debug","false");

        PrivilegedExceptionAction action = new GssServerAction(PORT);

        Jaas.loginAndAction("server", action);
    }

    static class GssServerAction implements PrivilegedExceptionAction {
        private int localPort;

        GssServerAction(int port) {
            this.localPort = port;
        }

        public Object run() throws Exception {

            ServerSocket ss = new ServerSocket(localPort);

            // Get own Kerberos credentials for accepting connection
            GSSManager manager = GSSManager.getInstance();
            Oid krb5Mechanism = new Oid("1.2.840.113554.1.2.2");
            GSSCredential serverCreds = manager.createCredential(null,
                    GSSCredential.DEFAULT_LIFETIME,
                    krb5Mechanism,
                    GSSCredential.ACCEPT_ONLY);
            while (loopCount++ < LOOP_LIMIT) {

                System.out.println("Waiting for incoming connection...");

                Socket socket = ss.accept();
                DataInputStream inStream =
                        new DataInputStream(socket.getInputStream());

                DataOutputStream outStream =
                        new DataOutputStream(socket.getOutputStream());

                System.out.println("Got connection from client " +
                        socket.getInetAddress());

                /*
                 * Create a GSSContext to receive the incoming request
                 * from the client. Use null for the server credentials
                 * passed in. This tells the underlying mechanism
                 * to use whatever credentials it has available that
                 * can be used to accept this connection.
                 */

                GSSContext context = manager.createContext(
                        (GSSCredential) serverCreds);

                // Do the context establishment loop

                byte[] token = null;

                while (!context.isEstablished()) {

                    if (verbose) {
                        System.out.println("Reading ...");
                    }
                    token = new byte[inStream.readInt()];

                    if (verbose) {
                        System.out.println("Will read input token of size " +
                                token.length + " for processing by acceptSecContext");
                    }
                    inStream.readFully(token);

                    if (token.length == 0) {
                        if (verbose) {
                            System.out.println("skipping zero length token");
                        }
                        continue;
                    }
                    if (verbose) {
                        System.out.println("Token = " + getHexBytes(token));
                        System.out.println("acceptSecContext..");
                    }
                    token = context.acceptSecContext(token, 0, token.length);

                    // Send a token to the peer if one was generated by
                    // acceptSecContext
                    if (token != null) {
                        if (verbose) {
                            System.out.println("Will send token of size " +
                                    token.length + " from acceptSecContext.");
                        }

                        outStream.writeInt(token.length);
                        outStream.write(token);
                        outStream.flush();
                    }
                }

                System.out.println("Context Established! ");
                System.out.println("Client principal is " + context.getSrcName());
                System.out.println("Server principal is " + context.getTargName());

                /*
                 * If mutual authentication did not take place, then
                 * only the client was authenticated to the
                 * server. Otherwise, both client and server were
                 * authenticated to each other.
                 */
                if (context.getMutualAuthState())
                    System.out.println("Mutual authentication took place!");

                /*
                 * Create a MessageProp which unwrap will use to return
                 * information such as the Quality-of-Protection that was
                 * applied to the wrapped token, whether or not it was
                 * encrypted, etc. Since the initial MessageProp values
                 * are ignored, just set them to the defaults of 0 and false.
                 */
                MessageProp prop = new MessageProp(0, false);

                /*
                 * Read the token. This uses the same token byte array
                 * as that used during context establishment.
                 */
                token = new byte[inStream.readInt()];
                if (verbose) {
                    System.out.println("Will read token of size " + token.length);
                }
                inStream.readFully(token);

                byte[] input = context.unwrap(token, 0, token.length, prop);
                String str = new String(input, "UTF-8");

                System.out.println("Received data \"" +
                        str + "\" of length " + str.length());

                System.out.println("Confidentiality applied: " +
                        prop.getPrivacy());

                /*
                 * Now generate reply that is the concatenation of the
                 * incoming string with the current time.
                 */

                /*
                 * First reset the QOP of the MessageProp to 0
                 * to ensure the default Quality-of-Protection
                 * is applied.
                 */
                prop.setQOP(0);

                String now = new Date().toString();
                byte[] nowBytes = now.getBytes("UTF-8");
                int len = input.length + 1 + nowBytes.length;
                byte[] reply = new byte[len];
                System.arraycopy(input, 0, reply, 0, input.length);
                reply[input.length] = ' ';
                System.arraycopy(nowBytes, 0, reply, input.length + 1,
                        nowBytes.length);

                System.out.println("Sending: " + new String(reply, "UTF-8"));
                token = context.wrap(reply, 0, reply.length, prop);

                outStream.writeInt(token.length);
                outStream.write(token);
                outStream.flush();

                System.out.println("Closing connection with client " +
                        socket.getInetAddress());
                context.dispose();
                socket.close();
            }
            return null;
        }
    }

    private static final String getHexBytes(byte[] bytes, int pos, int len) {

        StringBuffer sb = new StringBuffer();
        for (int i = pos; i < (pos + len); i++) {

            int b1 = (bytes[i] >> 4) & 0x0f;
            int b2 = bytes[i] & 0x0f;

            sb.append(Integer.toHexString(b1));
            sb.append(Integer.toHexString(b2));
            sb.append(' ');
        }
        return sb.toString();
    }

    private static final String getHexBytes(byte[] bytes) {
        return getHexBytes(bytes, 0, bytes.length);
    }
}