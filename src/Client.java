import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to the master server.");


            while (true) {
                System.out.print("Enter job type (A or B) or 'exit' to quit: ");
                String input = in.readLine();
                if (input.equalsIgnoreCase("exit")) {
                    break;
                }

                final String jobType = input.trim();
                final int jobId = Math.abs(new Random().nextInt());
                Thread t1 = new Thread(() -> {
                    out.println(jobType + "," + jobId + ":client"); // printwriter is thread safe see https://arc.net/l/quote/acsdmdvy
                    try {
                        System.out.println("Recived response! " + in.readLine()); // buffered reader is thread safe https://arc.net/l/quote/rdujavkp
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                System.out.println("Job submitted: " + jobType + ", " + jobId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
