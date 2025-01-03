import java.io.*;
import java.net.Socket;

public abstract class Slave {
    private static final String MASTER_HOST = "localhost";
    private final int masterPort;
    private final String slaveType;

    public Slave(String slaveType, int masterPort) {
        this.slaveType = slaveType;
        this.masterPort = masterPort;
    }

    public abstract Job processJob(Job job) throws InterruptedException;

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
                System.out.println("Processed job, result: " + result);
                System.out.println("Sending job: " + result + " to master");
                oos.writeObject(result);

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
