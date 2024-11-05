import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to the master server.");
            String jobType;
            String jobId;

            while (true) {
                System.out.print("Enter job type (A or B) and job ID (comma-separated, or 'exit' to quit): ");
                String input = in.readLine();
                if (input.equalsIgnoreCase("exit")) {
                    break;
                }

                String[] parts = input.split(",");
                jobType = parts[0].trim();
                jobId = parts[1].trim();
                out.println(jobType + "," + jobId);
                System.out.println("Job submitted: " + jobType + ", " + jobId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
