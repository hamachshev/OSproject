import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Master {
    private static final int PORT = 12345; // Master server port
    private static ExecutorService executor = Executors.newFixedThreadPool(2); // 2 slaves
    private static Socket slaveASocket;
    private static Socket slaveBSocket;

    public static void main(String[] args) {
        startServer();
    }

    public static void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Master server is running...");

            // Accept connections from slaves
            System.out.println("Waiting for Slave-A to connect...");
            slaveASocket = serverSocket.accept();
            System.out.println("Slave-A connected.");

            System.out.println("Waiting for Slave-B to connect...");
            slaveBSocket = serverSocket.accept();
            System.out.println("Slave-B connected.");

            // Accept connections from clients
            while (true) {
                System.out.println("Waiting for client connection...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected.");
                executor.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void assignJob(String jobType, String jobId) {
        try {
            Socket targetSlaveSocket;
            if (jobType.equals("A")) {
                targetSlaveSocket = slaveASocket;
            } else {
                targetSlaveSocket = slaveBSocket;
            }

            // Send job to the selected slave via its socket
            PrintWriter out = new PrintWriter(targetSlaveSocket.getOutputStream(), true);
            out.println(jobType + "," + jobId);
            System.out.println("Assigned job: " + jobId + " of type: " + jobType + " to slave " + jobType);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                String jobRequest;
                while ((jobRequest = in.readLine()) != null) {
                    String[] parts = jobRequest.split(",");
                    String jobType = parts[0];
                    String jobId = parts[1];
                    Master.assignJob(jobType, jobId); // Use static method to assign job
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
