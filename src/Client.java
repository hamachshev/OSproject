import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

                final String jobType = input.trim().toUpperCase();
                if (!(jobType.equals("A") || jobType.equals("B"))) {
                    continue;
                }
                final int jobId = Math.abs(new Random().nextInt());
                System.out.println("\nYou submitted: " + jobType + ", Job ID: " + jobId);
                jobExecutor.submit(() -> {

                    out.println("ID: " + jobId + "; Type: " + jobType.toUpperCase());
                    System.out.print("\nJob submitted to master: " + jobType + ", Job ID: " + jobId + "\nEnter job type (A or B): ");

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


