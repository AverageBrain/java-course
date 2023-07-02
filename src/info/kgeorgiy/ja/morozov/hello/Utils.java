package info.kgeorgiy.ja.morozov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Utils for HelloUDPClient and HelloUDPServer
 * */
public class Utils {
    /**
     * Pattern for correct response from server
     * */
    public static final Pattern PATTERN = Pattern.compile("\\D*(\\d+)\\D*(\\d+)\\D*");

    /**
     * Time for waiting answer from socket
     * */
    public static final int TIMEOUT = 50;


    /**
     * Check that message correct
     *
     * @param response response which was received
     * @param threadId number of thread, that sent the request
     * @param requestId number of request, which the request was sent
     * */
    public static boolean checkResponse(final String response, final int threadId, final int requestId) {
        final Matcher matcher = PATTERN.matcher(convertNumbersToNormal(response));
        return matcher.matches()
                && String.valueOf(threadId).equals(matcher.group(1))
                && String.valueOf(requestId).equals(matcher.group(2));
    }


    /**
     * Make correct request
     *
     * @param prefix prefix of request
     * @param threadId number of thread
     * @param requestId number of request
     * */
    public static String makeRequest(final String prefix, final int threadId, final int requestId) {
        return prefix + threadId + "_" + requestId;
    }


    /**
     * Replace all digits to normal numeric
     *
     * @param origin original string
     * */
    public static String convertNumbersToNormal(final String origin) {
        StringBuilder ans = new StringBuilder();
        for (int i = 0; i < origin.length(); i++) {
            char curChar = origin.charAt(i);
            if (Character.isDigit(curChar)) {
                ans.append(Character.getNumericValue(curChar));
            } else {
                ans.append(curChar);
            }
        }
        return ans.toString();
    }


    /**
     * Shut down service and wait
     *
     * @param service service which shut down
     * */
    public static void shutdownAndAwait(final ExecutorService service) {
        service.shutdown();
        try {
            service.awaitTermination(2, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            System.err.println("Service was interrupted: " + e.getMessage());
        }
    }


    /**
     * Start server which implements HelloServer
     *
     * @param args params for server
     * @param serverConstructor constructor of server, which received
     * */
    public static <T extends HelloServer> void startServer(String[] args, Supplier<T> serverConstructor) {
        if (args.length != 2) {
            System.err.println("Expected 2 arguments, actual: " + args.length);
            return;
        }
        if (args[0] == null || args[1] == null) {
            System.err.println("Expected non-null arguments");
        }
        try {
            T server = serverConstructor.get();
            server.start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            server.close();
        } catch (NumberFormatException e) {
            System.err.println("Wrong number format " + e.getMessage());
        }
    }

    /**
     * Run client which implements HelloServer
     *
     * @param args params for client
     * @param clientConstructor constructor of client, which received
     * */
    public static <T extends HelloClient> void runClient(String[] args, Supplier<T> clientConstructor) {
        if (args.length != 5) {
            System.err.println("Expected 5 arguments, actual: " + args.length);
            return;
        }
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Expected non-null arguments");
        }
        T client = clientConstructor.get();
        try {
            client.run(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
        } catch (NumberFormatException e) {
            System.err.println("Wrong number format " + e.getMessage());
        }
    }

    public static void close(Closeable obj) {
        if (obj == null) return;
        try {
            obj.close();
        } catch(IOException e) {
            System.err.println("Error while closing: " + e.getMessage());
        }
    }
}
