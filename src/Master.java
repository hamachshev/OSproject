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
    private static final Set<Socket> clients = Collections.synchronizedSet(new HashSet<>());
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
        finally {

            // close sockets
            try {
                synchronized (slaveASockets) {
                    for (Socket socket : slaveASockets) {
                        socket.close();
                    }
                }
                synchronized (slaveBSockets) {
                    for (Socket socket: slaveBSockets){
                        socket.close();
                    }
                }
                synchronized (slaveAWriters) {
                    for (ObjectOutputStream objectOutputStream : slaveAWriters.values()) {
                        objectOutputStream.close();
                    }
                }
                synchronized (slaveBWriters) {
                    for (ObjectOutputStream objectOutputStream : slaveBWriters.values()) {
                        objectOutputStream.close();
                    }
                }
                synchronized (clients) {
                    for (Socket client : clients) {
                        client.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void keepTime() {
        // this is not perfect because the adding the jobs to the jobs queue and the setting time are not synchronized.
        // you could technically have a thread for each job added to make the time go down, but that seems wasteful.
        // so this approach will cause some of the times to be off.
        try {
            Thread.sleep(2000);
            synchronized (aJobsQueue) {
                for (Job job : aJobsQueue.keySet()) {
                    job.setTime(job.getTime() - 2);
                }
            }

            synchronized (bJobsQueue) {
                for (Job job : bJobsQueue.keySet()) {
                    job.setTime(job.getTime() - 2);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void listenForSlaves(char type) {

        try(ServerSocket slaveServerSocket = new ServerSocket(type == 'A' ? PORT_SLAVE_A : PORT_SLAVE_B))  {
            while (true) {
                Socket slaveSocket = slaveServerSocket.accept(); // Wait for slave to connect
                System.out.println("SLAVE " + type + " CONNECTED");

                if (type == 'A') {
                    slaveASockets.add(slaveSocket);
                    slaveAWriters.put(slaveSocket, new ObjectOutputStream(slaveSocket.getOutputStream()));
                } else if (type == 'B') {
                    slaveBSockets.add(slaveSocket);
                    slaveBWriters.put(slaveSocket, new ObjectOutputStream(slaveSocket.getOutputStream()));
                }

                // listen for finished jobs
                slaveExecutor.submit(new SlaveHandler(slaveSocket, type));
            }

        } catch (IOException e) {
            System.err.println("Error accepting slave " + type + " connection: " + e.getMessage());
        }

    }

    private static void listenForClients() {
        while (true) {
            try(ServerSocket clientServerSocket = new ServerSocket(PORT_CLIENT))  {

                Socket clientSocket = clientServerSocket.accept();
                clients.add(clientSocket);
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
                System.out.println("Received job request from Client:\t" + clientRequest);

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
            if (calculateWaitTime(aActiveJobsQueue, aJobsQueue, slaveASockets) + 2 < calculateWaitTime(bActiveJobsQueue, bJobsQueue, slaveBSockets) + 10) { // go to one with less time (wait time + job time)
                job.setTime(2);
                queue(socket, job, 'A');
            } else {
                job.setTime(10);
                queue(socket, job, 'B');
            }
        } else if (job.getType() == 'B') {
            if (calculateWaitTime(bActiveJobsQueue, bJobsQueue, slaveBSockets) + 2 < calculateWaitTime(aActiveJobsQueue, aJobsQueue, slaveASockets) + 10) {
                job.setTime(2);
                queue(socket, job, 'B');
            } else {
                job.setTime(10);
                queue(socket, job, 'A');
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
            Iterator<Job> iterator = tempActiveJobQueue.iterator(); // need to do this to do .remove() on iterator later on; cannot concurrently modify while iteration without
            while (iterator.hasNext()) {
                Job job = iterator.next();
                if (job.getTime() <= 2) {
                    iterator.remove();
                } else {
                    job.setTime(job.getTime() - 2);
                }
            }

            while (tempActiveJobQueue.size() < sockets.size()) {
                tempActiveJobQueue.add(tempJobQueue.remove(0));
            }
        }

        // now go thru all the active jobs to find open spot

        outer:
        while (!(tempActiveJobQueue.size() < sockets.size())) {
            time += 2;
            for (Job job : tempActiveJobQueue) {
                if (job.getTime() <= 2) {
                    break outer; //open spot for this one
                } else {
                    job.setTime(job.getTime() - 2);
                }
            }
        }

        return time; // total wait time
    }

    private static void queue(Socket socket, Job job, char type) {
        Map<Job, Socket> activeJobsQueue = type == 'A' ? aActiveJobsQueue : bActiveJobsQueue;
        Set<Socket> slaveSockets = type == 'A' ? slaveASockets : slaveBSockets;
        Map<Job, Socket> jobsQueue = type == 'A' ? aJobsQueue : bJobsQueue;

        if (activeJobsQueue.size() < slaveSockets.size()) { // aka available socket
            send(job, socket, type); // send now
        } else {
            jobsQueue.put(job, socket); // put on queue
        }
    }
    private static void send(Job job, Socket socketOut, char type) {
        Map<Job, Socket> activeJobsQueue = type == 'A' ? aActiveJobsQueue : bActiveJobsQueue;
        Set<Socket> slaveSockets = type == 'A' ? slaveASockets : slaveBSockets;
        Map<Job, Socket> jobsQueue = type == 'A' ? aJobsQueue : bJobsQueue;
        Set<Socket> activeSockets = type == 'A' ? activeASockets : activeBSockets;
        Map<Socket, ObjectOutputStream>  slaveWriters = type == 'A' ? slaveAWriters : slaveBWriters;

        if (!slaveSockets.isEmpty()) {
            Socket socket = null; // should be ok at this point
            synchronized (slaveSockets) {
                for (Socket ASocket : slaveSockets) {
                    if (!activeSockets.contains(ASocket)) {
                        socket = ASocket;
                        break;
                    }
                }
            }

            ObjectOutputStream oos;

            oos = slaveWriters.get(socket);
            // Send the job to the selected slave and save the socket in the active sockets
            if (oos != null) {
                try {
                    oos.writeObject(job);  // Send the job to the slave
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                activeSockets.add(socket);
                activeJobsQueue.put(job, socketOut);
                System.out.println("Assigned job to Slave " + type + ":\t" + job.getName());
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

                // Listen for results from slave
                while (true) {
                    // Log the result received from the slave
                    Object object = ois.readObject();
                    Job job;
                    if (object instanceof Job) {
                        job = (Job) object;
                    } else {
                        continue;
                    }

                    System.out.println("Completed job: " + job);

                    Socket socket;
                    if (job.getType() == 'A') {
                        socket = aActiveJobsQueue.remove(job);
                    } else {
                        socket = bActiveJobsQueue.remove(job);
                    }

                    System.out.println("Sending job: " + job + " to the client");
                    //send response to the client socket
                    PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);

                    pw.println("Completed job: " + job);

                    //get next job if any;
                    doNextJob();


                }
            } catch (IOException e) {
                System.err.println("Error communicating with slave " + type + ":\t" + e.getMessage());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        private void doNextJob() throws IOException {

            char socketType = (slaveASockets.contains(slaveSocket) && activeASockets.contains(slaveSocket)) ? 'A' : 'B';
            Set<Socket> activeSockets = socketType == 'A' ? activeASockets : activeBSockets;
            Map<Socket, ObjectOutputStream> slaveWriters = socketType == 'A' ? slaveAWriters : slaveBWriters;
            Map<Job, Socket> jobsQueue = socketType == 'A' ? aJobsQueue : bJobsQueue;
            Map<Job, Socket> activeJobsQueue = socketType == 'A' ? aActiveJobsQueue : bActiveJobsQueue;


            activeSockets.remove(slaveSocket);
            if (!jobsQueue.isEmpty()) {
                ObjectOutputStream oos;
                oos = slaveWriters.get(slaveSocket);
                Job nextJob = jobsQueue.entrySet().iterator().next().getKey();
                Socket socketOut = jobsQueue.get(nextJob);
                jobsQueue.remove(nextJob);

                if (oos != null) {
                    oos.writeObject(nextJob);  // Send the job to the slave
                    activeSockets.add(slaveSocket);
                    activeJobsQueue.put(nextJob, socketOut);
                    System.out.println("Assigned job to Slave " + nextJob.getType() + ":\t" + nextJob.getName());
                }

            }
        }
    }
}
