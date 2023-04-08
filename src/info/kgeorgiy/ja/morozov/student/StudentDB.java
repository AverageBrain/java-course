package info.kgeorgiy.ja.morozov.student;

import info.kgeorgiy.java.advanced.student.AdvancedQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements AdvancedQuery {
    private static final Comparator<Student> COMPARATOR_BY_ID = Comparator.naturalOrder();

    private static final Comparator<Student> COMPARATOR_BY_STUDENT_NAME = Comparator
            .comparing(Student::getLastName, Comparator.reverseOrder())
            .thenComparing(Student::getFirstName, Comparator.reverseOrder())
            .thenComparingInt(Student::getId);

    private static Stream<Map.Entry<GroupName, List<Student>>> getGroupsStream(final Collection<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::getGroup, TreeMap::new, Collectors.toList()))
                .entrySet().stream();
    }

    private static List<Group> getGroupsByFunction(
            final Collection<Student> students,
            final Function<Collection<Student>, List<Student>> function
    ) {
        return getGroupsStream(students)
                .map(group -> new Group(group.getKey(), function.apply(group.getValue())))
                .collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsByName(final Collection<Student> students) {
        return getGroupsByFunction(students, this::sortStudentsByName);
    }

    @Override
    public List<Group> getGroupsById(final Collection<Student> students) {
        return getGroupsByFunction(students, this::sortStudentsById);
    }

    // :NOTE: indentation
    private <T> T getLargestByValue(final Stream<Map.Entry<T, Long>> stream,
                                    final Comparator<T> keyComparator, T defaultValue) {
        return stream.max(Comparator.comparing(Map.Entry<T, Long>::getValue)
                        .thenComparing(Map.Entry::getKey, keyComparator))
                .map(Map.Entry::getKey).orElse(defaultValue);
    }

    @Override
    public GroupName getLargestGroup(final Collection<Student> students) {
        return getLargestByValue(
                students.stream()
                        .collect(Collectors.groupingBy(Student::getGroup, Collectors.counting()))
                        .entrySet().stream(),
                Comparator.naturalOrder(), null);

    }

    @Override
    public GroupName getLargestGroupFirstName(final Collection<Student> students) {
        return getLargestByValue((Stream<Map.Entry<GroupName, Long>>) getGroupsStream(students)
                        .map(group -> Map.entry(group.getKey(), (long) getDistinctFirstNames(group.getValue()).size())),
                Comparator.reverseOrder(), null);
    }


    public static <T> Stream<T> getStreamByConditions(final Stream<Student> stream, final Function<Student, T> function) {
        return stream.map(function);
    }

    public static <T> List<T> getObjectsByCondition(final List<Student> students, final Function<Student, T> function) {
        return getStreamByConditions(students.stream(), function).toList();
    }

    // :NOTE: naming
    public static <T> List<T> getObjectsByCondition(final Stream<Student> students, final Function<Student, T> function) {
        return getStreamByConditions(students, function).toList();
    }

    @Override
    public List<String> getFirstNames(final List<Student> students) {
        return getObjectsByCondition(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final List<Student> students) {
        return getObjectsByCondition(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(final List<Student> students) {
        return getObjectsByCondition(students, Student::getGroup);
    }

    private String getFullName(final Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    @Override
    public List<String> getFullNames(final List<Student> students) {
        return getObjectsByCondition(students, this::getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(final List<Student> students) {
        return getStreamByConditions(students.stream(), Student::getFirstName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(final List<Student> students) {
        return students.stream()
                .max(COMPARATOR_BY_ID)
                .map(Student::getFirstName)
                .orElse("");
    }

    private static List<Student> sortStudentBy(final Collection<Student> students, final Comparator<Student> comparator) {
        return students.stream().sorted(comparator).toList();
    }

    @Override
    public List<Student> sortStudentsById(final Collection<Student> students) {
        return sortStudentBy(students, COMPARATOR_BY_ID);
    }

    @Override
    public List<Student> sortStudentsByName(final Collection<Student> students) {
        return sortStudentBy(students, COMPARATOR_BY_STUDENT_NAME);
    }

    private static <T, S> List<S> findStudentsByFunction(
            final Collection<S> students,
            final T obj,
            final Function<S, T> function,
            final Comparator<S> comparator
    ) {
        return students.stream()
                .filter(student -> obj.equals(function.apply(student)))
                .sorted(comparator)
                .toList();
    }


    @Override
    public List<Student> findStudentsByFirstName(final Collection<Student> students, final String name) {
        return findStudentsByFunction(students, name, Student::getFirstName, COMPARATOR_BY_STUDENT_NAME);
    }

    @Override
    public List<Student> findStudentsByLastName(final Collection<Student> students, final String name) {
        return findStudentsByFunction(students, name, Student::getLastName, COMPARATOR_BY_STUDENT_NAME);
    }

    @Override
    public List<Student> findStudentsByGroup(final Collection<Student> students, final GroupName group) {
        return findStudentsByFunction(students, group, Student::getGroup, COMPARATOR_BY_STUDENT_NAME);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(final Collection<Student> students, final GroupName group) {
        return findStudentsByGroup(students, group).stream()
                .collect(Collectors.toMap(Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(Comparator.naturalOrder())));
    }

    @Override
    public String getMostPopularName(final Collection<Student> students) {
        return getLargestByValue(getGroupsStream(students)
                .flatMap(group -> group.getValue().stream()
                        .map(Student::getFirstName)
                        .distinct())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream(), Comparator.reverseOrder(), "");
    }

    private Map<Integer, Student> getMapStudentId(final Collection<Student> students) {
        return students.stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
    }

    private Stream<Student> filterByIds(Map<Integer, Student> studentIds, final int[] ids) {
        return Arrays.stream(ids).mapToObj(studentIds::get);
    }

    private <T> List<T> getByFunctionsWithIds(final Collection<Student> students,
                                              final int[] ids,
                                              final Function<Student, T> function) {
        return getObjectsByCondition(filterByIds(getMapStudentId(students), ids), function);
    }

    @Override
    public List<String> getFirstNames(final Collection<Student> students, final int[] ids) {
        return getByFunctionsWithIds(students, ids, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final Collection<Student> students, final int[] ids) {
        return getByFunctionsWithIds(students, ids, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(final Collection<Student> students, final int[] ids) {
        return getByFunctionsWithIds(students, ids, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(final Collection<Student> students, final int[] ids) {
        return getByFunctionsWithIds(students, ids, this::getFullName);
    }
}
