package info.kgeorgiy.ja.morozov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Class help to write class use {@link ClassWriter#writeClass(Path, Class, List, List)}
 *
 * @author Anton Morozov
 * */
public class ClassWriter {

    /**
     * Return string of modifier of {@code executable}
     *
     * @param executable {@link Executable} - executable whose modifiers is returned
     * @return string of modifiers
     * */
    private static String getModifiersExecutable(Executable executable) {
        return Modifier.toString(executable.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.NATIVE & ~Modifier.TRANSIENT);
    }


    /**
     * Return string package of token if it exists
     *
     * @param token {@link Class} - class whose package is returned
     * @return string of package of token
     * */
    private static String getPackage(Class<?> token) {
        if (!token.getPackageName().isEmpty()) {
            return String.format("package %s;%n", token.getPackageName());
        }
        return "";
    }


    /**
     * Return class name of {@code token}
     *
     * @param token {@link Class} - class whose name is returned
     * @return string class name if token
     * */
    private static String getClassName(Class<?> token) {
        return String.format("public class %sImpl %s %s {%n",
                token.getSimpleName(),
                (token.isInterface() ? "implements" : "extends"),
                token.getCanonicalName());
    }

    /**
     * Return string return type of {@code method}
     *
     * @param method {@link Method} - method whose return type is returned
     * @return string return type
     * */
    private static String getReturnType(Method method) {
        return method.getReturnType().getCanonicalName();
    }

    /**
     * Return string consisting of type and parameters of {@code method}
     *
     * @param parameter {@link Parameter} - parameter whose full name of parameter is returned
     * @return string consisting of type and name of parameter
     * */
    private static String getFullParameterName(Parameter parameter) {
        return parameter.getType().getCanonicalName() + " " + parameter.getName();
    }

    /**
     * Return all parameter through given {@code function} {@code executable} names joining ','
     *
     * @param executable {@link Executable} - executable whose parameters is returned
     * @param function {@link Function} - function applied to parameters
     * @return string of parameters
     * */
    private static String getParameters(Executable executable, Function<Parameter, String> function) {
        return Arrays.stream(executable.getParameters())
                .map(function)
                .collect(Collectors.joining(", "));
    }

    /**
     * Return name default return type of {@code token}
     *
     * @param token {@link Class} - class whose default return type is returned
     * @return string of default return type
     * */
    private static String getReturnType(Class<?> token) {
        if (token.equals(boolean.class)) {
            return "false";
        } else if (token.equals(void.class)) {
            return "";
        } else if (token.isPrimitive()) {
            return "0";
        } else {
            return "null";
        }
    }


    /**
     * Return string of method body of {@code method}
     *
     * @param method {@link Method} - method whose body is returned
     * @return string of method
     * */
    private static String getMethodBody(Method method) {
        return String.format("%n\treturn %s;%n", getReturnType(method.getReturnType()));
    }

    /**
     * Return string of exceptions joining by ','
     *
     * @param executable {@link Executable} - executable whose exceptions is returned
     * @return string of exceptions
     * */
    private static String getExceptions(Executable executable) {
        Class<?>[] exceptions = executable.getExceptionTypes();
        if (exceptions.length == 0)
            return "";
        return "throws " + Arrays.stream(exceptions)
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(", "));
    }

    /**
     * Write string method by {@code bufferedWriter}
     *
     * @param method {@link Method} - method whose is written
     * @param bufferedWriter {@link BufferedWriter} - writer where write method
     * @throws IOException - if an error occurred during to write method
     * */
    private static void writeMethod(Method method, BufferedWriter bufferedWriter) throws IOException {
        bufferedWriter.write(String.format("%s %s %s(%s) %s {%s}%n%n",
                getModifiersExecutable(method), getReturnType(method), method.getName(),
                getParameters(method, ClassWriter::getFullParameterName), getExceptions(method), getMethodBody(method)
        ));
    }

    /**
     * Return string of constructor body of {@code method}
     *
     * @param constructor {@link Constructor} - constructor whose body is returned
     * @return containing string constructor body
     * */
    private static String getConstructorBody(Constructor<?> constructor) {
        return String.format("%n\tsuper(%s);%n", getParameters(constructor, Parameter::getName));
    }

    /**
     * Write string of constructor by {@code bufferedWriter}
     *
     * @param constructor {@link Constructor} - constructor whose is written
     * @param className {@link String} - name of class whose is realized
     * @param bufferedWriter {@link BufferedWriter} - writer where write constructor
     * @throws IOException - if an error occurred during to write constructor
     * */
    private static void writeConstructor(Constructor<?> constructor, String className, BufferedWriter bufferedWriter) throws IOException {
        bufferedWriter.write(String.format("%s %s(%s) %s {%s}%n%n",
                getModifiersExecutable(constructor), className,
                getParameters(constructor, ClassWriter::getFullParameterName),  getExceptions(constructor),
                getConstructorBody(constructor)
        ));
    }

    /**
     * Write method to root by token and used {@link List} constructor and methods
     *
     * @param root {@link Path} - where write class
     * @param token {@link Class} - token which class realize
     * @param constructors {@link List<Constructor>} - list of constructors to write
     * @param methods {@link List<Method>} - list of methods to write
     * @throws ImplerException if an error occurred during to open class or to write class
     * */
    public static void writeClass(Path root, Class<?> token, List<Constructor<?>> constructors, List<Method> methods) throws ImplerException {
        String className = token.getSimpleName() + "Impl";
        Path output = root.resolve(Path.of(token.getPackageName().replace(".", File.separator), className + ".java"));
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write(getPackage(token));
            writer.write(getClassName(token));
            for (Constructor<?> constructor : constructors) {
                writeConstructor(constructor, className, writer);
            }
            for (Method method : methods) {
                writeMethod(method, writer);
            }
            writer.write("}");
        } catch (IOException e) {
            throw new ImplerException("Error during out file");
        }
    }
}

