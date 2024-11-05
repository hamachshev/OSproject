public interface Slave {
    void performJob(String jobId);
    void connectToMaster(String address, int port);
}
