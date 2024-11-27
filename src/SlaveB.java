public class SlaveB extends Slave {

    public static void main(String[] args) {
        // Automatically start as SlaveB
        SlaveB slave = new SlaveB();
        slave.runSlave();
    }

    // Constructor, now passing the correct port number for SlaveB
    public SlaveB() {
        super("B", 12346);  // SlaveB connects to master on port 12346
    }

    @Override
    public String processJob(String job) {
        try {
            System.out.println("SlaveB processing job: " + job);
            if (job.endsWith("B"))
                Thread.sleep(2000);
            else
                Thread.sleep(10000);            return "Slave B completed job: " + job;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return "Error in processing job";
        }
    }
}
