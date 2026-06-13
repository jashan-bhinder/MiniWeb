import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Server implements Runnable {
    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server () {

        connections = new ArrayList<>();
        done = false;

    }
    @Override
    public void run(){
        try{
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch(IOException e){
            shutdown();
        }


    }
    public void broadcast(String message) {
        for (ConnectionHandler ch : connections){
            if (ch != null){
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        try {
            done = true;
            if (!server.isclosed()) {
                server.close();
            }
            for (ConnectionHandler ch : connections) {
                ch.shutdown();
            }
        } catch (IOException e){
            //Ignore
        }
    }

    class ConnectionHandler implements Runnable{

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ConnectionHandler (Socket client) {
            this.client = client;

        }

        @Override
        public void run() {
            try{
                out = new PrtinWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Please enter a nickname: ");
                nickname = in.readline();
                System.out.println(nickname + " connected!");
                broadcast(nickname + " joined the chat!");
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/nick")){
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2){
                            broadcast(nickname + " has renamed their nickname to " + messageSplit[1]);
                            System.out.println(nickname + " has renamed their nickname to " + messageSplit[1]);
                            nickname = messageSplit[1];
                            out.println("Successfuly changed nickname to " + nickname);
                        } else {
                            out.println("No nickname provided.");
                        }
                    } else if (message.startsWidth("/quit")){
                        broadcast(nickname + " has left the chat!");
                        shutdown();
                    } else{
                        broadcast(nickname + ": " + message);
                    }
                }
            }
            catch (IOException e){
                shutdown();
            }

        }
        public void sendMessage(String message) {
            out.println(message);
        }
        public void shutdown() {
            try {
                if (!client.isClosed()) {
                    in.close();
                    out.close();
                    client.close();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
        public static void main(String[] args) {
            Server server = new Server();
            server.run();
        }
    }

}