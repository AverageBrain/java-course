package info.kgeorgiy.ja.morozov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;
import org.junit.FixMethodOrder;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.channels.SelectableChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * Implementation {@link JarImpler} interfaces.
 * Generates a file with the class by the {@link Class} token.
 * When run with the <var>-jar</var> key generates a <var>.jar</var> file.
 *
 * @author Anton Morozov
 */
public class Implementor implements JarImpler {
    /**
     * Visitor which recursive delete files  */
    private static final SimpleFileVisitor<Path> DELETE_VISITOR = new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    /**
     * Return true if abstract method contains private arguments
     *
     * @param method {@code Method} - method for check
     * */
    private boolean AbstMethodContainsPrivateArgs(Method method) {
        if (!Modifier.isAbstract(method.getModifiers())) {
            return false;
        }
        if (Modifier.isPrivate(method.getReturnType().getModifiers())) {
            return true;
        }
        return Arrays.stream(method.getParameterTypes()).anyMatch((type) -> Modifier.isPrivate(type.getModifiers()));
    }


    /**
     * Add not private methods from {@code methods} to map of methods {@code map}
     * If map have method with same {@link MethodSignature}, method will add to map if it has more specific return type
     * @param methods {@code Method[]} - array of {@link Method} from which methods added to map
     * @param map {@code Map<Implementor.MethodSignature, Method>} - map to which methods can be added
     * */
    private void addToMap(Method[] methods, Map<MethodSignature, Method> map) throws ImplerException{
        for (Method method : methods) {
            if (AbstMethodContainsPrivateArgs(method)) {
                throw new ImplerException("Cannot implement this class, because it contains private arguments");
            }
            if (Modifier.isPrivate(method.getModifiers())) {
                continue;
            }
            MethodSignature methodSignature = new MethodSignature(method);
            if (map.containsKey(methodSignature)) {
                Method otherMethod = map.get(methodSignature);
                if (!method.getReturnType().isAssignableFrom(otherMethod.getReturnType())) {
                    map.replace(methodSignature, method);
                }
            } else {
                map.put(methodSignature, method);
            }
        }
    }

    /**
     * Write class by token and root
     * Write class to the correct subdirectory in {@code root} directory. For example, the implementation of the
     * interface {@link java.util.List} will go to {@code $root/java/util/ListImpl.java}
     * @param token {@code Class<?>} - token, which needed to implements or extends
     * @param root {@code Path} - a place, where realize class
     * @throws ImplerException if token is null, or non-private constructor does not exist in token
     * @see ClassWriter
     * */
    private void writeClass(Class<?> token, Path root) throws ImplerException {
        if (Objects.isNull(token)) {
            throw new ImplerException("Expected not null token");
        }
        Map<MethodSignature, Method> methods = new HashMap<>();
        for (Class<?> curToken = token; curToken != null; curToken = curToken.getSuperclass()) {
            addToMap(curToken.getDeclaredMethods(), methods);
        }
        addToMap(token.getMethods(), methods);
        List<Method> listOfMethods = methods.values().stream()
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .collect(Collectors.toList());

        List<Constructor<?>> constructors = Collections.emptyList();
        if (!token.isInterface()) {
            constructors = List.of(token.getDeclaredConstructors());
            constructors = constructors.stream()
                    .filter(constructor -> !Modifier.isPrivate(constructor.getModifiers()))
                    .toList();
            if (constructors.isEmpty()) {
                throw new ImplerException("Expected non-private constructors");
            }
        }
        ClassWriter.writeClass(root, token, constructors, listOfMethods);
    }


    /**
     * Create directories by path, if they don't exist
     *
     * @param path {@link Path} - a file/directory by which to create directories
     * @throws ImplerException - if an error occurred during to create directories
     * */
    private void createDirectory(Path path) throws ImplerException {
        try {
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
        } catch (SecurityException e) {
            throw new ImplerException("Error during to create directory", e);
        } catch (IOException ignored) {
            // ignored
        }
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (Objects.isNull(token)) {
            throw new ImplerException("Not null token expected");
        }
        if (Objects.isNull(root)) {
            throw new ImplerException("Not null root expected");
        }
        if (token.isArray() || token.isPrimitive() || token.equals(Enum.class)
                || Modifier.isFinal(token.getModifiers())
                || Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Unsupported class");
        }
        String className = token.getSimpleName() + "Impl";
        Path output = root.resolve(Path.of(token.getPackageName().replace(".", File.separator), className + ".java"));

        createDirectory(output);

        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writeClass(token, root);
        } catch (IOException e) {
            throw new ImplerException("Error during out file");
        }
    }

    /**
     * Compiles class in given {@link Path} with name which agreed with{@code Class}
     *
     * @param root {@code Path} for compiled class
     * @param token {@code Class<?>} - token on which to build a class
     * @throws ImplerException if the java compiler wasn't found or an error occurred during to compile classes
     * */
    private void compileFiles(final Path root, final Class<?> token) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Could not find java compiler, include tools.jar to classpath");
        }
        final String classpath;
        try {
            classpath = Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new ImplerException(e.getMessage());
        }
        final String[] args = {"-encoding", "UTF-8", "-cp", root + File.pathSeparator + classpath,
                root.resolve(Path.of(token.getPackageName().replace(".", File.separator), token.getSimpleName() + "Impl.java")).toString()};
        final int exitCode = compiler.run(null, null, null, args);
        if (exitCode != 0) {
            throw new ImplerException("Compiler exit code: expected - 0, actual - " + exitCode);
        }
    }

    /**
     * Delete all files in {@code root}
     *
     * @param root {@code Path} - root of directory or file, which files will delete
     * @throws IOException if an error occurred in walkFileTree
     * */
    private void clean(final Path root) throws IOException {
        if (Files.exists(root)) {
            Files.walkFileTree(root, DELETE_VISITOR);
        }
    }


    /**
     * Create a <var>.jar</var> file containing <var>.class</var> files.
     *
     * @param tempDir {@code Path} where to get <var>.class</var> files
     * @param jarFile {@code Path} where to save the <var>.jar</var> file
     * @param token {@code Class<?>} the {@link Class} object of a parent class or an interface that is being implemented
     * @throws ImplerException if an error occurred during to open or write in <var>.jar</var> file or to copy files
     */
    private void buildJar(Path tempDir, Path jarFile, Class<?> token) throws ImplerException {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (final JarOutputStream outputStream =
                     new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            final String className = token.getPackageName().replace(".", "/")
                    + String.format("/%sImpl.class", token.getSimpleName());
            outputStream.putNextEntry(new ZipEntry(className));
            Files.copy(tempDir.resolve(className), outputStream);
        } catch (IOException e) {
            throw new ImplerException("Error during a jar file writing " + e.getMessage());
        }
    }

    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        try {
            createDirectory(jarFile);
            Path tempDir = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "tmp");
            try {
                implement(token, tempDir);
                compileFiles(tempDir, token);
                buildJar(tempDir, jarFile, token);
            } finally {
                clean(tempDir.toFile().toPath());
            }
        } catch (IOException e) {
            throw new ImplerException(e.getMessage());
        }
    }

    /**
     * Class containing a name and parameters of {@link Method}
     *
     * @see Method
     * */
    private static class MethodSignature {
        /**
         * Name of method
         * */
        String name;
        /**
         * Method's parameters
         * */
        Class<?>[] parameters;

        /**
         * Constructor of {@code MethodSignarute}
         *
         * @param method {@code Method} - method whose signature we want to take
         * */
        public MethodSignature(Method method) {
            this.name = method.getName();
            this.parameters = method.getParameterTypes();
        }

        /**
         * Overridden method for comparing the contents of MethodSignature classes.
         * It returns {@code true} if and only if the given {@code obj} extends MethodSignature,
         * and have same name and parameters, and {@code false} otherwise
         *
         * @param obj {@code Object} a method wrapper to compare with
         * @return {@code true} if objects are equal, {@code false} otherwise
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof final MethodSignature otherMethod) {
                return (name.equals(otherMethod.name) &&
                        Arrays.equals(parameters, otherMethod.parameters));
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(name,
                    Arrays.hashCode(parameters));
        }
    }

    /**
     *
     *
     * @param args {@code String} - command line arguments {@code [-jar] class path}
     * @see #implement(Class, Path)
     * @see #implementJar(Class, Path)
     * @throws ImplerException if an error occurred during to write class
     * */
    public static void main(String[] args) throws ImplerException {
        if (args.length < 1 || args.length > 2) {
            throw new ImplerException("Expected one argument");
        }
        if (args.length == 2 && !"-jar".equals(args[0])) {
            throw new ImplerException("Unexpected token: " + args[0]);
        }
        if (args[0] == null || (args.length == 2 && args[1] == null)) {
            throw new ImplerException("Expected not-null arguments");
        }
        try {
            if (args.length == 1) {
                new Implementor().implement(Class.forName(args[0]), Paths.get("."));
            }
            if (args.length == 2) {
                new Implementor().implementJar(Class.forName(args[1]), Paths.get(args[1]+ ".jar"));
            }
        } catch (ClassNotFoundException e) {
            throw new ImplerException("Cant found file");
        }
    }
}
