package info.kgeorgiy.ja.morozov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Implementation {@link HelloServer} interfaces.
 * Server answered "Hello, ~request~", where request - request from client
 *
 * @author Anton Morozov
 * */
public class HelloUDPServer implements HelloServer {
    private ExecutorService service;
    private DatagramSocket socket;

    @Override
    public void start(int port, int threads) {
        try {
            socket = new DatagramSocket(port);
            service = Executors.newFixedThreadPool(threads);
            IntStream.range(0, threads).forEach((i) -> service.execute(new ServerTask(socket)));
        } catch (final SocketException e) {
            System.err.println("Error occurred while opening socket " + e);
        }
    }


    private record ServerTask(DatagramSocket socket) implements Runnable {
        @Override
        public void run() {
            try {
                final int bufferSize = socket.getReceiveBufferSize();
                final DatagramPacket packet = new DatagramPacket(new byte[bufferSize], bufferSize);

                while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                    try {
                        socket.receive(packet);
                        String data = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                        String hello = "Hello, " + data;
                        packet.setData(hello.getBytes(StandardCharsets.UTF_8));
                        socket.send(packet);
                    } catch (final IOException ignored) {
                    }
                }

            } catch (final SocketException ignored) {
            }
        }
    }

    @Override
    public void close() {
        socket.close();
        Utils.shutdownAndAwait(service);
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
        Utils.startServer(args, HelloUDPServer::new);
    }
}