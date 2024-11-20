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
            String jobType;
            int jobId;

            while (true) {
                System.out.print("Enter job type (A or B) or 'exit' to quit: ");
                String input = in.readLine(); // we probably need to threads one to wait for user input and one to wait for response to server
                if (input.equalsIgnoreCase("exit")) {
                    break;
                }

                jobType = input.trim();
                jobId = Math.abs(new Random().nextInt());
                out.println(jobType + "," + jobId + ":client");
                System.out.println("Job submitted: " + jobType + ", " + jobId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
