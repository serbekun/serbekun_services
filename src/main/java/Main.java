import io.javalin.Javalin;
import io.javalin.community.ssl.SslPlugin;

public class Main {
    

    public static void main(String[] args) {

        int port = 2000;

        Javalin svr = Javalin.create(config -> {
        config.registerPlugin(new SslPlugin(ssl -> {
                // Load PEM certificates
                try {
                    ssl.pemFromPath("keys/fullchain.pem", "keys/privkey.pem");

                } catch (Exception e) {
                    System.err.println("[main] Error could not find keys files in 'keys/fullchain.pem & 'keys/privkey.pem'");
                }
                
                // Configure HTTPS only
                ssl.secure = true;
                ssl.securePort = port;
                ssl.insecure = false;
                
                // HTTP/2 support
                ssl.http2 = true;
                ssl.sniHostCheck = false;
            }));
        });

        
    }
}