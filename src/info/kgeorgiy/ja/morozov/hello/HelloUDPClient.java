package info.kgeorgiy.ja.morozov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import info.kgeorgiy.java.advanced.hello.Util;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * Implementation {@link HelloClient} interfaces.
 *
 * @author Anton Morozov
 * */
public class HelloUDPClient implements HelloClient {

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        try {

            InetAddress IPAddress = InetAddress.getByName(host);
            SocketAddress socketAddress = new InetSocketAddress(IPAddress, port);

            ExecutorService service = Executors.newFixedThreadPool(threads);

            IntStream.range(1, threads + 1).forEach(
                    i -> service.execute(new ClientTask(socketAddress, prefix, requests, i))
            );
            Utils.shutdownAndAwait(service);
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + e);
        }
    }


    private record ClientTask(SocketAddress address, String prefix, int requests, int threadId) implements Runnable {


        @Override
        public void run() {
            try (final DatagramSocket socket = new DatagramSocket()) {

                socket.setSoTimeout(Utils.TIMEOUT);

                final int bufferSize = socket.getReceiveBufferSize();
                final DatagramPacket receivedPacket = new DatagramPacket(new byte[bufferSize], bufferSize);
                final DatagramPacket sendingPacket = new DatagramPacket(new byte[bufferSize], bufferSize, address);

                for (int i = 1; i < requests + 1; i++) {
                    final String request = Utils.makeRequest(prefix, threadId, i);
                    sendingPacket.setData(request.getBytes(StandardCharsets.UTF_8));

                    String response = "";
                    while (!socket.isClosed() && !Thread.currentThread().isInterrupted()
                            && !Utils.checkResponse(response, threadId, i)) {
                        try {
                            socket.send(sendingPacket);
                        } catch (IOException e) {
                            System.err.println("Error occurred while sending packet: " + e.getMessage());
                            continue;
                        }

                        try {
                            socket.receive(receivedPacket);
                        } catch (IOException e) {
                            System.err.println("Error occurred while receiving packet: " + e.getMessage());
                            continue;
                        }
                        response = new String(receivedPacket.getData(), receivedPacket.getOffset(),
                                receivedPacket.getLength(), StandardCharsets.UTF_8);
                    }

                    System.out.printf("Request: %s%nResponse: %s%n", request, response);
                }
            } catch (SocketException e) {
                System.err.println(e.getMessage());
            }
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
        Utils.runClient(args, HelloUDPClient::new);
    }
}