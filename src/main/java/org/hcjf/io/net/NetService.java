package org.hcjf.io.net;

import org.hcjf.errors.HCJFRuntimeException;
import org.hcjf.log.Log;
import org.hcjf.properties.IOSystemProperties;
import org.hcjf.properties.SystemProperties;
import org.hcjf.service.Service;
import org.hcjf.service.ServiceThread;
import org.hcjf.utils.LruMap;
import org.nd4j.shade.protobuf.ServiceException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class implements a service that provide an
 * up-level interface to open tcp and udp connections like a
 * server side or client side.
 *
 * @author javaito
 */
public final class NetService extends Service<NetServiceConsumer> {

    private static final NetService instance;

    static {
        instance = new NetService(SystemProperties.get(IOSystemProperties.Net.SERVICE_NAME));
    }

    private Map<NetServiceConsumer, ServerSocketChannel> serverSocketChannelMap;
    private Map<NetSession, SelectableChannel> channels;
    private Map<SelectableChannel, NetSession> sessionsByChannel;
    private DatagramChannel udpServer;
    private Map<NetSession, SocketAddress> addresses;
    private Map<SocketAddress, NetSession> sessionsByAddress;
    private Map<SelectableChannel, Long> lastWrite;
    private Map<SelectableChannel, Queue<NetPackage>> outputQueue;
    private Map<NetServiceConsumer,SelectorRunnable> selectors;
    private Map<NetServiceConsumer,Future> tasks;
    private SelectorHealthChecker selectorHealthChecker;
    private Timer timer;
    private boolean creationTimeoutAvailable;
    private long creationTimeout;
    private boolean shuttingDown;

    private NetService(String serviceName) {
        super(serviceName, 2);
    }

    /**
     * Return the unique instance of the service.
     *
     * @return Instance of the service.
     */
    public static NetService getInstance() {
        return instance;
    }

    /**
     * This method will be called immediately after
     * of the execution of the service's constructor method
     */
    @Override
    protected void init() {
        this.timer = new Timer();
        selectors = new HashMap<>();
        tasks = new HashMap<>();

        this.creationTimeoutAvailable = SystemProperties.getBoolean(IOSystemProperties.Net.CONNECTION_TIMEOUT_AVAILABLE);
        this.creationTimeout = SystemProperties.getLong(IOSystemProperties.Net.CONNECTION_TIMEOUT);
        if (creationTimeoutAvailable && creationTimeout <= 0) {
            throw new IllegalArgumentException("Illegal creation timeout value: " + creationTimeout);
        }

        lastWrite = Collections.synchronizedMap(new HashMap<>());
        outputQueue = Collections.synchronizedMap(new HashMap<>());
        serverSocketChannelMap = Collections.synchronizedMap(new HashMap<>());
        channels = Collections.synchronizedMap(new TreeMap<>());
        sessionsByChannel = Collections.synchronizedMap(new HashMap<>());
        sessionsByAddress = Collections.synchronizedMap(new LruMap(SystemProperties.getInteger(IOSystemProperties.Net.IO_UDP_LRU_SESSIONS_SIZE)));
        addresses = Collections.synchronizedMap(new LruMap<>(SystemProperties.getInteger(IOSystemProperties.Net.IO_UDP_LRU_ADDRESSES_SIZE)));
        selectorHealthChecker = new SelectorHealthChecker();
        fork(selectorHealthChecker);
    }

    /**
     * This method will be called immediately after the static
     * method 'shutdown' of the class has been called.
     *
     * @param stage Shutdown stage.
     */
    @Override
    protected void shutdown(ShutdownStage stage) {
        shuttingDown = true;
        for(SelectorRunnable selectorRunnable : selectors.values()) {
            selectorRunnable.shutdown(stage);
        }
    }

    /**
     * This method register the consumer in the service.
     *
     * @param consumer Consumer.
     * @throws NullPointerException     If the consumer is null.
     * @throws IllegalArgumentException If the consumer is not a NetClient instance
     *                                  of a NetServer instance.
     * @throws RuntimeException         With a IOException like a cause.
     */
    @Override
    public final void registerConsumer(NetServiceConsumer consumer) {
        if (consumer == null) {
            throw new NullPointerException("Net consumer null");
        }

        boolean illegal = false;
        try {
            switch (consumer.getProtocol()) {
                case TCP:
                case TCP_SSL: {
                    if (consumer instanceof NetServer) {
                        registerTCPNetServer((NetServer) consumer);
                    } else if (consumer instanceof NetClient) {
                        registerTCPNetClient((NetClient) consumer);
                    } else {
                        illegal = true;
                    }
                    break;
                }
                case UDP: {
                    if (consumer instanceof NetServer) {
                        registerUDPNetServer((NetServer) consumer);
                    } else if (consumer instanceof NetClient) {
                        registerUDPNetClient((NetClient) consumer);
                    } else {
                        illegal = true;
                    }
                    break;
                }
            }
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }

        if (illegal) {
            throw new IllegalArgumentException("Is not a legal consumer.");
        }

        consumer.setService(this);
    }

    @Override
    public void unregisterConsumer(NetServiceConsumer consumer) {

    }

    /**
     * This method registers a TCP server service.
     * @param server TCP Server.
     */
    private void registerTCPNetServer(NetServer server) throws IOException {
        ServerSocketChannel tcpServer = ServerSocketChannel.open();
        tcpServer.configureBlocking(false);
        InetSocketAddress tcpAddress = new InetSocketAddress(server.getPort());
        tcpServer.socket().bind(tcpAddress);
        registerChannel(server, tcpServer, SelectionKey.OP_ACCEPT, server);
        serverSocketChannelMap.put(server, tcpServer);
    }

    /**
     * This method registers a TCP client service.
     * @param client TCP Client.
     */
    private void registerTCPNetClient(NetClient client) throws IOException {
        final SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(new InetSocketAddress(client.getHost(), client.getPort()));
        registerChannel(client, channel, SelectionKey.OP_CONNECT | SelectionKey.OP_READ, client);
    }

    /**
     * This method registers a UDP server service.
     * @param server UDP Server.
     */
    private void registerUDPNetServer(NetServer server) throws IOException {
        udpServer = DatagramChannel.open();
        udpServer.configureBlocking(false);
        InetSocketAddress udpAddress = new InetSocketAddress(server.getPort());
        udpServer.socket().bind(udpAddress);
        registerChannel(server, udpServer, SelectionKey.OP_READ, server);
    }

    /**
     * This method registers a UDP client service.
     * @param client UDP Client.
     */
    private void registerUDPNetClient(NetClient client) throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        InetSocketAddress address = new InetSocketAddress(client.getHost(), client.getPort());
        channel.connect(address);
        addresses.put(client.getSession(), address);
        sessionsByAddress.put(channel.getRemoteAddress(), client.getSession());
        registerChannel(client, channel, SelectionKey.OP_READ, client);
        selectors.get(client).addSession(client.getSession());
    }

    /**
     * Return a value to indicate if the session creation timeout is available ot not.
     * @return True if it is available.
     */
    private boolean isCreationTimeoutAvailable() {
        return creationTimeoutAvailable;
    }

    /**
     * Return the value in milliseconds that the server wait before destroy the channel if
     * it has not session assigned.
     * @return Session creation timeout.
     */
    private long getCreationTimeout() {
        return creationTimeout;
    }

    /**
     * Return the server timer
     *
     * @return server timer.
     */
    private Timer getTimer() {
        return timer;
    }

    /**
     * Return a boolean to knows if the instance of the java vm is into the
     * shutdown process or not.
     * @return True if the vm is into the shutdown porcess and false in the otherwise.
     */
    public final boolean isShuttingDown() {
        return shuttingDown;
    }

    /**
     * Check if the specific session is active into the sercie.
     * @param session Specific session.
     * @return Return true if the session is active into the
     */
    public final boolean checkSession(NetSession session) {
        boolean result = false;

        SelectableChannel channel = channels.get(session);
        if (channel != null) {
            result = channel.isOpen();
        }

        return result;
    }

    /**
     * This method blocks the selector to add a new channel to the key system
     * @param channel   The new channel to be register
     * @param operation The first channel operation.
     * @param attach    Object to be attached into the registered key.
     * @throws ClosedChannelException
     */
    private void registerChannel(NetServiceConsumer consumer, SelectableChannel channel, int operation, Object attach) throws ClosedChannelException {
        selectors.put(consumer, new SelectorRunnable(consumer));
        tasks.put(consumer, fork(selectors.get(consumer)));
        selectors.get(consumer).registerChannel(channel, operation, attach);
    }

    /**
     * Creates a internal package of data.
     * @param channel Socket channel.
     * @param data Payload.
     * @param event Action event.
     * @return Returns the instance of net package.
     */
    private NetPackage createPackage(SelectableChannel channel, byte[] data, NetPackage.ActionEvent event) {
        NetPackage netPackage;
        String remoteHost;
        String remoteAddress;
        int remotePort;
        int localPort;
        if (channel instanceof SocketChannel) {
            remoteHost = "";
            if(SystemProperties.getBoolean(IOSystemProperties.Net.REMOTE_ADDRESS_INTO_NET_PACKAGE)) {
                remoteHost = ((SocketChannel) channel).socket().getInetAddress().getHostName();
            }
            remoteAddress = ((SocketChannel) channel).socket().getInetAddress().getHostAddress();
            remotePort = ((SocketChannel) channel).socket().getPort();
            localPort = ((SocketChannel) channel).socket().getLocalPort();
        } else if (channel instanceof DatagramChannel) {
            remoteHost = "";
            remoteAddress ="";
            remotePort = -1;
            localPort = -1;
            try {
                Field field = channel.getClass().getDeclaredField("sender");
                field.setAccessible(true);
                InetSocketAddress socketAddress = (InetSocketAddress) field.get(channel);
                if(SystemProperties.getBoolean(IOSystemProperties.Net.REMOTE_ADDRESS_INTO_NET_PACKAGE)) {
                    remoteHost = socketAddress.getAddress().getHostName();
                }
                remoteAddress = socketAddress.getAddress().getHostAddress();
                remotePort = socketAddress.getPort();
                localPort = ((DatagramChannel) channel).socket().getLocalPort();
            } catch (Exception ex){
                Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "createPackage method exception", ex);
            }
        } else {
            throw new IllegalArgumentException("Unknown channel type");
        }

        netPackage = new DefaultNetPackage(remoteHost, remoteAddress, remotePort,
                localPort, data, event);

        return netPackage;
    }

    /**
     * This method notify to all the writer and put into the output buffer some package.
     * @param channel Channel to write the package.
     * @param netPackage Package.
     */
    private void writeWakeup(SelectableChannel channel, NetPackage netPackage) {
        SelectorRunnable selectorRunnable = selectors.get(netPackage.getSession().getConsumer());
        selectorRunnable.writeWakeup(channel, netPackage);
    }

    /**
     * This method notify to all the readers.
     * @param channel Channel to write the package.
     * @param session Net session instance.
     */
    private void readWakeup(SelectableChannel channel, NetSession session) {
        SelectorRunnable selectorRunnable = selectors.get(session.getConsumer());
        selectorRunnable.readWakeup(channel);
    }

    /**
     * This method put a net package on the output queue of the session.
     *
     * @param session Net session.
     * @param data    Data to create the package.
     * @return Return the id of the created package.
     * @throws IOException Exception to the write operation.
     */
    public final NetPackage writeData(NetSession session, byte[] data) throws IOException {
        NetPackage netPackage;
        SelectableChannel channel = channels.get(session);
        if (channel != null) {
            netPackage = createPackage(channel, data, NetPackage.ActionEvent.WRITE);
            netPackage.setSession(session);
            writeWakeup(channel, netPackage);
        } else {
            throw new IOException("Unknown session");
        }

        return netPackage;
    }

    /**
     * This method force the selector wakeup in order to read information from channel.
     * @param session Session instance.
     */
    public final void readData(NetSession session) throws IOException {
        SelectableChannel channel = channels.get(session);
        if(channel != null) {
            readWakeup(channel, session);
        } else {
            throw new IOException("Unknown session");
        }
    }

    /**
     * Disconnect a specific session.
     *
     * @param session Session to disconnect.
     * @param message Disconnection message.
     */
    public final void disconnect(NetSession session, String message) {
        SelectableChannel channel = channels.get(session);
        if (channel != null) {
            synchronized (channel) {
                if (channels.containsKey(session)) {
                    NetPackage netPackage = createPackage(channel, message.getBytes(), NetPackage.ActionEvent.DISCONNECT);
                    netPackage.setSession(session);
                    writeWakeup(channel, netPackage);
                }
            }
        }
    }

    /**
     * This method must destroy the channel and remove all the
     * netPackage related.
     *
     * @param channel Channel that will destroy.
     */
    private void destroyChannel(SocketChannel channel) {
        synchronized (channel) {
            NetSession session = sessionsByChannel.remove(channel);
            lastWrite.remove(channel);
            outputQueue.remove(channel);
            List<NetSession> removedSessions = new ArrayList<>();

            try {
                if (session != null) {

                    if (session.getConsumer().getProtocol().equals(TransportLayerProtocol.TCP_SSL)) {
                        try {
                            session.getConsumer().getSslPeer().close(channel);
                        } catch (Exception ex) {
                            Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG),
                                    "Fail to trying to close ssl peer", ex);
                        }
                    }

                    channels.remove(session);

                    // Removes the session instance of the selector
                    selectors.get(session.getConsumer()).removeSession(session);

                    if (session.getConsumer() instanceof NetServer) {
                        NetServer server = (NetServer) session.getConsumer();
                        if (server.isDisconnectAndRemove()) {
                            destroySession(session);
                        }
                    }
                    removedSessions.add(session);

                    if(session.getConsumer() != null) {
                        session.getConsumer().onDisconnect(session, null);
                    }

                    if(session.getConsumer() instanceof NetClient) {
                        SelectorRunnable selectorRunnable = selectors.remove(session.getConsumer());
                        selectorRunnable.shutdown(ShutdownStage.START);
                        selectorRunnable.shutdown(ShutdownStage.END);
                    }
                }

                if (channel.isConnected()) {
                    channel.close();
                }
            } catch (Exception ex) {
                Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Destroy method exception", ex);
            }
        }
    }

    /**
     * This method updates the linking information  a channel with a particular session
     *
     * @param oldChannel Obsolete channel.
     * @param newChannel New channel.
     */
    private void updateChannel(SocketChannel oldChannel, SocketChannel newChannel) {
        NetSession session = sessionsByChannel.remove(oldChannel);

        try {
            if (oldChannel.isConnected()) {
                oldChannel.finishConnect();
                oldChannel.close();
            }
        } catch (Exception ex) {
        } finally {
            channels.put(session, newChannel);
        }

        sessionsByChannel.put(newChannel, session);
        outputQueue.put(newChannel, outputQueue.remove(oldChannel));
        lastWrite.put(newChannel, lastWrite.remove(oldChannel));
    }

    /**
     * Indicates if the session is connected or not
     *
     * @param session Session
     * @return Return true if the session is connected and false in the other case.
     */
    public final boolean isConnected(NetSession session) {
        return channels.containsKey(session);
    }

    /**
     * This method call the method to create the session implemented en the
     * instance of the consumer.
     *
     * @param consumer   Net consumer.
     * @param netPackage Net package.
     * @return Net session from the consumer.
     * @throws IllegalArgumentException If the consumer is not instance of org.hcjf.io.net.NetServer or org.hcjf.io.net.NetClient
     */
    private NetSession getSession(NetServiceConsumer consumer, NetPackage netPackage, SocketChannel socketChannel) {
        NetSession result;

        if (consumer instanceof NetServer) {
            result = ((NetServer) consumer).createSession(netPackage);
        } else if (consumer instanceof NetClient) {
            result = ((NetClient) consumer).getSession();
        } else {
            throw new IllegalArgumentException("The service consumer must be instance of org.hcjf.io.net.NetServer or org.hcjf.io.net.NetClient.");
        }

        if(SystemProperties.getBoolean(IOSystemProperties.Net.REMOTE_ADDRESS_INTO_NET_SESSION)) {
            result.setRemoteHost(socketChannel.socket().getInetAddress().getHostName());
            result.setRemotePort(socketChannel.socket().getPort());
        } else {
            result.setRemoteHost(socketChannel.socket().getInetAddress().getHostAddress());
            result.setRemotePort(socketChannel.socket().getPort());
        }

        return result;
    }

    /**
     * This method destroy the net session.
     *
     * @param session Net session.
     */
    private void destroySession(NetSession session) {
        session.getConsumer().destroySession(session);

    }

    /**
     * This runnable check all the time the state of all the selector threads in order to found if some of this
     * thread begins with a rogue state behavior.
     */
    private class SelectorHealthChecker implements Runnable {

        private final class Actions {
            private static final String RECREATE_SELECTOR = "RECREATE_SELECTOR";
            private static final String SHUTDOWN = "SHUTDOWN";
        }

        private final ThreadMXBean threadMxBean;
        private final Map<Long,AtomicInteger> dangerousBehaviorCounter;
        private final Map<Long,SelectorRunnable> selectors;

        public SelectorHealthChecker() {
            threadMxBean = ManagementFactory.getThreadMXBean();
            dangerousBehaviorCounter = new HashMap<>();
            selectors = new HashMap<>();
        }

        @Override
        public void run() {
            Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Starting selector health checker");
            Long[] threadIds;
            while(!Thread.currentThread().isInterrupted()) {
                try {
                    if (!selectors.isEmpty()) {
                        synchronized (selectors) {
                            threadIds = selectors.keySet().toArray(new Long[]{});
                        }

                        //Safe the timestamp of the lapse start.
                        Long time = System.currentTimeMillis();
                        //Safe the cpu time for each thread into the array at lapse starts
                        Map<Long, Long> threadInitialCPU = new HashMap<>();
                        for (Long id : threadIds) {
                            threadInitialCPU.put(id, threadMxBean.getThreadCpuTime(id));
                        }

                        //Sleep the time configured as a sample time.
                        try {
                            Thread.sleep(SystemProperties.getLong(IOSystemProperties.Net.NIO_SELECTOR_HEALTH_CHECKER_SAMPLE_TIME));
                        } catch (InterruptedException e) {
                        }

                        //Safe the lapse time between the start lapse and the end
                        time = System.currentTimeMillis() - time;
                        //Safe the cpu time for each thread into the array at lapse ends
                        Map<Long, Long> threadCurrentCPU = new HashMap<>();
                        for (Long id : threadIds) {
                            threadCurrentCPU.put(id, threadMxBean.getThreadCpuTime(id));
                        }

                        //Calculate the percentage of the cpu usage for the lapse for each thread into the array.
                        for (Long id : threadIds) {
                            try {
                                long elapsedCpu = threadCurrentCPU.get(id) - threadInitialCPU.get(id);
                                float oneCpuUsage = (elapsedCpu / (time * 1000f * 1000f)) * 100f;
                                float totalCpuUsage = oneCpuUsage / Runtime.getRuntime().availableProcessors();

                                AtomicInteger counter = dangerousBehaviorCounter.get(id);
                                SelectorRunnable selectorRunnable = selectors.get(id);
                                if (counter != null && selectorRunnable != null) {
                                    Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG),
                                            "Health check trace: Description: %s, Cpu Usage: %f%%, Total Cpu Usage: %f%%, DC: %d",
                                            selectorRunnable.getDescription(), oneCpuUsage, totalCpuUsage, counter.get());
                                    //System.out.printf("Health check trace: Description: %s, Cpu Usage: %f%%, Total Cpu Usage: %f%%, DC: %d\r\n",
                                    //       selectorRunnable.getDescription(), oneCpuUsage, totalCpuUsage, counter.get());

                                    //Verify if the cpu usage of the lapse is bigger than the dangerous threshold value.
                                    if (oneCpuUsage >
                                            SystemProperties.getDouble(IOSystemProperties.Net.NIO_SELECTOR_HEALTH_CHECKER_DANGEROUS_THRESHOLD)) {
                                        //If the cpu usage is bigger than the dangerous threshold then the counter is incremented.
                                        counter.incrementAndGet();
                                        Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG),
                                                "Dangerous threshold overcome for thread %s, value: %f%%, current dangerous counter: %d",
                                                selectorRunnable.getDescription(), oneCpuUsage, counter.get());
                                        //System.out.printf("Dangerous threshold overcome for thread %s, value: %f%%, current dangerous counter: %d\r\n",
                                        //        selectorRunnable.getDescription(), oneCpuUsage, counter.get());
                                    } else {
                                        //If the cpu usage is smaller than the dangerous threshold then the counter is reset.
                                        counter.set(0);
                                    }

                                    if (counter.get() >
                                            SystemProperties.getInteger(IOSystemProperties.Net.NIO_SELECTOR_HEALTH_CHECKER_DANGEROUS_REPEATS)) {
                                        Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG),
                                                "Rogue state detected for thread %s, %f%%, %d", selectorRunnable.getDescription(), oneCpuUsage, counter.get());
                                        //System.out.printf("Rogue state detected for thread %s, %f%%, %d\r\n", selectorRunnable.getDescription(), oneCpuUsage, counter.get());
                                        //If the dangerous counter is bigger than the dangerous repeats then the selector is rogue state.
                                        selectorRunnable.setRogueState();
                                        counter.set(0);
                                    }
                                }
                            } catch (Exception ex) {
                                //This catch is to guaranty that all the thread evaluates the health
                            }
                        }
                    }
                } catch (Exception ex) {
                }
                try {
                    Thread.sleep(SystemProperties.getLong(IOSystemProperties.Net.NIO_SELECTOR_HEALTH_CHECKER_RUNNING_TIME));
                } catch (InterruptedException e) {
                }
            }
            Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Finishing selector health checker");
        }

        /**
         * Add a new selector runnable indexed by his thread id.
         * @param threadId Thread id.
         * @param selectorRunnable Selector runnable instance.
         */
        public void addSelector(Long threadId, SelectorRunnable selectorRunnable) {
            synchronized (selectors) {
                selectors.put(threadId, selectorRunnable);
                dangerousBehaviorCounter.put(threadId, new AtomicInteger(0));
            }
        }

        /**
         * Removes the selector runnable associated to the thread id.
         * @param threadId Thread id.
         */
        public void removeSelector(Long threadId) {
            synchronized (selectors) {
                selectors.remove(threadId);
                dangerousBehaviorCounter.remove(threadId);
            }
        }
    }

    /**
     * This runnable encapsulate all the components needing to the selection process.
     */
    private class SelectorRunnable implements Runnable {

        private final NetServiceConsumer consumer;
        private Selector selector;
        private final Object monitor;
        private Boolean blocking;
        private Map<UUID, NetSession> sessions;
        private final Queue<SelectionKey> readableKeys;
        private final Queue<SelectionKey> writableKeys;
        private final ExecutorService readIoExecutor;
        private final ExecutorService writeIoExecutor;
        private Boolean rogueState;

        private SelectorRunnable(NetServiceConsumer consumer) {
            this.consumer = consumer;
            this.monitor = new Object();
            this.blocking = false;
            this.sessions = new HashMap<>();
            this.rogueState = false;
            try {
                createSelector();
            } catch (IOException ex) {
                throw new HCJFRuntimeException("Unable to create selector", ex);
            }

            readableKeys = new ArrayBlockingQueue<>(SystemProperties.getInteger(IOSystemProperties.Net.IO_QUEUE_SIZE));
            writableKeys = new ArrayBlockingQueue<>(SystemProperties.getInteger(IOSystemProperties.Net.IO_QUEUE_SIZE));

            readIoExecutor = createExecutorService(0, Integer.MAX_VALUE, SystemProperties.getLong(IOSystemProperties.Net.IO_THREAD_POOL_KEEP_ALIVE_TIME));
            writeIoExecutor = createExecutorService(0, Integer.MAX_VALUE, SystemProperties.getLong(IOSystemProperties.Net.IO_THREAD_POOL_KEEP_ALIVE_TIME));
            fork(new Reader(), SystemProperties.get(IOSystemProperties.Net.IO_THREAD_POOL_NAME), readIoExecutor);
            fork(new Writer(), SystemProperties.get(IOSystemProperties.Net.IO_THREAD_POOL_NAME), writeIoExecutor);
        }

        /**
         * Set the rogue state into the selector.
         */
        public void setRogueState() {
            rogueState = true;
        }

        /**
         * Returns the set of sessions.
         * @return Set of sessions.
         */
        private Collection<NetSession> getSessions() {
            return Collections.unmodifiableCollection(sessions.values());
        }

        /**
         * Add a session on the selector artifact.
         * @param session Net session instance.
         */
        public void addSession(NetSession session) {
            sessions.put(session.getId(), session);
        }

        /**
         * Removes a session stored into the selector
         * @param session Session to remove.
         */
        public void removeSession(NetSession session) {
            sessions.remove(session.getId());
        }

        /**
         * Returns the selector instance.
         * @return Selector instance.
         */
        private Selector getSelector() {
            return selector;
        }

        /**
         * Set the selector instance.
         * @param selector Selector instance.
         */
        private void setSelector(Selector selector) {
            this.selector = selector;
        }

        /**
         * Returns the monitor instance.
         * @return Monitor instance.
         */
        public Object getMonitor() {
            return monitor;
        }

        /**
         * Returns the blocking value.
         * @return Blocking value.
         */
        public Boolean getBlocking() {
            return blocking;
        }

        /**
         * This method blocks the selector to add a new channel to the key system
         * @param channel   The new channel to be register
         * @param operation The first channel operation.
         * @param attach    Object to be attached into the registered key.
         * @throws ClosedChannelException
         */
        private void registerChannel(SelectableChannel channel, int operation, Object attach) throws ClosedChannelException {
            synchronized (monitor) {
                channel.register(getSelector(), operation, attach);
                wakeup();
            }
        }

        private void readWakeup(SelectableChannel channel) {
            SelectionKey key = channel.keyFor(getSelector());
            synchronized (readableKeys) {
                if (key.isValid() && !writableKeys.contains(key)) {
                    if(!readableKeys.offer(key)) {
                        Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Unable to add readable key!!!!");
                    }
                }
            }
            writableKeys.notifyAll();
        }

        private void writeWakeup(SelectableChannel channel, NetPackage netPackage) {
            outputQueue.get(channel).add(netPackage);

            SelectionKey key = channel.keyFor(getSelector());
            synchronized (writableKeys) {
                if (key.isValid() && !writableKeys.contains(key)) {
                    if (!writableKeys.offer(key)) {
                        Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Unable to add writable key!!!!");
                    }
                }
                writableKeys.notifyAll();
            }
        }

        /**
         * This method will be called immediately after the static
         * method 'shutdown' of the class has been called.
         * @param stage Shutdown stage.
         */
        private void shutdown(ShutdownStage stage) {
            switch (stage) {
                case START: {
                    for (NetSession session : getSessions()) {
                        try {
                            writeData(session, session.getConsumer().getShutdownFrame(session));
                        } catch (IOException e) { }
                    }
                    wakeup();
                    break;
                }
                case END: {
                    for (NetSession session : getSessions()) {
                        disconnect(session, "");
                    }

                    tasks.remove(consumer).cancel(true);
                    wakeup();
                    break;
                }
            }
        }

        /**
         * Creates a new instance of a selector, if there are a previous instance then the previous keys are registered into
         * the new selector instance.
         * @throws IOException Throws io exception if the creation of the selector fail
         */
        private void createSelector() throws IOException {
            Selector newSelector = Selector.open();
            Selector selector = getSelector();

            if(selector != null) {
                //This loop is to register all the channels of the previous keys into the new selector.
                for (SelectionKey key : selector.keys()) {
                    try {
                        SelectableChannel selectableChannel = key.channel();
                        if(selectableChannel instanceof  ServerSocketChannel) {
                            int ops = key.interestOps();
                            if(ops == SelectionKey.OP_ACCEPT) {
                                Log.w(SystemProperties.get(IOSystemProperties.Net.LOG_TAG),
                                        "Key accept key registered: %s", key.toString());
                                Object att = key.attachment();
                                // Register the channel with the new selector
                                selectableChannel.register(newSelector, ops, att);
                            } else {
                                key.cancel();
                            }
                        } else {
                            destroyChannel((SocketChannel) selectableChannel);
                        }
                    } catch (Exception ex){
                        Log.w(SystemProperties.get(IOSystemProperties.Net.LOG_TAG),
                                "Selection key clean process fail: %s", ex, key.toString());
                    }
                }
                try {
                    selector.close();
                    Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Previous selector closed");
                } catch (Throwable ex) {
                    Log.w(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Fail to close the old selector", ex);
                }
            }

            setSelector(newSelector);
            Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "New selector created");
        }

        /**
         * This method performs a non-blocking select operation over the selector and check if the number of available
         * keys is bigger than zero. If the available keys are zero then the thread are waiting until some operation invoke
         * the wakeup method.
         * @return Returns the number of available keys into the selector.
         * @throws IOException
         */
        private int select() throws IOException {
            int result;
            synchronized (monitor) {
                blocking = true;
            }
            result = getSelector().select();
            synchronized (monitor) {
                blocking = false;
            }
            return result;
        }

        /**
         * This method wakeup the main thread in order to verify if there are some available keys into selector. All the
         * times verify if the selector is blocking into the select method, because only invoke the method wakeup of the
         * selector if it is blocking in the select method.
         */
        private void wakeup() {
            synchronized (monitor) {
                if(blocking) {
                    getSelector().wakeup();
                    blocking = false;
                }
            }
        }

        private String getDescription() {
            return consumer.getName();
        }

        /**
         * This method run continuously the selection process.
         */
        @Override
        public void run() {
            try {
                selectorHealthChecker.addSelector(Thread.currentThread().getId(), this);
                try {
                    Thread.currentThread().setName(SystemProperties.get(IOSystemProperties.Net.LOG_TAG));
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                } catch (SecurityException ex) {
                }

                int selectionSize;
                Iterator<SelectionKey> selectedKeys;
                SelectionKey key;
                while (!Thread.currentThread().isInterrupted()) {
                    //Select the next schedule key or sleep if the aren't any key to select.
                    selectionSize = select();

                    if(rogueState) {
                        String action = SystemProperties.get(IOSystemProperties.Net.NIO_SELECTOR_HEALTH_CHECKER_DANGEROUS_ACTION);
                        Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG),
                                "Executing action %s for rogue state in server %s", action, getDescription());
                        if(consumer instanceof NetServer) {
                            switch (action) {
                                case SelectorHealthChecker.Actions.SHUTDOWN: {System.exit(1); break;}
                                case SelectorHealthChecker.Actions.RECREATE_SELECTOR: {createSelector(); break;}
                            }
                        } else {
                            break;
                        }
                        rogueState = false;
                    }

                    if(selectionSize > 0) {
                        selectedKeys = getSelector().selectedKeys().iterator();
                        while (selectedKeys.hasNext()) {
                            key = selectedKeys.next();
                            selectedKeys.remove();

                            if (key.isValid()) {
                                try {
                                    final SelectableChannel keyChannel = key.channel();
                                    if (keyChannel != null && key.channel().isOpen() && key.isValid()) {
                                        final NetServiceConsumer consumer = (NetServiceConsumer) key.attachment();
                                        //If the kind of key is acceptable or connectable then
                                        //the processing do over this thread in the other case
                                        //the processing is delegated to the thread pool
                                        if (key.isAcceptable()) {
                                            accept(key.channel(), (NetServer) consumer);
                                        } else if (key.isConnectable()) {
                                            connect(key.channel(), (NetClient) consumer);
                                        } else if (key.isReadable()) {
                                            synchronized (readableKeys) {
                                                if (key.isValid() && !readableKeys.contains(key)) {
                                                    if (!readableKeys.offer(key)) {
                                                        Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Unable to add readable key!!!!");
                                                    }
                                                }
                                                readableKeys.notifyAll();
                                            }
                                        } else if (key.isWritable()) {
                                            synchronized (writableKeys) {
                                                if (key.isValid() && !writableKeys.contains(key)) {
                                                    if (!writableKeys.offer(key)) {
                                                        Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Unable to add writable key!!!!");
                                                    }
                                                }
                                                writableKeys.notifyAll();
                                            }
                                        }
                                    } else {
                                        key.cancel();
                                    }
                                } catch (CancelledKeyException ex) {
                                    Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Cancelled key");
                                } catch (Exception ex) {
                                    Log.e(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Net service main thread exception", ex);
                                }
                            }
                        }
                    }
                }

                try {
                    getSelector().close();
                } catch (IOException ex) {
                    Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Closing selector...", ex);
                }

                //Close all the servers.
                if(NetServer.class.isAssignableFrom(consumer.getClass())) {
                    ServerSocketChannel channel = serverSocketChannelMap.get(consumer);
                    try {
                        channel.close();
                    } catch (IOException ex) {
                        Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Closing channel...", ex);
                    }
                }
                selectors.remove(consumer);
            } catch (Exception ex) {
                Log.e(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Unexpected error", ex);
            }

            selectorHealthChecker.removeSelector(Thread.currentThread().getId());
            readIoExecutor.shutdownNow();
            writeIoExecutor.shutdownNow();
            Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Selector stopped");
        }

        /**
         * This class is a runnable that consume the readable keys queue and call the read method.
         */
        private class Reader implements Runnable {

            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()) {
                    SelectionKey key;
                    synchronized (readableKeys) {
                        key = readableKeys.poll();
                    }
                    if (key != null) {
                        try {
                            NetServiceConsumer consumer = (NetServiceConsumer) key.attachment();
                            SelectableChannel keyChannel = key.channel();
                            if (keyChannel != null && key.channel().isOpen()) {
                                synchronized (keyChannel) {
                                    try {
                                        if (key.isValid()) {
                                            read(keyChannel, consumer);
                                        }
                                    } catch (Exception ex) {
                                        Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Internal read exception", ex);
                                    } finally {
                                        ServiceThread.getServiceThreadInstance().setSession(null);
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Internal IO thread exception, before to read process", ex);
                        }
                    } else {
                        try {
                            if(readableKeys.isEmpty()) {
                                synchronized (readableKeys) {
                                    readableKeys.wait();
                                }
                            }
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
                Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Reader finished");
            }
        }

        /**
         * This class is a runnable that consume the writable keys queue and call the read method.
         */
        private class Writer implements Runnable {

            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()) {
                    SelectionKey key;
                    synchronized (writableKeys) {
                        key = writableKeys.poll();
                    }
                    if (key != null) {
                        try {
                            NetServiceConsumer consumer = (NetServiceConsumer) key.attachment();
                            SelectableChannel keyChannel = key.channel();
                            if (keyChannel != null && key.channel().isOpen()) {
                                synchronized (keyChannel) {
                                    try {
                                        if (key.isValid()) {
                                            write(keyChannel, consumer);
                                        }
                                    } catch (Exception ex) {
                                        Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Internal write exception", ex);
                                    } finally {
                                        ServiceThread.getServiceThreadInstance().setSession(null);
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Internal IO thread exception, before to write process", ex);
                        }
                    } else {
                        try {
                            if(writableKeys.isEmpty()) {
                                synchronized (writableKeys) {
                                    writableKeys.wait();
                                }
                            }
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
                Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Writer finished");
            }
        }
    }

    /**
     * Set the socket options into the specific socket channel.
     * @param socketChannel Socket channel instance.
     * @param consumer Consumer to obtain the options.
     * @throws IOException IO Exception.
     */
    private void setSocketOptions(SocketChannel socketChannel, NetServiceConsumer consumer) throws IOException {
        Map<SocketOption, Object> socketOptions = consumer.getSocketOptions();
        if (socketOptions != null) {
            for (SocketOption socketOption : socketOptions.keySet()) {
                socketChannel.setOption(socketOption, socketOptions.get(socketOption));
            }
        }
    }

    /**
     * This method finalize the connection process when start a client connection.
     *
     * @param keyChannel Key associated to the connection channel.
     * @param client     Net client asociated to the connectable key.
     */
    private void connect(SelectableChannel keyChannel, NetClient client) {
        if (!isShuttingDown()) {
            try {
                SocketChannel channel = (SocketChannel) keyChannel;
                channel.configureBlocking(false);
                channel.socket().setKeepAlive(true);
                channel.socket().setSoTimeout(100);
                channel.finishConnect();
                setSocketOptions(channel, client);

                NetSession session = getSession(client,
                        createPackage(channel, null, NetPackage.ActionEvent.CONNECT),
                        (SocketChannel) keyChannel);
                if(session != null) {
                    selectors.get(client).addSession(session);
                    sessionsByChannel.put(channel, session);
                    channels.put(session, channel);
                    outputQueue.put(channel, new LinkedBlockingQueue<>());
                    lastWrite.put(channel, System.currentTimeMillis());

                    if (client.getProtocol().equals(TransportLayerProtocol.TCP_SSL)) {
                        client.getSslPeer().init(channel);
                    }

                    NetPackage connectionPackage = createPackage(keyChannel, new byte[]{}, NetPackage.ActionEvent.CONNECT);
                    onAction(connectionPackage, client);
                } else {
                    Log.w(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Rejected connection, session null");
                    channel.close();
                    client.onConnectFail();
                    SelectorRunnable selectorRunnable = selectors.remove(client);
                    selectorRunnable.shutdown(ShutdownStage.START);
                    selectorRunnable.shutdown(ShutdownStage.END);
                }
            } catch (Exception ex) {
                Log.w(SystemProperties.get(IOSystemProperties.Net.LOG_TAG),
                        "Error creating new client connection, %s:%d", ex, client.getHost(), client.getPort());
                client.onConnectFail();
                SelectorRunnable selectorRunnable = selectors.remove(client);
                selectorRunnable.shutdown(ShutdownStage.START);
                selectorRunnable.shutdown(ShutdownStage.END);
            }
        }
    }

    /**
     * This internal method is called for the main thread when the selector accept
     * an acceptable key to create a new socket with a remote host.
     * This method only will create a socket but without session because the session
     * depends on the communication payload
     *
     * @param keyChannel Select's key.
     */
    private void accept(SelectableChannel keyChannel, NetServer server) {
        if (!isShuttingDown()) {
            try {
                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) keyChannel;

                SocketChannel socketChannel = serverSocketChannel.accept();
                socketChannel.configureBlocking(false);
                setSocketOptions(socketChannel, server);

                NetSession session = getSession(server,
                        createPackage(socketChannel, null, NetPackage.ActionEvent.CONNECT),
                        socketChannel);
                if(session != null) {
                    if (channels.containsKey(session)) {
                        updateChannel((SocketChannel) channels.remove(session), socketChannel);
                    } else {
                        sessionsByChannel.put(socketChannel, session);
                        outputQueue.put(socketChannel, new LinkedBlockingQueue<>());
                        lastWrite.put(socketChannel, System.currentTimeMillis());
                        channels.put(session, socketChannel);
                        selectors.get(server).addSession(session);
                    }

                    if (server.getProtocol().equals(TransportLayerProtocol.TCP_SSL)) {
                        server.getSslPeer().init(socketChannel);
                    }

                    //A new readable key is created associated to the channel.
                    socketChannel.register(selectors.get(server).getSelector(), SelectionKey.OP_READ, server);

                    if (isCreationTimeoutAvailable() && server.isCreationTimeoutAvailable()) {
                        getTimer().schedule(new ConnectionTimeout(socketChannel), getCreationTimeout());
                    }
                } else {
                    Log.w(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Rejected connection, session null");
                    socketChannel.close();
                }
            } catch (Exception ex) {
                Log.w(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Error accepting a new connection.", ex);
            }
        }
    }

    /**
     * This method is called from the main thread in order to read data
     * from a particular key.
     *
     * @param keyChannel Readable key from selector.
     */
    private void read(SelectableChannel keyChannel, NetServiceConsumer consumer) {
        if (!isShuttingDown()) {
            if (keyChannel instanceof SocketChannel) {
                SocketChannel channel = (SocketChannel) keyChannel;

                //Ger the instance of the current IO thread.
                NetIOThread ioThread = ServiceThread.getServiceThreadInstance(NetIOThread.class);

                try  {
                    int readSize;
                    int totalSize = 0;
                    ByteBuffer inputBuffer = ioThread.getInputBuffer();
                    inputBuffer.clear();
                    inputBuffer.rewind();
                    try {
                        if (consumer.getProtocol().equals(TransportLayerProtocol.TCP_SSL)) {
                            totalSize = consumer.getSslPeer().read(channel, inputBuffer);
                        } else {
                            //Put all the bytes into the buffer of the IO thread.
                            totalSize += readSize = channel.read(inputBuffer);
                            while (readSize > 0) {
                                totalSize += readSize = channel.read(inputBuffer);
                            }
                        }
                    } catch (IOException ex) {
                        destroyChannel(channel);
                    }

                    if (totalSize == -1) {
                        destroyChannel(channel);
                    } else if (totalSize > 0) {
                        Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Total size read: %d", totalSize);
                        byte[] data = new byte[inputBuffer.position()];
                        inputBuffer.rewind();
                        inputBuffer.get(data);
                        NetPackage netPackage = createPackage(channel, data, NetPackage.ActionEvent.READ);

                        NetSession session = sessionsByChannel.get(channel);
                        //Here the session is linked with the current thread
                        ServiceThread.getServiceThreadInstance().setSession(session);

                        netPackage.setSession(session);
                        onAction(netPackage, consumer);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Log.e(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Net service read exception, on TCP context", ex);
                    destroyChannel(channel);
                }
            } else if (keyChannel instanceof DatagramChannel) {
                DatagramChannel channel = (DatagramChannel) keyChannel;

                //Ger the instance of the current IO thread.
                NetIOThread ioThread = ServiceThread.getServiceThreadInstance(NetIOThread.class);

                try {
                    ByteArrayOutputStream readData = new ByteArrayOutputStream();
                    ioThread.getInputBuffer().clear();
                    ioThread.getInputBuffer().rewind();

                    InetSocketAddress address = (InetSocketAddress) channel.receive(ioThread.getInputBuffer());
                    readData.write(ioThread.getInputBuffer().array(), 0, ioThread.getInputBuffer().position());

                    if (address != null) {
                        NetPackage netPackage = createPackage(channel, readData.toByteArray(), NetPackage.ActionEvent.READ);
                        NetSession session;
                        session = sessionsByAddress.get(address);

                        if(session == null && consumer instanceof NetServer) {
                            session = ((NetServer) consumer).createSession(netPackage);
                            sessionsByAddress.put(address, session);
                        }

                        if (!addresses.containsKey(session)) {
                            addresses.put(session, address);
                        }

                        if (!channels.containsKey(session)) {
                            channels.put(session, channel);
                        }

                        //Here the session is linked with the current thread
                        ServiceThread.getServiceThreadInstance().setSession(session);

                        netPackage.setSession(session);

                        if (!outputQueue.containsKey(channel)) {
                            outputQueue.put(channel, new LinkedBlockingQueue<>());
                            lastWrite.put(channel, System.currentTimeMillis());
                        }

                        if (readData.size() > 0) {
                            onAction(netPackage, consumer);
                        }
                    }
                } catch (Exception ex) {
                    Log.e(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Net service read exception, on UDP context", ex);
                }
            }
        }
    }

    /**
     * This method take the output queue associated to the consumer and write over the
     * session channel all the packages.
     * If one of the packages is a disconnection package then the channel is closed and
     * the rest of the packages are discarded.
     *
     * @param channel  Session channel.
     * @param consumer Net service consumer.
     */
    private void write(SelectableChannel channel, NetServiceConsumer consumer) {
        NetIOThread ioThread = ServiceThread.getServiceThreadInstance(NetIOThread.class);
        try {
            Queue<NetPackage> queue = outputQueue.get(channel);

            if (queue != null) {
                lastWrite.put(channel, System.currentTimeMillis());
                boolean stop = false;

                while (!queue.isEmpty() && !stop) {
                    NetPackage netPackage = queue.poll();
                    if (netPackage == null) {
                        break;
                    }

                    NetSession session = netPackage.getSession();

                    switch (netPackage.getActionEvent()) {
                        case WRITE: {
                            try {
                                byte[] byteData = netPackage.getPayload();
                                if (consumer.getProtocol().equals(TransportLayerProtocol.TCP_SSL)) {
                                    consumer.getSslPeer().write((SocketChannel) channel, ByteBuffer.wrap(byteData));
                                } else {
                                    if(byteData != null){
                                        if (byteData.length == 0) {
                                            Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Empty write data");
                                        }
                                        int begin = 0;
                                        int length = Math.min((byteData.length - begin), ioThread.getOutputBufferSize());

                                        while (begin < byteData.length) {
                                            ioThread.getOutputBuffer().limit(length);
                                            ioThread.getOutputBuffer().put(byteData, begin, length);
                                            ioThread.getOutputBuffer().rewind();

                                            if (channel instanceof SocketChannel) {
                                                int writtenData = 0;
                                                while (writtenData < length) {
                                                    writtenData += ((SocketChannel) channel).write(ioThread.getOutputBuffer());
                                                }
                                            } else if (channel instanceof DatagramChannel) {
                                                SocketAddress address = addresses.get(netPackage.getSession());
                                                if (sessionsByAddress.get(address).equals(netPackage.getSession())) {
                                                    ((DatagramChannel) channel).send(ioThread.getOutputBuffer(), address);
                                                }
                                            }

                                            ioThread.getOutputBuffer().rewind();
                                            begin += length;
                                            length = Math.min((byteData.length - begin), ioThread.getOutputBufferSize());
                                        }
                                    }
                                }

                                if (netPackage != null) {
                                    netPackage.setPackageStatus(NetPackage.PackageStatus.OK);
                                }
                            } catch (Exception ex) {
                                netPackage.setPackageStatus(NetPackage.PackageStatus.IO_ERROR);
                                throw ex;
                            } finally {
                                onAction(netPackage, consumer);
                            }

                            try {
                                //Change the key operation to finish write loop
                                channel.keyFor(selectors.get(consumer).getSelector()).interestOps(SelectionKey.OP_READ);
                            } catch (Exception ex) {
                                Log.e(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Write error", ex);
                            }

                            break;
                        }
                        case DISCONNECT: {
                            if (channel instanceof SocketChannel) {
                                destroyChannel((SocketChannel) channel);
                            } else if (channel instanceof DatagramChannel && !channel.equals(udpServer)) {
                                outputQueue.remove(channel);
                                lastWrite.remove(channel);
                                channels.remove(netPackage.getSession());
                                if (netPackage.getSession().getConsumer() instanceof NetServer) {
                                    NetServer server = (NetServer) netPackage.getSession().getConsumer();
                                    if (server.isDisconnectAndRemove()) {
                                        destroySession(session);
                                    }
                                }
                            }
                            onAction(netPackage, consumer);
                            stop = true;
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.d(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Write global thread exception", ex);
        } finally {
            ioThread.getOutputBuffer().clear();
            ioThread.getOutputBuffer().rewind();
        }
    }

    /**
     * This method put all the action events in a queue by session and then start a
     * new thread to notify all the consumers
     *
     * @param netPackage Received data.
     * @param consumer   Consumer associated to the session.
     */
    private void onAction(final NetPackage netPackage, final NetServiceConsumer consumer) {
        if (netPackage != null) {
            try {
                switch (netPackage.getActionEvent()) {
                    case CONNECT:
                        consumer.onConnect(netPackage);
                        break;
                    case DISCONNECT:
                        consumer.onDisconnect(netPackage);
                        break;
                    case READ:
                        if(netPackage.getSession() != null && netPackage.getPayload() != null) {
                            netPackage.getSession().addIngressPackage(netPackage.getPayload().length);
                        }
                        consumer.onRead(netPackage);
                        break;
                    case WRITE:
                        if(netPackage.getSession() != null && netPackage.getPayload() != null) {
                            netPackage.getSession().addEgressPackage(netPackage.getPayload().length);
                        }
                        consumer.onWrite(netPackage);
                        break;
                }
            } catch (Exception ex) {
                Log.e(SystemProperties.get(IOSystemProperties.Net.LOG_TAG), "Action consumer exception", ex);
            }
        }
    }

    /**
     * Net IO thread.
     */
    public static class NetIOThread extends ServiceThread {

        private final ByteBuffer inputBuffer;
        private final ByteBuffer outputBuffer;
        private final int inputBufferSize;
        private final int outputBufferSize;

        public NetIOThread(ServiceThread serviceThread) {
            super(serviceThread);
            inputBufferSize = SystemProperties.getInteger(IOSystemProperties.Net.DEFAULT_INPUT_BUFFER_SIZE);
            outputBufferSize = SystemProperties.getInteger(IOSystemProperties.Net.DEFAULT_OUTPUT_BUFFER_SIZE);
            if(SystemProperties.getBoolean(IOSystemProperties.Net.IO_THREAD_DIRECT_ALLOCATE_MEMORY)) {
                inputBuffer = ByteBuffer.allocateDirect(getInputBufferSize());
                outputBuffer = ByteBuffer.allocateDirect(getOutputBufferSize());
            } else {
                inputBuffer = ByteBuffer.allocate(getInputBufferSize());
                outputBuffer = ByteBuffer.allocate(getOutputBufferSize());
            }
        }

        public NetIOThread() {
            inputBufferSize = SystemProperties.getInteger(IOSystemProperties.Net.DEFAULT_INPUT_BUFFER_SIZE);
            outputBufferSize = SystemProperties.getInteger(IOSystemProperties.Net.DEFAULT_OUTPUT_BUFFER_SIZE);
            if(SystemProperties.getBoolean(IOSystemProperties.Net.IO_THREAD_DIRECT_ALLOCATE_MEMORY)) {
                inputBuffer = ByteBuffer.allocateDirect(getInputBufferSize());
                outputBuffer = ByteBuffer.allocateDirect(getOutputBufferSize());
            } else {
                inputBuffer = ByteBuffer.allocate(getInputBufferSize());
                outputBuffer = ByteBuffer.allocate(getOutputBufferSize());
            }
        }

        /**
         * Return the input buffer of the thread.
         * @return Input buffer.
         */
        public final ByteBuffer getInputBuffer() {
            return inputBuffer;
        }

        /**
         * Return the output buffer of the thread.
         * @return Output buffer.
         */
        public final ByteBuffer getOutputBuffer() {
            return outputBuffer;
        }

        /**
         * Return the size of the internal buffer used to read input data.
         * @return Size of the internal input buffer.
         */
        public int getInputBufferSize() {
            return inputBufferSize;
        }

        /**
         * Return the size of the internal buffer used to write output data.
         * @return Size of the internal output buffer.
         */
        public int getOutputBufferSize() {
            return outputBufferSize;
        }
    }

    /**
     * Timer task to destroy the channel if has not session assigned.
     */
    private class ConnectionTimeout extends TimerTask {

        private final SocketChannel channel;

        public ConnectionTimeout(SocketChannel channel) {
            this.channel = channel;
        }

        /**
         * Destroy channel
         */
        @Override
        public void run() {
            fork(() -> {
                if (!sessionsByChannel.containsKey(channel)) {
                    try {
                        destroyChannel(channel);
                    } catch (Exception ex) {
                        Log.e("CONNECTION_TIMEOUT_TASK", "Fork fail", ex);
                    }
                }
            });
        }

    }

    /**
     * Transport layer protocols.
     */
    public enum TransportLayerProtocol {

        TCP,

        TCP_SSL,

        UDP
    }

}
