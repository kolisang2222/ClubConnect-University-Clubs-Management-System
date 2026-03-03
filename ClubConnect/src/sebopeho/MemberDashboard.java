package sebopeho;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;
import moetso.Club;
import moetso.Membership;
import moetso.User;
import moetso.Event;
import phethiso.ClubDAO;
import phethiso.MembershipDAO;
import phethiso.EventDAO;
import phethiso.NotificationDAO;

public class MemberDashboard extends JFrame {

    private JTable tableClubs, tableEvents, tableAnnouncements;
    private DefaultTableModel clubTableModel, eventTableModel, announcementTableModel;
    private JButton btnJoinClub, btnRSVPEvent, btnViewProfile, btnSubmitFeedback, btnSearchClubs, btnViewEventHistory;
    private JTextField txtSearch;
    private User user;

    /**
     * Constructor for MemberDashboard.
     * @param user The logged-in user with Member role.
     */
    public MemberDashboard(User user) {
        this.user = user;
        setTitle("ClubConnect - Member Dashboard");
        setSize(950, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main panel with gradient background
        JPanel mainPanel = new GradientPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        setContentPane(mainPanel);

        // Title
        JLabel titleLabel = new JLabel("Member Dashboard - " + user.getUsername(), SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(40, 40, 40));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setOpaque(false);
        txtSearch = new JTextField(20);
        styleInputComponent(txtSearch);
        btnSearchClubs = new CustomButton("Search Clubs");
        searchPanel.add(createStyledLabel("Search Clubs:"));
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearchClubs);
        mainPanel.add(searchPanel, BorderLayout.NORTH);

        // Club table
        clubTableModel = new DefaultTableModel(new Object[]{"ID", "Name", "Category", "Mission", "Status"}, 0);
        tableClubs = new JTable(clubTableModel);
        styleTable(tableClubs);
        refreshClubTable();

        // Event table
        eventTableModel = new DefaultTableModel(new Object[]{"ID", "Title", "Date", "Time", "Venue", "Type", "RSVP Status"}, 0);
        tableEvents = new JTable(eventTableModel);
        styleTable(tableEvents);
        refreshEventTable();

        // Announcement table
        announcementTableModel = new DefaultTableModel(new Object[]{"ID", "Club", "Message", "Sent At"}, 0);
        tableAnnouncements = new JTable(announcementTableModel);
        styleTable(tableAnnouncements);
        refreshAnnouncementTable();

        // Split pane for tables
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tableClubs), new JScrollPane(tableEvents)),
            new JScrollPane(tableAnnouncements));
        splitPane.setDividerLocation(300);
        ((JSplitPane) splitPane.getTopComponent()).setDividerLocation(150);
        splitPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // Buttons panel
        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panelButtons.setOpaque(false);
        btnJoinClub = new CustomButton("Join Club");
        btnRSVPEvent = new CustomButton("RSVP Event");
        btnViewProfile = new CustomButton("View Profile");
        btnSubmitFeedback = new CustomButton("Submit Feedback");
        btnViewEventHistory = new CustomButton("View Event History");

        panelButtons.add(btnJoinClub);
        panelButtons.add(btnRSVPEvent);
        panelButtons.add(btnViewProfile);
        panelButtons.add(btnSubmitFeedback);
        panelButtons.add(btnViewEventHistory);

        mainPanel.add(panelButtons, BorderLayout.SOUTH);

        // Button actions
        btnJoinClub.addActionListener(e -> joinClub());
        btnRSVPEvent.addActionListener(e -> rsvpEvent());
        btnViewProfile.addActionListener(e -> viewProfile());
        btnSubmitFeedback.addActionListener(e -> submitFeedback());
        btnSearchClubs.addActionListener(e -> searchClubs());
        btnViewEventHistory.addActionListener(e -> viewEventHistory());

        setVisible(true);
    }

    /**
     * Styles a JTable with consistent formatting.
     * @param table The table to style.
     */
    private void styleTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(30);
        table.setGridColor(new Color(200, 200, 200));
        table.setShowGrid(true);
        table.setSelectionBackground(new Color(33, 150, 243));
        table.setSelectionForeground(Color.WHITE);

        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(240, 240, 240));
        table.getTableHeader().setForeground(new Color(40, 40, 40));
        table.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 245, 245));
                }
                return c;
            }
        };
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    /**
     * Refreshes the club table with all active clubs.
     */
    private void refreshClubTable() {
        clubTableModel.setRowCount(0);
        List<Club> clubs = ClubDAO.getAllClubs();
        for (Club c : clubs) {
            if (c.getStatus().equals("Active")) {
                clubTableModel.addRow(new Object[]{c.getId(), c.getName(), c.getCategory(), c.getMission(), c.getStatus()});
            }
        }
    }

    /**
     * Refreshes the event table with events from joined clubs.
     */
    private void refreshEventTable() {
        eventTableModel.setRowCount(0);
        List<Event> events = EventDAO.getEventsByUser(user.getId());
        for (Event e : events) {
            String rsvpStatus = EventDAO.hasRSVPed(user.getId(), e.getId()) ? "RSVPed" : "Not RSVPed";
            eventTableModel.addRow(new Object[]{e.getId(), e.getTitle(), e.getDate(), e.getTime(), e.getVenue(), e.getType(), rsvpStatus});
        }
    }

    /**
     * Refreshes the announcement table with notifications from joined clubs.
     */
    private void refreshAnnouncementTable() {
        announcementTableModel.setRowCount(0);
        List<Notification> notifications = NotificationDAO.getNotificationsByUser(user.getId());
        for (Notification n : notifications) {
            Club c = ClubDAO.getClubById(n.getClubId());
            announcementTableModel.addRow(new Object[]{n.getId(), c.getName(), n.getMessage(), n.getSentAt()});
        }
    }

    /**
     * Handles club membership application.
     */
    private void joinClub() {
        int row = tableClubs.getSelectedRow();
        if (row >= 0) {
            int clubId = (int) tableClubs.getValueAt(row, 0);
            try {
                if (MembershipDAO.isMember(user.getId(), clubId)) {
                    JOptionPane.showMessageDialog(this, "You are already a member or have a pending application!", "Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                MembershipDAO.applyForMembership(user.getId(), clubId);
                JOptionPane.showMessageDialog(this, "Membership application submitted!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshClubTable();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error applying for membership: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Select a club first!", "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Handles event RSVP.
     */
    private void rsvpEvent() {
        int row = tableEvents.getSelectedRow();
        if (row >= 0) {
            int eventId = (int) tableEvents.getValueAt(row, 0);
            try {
                if (EventDAO.hasRSVPed(user.getId(), eventId)) {
                    JOptionPane.showMessageDialog(this, "You have already RSVPed to this event!", "Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (EventDAO.isEventFull(eventId)) {
                    JOptionPane.showMessageDialog(this, "Event is at capacity!", "Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                EventDAO.rsvpEvent(user.getId(), eventId);
                JOptionPane.showMessageDialog(this, "RSVP submitted!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshEventTable();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error RSVPing to event: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Select an event first!", "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Displays and allows editing of user profile.
     */
    private void viewProfile() {
        JTextField txtUsername = new JTextField(user.getUsername());
        JTextField txtEmail = new JTextField(user.getEmail());
        JPasswordField txtPassword = new JPasswordField();
        txtUsername.setEditable(false);
        styleInputComponent(txtUsername);
        styleInputComponent(txtEmail);
        styleInputComponent(txtPassword);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createStyledLabel("Username:"), gbc);
        gbc.gridx = 1;
        panel.add(txtUsername, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(createStyledLabel("Email:"), gbc);
        gbc.gridx = 1;
        panel.add(txtEmail, gbc);
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(createStyledLabel("New Password (optional):"), gbc);
        gbc.gridx = 1;
        panel.add(txtPassword, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "User Profile", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String email = txtEmail.getText().trim();
            String password = new String(txtPassword.getPassword());
            try {
                if (!email.isEmpty() && !email.matches("^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,6}$")) {
                    JOptionPane.showMessageDialog(this, "Invalid email address!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (!email.isEmpty()) {
                    user.setEmail(email);
                }
                if (!password.isEmpty()) {
                    user.setPassword(hashPassword(password));
                    UserDAO.updatePassword(user.getId(), user.getPassword());
                }
                UserDAO.updateUser(user);
                JOptionPane.showMessageDialog(this, "Profile updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error updating profile: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Handles feedback submission for a club.
     */
    private void submitFeedback() {
        int row = tableClubs.getSelectedRow();
        if (row >= 0) {
            int clubId = (int) tableClubs.getValueAt(row, 0);
            JTextArea txtFeedback = new JTextArea(5, 20);
            styleTextArea(txtFeedback);

            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(Color.WHITE);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0; gbc.gridy = 0;
            panel.add(createStyledLabel("Feedback:"), gbc);
            gbc.gridx = 1;
            panel.add(new JScrollPane(txtFeedback), gbc);

            int result = JOptionPane.showConfirmDialog(this, panel, "Submit Feedback", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String feedback = txtFeedback.getText().trim();
                if (feedback.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Feedback is required!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                try {
                    NotificationDAO.submitFeedback(user.getId(), clubId, feedback);
                    JOptionPane.showMessageDialog(this, "Feedback submitted!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Error submitting feedback: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Select a club first!", "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Searches for clubs based on user input.
     */
    private void searchClubs() {
        String query = txtSearch.getText().trim();
        if (query.isEmpty()) {
            refreshClubTable();
            return;
        }
        try {
            clubTableModel.setRowCount(0);
            List<Club> clubs = ClubDAO.searchClubs(query);
            for (Club c : clubs) {
                if (c.getStatus().equals("Active")) {
                    clubTableModel.addRow(new Object[]{c.getId(), c.getName(), c.getCategory(), c.getMission(), c.getStatus()});
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error searching clubs: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Displays event history for the user.
     */
    private void viewEventHistory() {
        DefaultTableModel historyModel = new DefaultTableModel(new Object[]{"ID", "Title", "Date", "Time", "Venue", "Type", "Attended"}, 0);
        JTable historyTable = new JTable(historyModel);
        styleTable(historyTable);

        List<Event> events = EventDAO.getEventHistory(user.getId());
        for (Event e : events) {
            boolean attended = EventDAO.getAttendanceStatus(user.getId(), e.getId());
            historyModel.addRow(new Object[]{e.getId(), e.getTitle(), e.getDate(), e.getTime(), e.getVenue(), e.getType(), attended});
        }

        JOptionPane.showMessageDialog(this, new JScrollPane(historyTable), "Event History", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Hashes a password using SHA-256.
     * @param password The password to hash.
     * @return The hashed password.
     */
    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Password hashing failed: " + e.getMessage());
        }
    }

    /**
     * Styles a text field for consistent UI.
     * @param field The text field to style.
     */
    private void styleInputComponent(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 150, 150), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
    }

    /**
     * Styles a text area for consistent UI.
     * @param area The text area to style.
     */
    private void styleTextArea(JTextArea area) {
        area.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        area.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 150, 150), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
    }

    /**
     * Styles a combo box for consistent UI.
     * @param combo The combo box to style.
     */
    private void styleComboBox(JComboBox<?> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setBackground(Color.WHITE);
        combo.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 1));
    }

    /**
     * Creates a styled label for consistent UI.
     * @param text The label text.
     * @return The styled JLabel.
     */
    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(new Color(40, 40, 40));
        return label;
    }

    /**
     * Custom button with hover effect and rounded corners.
     */
    private class CustomButton extends JButton {
        private Color normalColor = new Color(33, 150, 243);
        private Color hoverColor = new Color(25, 118, 210);
        private Color textColor = Color.WHITE;

        public CustomButton(String text) {
            super(text);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setForeground(textColor);
            setBackground(normalColor);
            setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setBackground(hoverColor);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setBackground(normalColor);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            super.paintComponent(g);
            g2.dispose();
        }
    }

    /**
     * Gradient panel for background.
     */
    private class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gp = new GradientPaint(
                0, 0, new Color(240, 248, 255),
                0, getHeight(), new Color(255, 255, 255)
            );
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /**
     * Main method for testing.
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new MemberDashboard(new User("member", "member@example.com", "", "Member")));
    }
}