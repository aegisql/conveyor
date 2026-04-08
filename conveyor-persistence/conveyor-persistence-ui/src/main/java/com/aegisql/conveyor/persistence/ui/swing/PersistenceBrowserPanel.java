package com.aegisql.conveyor.persistence.ui.swing;

import com.aegisql.conveyor.persistence.ui.backend.PersistenceBackend;
import com.aegisql.conveyor.persistence.ui.backend.PersistenceBackendFactory;
import com.aegisql.conveyor.persistence.ui.model.ConnectionStatus;
import com.aegisql.conveyor.persistence.ui.model.PersistenceProfile;
import com.aegisql.conveyor.persistence.ui.model.PersistenceSnapshot;
import com.aegisql.conveyor.persistence.ui.model.SummaryEntry;
import com.aegisql.conveyor.persistence.ui.model.TableData;
import com.aegisql.conveyor.persistence.ui.store.SqliteProfileStore;

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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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
    private final SqliteProfileStore store;
    private final PersistenceBackend backend;
    private final Executor executor;
    private final ScheduledExecutorService statusScheduler;
    private final Consumer<ConnectionStatus> statusListener;

    private final JTabbedPane tablesPane = new JTabbedPane();
    private final JTextArea cellDetailArea = new JTextArea();
    private final JLabel cellDetailStatusBar = new JLabel(" ");
    private final JSplitPane contentSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    private final JButton infoButton = actionButton(WorkbenchIcons.info(), "Connection Info");
    private final JButton columnsButton = actionButton(WorkbenchIcons.columns(), "Choose Visible Columns");
    private final JButton previousPageButton = actionButton(WorkbenchIcons.previousPage(), "Previous Page");
    private final JLabel pageLabel = new JLabel("Page 1");
    private final JButton nextPageButton = actionButton(WorkbenchIcons.nextPage(), "Next Page");
    private final JButton refreshButton = actionButton(WorkbenchIcons.refresh(), "Refresh");
    private final JButton initializeButton = actionButton(WorkbenchIcons.initialize(), "Initialize Persistence");
    private final JButton showSqlButton = actionButton(WorkbenchIcons.script(), "Show Initialization Preview");
    private final JButton archiveExpiredButton = actionButton(WorkbenchIcons.archiveExpired(), "Archive Expired Data");
    private final JButton archiveAllButton = actionButton(WorkbenchIcons.archiveAll(), "Archive All Data");
    private PersistenceSnapshot currentSnapshot;
    private int pageIndex;
    private String lastStatusText = "Not connected yet";
    private volatile ConnectionStatus latestStatus = ConnectionStatus.FAILED;
    private final ScheduledFuture<?> statusPollTask;
    private final Map<String, Set<String>> hiddenColumnsByTable;

    public PersistenceBrowserPanel(
            PersistenceProfile profile,
            SqliteProfileStore store,
            Executor executor,
            ScheduledExecutorService statusScheduler,
            Consumer<ConnectionStatus> statusListener
    ) {
        super(new BorderLayout(8, 8));
        this.profile = profile.normalized();
        this.store = store;
        this.backend = PersistenceBackendFactory.forProfile(this.profile);
        this.executor = executor;
        this.statusScheduler = statusScheduler;
        this.statusListener = statusListener == null ? status -> { } : statusListener;
        this.hiddenColumnsByTable = loadHiddenColumns();

        add(topPanel(), BorderLayout.NORTH);
        add(contentPanel(), BorderLayout.CENTER);
        tablesPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tablesPane.setPreferredSize(new java.awt.Dimension(900, 360));
        tablesPane.addChangeListener(e -> updateColumnsButtonState());

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
        actions.add(columnsButton);
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
        cellDetailArea.setText("");

        JScrollPane detailScrollPane = new JScrollPane(cellDetailArea);
        detailScrollPane.setPreferredSize(new Dimension(900, 160));

        JPanel detailPanel = new JPanel(new BorderLayout());
        detailPanel.add(detailScrollPane, BorderLayout.CENTER);
        cellDetailStatusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new java.awt.Color(209, 213, 219)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        detailPanel.add(cellDetailStatusBar, BorderLayout.SOUTH);

        contentSplitPane.setTopComponent(tablesPane);
        contentSplitPane.setBottomComponent(detailPanel);
        contentSplitPane.setResizeWeight(0.8);
        contentSplitPane.setContinuousLayout(true);
        contentSplitPane.setOneTouchExpandable(true);
        return contentSplitPane;
    }

    private void wireActions() {
        infoButton.addActionListener(e -> showStatusDialog());
        columnsButton.addActionListener(e -> chooseVisibleColumns());
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
        setBusy(true, "Generating initialization preview...");
        CompletableFuture
                .supplyAsync(() -> backend.initializationPreview(profile), executor)
                .whenComplete((preview, error) -> SwingUtilities.invokeLater(() -> {
                    setBusy(false, lastStatusText);
                    if (error != null) {
                        JOptionPane.showMessageDialog(this, rootMessage(error), "Preview Generation Failed", JOptionPane.ERROR_MESSAGE);
                    } else {
                        SqlTextDialog.showDialog(this, profile.label() + " Initialization Preview", preview);
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
        updateColumnsButtonState();
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
        int selectedIndex = tablesPane.getSelectedIndex();
        tablesPane.removeAll();
        int firstRowNumber = snapshot.pageIndex() * PREVIEW_LIMIT + 1;
        for (TableData table : snapshot.tables()) {
            TableData filteredTable = applyColumnPreferences(table.withRowNumbers(firstRowNumber));
            JTable jTable = new JTable(filteredTable.toTableModel());
            WorkbenchTables.style(jTable);
            jTable.setCellSelectionEnabled(true);
            jTable.getSelectionModel().addListSelectionListener(e -> updateCellDetailFromSelection(filteredTable, jTable));
            jTable.getColumnModel().getSelectionModel().addListSelectionListener(e -> updateCellDetailFromSelection(filteredTable, jTable));
            tablesPane.addTab(filteredTable.title(), new JScrollPane(jTable));
        }
        if (snapshot.tables().isEmpty()) {
            tablesPane.addTab("No Data", new JScrollPane(new JTextArea("No preview data available for the current state.")));
            tablesPane.setSelectedIndex(0);
        } else if (selectedIndex >= 0 && selectedIndex < tablesPane.getTabCount()) {
            tablesPane.setSelectedIndex(selectedIndex);
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
            columnsButton.setEnabled(false);
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
            updateColumnsButtonState();
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
        PersistenceStatusDialog.showDialog(
                this,
                profile.label() + " Info",
                latestStatus,
                currentSnapshot,
                lastStatusText,
                store.credentialStoreDisplayName()
        );
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

    private Map<String, Set<String>> loadHiddenColumns() {
        if (profile.id() == null) {
            return new HashMap<>();
        }
        return new HashMap<>(store.loadHiddenColumns(profile.id()));
    }

    private TableData applyColumnPreferences(TableData table) {
        Set<String> hiddenColumns = hiddenColumnsByTable.get(table.title());
        if (hiddenColumns == null || hiddenColumns.isEmpty() || hiddenColumns.size() >= table.columns().size()) {
            return table;
        }
        return table.filteredColumns(hiddenColumns);
    }

    private void chooseVisibleColumns() {
        TableData sourceTable = selectedSourceTable();
        if (sourceTable == null) {
            return;
        }
        Set<String> hiddenColumns = new LinkedHashSet<>(hiddenColumnsByTable.getOrDefault(sourceTable.title(), Set.of()));
        Set<String> visibleColumns = new LinkedHashSet<>(sourceTable.columns());
        visibleColumns.removeAll(hiddenColumns);
        ColumnVisibilityDialog.chooseVisibleColumns(this, sourceTable.title(), sourceTable.columns(), visibleColumns)
                .ifPresent(selectedColumns -> {
                    if (selectedColumns.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "At least one column must remain visible.",
                                "Invalid Column Selection",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                    }
                    Set<String> updatedHiddenColumns = new LinkedHashSet<>(sourceTable.columns());
                    updatedHiddenColumns.removeAll(selectedColumns);
                    hiddenColumnsByTable.put(sourceTable.title(), updatedHiddenColumns);
                    if (profile.id() != null) {
                        store.saveHiddenColumns(profile.id(), sourceTable.title(), updatedHiddenColumns);
                    }
                    rebuildTables(currentSnapshot);
                    updateColumnsButtonState();
                });
    }

    private TableData selectedSourceTable() {
        if (currentSnapshot == null || currentSnapshot.tables().isEmpty()) {
            return null;
        }
        int selectedIndex = tablesPane.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= currentSnapshot.tables().size()) {
            return null;
        }
        int firstRowNumber = currentSnapshot.pageIndex() * PREVIEW_LIMIT + 1;
        return currentSnapshot.tables().get(selectedIndex).withRowNumbers(firstRowNumber);
    }

    private void updateColumnsButtonState() {
        TableData selectedTable = selectedSourceTable();
        columnsButton.setEnabled(selectedTable != null && !selectedTable.columns().isEmpty());
    }

    private void resetCellDetail() {
        cellDetailArea.setText("");
        cellDetailStatusBar.setText(" ");
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
        cellDetailArea.setText(value == null ? "" : value);
        cellDetailStatusBar.setText("Table: " + table.title() + "    Row: " + (row + 1) + "    Column: " + table.columns().get(column));
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
