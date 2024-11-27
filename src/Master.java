import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Master {
    private static final int PORT_SLAVE_A = 12345;  // Server port for slave group A
    private static final int PORT_SLAVE_B = 12346;  // Server port for slave group B
    private static final int PORT_CLIENT = 12347; // Server port for clients
    private static final List<Socket> slaveASockets = new CopyOnWriteArrayList<>();  // Thread-safe list for slave group A
    private static final List<Socket> slaveBSockets = new CopyOnWriteArrayList<>();  // Thread-safe list for slave group B
    private static final Map<Socket, PrintWriter> slaveAWriters = new ConcurrentHashMap<>();
    private static final Map<Socket, PrintWriter> slaveBWriters = new ConcurrentHashMap<>();
    private static final ExecutorService slaveExecutor = Executors.newCachedThreadPool(); // Unified thread pool for both slave groups
    private static final ExecutorService clientExecutor = Executors.newCachedThreadPool(); // Thread pool for clients

    public static void main(String[] args) {
        try (
            ServerSocket slaveAServerSocket = new ServerSocket(PORT_SLAVE_A);
            ServerSocket slaveBServerSocket = new ServerSocket(PORT_SLAVE_B);
            ServerSocket clientServerSocket = new ServerSocket(PORT_CLIENT)
        ) {
            System.out.println("Master Server started. Slave A port: " + PORT_SLAVE_A +
                    ", Slave B port: " + PORT_SLAVE_B +
                    ", Client port: " + PORT_CLIENT);

            // Start listening for slave group A connections
            new Thread(() -> listenForSlaves(slaveAServerSocket, "A")).start();

            // Start listening for slave group B connections
            new Thread(() -> listenForSlaves(slaveBServerSocket, "B")).start();

            // Start listening for client connections
            new Thread(() -> listenForClients(clientServerSocket)).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to listen for slave connections (both type A and type B)
    private static void listenForSlaves(ServerSocket serverSocket, String type) {
        while (true) {
            try {
                Socket slaveSocket = serverSocket.accept(); // Wait for slave to connect
                System.out.println("SLAVE " + type + " CONNECTED");

                // Add slave socket to the corresponding list and create a writer for communication
                if ("A".equals(type)) {
                    slaveASockets.add(slaveSocket);
                    slaveAWriters.put(slaveSocket, new PrintWriter(slaveSocket.getOutputStream(), true));
                } else if ("B".equals(type)) {
                    slaveBSockets.add(slaveSocket);
                    slaveBWriters.put(slaveSocket, new PrintWriter(slaveSocket.getOutputStream(), true));
                }

                // Start handling communication with the slave
                slaveExecutor.submit(new SlaveHandler(slaveSocket, type));

            } catch (IOException e) {
                System.err.println("Error accepting slave " + type + " connection: " + e.getMessage());
            }
        }
    }

    // Method to listen for client connections
    private static void listenForClients(ServerSocket serverSocket) {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept(); // Wait for client to connect
                System.out.println("CLIENT CONNECTED");

                // Handle the client connection
                clientExecutor.submit(() -> handleClientConnection(clientSocket));

            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Error accepting client connection: " + e.getMessage());
            }
        }
    }

    // Method to handle communication with the client
    private static void handleClientConnection(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            String clientRequest;
            while ((clientRequest = in.readLine()) != null) {
                System.out.println("Received job request from client:\t" + clientRequest);
                // Assign job to the best available slave
                assignJobToSlave(clientRequest);
            }

        } catch (IOException e) {
            System.err.println("Error communicating with client:\t" + e.getMessage());
        }
    }

    // Method to assign a job to an available slave (dynamic load balancing)
    private static void assignJobToSlave(String jobRequest) {
        if (!slaveASockets.isEmpty() || !slaveBSockets.isEmpty()) {
            // Dynamically select the best slave from either type A or type B
            Socket bestSlave = selectBestSlave();

            if (bestSlave != null) {
                // Determine the type of the selected slave
                String type;
                PrintWriter writer;

                if (slaveAWriters.containsKey(bestSlave)) {
                    type = "A";
                    writer = slaveAWriters.get(bestSlave);
                } else {
                    type = "B";
                    writer = slaveBWriters.get(bestSlave);
                }

                // Send the job to the selected slave and log the action
                if (writer != null) {
                    writer.println(jobRequest);  // Send the job to the slave
                    System.out.println("Assigned job to Slave " + type + ":\t" + jobRequest);
                }
            }
        } else {
            System.err.println("Cannot assign job: No slaves connected.");
        }
    }

    // Method to select the best slave (simple round-robin for now)
    private static Socket selectBestSlave() {
        // For now, return the first available slave from either group A or B
        if (!slaveASockets.isEmpty()) {
            return slaveASockets.get(0);
        } else if (!slaveBSockets.isEmpty()) {
            return slaveBSockets.get(0);
        }
        return null;
    }

    // This class handles communication with a single slave
    static class SlaveHandler implements Runnable {
        private final Socket slaveSocket;
        private final String group;

        public SlaveHandler(Socket slaveSocket, String group) {
            this.slaveSocket = slaveSocket;
            this.group = group;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(slaveSocket.getInputStream()))) {
                String jobResult;

                // Listen for results from slave
                while ((jobResult = in.readLine()) != null) {
                    // Log the result received from the slave
                    System.out.println(jobResult);
                    // You can process the result here if needed
                }
            } catch (IOException e) {
                System.err.println("Error communicating with slave " + group + ":\t" + e.getMessage());
            }
        }
    }
}
