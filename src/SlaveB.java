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
    public Job processJob(Job job) throws InterruptedException {

        System.out.println("SlaveB processing job: " + job);
        if (job.getType() == 'B')
            Thread.sleep(2000);
        else
            Thread.sleep(10000);
        return job;
    }

}
