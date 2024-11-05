import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SlaveB implements Slave {
    private static String masterAddress;
    private static int masterPort;

    public static void main(String[] args) {
        SlaveB slaveB = new SlaveB();
        slaveB.connectToMaster("localhost", 12345);
    }

    @Override
    public void connectToMaster(String address, int port) {
        masterAddress = address;
        masterPort = port;

        try (Socket socket = new Socket(masterAddress, masterPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Slave-B connected to master.");

            // Listening for job assignments
            String jobRequest;
            while ((jobRequest = in.readLine()) != null) {
                String[] parts = jobRequest.split(",");
                String jobId = parts[1];
                performJob(jobId);
                // Notify master of completion
                System.out.println("Slave-B completed job: " + jobId);
                out.println("Job " + jobId + " completed by Slave-B");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void performJob(String jobId) {
        // Simulate non-optimal job processing time
        try {
            Thread.sleep(10000); // 10 seconds for non-optimal job
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
