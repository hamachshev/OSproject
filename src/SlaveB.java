public class SlaveB extends Slave {

    public static void main(String[] args) {
        SlaveB slave = new SlaveB();
        slave.runSlave();
    }

    public SlaveB() {
        super("B", 12346);
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
