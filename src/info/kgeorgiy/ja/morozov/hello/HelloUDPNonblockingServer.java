package info.kgeorgiy.ja.morozov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import info.kgeorgiy.java.advanced.hello.Util;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Implementation {@link HelloServer} interfaces.
 * But works with nonblocking type of communication
 * Server answered "Hello, ~request~"
 *
 * @author Anton Morozov
 * */
public class HelloUDPNonblockingServer implements HelloServer {
    private Selector selector;
    private ExecutorService service, mainThread;
    private DatagramChannel channel;
    private final Queue<ByteBuffer> buffers = new ConcurrentLinkedQueue<>();
    private final Queue<DatagramPacket> responses = new ConcurrentLinkedQueue<>();

    @Override
    public void start(int port, int threads) {
        try {
            selector = Selector.open();
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.bind(new InetSocketAddress(port));
            channel.register(selector, SelectionKey.OP_READ);

            service = Executors.newFixedThreadPool(threads);
            mainThread = Executors.newSingleThreadExecutor();
            try {
                int bufferSize = channel.socket().getReceiveBufferSize();
                for (int i = 0; i < threads; i++) {
                    buffers.add(ByteBuffer.allocate(bufferSize));
                }
            } catch (SocketException e) {
                close();
                System.err.println("Error occurred while creating buffers");
                return ;
            }

            mainThread.execute(this::start);
        } catch (IOException e) {
            System.err.println("Error occurred while starting server" + e.getMessage());
        }
    }

    private void response(final SelectionKey key, final ByteBuffer buffer, final SocketAddress address) {
        buffer.flip();
        final String data = new String(buffer.array(), 0, buffer.limit(), StandardCharsets.UTF_8);
        final String hello = "Hello, " + data;
        byte[] res = hello.getBytes(StandardCharsets.UTF_8);
        responses.add(new DatagramPacket(res, res.length, address));
        buffer.clear();
        buffers.add(buffer);
        key.interestOpsOr(SelectionKey.OP_WRITE);
        selector.wakeup();
    }

    private void read(SelectionKey key) {
        if (buffers.isEmpty()) {
            key.interestOpsAnd(~SelectionKey.OP_READ);
            return;
        }
        ByteBuffer buffer = buffers.poll();
        if (buffer == null) {
            return;
        }
        try {
            SocketAddress address = channel.receive(buffer);
            service.execute(() -> response(key, buffer, address));
        } catch (IOException e) {
            System.err.println("Error occurred while receiving message " + e.getMessage());
        }
    }

    private void write(SelectionKey key) {
        if (responses.isEmpty()) {
            key.interestOps(SelectionKey.OP_READ);
            return ;
        }
        DatagramPacket packet = responses.poll();
        try {
            channel.send(ByteBuffer.wrap(packet.getData()), packet.getSocketAddress());
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return;
        }
        key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
    }

    private void start() {
        while (selector.isOpen()) {
            try {
                int cnt = selector.select();
                if (cnt == 0) continue;
                Set<SelectionKey> keys = selector.selectedKeys();
                for (final Iterator<SelectionKey> it = keys.iterator(); it.hasNext();) {
                    final SelectionKey key = it.next();
                    try {
                        if (key.isReadable()) {
                            read(key);
                        }
                        if (key.isWritable()) {
                            write(key);
                        }
                    } finally {
                        it.remove();
                    }
                }
            } catch (IOException e) {
                close();
                System.err.println("Error occurred while selecting: " + e.getMessage());
                return;
            }
        }
    }

    @Override
    public void close() {
        Utils.close(selector);
        Utils.close(channel);

        Utils.shutdownAndAwait(service);
        Utils.shutdownAndAwait(mainThread);
    }


    /**
     * Start server with parameters from args
     * <ul>
     *     <li>{@code port} - 1st arg, port number </li>
     *     <li>{@code threads} - 2nd arg, count of threads in server</li>
     * </ul>
     *
     * @param args arguments of terminal
     * */
    public static void main(String[] args) {
        Utils.startServer(args, HelloUDPNonblockingServer::new);
    }
}
