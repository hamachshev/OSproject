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

            BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));

            ExecutorService jobExecutor = Executors.newFixedThreadPool(10);

            while (true) {
                System.out.print("Enter job type (A or B): ");
                String input = userIn.readLine();

                final String jobType = input.trim();
                final int jobId = Math.abs(new Random().nextInt());

                jobExecutor.submit(() -> {

                    out.println("ID: " + jobId + "; Type: " + jobType.toUpperCase());
                    System.out.print("\nJob submitted: " + jobType + ", Job ID: " + jobId + "\nEnter job type (A or B): ");

                    try {
                        String response = in.readLine();
                        if (response != null) {
                            System.out.print("\nReceived response from server:\t" + response + "\nEnter job type (A or B): ");
                        } else {
                            System.out.println("\nNo response from server.");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


