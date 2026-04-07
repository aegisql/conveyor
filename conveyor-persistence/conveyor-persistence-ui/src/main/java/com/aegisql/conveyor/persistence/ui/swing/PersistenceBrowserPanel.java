package com.aegisql.conveyor.persistence.ui.swing;

import com.aegisql.conveyor.persistence.ui.backend.PersistenceBackend;
import com.aegisql.conveyor.persistence.ui.backend.PersistenceBackendFactory;
import com.aegisql.conveyor.persistence.ui.model.ConnectionStatus;
import com.aegisql.conveyor.persistence.ui.model.PersistenceProfile;
import com.aegisql.conveyor.persistence.ui.model.PersistenceSnapshot;
import com.aegisql.conveyor.persistence.ui.model.SummaryEntry;
import com.aegisql.conveyor.persistence.ui.model.TableData;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class PersistenceBrowserPanel extends JPanel {

    private static final int PREVIEW_LIMIT = 200;
    private static final long STATUS_POLL_SECONDS = 3L;

    private final PersistenceProfile profile;
    private final PersistenceBackend backend;
    private final Executor executor;
    private final ScheduledExecutorService statusScheduler;
    private final Consumer<ConnectionStatus> statusListener;

    private final JTabbedPane tablesPane = new JTabbedPane();
    private final JTextArea cellDetailArea = new JTextArea();
    private final JSplitPane contentSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    private final JButton infoButton = actionButton(WorkbenchIcons.info(), "Connection Info");
    private final JButton previousPageButton = actionButton(WorkbenchIcons.previousPage(), "Previous Page");
    private final JLabel pageLabel = new JLabel("Page 1");
    private final JButton nextPageButton = actionButton(WorkbenchIcons.nextPage(), "Next Page");
    private final JButton refreshButton = actionButton(WorkbenchIcons.refresh(), "Refresh");
    private final JButton initializeButton = actionButton(WorkbenchIcons.initialize(), "Initialize Persistence");
    private final JButton showSqlButton = actionButton(WorkbenchIcons.script(), "Show Initialization SQL");
    private final JButton archiveExpiredButton = actionButton(WorkbenchIcons.archiveExpired(), "Archive Expired Data");
    private final JButton archiveAllButton = actionButton(WorkbenchIcons.archiveAll(), "Archive All Data");
    private PersistenceSnapshot currentSnapshot;
    private int pageIndex;
    private String lastStatusText = "Not connected yet";
    private volatile ConnectionStatus latestStatus = ConnectionStatus.FAILED;
    private final ScheduledFuture<?> statusPollTask;

    public PersistenceBrowserPanel(
            PersistenceProfile profile,
            Executor executor,
            ScheduledExecutorService statusScheduler,
            Consumer<ConnectionStatus> statusListener
    ) {
        super(new BorderLayout(8, 8));
        this.profile = profile.normalized();
        this.backend = PersistenceBackendFactory.forProfile(this.profile);
        this.executor = executor;
        this.statusScheduler = statusScheduler;
        this.statusListener = statusListener == null ? status -> { } : statusListener;

        add(topPanel(), BorderLayout.NORTH);
        add(contentPanel(), BorderLayout.CENTER);
        tablesPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tablesPane.setPreferredSize(new java.awt.Dimension(900, 360));

        wireActions();
        this.statusPollTask = this.statusScheduler.scheduleWithFixedDelay(
                this::pollConnectionStatus,
                STATUS_POLL_SECONDS,
                STATUS_POLL_SECONDS,
                TimeUnit.SECONDS
        );
        refreshSnapshot();
    }

    public PersistenceProfile profile() {
        return profile;
    }

    private JPanel topPanel() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(infoButton);
        actions.add(previousPageButton);
        actions.add(pageLabel);
        actions.add(nextPageButton);
        actions.add(refreshButton);
        actions.add(initializeButton);
        actions.add(showSqlButton);
        actions.add(archiveExpiredButton);
        actions.add(archiveAllButton);
        return actions;
    }

    private JSplitPane contentPanel() {
        cellDetailArea.setEditable(false);
        cellDetailArea.setLineWrap(false);
        cellDetailArea.setWrapStyleWord(false);
        cellDetailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        cellDetailArea.setText("Select a table cell to view the full value here.");
        cellDetailArea.setCaretPosition(0);

        JScrollPane detailScrollPane = new JScrollPane(cellDetailArea);
        detailScrollPane.setBorder(BorderFactory.createTitledBorder("Cell Details"));
        detailScrollPane.setPreferredSize(new Dimension(900, 160));

        contentSplitPane.setTopComponent(tablesPane);
        contentSplitPane.setBottomComponent(detailScrollPane);
        contentSplitPane.setResizeWeight(0.8);
        contentSplitPane.setContinuousLayout(true);
        contentSplitPane.setOneTouchExpandable(true);
        return contentSplitPane;
    }

    private void wireActions() {
        infoButton.addActionListener(e -> showStatusDialog());
        previousPageButton.addActionListener(e -> {
            if (pageIndex > 0) {
                pageIndex--;
                refreshSnapshot();
            }
        });
        nextPageButton.addActionListener(e -> {
            pageIndex++;
            refreshSnapshot();
        });
        refreshButton.addActionListener(e -> refreshSnapshot());
        initializeButton.addActionListener(e -> runAction("Initialize persistence", () -> backend.initialize(profile), true));
        showSqlButton.addActionListener(e -> loadInitializationScript());
        archiveExpiredButton.addActionListener(e -> runAction("Archive expired data", () -> backend.archiveExpired(profile), true));
        archiveAllButton.addActionListener(e -> {
            int answer = JOptionPane.showConfirmDialog(
                    this,
                    "Archive all persisted data for this profile?",
                    "Archive All",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (answer == JOptionPane.OK_OPTION) {
                runAction("Archive all data", () -> backend.archiveAll(profile), true);
            }
        });
    }

    private void refreshSnapshot() {
        setBusy(true, "Loading snapshot...");
        CompletableFuture
                .supplyAsync(() -> backend.inspect(profile, PREVIEW_LIMIT, pageIndex), executor)
                .whenComplete((snapshot, error) -> SwingUtilities.invokeLater(() -> {
                    if (error != null) {
                        setBusy(false, "Load failed");
                        JOptionPane.showMessageDialog(this, rootMessage(error), "Load Failed", JOptionPane.ERROR_MESSAGE);
                    } else {
                        showSnapshot(snapshot);
                    }
                }));
    }

    private void loadInitializationScript() {
        setBusy(true, "Generating initialization script...");
        CompletableFuture
                .supplyAsync(() -> backend.initializationScript(profile), executor)
                .whenComplete((script, error) -> SwingUtilities.invokeLater(() -> {
                    setBusy(false, lastStatusText);
                    if (error != null) {
                        JOptionPane.showMessageDialog(this, rootMessage(error), "Script Generation Failed", JOptionPane.ERROR_MESSAGE);
                    } else {
                        SqlTextDialog.showDialog(this, profile.label() + " Initialization Script", script);
                    }
                }));
    }

    private void runAction(String actionName, Runnable action, boolean refreshAfter) {
        setBusy(true, actionName + "...");
        CompletableFuture.runAsync(action, executor)
                .whenComplete((ignored, error) -> SwingUtilities.invokeLater(() -> {
                    if (error != null) {
                        setBusy(false, actionName + " failed");
                        JOptionPane.showMessageDialog(this, rootMessage(error), actionName + " Failed", JOptionPane.ERROR_MESSAGE);
                    } else if (refreshAfter) {
                        refreshSnapshot();
                    } else {
                        setBusy(false, actionName + " completed");
                    }
                }));
    }

    private void showSnapshot(PersistenceSnapshot snapshot) {
        this.currentSnapshot = snapshot;
        this.pageIndex = snapshot.pageIndex();
        updateConnectionStatus(snapshot.status());
        lastStatusText = statusText(snapshot);
        pageLabel.setText("Page " + (snapshot.pageIndex() + 1));
        rebuildTables(snapshot);
        previousPageButton.setEnabled(snapshot.hasPreviousPage());
        nextPageButton.setEnabled(snapshot.hasNextPage());
        refreshButton.setEnabled(true);
        initializeButton.setEnabled(snapshot.canInitialize());
        showSqlButton.setEnabled(snapshot.canGenerateInitializationScript());
        archiveExpiredButton.setEnabled(snapshot.canArchiveExpired());
        archiveAllButton.setEnabled(snapshot.canArchiveAll());
        updateBusyCursor(false);
    }

    private void rebuildTables(PersistenceSnapshot snapshot) {
        tablesPane.removeAll();
        for (TableData table : snapshot.tables()) {
            JTable jTable = new JTable(table.toTableModel());
            jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            jTable.setCellSelectionEnabled(true);
            jTable.getSelectionModel().addListSelectionListener(e -> updateCellDetailFromSelection(table, jTable));
            jTable.getColumnModel().getSelectionModel().addListSelectionListener(e -> updateCellDetailFromSelection(table, jTable));
            tablesPane.addTab(table.title(), new JScrollPane(jTable));
        }
        if (snapshot.tables().isEmpty()) {
            tablesPane.addTab("No Data", new JScrollPane(new JTextArea("No preview data available for the current state.")));
        }
        resetCellDetail();
    }

    private String formatSummary(PersistenceSnapshot snapshot) {
        StringBuilder builder = new StringBuilder();
        builder.append(snapshot.headline()).append('\n');
        if (!snapshot.detailMessage().isBlank()) {
            builder.append(snapshot.detailMessage()).append("\n\n");
        }
        for (SummaryEntry entry : snapshot.summaryEntries()) {
            builder.append(entry.label()).append(": ").append(entry.value()).append('\n');
        }
        return builder.toString();
    }

    private void setBusy(boolean busy, String text) {
        if (busy) {
            previousPageButton.setEnabled(false);
            nextPageButton.setEnabled(false);
            refreshButton.setEnabled(false);
            initializeButton.setEnabled(false);
            showSqlButton.setEnabled(false);
            archiveExpiredButton.setEnabled(false);
            archiveAllButton.setEnabled(false);
        } else if (currentSnapshot != null) {
            showSnapshot(currentSnapshot);
        } else {
            pageLabel.setText("Page " + (pageIndex + 1));
            previousPageButton.setEnabled(pageIndex > 0);
            nextPageButton.setEnabled(false);
            refreshButton.setEnabled(true);
            initializeButton.setEnabled(false);
            showSqlButton.setEnabled(profile.kind().supportsInitializationScript());
            archiveExpiredButton.setEnabled(false);
            archiveAllButton.setEnabled(false);
        }
        if (currentSnapshot == null) {
            lastStatusText = text;
        }
        updateBusyCursor(busy);
    }

    private void showStatusDialog() {
        PersistenceStatusDialog.showDialog(this, profile.label() + " Info", latestStatus, currentSnapshot, lastStatusText);
    }

    private String statusText(PersistenceSnapshot snapshot) {
        String statusText = '[' + snapshot.status().name() + "] " + snapshot.headline();
        if (!snapshot.detailMessage().isBlank()) {
            statusText += " - " + snapshot.detailMessage();
        }
        return statusText;
    }

    private void pollConnectionStatus() {
        ConnectionStatus status = backend.connectionStatus(profile);
        SwingUtilities.invokeLater(() -> updateConnectionStatus(status));
    }

    private void updateConnectionStatus(ConnectionStatus status) {
        latestStatus = status;
        if (status == ConnectionStatus.FAILED) {
            lastStatusText = "[FAILED] Connection check failed";
        } else if (currentSnapshot == null) {
            lastStatusText = '[' + status.name() + "] Connection check succeeded";
        }
        statusListener.accept(status);
    }

    private void updateBusyCursor(boolean busy) {
        Cursor cursor = busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor();
        setCursor(cursor);
        java.awt.Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof RootPaneContainer container) {
            java.awt.Component glassPane = container.getGlassPane();
            glassPane.setCursor(cursor);
            glassPane.setVisible(busy);
        }
    }

    private JButton actionButton(javax.swing.Icon icon, String tooltip) {
        JButton button = new JButton(icon);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setPreferredSize(new Dimension(40, 40));
        return button;
    }

    public void close() {
        statusPollTask.cancel(true);
    }

    private void resetCellDetail() {
        cellDetailArea.setText("Select a table cell to view the full value here.");
        cellDetailArea.setCaretPosition(0);
    }

    private void updateCellDetailFromSelection(TableData table, JTable jTable) {
        if (jTable.getSelectedRow() < 0 || jTable.getSelectedColumn() < 0) {
            return;
        }
        int row = jTable.convertRowIndexToModel(jTable.getSelectedRow());
        int column = jTable.convertColumnIndexToModel(jTable.getSelectedColumn());
        if (row < 0 || row >= table.rows().size() || column < 0 || column >= table.columns().size()) {
            return;
        }
        String value = table.rows().get(row).get(column);
        StringBuilder builder = new StringBuilder();
        builder.append("Table: ").append(table.title()).append('\n');
        builder.append("Row: ").append(row + 1).append('\n');
        builder.append("Column: ").append(table.columns().get(column)).append('\n');
        builder.append('\n');
        builder.append(value == null ? "" : value);
        cellDetailArea.setText(builder.toString());
        cellDetailArea.setCaretPosition(0);
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.toString() : current.getMessage();
    }
}
