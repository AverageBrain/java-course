package info.kgeorgiy.ja.morozov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BaseWalk {
    public void run(String[] args, TypeWalk typeWalk) {
        if (args == null) {
            printErrorMessage("Args in null");
            return ;
        }
        if (args.length != 2) {
            printErrorMessage("Expected 2 arguments - actual " + args.length);
            return ;
        }
        if (args[0] == null || args[1] == null) {
            printErrorMessage("Expected not null args");
            return ;
        }
        Path inputFilePath, outputFilePath;
        try {
            inputFilePath = Paths.get(args[0]);
        } catch (InvalidPathException e) {
            printErrorMessage("Input file name is wrong: " + e.getMessage());
            return ;
        }
        try {
            outputFilePath = Paths.get(args[1]);
        } catch (InvalidPathException e) {
            printErrorMessage("Output file name is wrong: " + e.getMessage());
            return ;
        }
        walk(inputFilePath, outputFilePath, typeWalk);
    }

    private void walk(Path inputFilePath, Path outputFilePath, TypeWalk typeWalk) {
        try (BufferedReader bufferedReader = Files.newBufferedReader(inputFilePath)) {
            try {
                if (outputFilePath.getParent() != null && !Files.exists(outputFilePath.getParent())) {
                    Files.createDirectories(outputFilePath.getParent());
                }
            } catch (IOException ignored) {
                //ignored
            }
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(outputFilePath)) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                HashRecursiveFileVisitor visitor = new HashRecursiveFileVisitor(bufferedWriter, typeWalk, digest);
                String fileOrDirName;
                while ((fileOrDirName = bufferedReader.readLine()) != null) {
                    try {
                        visitor.visitFile(fileOrDirName);
                    } catch (IOException e) {
                        printErrorMessage("Writing error: error while writing file" + e.getMessage());
                    }
                }
            } catch (IOException e) {
                printErrorMessage("Output error: cant open output file" + e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                throw new AssertionError("Not support sha-256 algorithm", e);
            }
        } catch (IOException e) {
            printErrorMessage("Input error: cant open input file" + e.getMessage());
        }
    }

    private void printErrorMessage(String errorMessage) {
        System.err.println(errorMessage);
    }

    public enum TypeWalk {
        RECURSIVE,
        BASE
    }
}
