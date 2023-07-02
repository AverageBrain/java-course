package info.kgeorgiy.ja.morozov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Implementation {@link HelloClient} interfaces.
 * Send request to server
 *
 * @author Anton Morozov
 * */
public class HelloUDPNonblockingClient implements HelloClient {
    private Selector selector;
    private final List<DatagramChannel> channels = new ArrayList<>();
    private SocketAddress socket;
    private int processedThreads = 0;
    private int requests;

    private void write(final String prefix, SelectionKey key) {
        final DatagramChannel channel = (DatagramChannel) key.channel();
        final ChannelAttributes attr = (ChannelAttributes) key.attachment();

        final int threadId = attr.getChannelId() + 1;
        final int requestId = attr.getFinishedRequests() + 1;
        final String request = Utils.makeRequest(prefix, threadId, requestId);

        try {
            channel.send(ByteBuffer.wrap(request.getBytes(StandardCharsets.UTF_8)), socket);
            key.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
            System.err.println("Error" + e.getMessage());
        }
    }

    private void read(SelectionKey key) {
        final DatagramChannel channel = (DatagramChannel) key.channel();
        final ChannelAttributes attr = (ChannelAttributes) key.attachment();

        final int threadId = attr.getChannelId() + 1;
        final int requestId = attr.getFinishedRequests() + 1;
        try {
            final ByteBuffer buffer = attr.getBuffer();
            buffer.clear();
            channel.receive(buffer);
            buffer.flip();
            final String response = new String(buffer.array(), 0, buffer.limit(), StandardCharsets.UTF_8);
            key.interestOps(SelectionKey.OP_WRITE);

            if (Utils.checkResponse(response, threadId, requestId)) {
                attr.incFinishedRequests();
                if (attr.getFinishedRequests() == requests) {
                    processedThreads++;
                    channel.close();
                }
            }
        } catch (IOException e) {
            System.err.println("Error while reading request: " + e.getMessage());
        }
    }

    private void send(final String prefix, int threads) {
        while (processedThreads < threads) {
            try {
                if (selector.select(Utils.TIMEOUT) == 0) {
                    selector.keys().forEach((key -> key.interestOps(SelectionKey.OP_WRITE)));
                    continue;
                }
                Set<SelectionKey> keys = selector.selectedKeys();
                for (final Iterator<SelectionKey> it = keys.iterator(); it.hasNext();) {
                    final SelectionKey key = it.next();
                    try {
                        if (key.isWritable()) {
                            write(prefix, key);
                        }
                        if (key.isReadable()) {
                            read(key);
                        }
                    } finally {
                        it.remove();
                    }
                }
            } catch (final IOException e) {
                System.err.println("Error while sending response" + e.getMessage());
            }
        }
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        try {
            InetAddress IPAddress = InetAddress.getByName(host);
            socket = new InetSocketAddress(IPAddress, port);
            this.requests = requests;
            selector = Selector.open();
            for (int i = 0; i < threads; i++) {
                DatagramChannel channel = DatagramChannel.open();
                channel.configureBlocking(false);
                channel.register(selector, SelectionKey.OP_WRITE, new ChannelAttributes(i, channel.socket().getReceiveBufferSize()));
                channels.add(channel);
            }
            send(prefix, threads);
            for (final DatagramChannel channel : channels) {
                try {
                    channel.close();
                } catch (final IOException ignored) {

                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error occurred while client init" + e.getMessage());
        }
    }

    private static class ChannelAttributes {
        private final int channelId;
        private int finishedRequests = 0;
        private final ByteBuffer buffer;

        ChannelAttributes(final int id, final int bufferSize) {
            this.channelId = id;
            this.buffer = ByteBuffer.allocate(bufferSize);
        }

        public void incFinishedRequests() {
            finishedRequests++;
        }

        public ByteBuffer getBuffer() {
            return buffer;
        }

        public int getChannelId() {
            return channelId;
        }

        public int getFinishedRequests() {
            return finishedRequests;
        }
    }

    /**
     * Run client with parameters from args
     * <ul>
     *     <li>{@code host} - 1st arg, host or ip-address </li>
     *     <li>{@code port} - 2nd arg, port number</li>
     *     <li>{@code prefix} - 3rd arg, prefix of requests</li>
     *     <li>{@code threads} - 4th arg, count of threads in client</li>
     *     <li>{@code requests} - 5th arg, count of requests in a thread</li>
     * </ul>
     *
     * @param args arguments of terminal
     * */
    public static void main(String[] args) {
        Utils.runClient(args, HelloUDPNonblockingClient::new);
    }
}
