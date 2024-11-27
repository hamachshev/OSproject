public class SlaveA extends Slave {

    public static void main(String[] args) {
        // Automatically start as SlaveA
        SlaveA slave = new SlaveA();
        slave.runSlave();
    }

    // Constructor, now passing the correct port number for SlaveA
    public SlaveA() {
        super("A", 12345);  // SlaveA connects to master on port 12345
    }

    @Override
    public String processJob(String job) {
        try {
            System.out.println("SlaveA processing job: " + job);
            if (job.endsWith("A"))
                Thread.sleep(2000);
            else
                Thread.sleep(10000);
            return "Slave A completed job: " + job;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return "Error in processing job";
        }
    }
}
