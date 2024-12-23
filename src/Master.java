import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Master {
    private static final int PORT_SLAVE_A = 12345;
    private static final int PORT_SLAVE_B = 12346;
    private static final int PORT_CLIENT = 12347;
    private static final Set<Socket> slaveASockets = Collections.synchronizedSet(new HashSet<>());
    private static final Set<Socket> slaveBSockets = Collections.synchronizedSet(new HashSet<>());
    private static final Set<Socket> activeASockets = Collections.synchronizedSet(new HashSet<>());
    private static final Set<Socket> activeBSockets = Collections.synchronizedSet(new HashSet<>());
    private static final Map<Job, Socket> aJobsQueue = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<Job, Socket> bJobsQueue = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<Job, Socket> aActiveJobsQueue = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<Job, Socket> bActiveJobsQueue = Collections.synchronizedMap(new LinkedHashMap<>());

    private static final Map<Socket, ObjectOutputStream> slaveAWriters = new ConcurrentHashMap<>();
    private static final Map<Socket, ObjectOutputStream> slaveBWriters = new ConcurrentHashMap<>();
    private static final ExecutorService slaveExecutor = Executors.newCachedThreadPool();
    private static final ExecutorService clientExecutor = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        try {
            System.out.println("Master Server started. Slave A port: " + PORT_SLAVE_A +
                    ", Slave B port: " + PORT_SLAVE_B +
                    ", Client port: " + PORT_CLIENT);

            new Thread(() -> listenForSlaves('A')).start();

            new Thread(() -> listenForSlaves('B')).start();

            new Thread(Master::listenForClients).start();

            new Thread(Master::keepTime).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void keepTime() {
        // this is not perfect because the adding the jobs to the jobs queue and the setting time are not synchronized.
        // you could technically have a thread for each job added to make the time go down, but that seems wasteful.
        // so this approach will cause some of the times to be off.
        try {
            Thread.sleep(2000);
            for (Job job : aJobsQueue.keySet()) {
                job.setTime(job.getTime() - 2);
            }

            for (Job job : bJobsQueue.keySet()) {
                job.setTime(job.getTime() - 2);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void listenForSlaves(char type) {

        try (ServerSocket slaveServerSocket = new ServerSocket(type == 'A' ? PORT_SLAVE_A : PORT_SLAVE_B)) {
            while (true) {
                Socket slaveSocket = slaveServerSocket.accept();
                System.out.println("SLAVE " + type + " CONNECTED");

                if (type == 'A') {
                    slaveASockets.add(slaveSocket);
                    slaveAWriters.put(slaveSocket, new ObjectOutputStream(slaveSocket.getOutputStream()));
                } else if (type == 'B') {
                    slaveBSockets.add(slaveSocket);
                    slaveBWriters.put(slaveSocket, new ObjectOutputStream(slaveSocket.getOutputStream()));
                }

                slaveExecutor.submit(new SlaveHandler(slaveSocket, type));
            }

        } catch (IOException e) {
            System.err.println("Error accepting slave " + type + " connection: " + e.getMessage());
        }
    }

    private static void listenForClients() {
        while (true) {
            try (ServerSocket clientServerSocket = new ServerSocket(PORT_CLIENT)) {

                Socket clientSocket = clientServerSocket.accept();
                System.out.println("CLIENT CONNECTED");

                clientExecutor.submit(() -> handleClientConnection(clientSocket));

            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Error accepting client connection: " + e.getMessage());
            }
        }
    }

    private static void handleClientConnection(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            String clientRequest;
            while ((clientRequest = in.readLine()) != null) {
                System.out.println("Received job request from client:\t" + clientRequest);

                if (clientRequest.endsWith("A")) {
                    assignJobToBestSlave(clientSocket, new Job(clientRequest, 'A', 0));
                } else if (clientRequest.endsWith("B")) {
                    assignJobToBestSlave(clientSocket, new Job(clientRequest, 'B', 0));
                }
            }

        } catch (IOException e) {
            System.err.println("Error communicating with client:\t" + e.getMessage());
        }
    }

    private static void assignJobToBestSlave(Socket socket, Job job) {

        if (job.getType() == 'A') {
            if (calculateWaitTime(aActiveJobsQueue, aJobsQueue, slaveASockets) + 2 < calculateWaitTime(bActiveJobsQueue, bJobsQueue, slaveBSockets) + 10) {
                job.setTime(2);
                queueA(socket, job);
            } else {
                job.setTime(10);
                queueB(socket, job);
            }
        } else if (job.getType() == 'B') {
            if (calculateWaitTime(bActiveJobsQueue, bJobsQueue, slaveBSockets) + 2 < calculateWaitTime(aActiveJobsQueue, aJobsQueue, slaveASockets) + 10) {
                job.setTime(2);
                queueB(socket, job);
            } else {
                job.setTime(10);
                queueA(socket, job);
            }
        }

    }

    private static int calculateWaitTime(Map<Job, Socket> activeJobs, Map<Job, Socket> jobsQueue, Set<Socket> sockets) {
        ArrayList<Job> tempActiveJobQueue = new ArrayList<>(activeJobs.keySet());
        ArrayList<Job> tempJobQueue = new ArrayList<>(jobsQueue.keySet());

        int time = 0;

        // go 2 seconds into the future
        while (!tempJobQueue.isEmpty()) { // add in all the waiting jobs
            time += 2;
            for (Job job : tempActiveJobQueue) {
                if (job.getTime() <= 2) {
                    tempActiveJobQueue.remove(job);
                } else {
                    job.setTime(job.getTime() - 2);
                }
            }

            while (tempActiveJobQueue.size() < sockets.size()) {
                tempActiveJobQueue.add(tempJobQueue.remove(0));
            }
        }

        // now add in this job
        while (tempActiveJobQueue.size() > sockets.size()) {
            time += 2;
            for (Job job : tempActiveJobQueue) {
                if (job.getTime() <= 2) {
                    break;
                } else {
                    job.setTime(job.getTime() - 2);
                }
            }
        }

        return time;
    }


    private static void queueA(Socket socket, Job job) {
        if (aActiveJobsQueue.size() < slaveASockets.size()) { // aka available socket
            sendToA(job, socket);
        } else {
            aJobsQueue.put(job, socket);
        }
    }


    private static void queueB(Socket socket, Job job) {
        if (bActiveJobsQueue.size() < slaveBSockets.size()) {
            sendToB(job, socket);
        } else {
            bJobsQueue.put(job, socket);
        }
    }

    private static void sendToA(Job job, Socket socketOut) {
        if (!slaveASockets.isEmpty() || !slaveBSockets.isEmpty()) {
            Socket socket = null;
            for (Socket ASocket : slaveASockets) {
                if (!activeASockets.contains(ASocket)) {
                    socket = ASocket;
                    break;
                }
            }
            try (ObjectOutputStream oos = slaveAWriters.get(socket)) {
                if (oos != null) {
                    oos.writeObject(job);
                    activeASockets.add(socket);
                    aActiveJobsQueue.put(job, socketOut);
                    System.out.println("Assigned job to Slave " + job.getType() + ":\t" + job.getName());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("Cannot assign job: No slaves connected.");
        }
    }

    private static void sendToB(Job job, Socket socketOut) {
        if (!slaveASockets.isEmpty() || !slaveBSockets.isEmpty()) {
            Socket socket = null;
            for (Socket BSocket : slaveBSockets) {
                if (!activeASockets.contains(BSocket)) {
                    socket = BSocket;
                }
            }
            try (ObjectOutputStream oos = slaveBWriters.get(socket)) {
                if (oos != null) {
                    oos.writeObject(job);
                    activeBSockets.add(socket);
                    bActiveJobsQueue.put(job, socketOut);
                    System.out.println("Assigned job to Slave " + job.getType() + ":\t" + job.getName());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("Cannot assign job: No slaves connected.");
        }
    }

    static class SlaveHandler implements Runnable {
        private final Socket slaveSocket;
        private final char type;

        public SlaveHandler(Socket slaveSocket, char type) {
            this.slaveSocket = slaveSocket;
            this.type = type;
        }

        @Override
        public void run() {
            try (ObjectInputStream ois = new ObjectInputStream(slaveSocket.getInputStream())) {

                while (true) {
                    Object object = ois.readObject();
                    Job job;
                    if (object instanceof Job) {
                        job = (Job) object;
                    } else {
                        continue;
                    }

                    System.out.println(job);
                    Socket socket;
                    if (job.getType() == 'A') {
                        socket = aActiveJobsQueue.remove(job);
                    } else {
                        socket = bActiveJobsQueue.remove(job);
                    }

                    //send response to the client socket
                    PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                    pw.println("finished " + job);

                    //get next job if any
                    if (slaveASockets.contains(slaveSocket) && activeASockets.contains(slaveSocket)) {
                        activeASockets.remove(slaveSocket);
                        if (!aJobsQueue.isEmpty()) {
                            ObjectOutputStream oos;
                            oos = slaveAWriters.get(slaveSocket);
                            Job nextJob = aJobsQueue.entrySet().iterator().next().getKey();
                            Socket socketOut = aJobsQueue.get(nextJob);
                            aJobsQueue.remove(nextJob);

                            if (oos != null) {
                                oos.writeObject(job);
                                activeASockets.add(slaveSocket);
                                aActiveJobsQueue.put(nextJob, socketOut);
                                System.out.println("Assigned job to Slave " + job.getType() + ":\t" + job.getName());
                            }

                        }
                    } else if (slaveBSockets.contains(slaveSocket) && activeBSockets.contains(slaveSocket)) {
                        activeBSockets.remove(slaveSocket);
                        if (!bJobsQueue.isEmpty()) {
                            ObjectOutputStream oos;
                            oos = slaveBWriters.get(slaveSocket);
                            Job nextJob = bJobsQueue.entrySet().iterator().next().getKey();
                            Socket socketOut = aJobsQueue.get(nextJob);
                            bJobsQueue.remove(nextJob);

                            if (oos != null) {
                                oos.writeObject(job);
                                activeBSockets.add(slaveSocket);
                                bActiveJobsQueue.put(nextJob, socketOut);
                                System.out.println("Assigned job to Slave " + job.getType() + ":\t" + job.getName());
                            }

                        }
                    }

                }
            } catch (IOException e) {
                System.err.println("Error communicating with slave " + type + ":\t" + e.getMessage());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
