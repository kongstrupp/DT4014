import javax.print.attribute.standard.RequestingUserName;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;


    public Server() {
        connections = new ArrayList<>();
        done = false;
    }


    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }

        } catch (IOException e) {
            shutdown();
        }
    }

    public void broadcast(String message) {
        for (ConnectionHandler ch : connections) {
            if (ch != null) {
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        try {
            done = true;
            pool.shutdown();
            if (!server.isClosed()) {
                server.close();
            }

            for (ConnectionHandler ch : connections) {
                ch.shutdown();
            }

        } catch (IOException e) {

        }

    }

    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String username;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }


        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("username: ");
                username = in.readLine();
                System.out.println(username + " connected!");
                broadcast(username + " joined!");
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/user ")) {
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2) {
                            broadcast(username + " renamed themselves to " + messageSplit[1]);
                            System.out.println(username + " renamed themselves to " + messageSplit[1]);
                            username = messageSplit[1];
                            out.println("Changed username to" + username);
                        } else {
                            out.println("no username provided");
                        }
                    } else if (message.startsWith("/quit")) {
                        broadcast(username + " left the chat");
                        shutdown();
                    } else
                        broadcast(username + ": " + message);
                }

            } catch (IOException e) {
                shutdown();
            }

        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {

            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
