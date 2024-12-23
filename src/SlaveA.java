public class SlaveA extends Slave {

    public static void main(String[] args) {
        SlaveA slave = new SlaveA();
        slave.runSlave();
    }

    public SlaveA() {
        super("A", 12345);
    }

    @Override
    public Job processJob(Job job) throws InterruptedException {

            System.out.println("SlaveA processing job: " + job);
            if (job.getType() == 'A')
                Thread.sleep(2000);
            else
                Thread.sleep(10000);
            return job;
    }
}
