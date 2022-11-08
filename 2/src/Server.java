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
            System.out.println("Server running");
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

        } catch (IOException ignored) {

        }
    }

    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String username;
        private String password;
        private boolean privilege;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                while (username == null) {
                    out.println("username: "); //TODO SendMessage instead?
                    username = in.readLine();

                    if (username.equals("admin")) {
                        out.println("Enter password: ");
                        password = in.readLine();
                        if (password.equals("123")){
                            out.println("Permission Granted");
                            privilege = true;
                        } else {
                            out.println("Wrong password, try again");
                            username = null;
                        }
                    }
                }

                System.out.println(username + " connected");
                broadcast(username + " connected");
                String message;

                while (client.isConnected() && (message = in.readLine()) != null)  { // Is connected?

                    if (privilege && message.startsWith("/ban")){
                        String temp;
                        sendMessage("Enter user to ban: ");
                        temp = in.readLine();

                        for (ConnectionHandler ch : connections){
                            if (ch.username.equals(temp)){
                                ch.shutdown();
                                connections.remove(ch);
                            } else {
                                out.println("User not found");
                            }
                        }

                        broadcast(temp + " disconnected");
                        System.out.println(temp + " disconnected");

                    }

                    if (message.startsWith("/quit")) {
                        broadcast(username + " disconnected");
                        System.out.println(username + " disconnected");
                        connections.remove(this);
                        shutdown();
                        break;
                    }

                    if (message.startsWith("/list")){
                        sendMessage("Online in chat: ");
                        for (ConnectionHandler ch : connections){
                            sendMessage(ch.username);
                        }
                    } else {
                        broadcast(username + ": " + message);
                        System.out.println(username + ": " + message);
                    }
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
            } catch (IOException ignored) {

            }
        }

        public void banUser(){


        }


    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
