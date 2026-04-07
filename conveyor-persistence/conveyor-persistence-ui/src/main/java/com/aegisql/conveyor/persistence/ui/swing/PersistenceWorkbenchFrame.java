package com.aegisql.conveyor.persistence.ui.swing;

import com.aegisql.conveyor.persistence.ui.model.ConnectionStatus;
import com.aegisql.conveyor.persistence.ui.model.PersistenceKind;
import com.aegisql.conveyor.persistence.ui.model.PersistenceProfile;
import com.aegisql.conveyor.persistence.ui.store.SqliteProfileStore;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class PersistenceWorkbenchFrame extends javax.swing.JFrame {

    private final SqliteProfileStore store;
    private final ExecutorService executor;
    private final ScheduledExecutorService statusScheduler;
    private final DefaultListModel<PersistenceProfile> profileModel = new DefaultListModel<>();
    private final JList<PersistenceProfile> profileList = new JList<>(profileModel);
    private final JTabbedPane tabs = new JTabbedPane();
    private final Map<Long, PersistenceBrowserPanel> openPanels = new HashMap<>();
    private final Map<Long, StatusTabTitle> tabTitles = new HashMap<>();

    public PersistenceWorkbenchFrame(SqliteProfileStore store) {
        super("Conveyor Persistence Workbench");
        this.store = store;
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "conveyor-persistence-ui");
            thread.setDaemon(true);
            return thread;
        });
        this.statusScheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "conveyor-persistence-ui-status");
            thread.setDaemon(true);
            return thread;
        });
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(content(), BorderLayout.CENTER);
        loadProfiles();
    }

    private JSplitPane content() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel(), tabs);
        splitPane.setDividerLocation(320);
        return splitPane;
    }

    private JPanel leftPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        profileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        profileList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            javax.swing.JLabel label = new javax.swing.JLabel(value.label() + " [" + value.kind().displayName() + "]");
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            } else {
                label.setBackground(list.getBackground());
                label.setForeground(list.getForeground());
            }
            return label;
        });
        profileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedProfile();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowProfileContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowProfileContextMenu(e);
            }
        });
        panel.add(new JScrollPane(profileList), BorderLayout.CENTER);
        panel.add(buttonBar(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buttonBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton newButton = actionButton(WorkbenchIcons.add(), "New Connection");
        newButton.addActionListener(e -> createProfile());
        JButton editButton = actionButton(WorkbenchIcons.edit(), "Edit Connection");
        editButton.addActionListener(e -> editSelectedProfile());
        JButton duplicateButton = actionButton(WorkbenchIcons.duplicate(), "Duplicate Connection");
        duplicateButton.addActionListener(e -> duplicateSelectedProfile());
        JButton deleteButton = actionButton(WorkbenchIcons.delete(), "Delete Connection");
        deleteButton.addActionListener(e -> deleteSelectedProfile());
        JButton openButton = actionButton(WorkbenchIcons.connect(), "Connect / Open");
        openButton.addActionListener(e -> openSelectedProfile());
        JButton refreshButton = actionButton(WorkbenchIcons.refresh(), "Reload Profiles");
        refreshButton.addActionListener(e -> loadProfiles());
        panel.add(newButton);
        panel.add(editButton);
        panel.add(duplicateButton);
        panel.add(deleteButton);
        panel.add(openButton);
        panel.add(refreshButton);
        return panel;
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

    private void createProfile() {
        ProfileEditorDialog.edit(this, PersistenceProfile.defaults(PersistenceKind.SQLITE)).ifPresent(profile -> {
            PersistenceProfile saved = store.save(profile);
            loadProfiles();
            selectProfile(saved.id());
        });
    }

    private void editSelectedProfile() {
        editProfile(profileList.getSelectedValue());
    }

    private void editProfile(PersistenceProfile selected) {
        if (selected == null) {
            return;
        }
        ProfileEditorDialog.edit(this, selected).ifPresent(profile -> {
            PersistenceProfile saved = store.save(profile.withId(selected.id()));
            loadProfiles();
            selectProfile(saved.id());
            closeTab(saved.id());
        });
    }

    private void duplicateSelectedProfile() {
        PersistenceProfile selected = profileList.getSelectedValue();
        if (selected == null) {
            return;
        }
        ProfileEditorDialog.edit(this, selected.duplicateAsNew()).ifPresent(profile -> {
            PersistenceProfile saved = store.save(profile);
            loadProfiles();
            selectProfile(saved.id());
        });
    }

    private void deleteSelectedProfile() {
        deleteProfile(profileList.getSelectedValue());
    }

    private void deleteProfile(PersistenceProfile selected) {
        if (selected == null || selected.id() == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(
                this,
                "Delete profile '" + selected.label() + "'?",
                "Delete Profile",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (answer == JOptionPane.OK_OPTION) {
            store.delete(selected.id());
            closeTab(selected.id());
            loadProfiles();
        }
    }

    private void openSelectedProfile() {
        openProfile(profileList.getSelectedValue());
    }

    private void openProfile(PersistenceProfile selected) {
        if (selected == null || selected.id() == null) {
            return;
        }
        PersistenceBrowserPanel existing = openPanels.get(selected.id());
        if (existing != null) {
            tabs.setSelectedComponent(existing);
            return;
        }
        PersistenceBrowserPanel panel = new PersistenceBrowserPanel(selected, executor, statusScheduler, status -> updateTabStatus(selected.id(), status));
        openPanels.put(selected.id(), panel);
        tabs.addTab(selected.label(), panel);
        int index = tabs.indexOfComponent(panel);
        StatusTabTitle title = new StatusTabTitle(
                selected.label(),
                () -> tabs.setSelectedComponent(panel),
                () -> closeTab(selected.id()),
                event -> showTabContextMenu(selected.id(), event)
        );
        tabTitles.put(selected.id(), title);
        tabs.setTabComponentAt(index, title);
        tabs.setSelectedComponent(panel);
    }

    private void closeTab(Long id) {
        PersistenceBrowserPanel panel = openPanels.remove(id);
        tabTitles.remove(id);
        if (panel != null) {
            panel.close();
            tabs.remove(panel);
        }
    }

    private void updateTabStatus(Long profileId, ConnectionStatus status) {
        StatusTabTitle title = tabTitles.get(profileId);
        if (title != null) {
            title.setStatus(status);
        }
    }

    private void maybeShowProfileContextMenu(MouseEvent event) {
        if (!event.isPopupTrigger()) {
            return;
        }
        int index = profileList.locationToIndex(event.getPoint());
        if (index < 0) {
            return;
        }
        java.awt.Rectangle bounds = profileList.getCellBounds(index, index);
        if (bounds == null || !bounds.contains(event.getPoint())) {
            return;
        }
        profileList.setSelectedIndex(index);
        PersistenceProfile selected = profileList.getModel().getElementAt(index);
        JPopupMenu menu = new JPopupMenu();
        JMenuItem openItem = new JMenuItem("Open");
        openItem.addActionListener(e -> openProfile(selected));
        JMenuItem editItem = new JMenuItem("Edit");
        editItem.addActionListener(e -> editProfile(selected));
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e -> deleteProfile(selected));
        menu.add(openItem);
        menu.add(editItem);
        menu.add(deleteItem);
        menu.show(profileList, event.getX(), event.getY());
    }

    private void showTabContextMenu(Long profileId, MouseEvent event) {
        if (!event.isPopupTrigger()) {
            return;
        }
        PersistenceBrowserPanel panel = openPanels.get(profileId);
        if (panel != null) {
            tabs.setSelectedComponent(panel);
        }
        JPopupMenu menu = new JPopupMenu();
        JMenuItem closeItem = new JMenuItem("Close");
        closeItem.addActionListener(e -> closeTab(profileId));
        menu.add(closeItem);
        menu.show(event.getComponent(), event.getX(), event.getY());
    }

    private void loadProfiles() {
        profileModel.clear();
        for (PersistenceProfile profile : store.listProfiles()) {
            profileModel.addElement(profile);
        }
    }

    private void selectProfile(Long id) {
        if (id == null) {
            return;
        }
        for (int i = 0; i < profileModel.size(); i++) {
            PersistenceProfile profile = profileModel.getElementAt(i);
            if (id.equals(profile.id())) {
                profileList.setSelectedIndex(i);
                profileList.ensureIndexIsVisible(i);
                break;
            }
        }
    }

    @Override
    public void dispose() {
        openPanels.values().forEach(PersistenceBrowserPanel::close);
        statusScheduler.shutdownNow();
        executor.shutdownNow();
        super.dispose();
    }

    private static final class StatusTabTitle extends JPanel {
        private static final Color READY_COLOR = new Color(34, 139, 34);
        private static final Color UNINITIALIZED_COLOR = new Color(214, 158, 46);
        private static final Color FAILED_COLOR = new Color(192, 57, 43);

        private final JLabel dotLabel = new JLabel("\u25cf");

        private StatusTabTitle(
                String title,
                Runnable selectAction,
                Runnable closeAction,
                java.util.function.Consumer<MouseEvent> popupAction
        ) {
            super(new FlowLayout(FlowLayout.LEFT, 6, 0));
            setOpaque(false);
            dotLabel.setFont(dotLabel.getFont().deriveFont(Font.BOLD, 14f));
            JLabel titleLabel = new JLabel(title);
            add(dotLabel);
            add(titleLabel);
            JButton closeButton = new JButton(WorkbenchIcons.closeTab());
            closeButton.setToolTipText("Close / Disconnect");
            closeButton.setFocusable(false);
            closeButton.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            closeButton.setContentAreaFilled(false);
            closeButton.setOpaque(false);
            closeButton.setPreferredSize(new Dimension(22, 22));
            closeButton.addActionListener(e -> closeAction.run());
            add(closeButton);
            MouseAdapter popupListener = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    selectAction.run();
                    popupAction.accept(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    selectAction.run();
                    popupAction.accept(e);
                }
            };
            addMouseListener(popupListener);
            dotLabel.addMouseListener(popupListener);
            titleLabel.addMouseListener(popupListener);
            setStatus(ConnectionStatus.FAILED);
        }

        private void setStatus(ConnectionStatus status) {
            dotLabel.setForeground(switch (status) {
                case READY -> READY_COLOR;
                case CONNECTED_UNINITIALIZED -> UNINITIALIZED_COLOR;
                case FAILED -> FAILED_COLOR;
            });
        }
    }
}
