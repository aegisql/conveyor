package com.aegisql.conveyor.doc;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.consumers.result.LastResultReference;
import com.aegisql.conveyor.consumers.scrap.LastScrapReference;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PromptDrivenBankStatementConveyorTest {

    /*
     * This test is an example of using doc/conveyor-authoring-prompt.md as an implementation guide.
     *
     * User specification:
     * - Build a simple bank statement-like report
     * - Template:
     *   Dear {{user name}},
     *   This is a statement for the date period {{range_of_dates}}
     *
     *   List of known transactions:
     *
     *   {{list_of_transactions}}
     *
     *   Remaining balance: {{balance}}
     * - Building parts:
     *   - USER_NAME
     *   - DATE_RANGE
     *   - TRANSACTION (repeatable, one transaction per part)
     *   - BALANCE
     * - Missing completion detail was asked explicitly and provided by the user:
     *   - use NO_MORE_TRANSACTIONS as a label to indicate end of transaction input
     *
     * Design chosen from the prompt:
     * - AssemblingConveyor<String, String, String>
     * - explicit string labels
     * - explicit LabeledValueConsumer
     * - readiness based on required labels + NO_MORE_TRANSACTIONS
     * - result and scrap consumers included
     */

    @Test
    void shouldBuildStatementFromIndividuallySubmittedTransactions() {
        LastResultReference<String, String> resultRef = new LastResultReference<>();
        LastScrapReference<String> scrapRef = new LastScrapReference<>();

        AssemblingConveyor<String, String, String> conveyor = newStatementConveyor(resultRef, scrapRef, Duration.ofSeconds(5));
        try {
            CompletableFuture<String> future = conveyor.build().id("statement-101").createFuture();

            conveyor.part().id("statement-101").label("USER_NAME").value("Jane Doe").place().join();
            conveyor.part().id("statement-101").label("DATE_RANGE").value("2026-03-01 to 2026-03-31").place().join();
            conveyor.part().id("statement-101").label("TRANSACTION").value("2026-03-02 | Coffee shop | -5.50").place().join();
            conveyor.part().id("statement-101").label("TRANSACTION").value("2026-03-10 | Salary | +1200.00").place().join();
            conveyor.part().id("statement-101").label("TRANSACTION").value("2026-03-21 | Rent | -700.00").place().join();
            conveyor.part().id("statement-101").label("BALANCE").value(new BigDecimal("1540.22")).place().join();
            conveyor.part().id("statement-101").label("NO_MORE_TRANSACTIONS").place().join();

            String statement = future.join();

            String expected = """
                    Dear Jane Doe,
                    This is a statement for the date period 2026-03-01 to 2026-03-31

                    List of known transactions:

                    - 2026-03-02 | Coffee shop | -5.50
                    - 2026-03-10 | Salary | +1200.00
                    - 2026-03-21 | Rent | -700.00

                    Remaining balance: 1540.22
                    """;

            assertEquals(expected, statement);
            assertEquals(statement, resultRef.getCurrent());
            assertNull(scrapRef.getCurrent());
        } finally {
            conveyor.completeAndStop().join();
        }
    }

    @Test
    void shouldScrapIncompleteStatementWhenNoMoreTransactionsIsMissing() throws InterruptedException {
        LastResultReference<String, String> resultRef = new LastResultReference<>();
        LastScrapReference<String> scrapRef = new LastScrapReference<>();

        AssemblingConveyor<String, String, String> conveyor = newStatementConveyor(resultRef, scrapRef, Duration.ofMillis(120));
        try {
            conveyor.part().id("statement-102").label("USER_NAME").value("John Doe").place().join();
            conveyor.part().id("statement-102").label("DATE_RANGE").value("2026-04-01 to 2026-04-30").place().join();
            conveyor.part().id("statement-102").label("TRANSACTION").value("2026-04-03 | Grocery store | -74.10").place().join();
            conveyor.part().id("statement-102").label("BALANCE").value(new BigDecimal("991.42")).place().join();

            Thread.sleep(220);
            conveyor.completeAndStop().join();

            ScrapBin<String, Object> scrap = scrapRef.getCurrent();
            assertNotNull(scrap);
            assertEquals("statement-102", scrap.key);
            assertEquals(ScrapBin.FailureType.BUILD_EXPIRED, scrap.failureType);
            assertNull(resultRef.getCurrent());
        } finally {
            conveyor.stop();
        }
    }

    private AssemblingConveyor<String, String, String> newStatementConveyor(
            LastResultReference<String, String> resultRef,
            LastScrapReference<String> scrapRef,
            Duration timeout
    ) {
        AssemblingConveyor<String, String, String> conveyor = new AssemblingConveyor<>();
        conveyor.setName("promptDrivenBankStatement");
        conveyor.setBuilderSupplier(BankStatementBuilder::new);
        conveyor.setDefaultBuilderTimeout(timeout);
        conveyor.setIdleHeartBeat(Duration.ofMillis(25));
        conveyor.setDefaultCartConsumer(statementConsumer());
        conveyor.resultConsumer(resultRef).set();
        conveyor.scrapConsumer(scrapRef).set();
        conveyor.setReadinessEvaluator(
                Conveyor.getTesterFor(conveyor).accepted(
                        "USER_NAME",
                        "DATE_RANGE",
                        "BALANCE",
                        "NO_MORE_TRANSACTIONS"
                )
        );
        return conveyor;
    }

    private LabeledValueConsumer<String, Object, BankStatementBuilder> statementConsumer() {
        return (label, value, builder) -> {
            switch (label) {
                case "USER_NAME" -> builder.setUserName((String) value);
                case "DATE_RANGE" -> builder.setDateRange((String) value);
                case "TRANSACTION" -> builder.addTransaction((String) value);
                case "BALANCE" -> builder.setBalance((BigDecimal) value);
                case "NO_MORE_TRANSACTIONS" -> builder.markTransactionsCompleted();
                default -> throw new IllegalArgumentException("Unknown label " + label);
            }
        };
    }

    private static final class BankStatementBuilder implements Supplier<String> {
        private String userName;
        private String dateRange;
        private final List<String> transactions = new ArrayList<>();
        private BigDecimal balance;
        private boolean transactionsCompleted;

        void setUserName(String userName) {
            this.userName = userName;
        }

        void setDateRange(String dateRange) {
            this.dateRange = dateRange;
        }

        void addTransaction(String transaction) {
            this.transactions.add(transaction);
        }

        void setBalance(BigDecimal balance) {
            this.balance = balance;
        }

        void markTransactionsCompleted() {
            this.transactionsCompleted = true;
        }

        @Override
        public String get() {
            StringBuilder statement = new StringBuilder();
            statement.append("Dear ").append(userName).append(",\n");
            statement.append("This is a statement for the date period ").append(dateRange).append("\n\n");
            statement.append("List of known transactions:\n\n");
            for (String transaction : transactions) {
                statement.append("- ").append(transaction).append("\n");
            }
            statement.append("\n");
            statement.append("Remaining balance: ").append(balance);
            if (!transactionsCompleted) {
                throw new IllegalStateException("Transactions were not explicitly finalized");
            }
            statement.append("\n");
            return statement.toString();
        }
    }
}
