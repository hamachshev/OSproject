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
    public abstract Job processJob(Job job) throws InterruptedException;

    // Connect to the master server and listen for jobs
    public void runSlave() {
        try (Socket masterSocket = new Socket(MASTER_HOST, masterPort)) {
            System.out.println("Connected to master server as " + slaveType + " slave on port " + masterPort);

            ObjectInputStream ois = new ObjectInputStream(masterSocket.getInputStream());
            ObjectOutputStream oos = new ObjectOutputStream(masterSocket.getOutputStream());

            while (true) {

                Object object;
                try {
                     object = ois.readObject();
                } catch (EOFException e) { // no object yet
                    continue;
                }

                Job job;
                if (object instanceof Job) {
                    job = (Job) object;
                } else {
                    continue;
                }

                System.out.println("Received job:\t" + job.getName());

                Job result = processJob(job);
                oos.writeObject(result);
                System.out.println("Processed job, result: " + result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
