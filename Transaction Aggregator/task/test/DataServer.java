import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.hyperskill.hstest.exception.outcomes.WrongAnswer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class DataServer {
    private final HttpServer server;
    public DataServer(String id) {
        try {
            InetAddress address = InetAddress.getByName("127.0.0.1");
            server = HttpServer.create(new InetSocketAddress(address, 8889), 0);

            HttpContext pingContext = server.createContext("/ping");
            pingContext.setHandler(new PingHttpHandler(id));

            server.start();
        } catch (IOException e) {
            throw new WrongAnswer("Failed to start the local data service: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
        }
    }
}

class PingHttpHandler implements HttpHandler {
    private final String serverId;

    public PingHttpHandler(String id) {
        serverId = id;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            var body = "Pong from " + serverId;
            exchange.sendResponseHeaders(200, body.getBytes().length);

            try (OutputStream outputStream = exchange.getResponseBody()){
                outputStream.write(body.getBytes());
            }
        }
    }
}
