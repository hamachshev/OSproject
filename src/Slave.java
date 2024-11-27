import java.io.*;
import java.net.Socket;  // Import the Socket class

public abstract class Slave {
    private static final String MASTER_HOST = "localhost";
    private final int masterPort;  // Port number for communication with the master
    private final String slaveType;  // Type of the slave (A or B)

    // Constructor to initialize slave with a specific port and type
    public Slave(String slaveType, int masterPort) {
        this.slaveType = slaveType;
        this.masterPort = masterPort;
    }

    // Abstract method to be implemented by subclasses to process jobs
    public abstract String processJob(String job);

    // Connect to the master server and listen for jobs
    public void runSlave() {
        try (Socket masterSocket = new Socket(MASTER_HOST, masterPort)) {
            System.out.println("Connected to master server as " + slaveType + " slave on port " + masterPort);

            // Set up input/output streams for communication with the Master
            BufferedReader in = new BufferedReader(new InputStreamReader(masterSocket.getInputStream()));
            PrintWriter out = new PrintWriter(masterSocket.getOutputStream(), true);

            // Listen for jobs from the Master
            String jobAssigned;
            while ((jobAssigned = in.readLine()) != null) {
                System.out.println("Received job:\t" + jobAssigned);
                String result = processJob(jobAssigned);

                // Send the result back to the Master
                out.println(result);
                System.out.println("Processed job, result: " + result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
