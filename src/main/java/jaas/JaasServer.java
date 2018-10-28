package jaas;

import com.sun.security.auth.callback.TextCallbackHandler;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

public class JaasServer {
    private static String name;
    private static final boolean verbose = false;

    public static void main(String[] args) throws Exception {
        String path = "C:\\Users\\q1062\\IdeaProjects\\kerberos\\src\\main\\resources\\";
        System.setProperty("java.security.auth.login.config",path+"jaas-krb5.conf");
        System.setProperty("java.security.krb5.conf",path+"krb5.conf");
        System.setProperty("sun.security.krb5.debug","false");

        name = "server";

        // Create action to perform
        PrivilegedExceptionAction action = new MyAction();

        loginAndAction(name, action);
    }

    static void loginAndAction(String name, PrivilegedExceptionAction action)
            throws LoginException, PrivilegedActionException {

        // Create a callback handler
        CallbackHandler callbackHandler = new TextCallbackHandler();

        LoginContext context = null;

        try {
            // Create a LoginContext with a callback handler
            context = new LoginContext(name, callbackHandler);

            // Perform authentication
            context.login();
        } catch (LoginException e) {
            System.err.println("Login failed");
            e.printStackTrace();
            System.exit(-1);
        }

        // Perform action as authenticated user
        Subject subject = context.getSubject();
        if (verbose) {
            System.out.println(subject.toString());
        } else {
            System.out.println("Authenticated principal: " +
                    subject.getPrincipals());
        }

        Subject.doAs(subject, action);

        context.logout();
    }

    // Action to perform
    static class MyAction implements PrivilegedExceptionAction {
        MyAction() {
        }

        public Object run() throws Exception {
            // Replace the following with an action to be performed
            // by authenticated user
            System.out.println("Performing secure action ...");
            return null;
        }
    }
}
