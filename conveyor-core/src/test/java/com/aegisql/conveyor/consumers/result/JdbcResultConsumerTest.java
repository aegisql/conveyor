package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.Status;
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
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class JdbcResultConsumerTest {

    private record Person(String name, int age) {}

    private static <K, V> ProductBin<K, V> productBin(K key, V product) {
        return new ProductBin<>(null, key, product, 123L, Status.READY, new HashMap<>(Map.of("source", "unit")), null);
    }

    @Test
    public void jdbcProductConsumerUsesSharedConnectionWithoutClosingIt() {
        RecordingJdbc jdbc = new RecordingJdbc();
        JdbcProductResultConsumer<String, Person> consumer = new JdbcProductResultConsumer<>(
                jdbc.supplier(),
                "insert into person(name, age) values (?, ?)",
                Person::name,
                Person::age
        );

        consumer.accept(productBin("A", new Person("John", 30)));
        consumer.accept(productBin("B", new Person("Jane", 31)));

        assertEquals(1, jdbc.connectionsOpened.get());
        assertEquals(0, jdbc.connectionsClosed.get());
        assertEquals(2, jdbc.executedParameters.size());
        assertEquals(List.of("John", 30), jdbc.executedParameters.get(0));
        assertEquals(List.of("Jane", 31), jdbc.executedParameters.get(1));
    }

    @Test
    public void pooledProductConsumerClosesConnectionAfterEachOperation() {
        RecordingJdbc jdbc = new RecordingJdbc();
        PooledJdbcProductResultConsumer<String, Person> consumer = new PooledJdbcProductResultConsumer<>(
                jdbc.supplier(),
                "update person set age=? where name=?",
                Person::age,
                Person::name
        );

        consumer.accept(productBin("A", new Person("John", 30)));
        consumer.accept(productBin("B", new Person("Jane", 31)));

        assertEquals(2, jdbc.connectionsOpened.get());
        assertEquals(2, jdbc.connectionsClosed.get());
        assertEquals(List.of(30, "John"), jdbc.executedParameters.get(0));
        assertEquals(List.of(31, "Jane"), jdbc.executedParameters.get(1));
    }

    @Test
    public void productConsumerExpandsListIntoMultipleExecutions() {
        RecordingJdbc jdbc = new RecordingJdbc();
        JdbcProductResultConsumer<String, Person> consumer = new JdbcProductResultConsumer<>(
                jdbc.supplier(),
                "insert into person(name, age) values (?, ?)",
                Person::name,
                Person::age
        );

        ProductBin<String, List<Person>> listBin = productBin(
                "batch-1",
                List.of(new Person("A", 10), new Person("B", 11), new Person("C", 12))
        );

        @SuppressWarnings("unchecked")
        ProductBin<String, Person> casted = (ProductBin<String, Person>) (ProductBin<?, ?>) listBin;
        consumer.accept(casted);

        assertEquals(3, jdbc.executedParameters.size());
        assertEquals(List.of("A", 10), jdbc.executedParameters.get(0));
        assertEquals(List.of("B", 11), jdbc.executedParameters.get(1));
        assertEquals(List.of("C", 12), jdbc.executedParameters.get(2));
        assertEquals(1, jdbc.batchExecutions.get());
        assertEquals(3, jdbc.addBatchCalls.get());
    }

    @Test
    public void binConsumerRepackagesListElementsKeepingBinMetadata() {
        RecordingJdbc jdbc = new RecordingJdbc();
        JdbcBinResultConsumer<String, Object> consumer = new JdbcBinResultConsumer<>(
                jdbc.supplier(),
                "insert into result(key,status,exp,source,name) values (?,?,?,?,?)",
                bin -> bin.key,
                bin -> bin.status.name(),
                bin -> bin.expirationTime,
                bin -> bin.properties.get("source"),
                bin -> ((Person) bin.product).name()
        );

        ProductBin<String, Object> listBin = productBin(
                "k1",
                List.of(new Person("M", 1), new Person("N", 2))
        );
        consumer.accept(listBin);

        assertEquals(2, jdbc.executedParameters.size());
        assertEquals(List.of("k1", "READY", 123L, "unit", "M"), jdbc.executedParameters.get(0));
        assertEquals(List.of("k1", "READY", 123L, "unit", "N"), jdbc.executedParameters.get(1));
    }

    @Test
    public void pooledBinConsumerClosesConnectionAfterBatchOperation() {
        RecordingJdbc jdbc = new RecordingJdbc();
        PooledJdbcBinResultConsumer<String, Object> consumer = new PooledJdbcBinResultConsumer<>(
                jdbc.supplier(),
                "insert into result(name) values (?)",
                bin -> ((Person) bin.product).name()
        );

        ProductBin<String, Object> listBin = productBin(
                "k2",
                List.of(new Person("X", 9), new Person("Y", 8), new Person("Z", 7))
        );
        consumer.accept(listBin);

        assertEquals(1, jdbc.connectionsOpened.get());
        assertEquals(1, jdbc.connectionsClosed.get());
        assertEquals(3, jdbc.executedParameters.size());
        assertEquals(1, jdbc.batchExecutions.get());
    }

    @Test
    public void wrapsSqlExceptionsWithRuntimeException() {
        RecordingJdbc jdbc = new RecordingJdbc();
        jdbc.failOnExecute = true;
        JdbcProductResultConsumer<String, Person> consumer = new JdbcProductResultConsumer<>(
                jdbc.supplier(),
                "insert into person(name) values (?)",
                Person::name
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () -> consumer.accept(productBin("A", new Person("Err", 0))));
        assertTrue(ex.getMessage().contains("Failed to execute JDBC statement"));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof SQLException);
    }

    @Test
    public void commitsWhenAutoCommitIsDisabledForSingleExecution() {
        RecordingJdbc jdbc = new RecordingJdbc();
        jdbc.autoCommit = false;
        JdbcProductResultConsumer<String, Person> consumer = new JdbcProductResultConsumer<>(
                jdbc.supplier(),
                "insert into person(name) values (?)",
                Person::name
        );

        consumer.accept(productBin("A", new Person("John", 30)));

        assertEquals(1, jdbc.commits.get());
        assertEquals(0, jdbc.rollbacks.get());
    }

    @Test
    public void commitsWhenAutoCommitIsDisabledForBatchExecution() {
        RecordingJdbc jdbc = new RecordingJdbc();
        jdbc.autoCommit = false;
        JdbcProductResultConsumer<String, Person> consumer = new JdbcProductResultConsumer<>(
                jdbc.supplier(),
                "insert into person(name, age) values (?, ?)",
                Person::name,
                Person::age
        );

        ProductBin<String, List<Person>> listBin = productBin(
                "batch-2",
                List.of(new Person("A", 1), new Person("B", 2))
        );
        @SuppressWarnings("unchecked")
        ProductBin<String, Person> casted = (ProductBin<String, Person>) (ProductBin<?, ?>) listBin;
        consumer.accept(casted);

        assertEquals(1, jdbc.batchExecutions.get());
        assertEquals(1, jdbc.commits.get());
        assertEquals(0, jdbc.rollbacks.get());
    }

    private static final class RecordingJdbc {
        final AtomicInteger connectionsOpened = new AtomicInteger();
        final AtomicInteger connectionsClosed = new AtomicInteger();
        final AtomicInteger statementsClosed = new AtomicInteger();
        final AtomicInteger commits = new AtomicInteger();
        final AtomicInteger rollbacks = new AtomicInteger();
        final AtomicInteger addBatchCalls = new AtomicInteger();
        final AtomicInteger batchExecutions = new AtomicInteger();
        final List<String> executedSql = new ArrayList<>();
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
                            yield createStatement((String) args[0]);
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

        private PreparedStatement createStatement(String sql) {
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
                            executedSql.add(sql);
                            executedParameters.add(snapshot(params));
                            yield 1;
                        }
                        case "executeBatch" -> {
                            if (failOnExecute) {
                                throw new SQLException("Synthetic execute failure");
                            }
                            batchExecutions.incrementAndGet();
                            executedSql.add(sql);
                            executedParameters.addAll(batch);
                            int[] res = new int[batch.size()];
                            for (int i = 0; i < res.length; i++) {
                                res[i] = 1;
                            }
                            yield res;
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
