package com.aegisql.conveyor.persistence.ui;

import com.aegisql.conveyor.persistence.ui.store.SqliteProfileStore;
import com.aegisql.conveyor.persistence.ui.swing.PersistenceWorkbenchFrame;

import javax.swing.SwingUtilities;

public final class PersistenceWorkbenchApp {

    private PersistenceWorkbenchApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SqliteProfileStore store = new SqliteProfileStore(SqliteProfileStore.defaultDatabasePath());
            PersistenceWorkbenchFrame frame = new PersistenceWorkbenchFrame(store);
            frame.setVisible(true);
        });
    }
}
