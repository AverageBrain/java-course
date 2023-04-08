package info.kgeorgiy.ja.morozov.walk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.HexFormat;

public class HashRecursiveFileVisitor extends SimpleFileVisitor<Path> {
    private final HexFormat hexFormat = HexFormat.of();
    private final BufferedWriter bufferedWriter;
    private final int BUFFER_SIZE = 1024 * 1024;
    private final String ZERO = String.format("%064x", 0);
    private final BaseWalk.TypeWalk typeWalk;
    private final byte[] buffer = new byte[BUFFER_SIZE];
    private final MessageDigest digest;

    HashRecursiveFileVisitor(final BufferedWriter bufferedWriter, BaseWalk.TypeWalk typeWalk, MessageDigest digest) {
        this.bufferedWriter = bufferedWriter;
        this.typeWalk = typeWalk;
        this.digest = digest;
    }

    public void visitFile(String fileOrDirName) throws IOException {
        try {
            final Path file = Paths.get(fileOrDirName);
            switch (typeWalk) {
                case RECURSIVE -> {
                    Files.walkFileTree(file, this);
                }
                case BASE -> {
                    if (Files.isRegularFile(file)) {
                        visitFile(file, Files.readAttributes(file, BasicFileAttributes.class));
                        return ;
                    }
                    throw new IOException("Can't visit this file " + file);
                }
            }
        } catch (IOException | InvalidPathException e) {
            write(ZERO, fileOrDirName);
        }
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file)) {
            int c = 0;
            while ((c = inputStream.read(buffer)) >= 0) {
                digest.update(buffer, 0, c);
            }
        } catch (IOException e) {
            write(ZERO, getName(file));
            return FileVisitResult.CONTINUE;
        }
        write(hexFormat.formatHex(digest.digest()), getName(file));
        return FileVisitResult.CONTINUE;
    }
    @Override
    public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
        if (typeWalk == BaseWalk.TypeWalk.BASE && Files.isDirectory(file)) {
            return FileVisitResult.CONTINUE;
        }
        write(ZERO, getName(file));
        return FileVisitResult.CONTINUE;
    }

    private String getName(Path file) {
        return file.toString();
    }

    private void write(String hash, String fileName) throws IOException {
        bufferedWriter.write(String.format("%s %s%n", hash, fileName));
    }
}
