import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12347;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connected to the master server.");

            // Input stream to read job type from the user
            BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));

            // Thread pool for managing multiple jobs
            ExecutorService jobExecutor = Executors.newFixedThreadPool(10);

            while (true) {
                System.out.print("Enter job type (A or B) or 'exit' to quit: ");
                String input = userIn.readLine();
                if (input.equalsIgnoreCase("exit")) {
                    break;
                }

                final String jobType = input.trim();
                final int jobId = Math.abs(new Random().nextInt());

                // Submit the job to the thread pool for concurrent execution
                jobExecutor.submit(() -> {
                    // Send job request to the master server
                    out.println("ID: " + jobId + "; Type: " + jobType.toUpperCase());
                    System.out.println("Job submitted: " + jobType + ", Job ID: " + jobId);

                    // Wait for response from the server (assigned job to slave)
                    try {
                        String response = in.readLine();
                        if (response != null) {
                            System.out.println("\nReceived response from server:\t" + response);
                        } else {
                            System.out.println("No response from server.");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                });
            }

            // Shutdown jobExecutor when done
            jobExecutor.shutdown();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


