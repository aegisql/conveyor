package com.aegisql.conveyor.utils.jdbc;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class JdbcOperationExecutorsTest {

    private record Person(String name, int age) {}

    @Test
    public void nonPooledExecutorReusesConnectionAndDoesNotCloseDuringExecution() {
        RecordingJdbc jdbc = new RecordingJdbc();
        SharedConnectionJdbcOperationExecutor<Person> executor = new SharedConnectionJdbcOperationExecutor<>(
                new JdbcOperationConfig<>(
                        jdbc.supplier(),
                        "insert into person(name, age) values (?, ?)",
                        List.of(Person::name, Person::age)
                )
        );

        executor.execute(new Person("John", 10));
        executor.execute(new Person("Jane", 20));

        assertEquals(1, jdbc.connectionsOpened.get());
        assertEquals(0, jdbc.connectionsClosed.get());
        assertEquals(List.of("John", 10), jdbc.executedParameters.get(0));
        assertEquals(List.of("Jane", 20), jdbc.executedParameters.get(1));
    }

    @Test
    public void pooledExecutorClosesAfterEachSingleExecution() {
        RecordingJdbc jdbc = new RecordingJdbc();
        PooledConnectionJdbcOperationExecutor<Person> executor = new PooledConnectionJdbcOperationExecutor<>(
                new JdbcOperationConfig<>(
                        jdbc.supplier(),
                        "insert into person(name, age) values (?, ?)",
                        List.of(Person::name, Person::age)
                )
        );

        executor.execute(new Person("John", 10));
        executor.execute(new Person("Jane", 20));

        assertEquals(2, jdbc.connectionsOpened.get());
        assertEquals(2, jdbc.connectionsClosed.get());
    }

    @Test
    public void executesBatchAndKeepsMapperOrder() {
        RecordingJdbc jdbc = new RecordingJdbc();
        SharedConnectionJdbcOperationExecutor<Person> executor = new SharedConnectionJdbcOperationExecutor<>(
                new JdbcOperationConfig<>(
                        jdbc.supplier(),
                        "insert into person(age, name) values (?, ?)",
                        List.of(Person::age, Person::name)
                )
        );

        executor.executeBatch(List.of(
                new Person("A", 1),
                new Person("B", 2),
                new Person("C", 3)
        ));

        assertEquals(1, jdbc.connectionsOpened.get());
        assertEquals(0, jdbc.connectionsClosed.get());
        assertEquals(1, jdbc.batchExecutions.get());
        assertEquals(3, jdbc.addBatchCalls.get());
        assertEquals(List.of(1, "A"), jdbc.executedParameters.get(0));
        assertEquals(List.of(2, "B"), jdbc.executedParameters.get(1));
        assertEquals(List.of(3, "C"), jdbc.executedParameters.get(2));
    }

    @Test
    public void pooledExecutesBatchAndKeepsMapperOrder() {
        RecordingJdbc jdbc = new RecordingJdbc();
        PooledConnectionJdbcOperationExecutor<Person> executor = new PooledConnectionJdbcOperationExecutor<>(
                new JdbcOperationConfig<>(
                        jdbc.supplier(),
                        "insert into person(age, name) values (?, ?)",
                        List.of(Person::age, Person::name)
                )
        );

        executor.executeBatch(List.of(
                new Person("A", 1),
                new Person("B", 2),
                new Person("C", 3)
        ));

        assertEquals(1, jdbc.connectionsOpened.get());
        assertEquals(1, jdbc.connectionsClosed.get());
        assertEquals(1, jdbc.batchExecutions.get());
        assertEquals(3, jdbc.addBatchCalls.get());
        assertEquals(List.of(1, "A"), jdbc.executedParameters.get(0));
        assertEquals(List.of(2, "B"), jdbc.executedParameters.get(1));
        assertEquals(List.of(3, "C"), jdbc.executedParameters.get(2));
    }

    @Test
    public void commitsWhenAutoCommitDisabled() {
        RecordingJdbc jdbc = new RecordingJdbc();
        jdbc.autoCommit = false;
        SharedConnectionJdbcOperationExecutor<Person> executor = new SharedConnectionJdbcOperationExecutor<>(
                new JdbcOperationConfig<>(
                        jdbc.supplier(),
                        "insert into person(name) values (?)",
                        List.of(Person::name)
                )
        );

        executor.execute(new Person("John", 1));
        assertEquals(1, jdbc.commits.get());
        assertEquals(0, jdbc.rollbacks.get());

        executor.executeBatch(List.of(new Person("A", 1), new Person("B", 2)));
        assertEquals(2, jdbc.commits.get());
        assertEquals(0, jdbc.rollbacks.get());
    }

    @Test
    public void rollsBackWhenExecuteFailsAndAutoCommitDisabled() {
        RecordingJdbc jdbc = new RecordingJdbc();
        jdbc.autoCommit = false;
        jdbc.failOnExecute = true;
        SharedConnectionJdbcOperationExecutor<Person> executor = new SharedConnectionJdbcOperationExecutor<>(
                new JdbcOperationConfig<>(
                        jdbc.supplier(),
                        "insert into person(name) values (?)",
                        List.of(Person::name)
                )
        );

        JdbcExecutionException ex = assertThrows(JdbcExecutionException.class, () -> executor.execute(new Person("X", 1)));
        assertTrue(ex.getMessage().contains("Failed to execute JDBC statement"));
        assertEquals(1, jdbc.rollbacks.get());
    }

    @Test
    public void noConnectionOpenedForEmptyBatch() {
        RecordingJdbc jdbc = new RecordingJdbc();
        PooledConnectionJdbcOperationExecutor<Person> executor = new PooledConnectionJdbcOperationExecutor<>(
                new JdbcOperationConfig<>(
                        jdbc.supplier(),
                        "insert into person(name) values (?)",
                        List.of(Person::name)
                )
        );

        executor.executeBatch(List.of());
        assertEquals(0, jdbc.connectionsOpened.get());
    }

    @Test
    public void configValidatesRequiredFields() {
        List<Function<Person, ?>> mappers = List.of(Person::name);
        assertThrows(NullPointerException.class, () -> new JdbcOperationConfig<Person>(null, "sql", mappers));
        assertThrows(NullPointerException.class, () -> new JdbcOperationConfig<>(() -> null, null, mappers));
        assertThrows(NullPointerException.class, () -> new JdbcOperationConfig<>(() -> null, "sql", null));
    }

    private static final class RecordingJdbc {
        final AtomicInteger connectionsOpened = new AtomicInteger();
        final AtomicInteger connectionsClosed = new AtomicInteger();
        final AtomicInteger statementsClosed = new AtomicInteger();
        final AtomicInteger commits = new AtomicInteger();
        final AtomicInteger rollbacks = new AtomicInteger();
        final AtomicInteger addBatchCalls = new AtomicInteger();
        final AtomicInteger batchExecutions = new AtomicInteger();
        final List<List<Object>> executedParameters = new ArrayList<>();
        volatile boolean failOnExecute;
        volatile boolean autoCommit = true;

        Supplier<Connection> supplier() {
            return this::openConnection;
        }

        private Connection openConnection() {
            connectionsOpened.incrementAndGet();
            AtomicBoolean closed = new AtomicBoolean(false);
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "prepareStatement" -> {
                            if (closed.get()) {
                                throw new SQLException("Connection is closed");
                            }
                            yield createStatement();
                        }
                        case "close" -> {
                            closed.set(true);
                            connectionsClosed.incrementAndGet();
                            yield null;
                        }
                        case "isClosed" -> closed.get();
                        case "getAutoCommit" -> autoCommit;
                        case "commit" -> {
                            commits.incrementAndGet();
                            yield null;
                        }
                        case "rollback" -> {
                            rollbacks.incrementAndGet();
                            yield null;
                        }
                        case "toString" -> "RecordingConnection";
                        case "isWrapperFor" -> false;
                        case "unwrap" -> null;
                        default -> throw new UnsupportedOperationException("Connection method is not supported in tests: " + method.getName());
                    }
            );
        }

        private PreparedStatement createStatement() {
            Map<Integer, Object> params = new HashMap<>();
            ArrayList<List<Object>> batch = new ArrayList<>();
            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setObject" -> {
                            params.put((Integer) args[0], args[1]);
                            yield null;
                        }
                        case "addBatch" -> {
                            addBatchCalls.incrementAndGet();
                            batch.add(snapshot(params));
                            params.clear();
                            yield null;
                        }
                        case "executeUpdate" -> {
                            if (failOnExecute) {
                                throw new SQLException("Synthetic execute failure");
                            }
                            executedParameters.add(snapshot(params));
                            yield 1;
                        }
                        case "executeBatch" -> {
                            if (failOnExecute) {
                                throw new SQLException("Synthetic execute failure");
                            }
                            batchExecutions.incrementAndGet();
                            executedParameters.addAll(batch);
                            int[] result = new int[batch.size()];
                            for (int i = 0; i < result.length; i++) {
                                result[i] = 1;
                            }
                            yield result;
                        }
                        case "close" -> {
                            statementsClosed.incrementAndGet();
                            yield null;
                        }
                        case "toString" -> "RecordingPreparedStatement";
                        case "isWrapperFor" -> false;
                        case "unwrap" -> null;
                        default -> throw new UnsupportedOperationException("PreparedStatement method is not supported in tests: " + method.getName());
                    }
            );
        }

        private List<Object> snapshot(Map<Integer, Object> params) {
            int max = params.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
            ArrayList<Object> ordered = new ArrayList<>(max);
            for (int i = 1; i <= max; i++) {
                ordered.add(params.get(i));
            }
            return ordered;
        }
    }
}
