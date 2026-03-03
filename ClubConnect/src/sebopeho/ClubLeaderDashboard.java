package sebopeho;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.io.FileWriter;
import java.io.IOException;
import moetso.Club;
import moetso.Membership;
import moetso.User;
import moetso.Event;
import phethiso.ClubDAO;
import phethiso.MembershipDAO;
import phethiso.UserDAO;
import phethiso.EventDAO;

public class ClubLeaderDashboard extends JFrame {

    private JTable tableMembers, tableEvents;
    private DefaultTableModel memberTableModel, eventTableModel;
    private JButton btnEditClub, btnCreateEvent, btnMarkAttendance, btnSendNotification, btnExportAttendance;
    private User user;

    public ClubLeaderDashboard(User user) {
        this.user = user;
        setTitle("ClubConnect - Club Leader Dashboard");
        setSize(950, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new GradientPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        setContentPane(mainPanel);

        JLabel titleLabel = new JLabel("Club Leader Dashboard - " + user.getUsername(), SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(40, 40, 40));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        memberTableModel = new DefaultTableModel(new Object[]{"ID", "Username", "Email", "Status"}, 0);
        tableMembers = new JTable(memberTableModel);
        styleTable(tableMembers);
        refreshMemberTable();

        eventTableModel = new DefaultTableModel(new Object[]{"ID", "Title", "Date", "Time", "Venue", "Type", "Budget Status"}, 0);
        tableEvents = new JTable(eventTableModel);
        styleTable(tableEvents);
        refreshEventTable();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tableMembers), new JScrollPane(tableEvents));
        splitPane.setDividerLocation(300);
        splitPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(splitPane, BorderLayout.CENTER);

        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panelButtons.setOpaque(false);
        btnEditClub = new CustomButton("Edit Club");
        btnCreateEvent = new CustomButton("Create Event");
        btnMarkAttendance = new CustomButton("Mark Attendance");
        btnSendNotification = new CustomButton("Send Notification");
        btnExportAttendance = new CustomButton("Export Attendance");

        panelButtons.add(btnEditClub);
        panelButtons.add(btnCreateEvent);
        panelButtons.add(btnMarkAttendance);
        panelButtons.add(btnSendNotification);
        panelButtons.add(btnExportAttendance);

        mainPanel.add(panelButtons, BorderLayout.SOUTH);

        btnEditClub.addActionListener(e -> editClub());
        btnCreateEvent.addActionListener(e -> createEvent());
        btnMarkAttendance.addActionListener(e -> markAttendance());
        btnSendNotification.addActionListener(e -> sendNotification());
        btnExportAttendance.addActionListener(e -> exportAttendance());

        // Background thread for reminders
        new Thread(() -> {
            while (true) {
                try {
                    List<Event> upcomingEvents = EventDAO.getUpcomingEvents(user.getId());
                    for (Event e : upcomingEvents) {
                        JOptionPane.showMessageDialog(this, "Reminder: Event " + e.getTitle() + " on " + e.getDate());
                    }
                    Thread.sleep(24 * 60 * 60 * 1000); // Check daily
                } catch (InterruptedException ex) {
                    System.err.println("Reminder thread interrupted: " + ex.getMessage());
                }
            }
        }).start();

        setVisible(true);
    }

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

    private void refreshMemberTable() {
        memberTableModel.setRowCount(0);
        List<Membership> members = MembershipDAO.getMembersByClub(ClubDAO.getClubByLeader(user.getId()).getId());
        for (Membership m : members) {
            User u = UserDAO.getUserById(m.getUserId());
            memberTableModel.addRow(new Object[]{u.getId(), u.getUsername(), u.getEmail(), m.getStatus()});
        }
    }

    private void refreshEventTable() {
        eventTableModel.setRowCount(0);
        List<Event> events = EventDAO.getEventsByClub(ClubDAO.getClubByLeader(user.getId()).getId());
        for (Event e : events) {
            eventTableModel.addRow(new Object[]{e.getId(), e.getTitle(), e.getDate(), e.getTime(), e.getVenue(), e.getType(), e.getBudgetStatus()});
        }
    }

    private void editClub() {
        Club club = ClubDAO.getClubByLeader(user.getId());
        if (club == null) {
            JOptionPane.showMessageDialog(this, "No club assigned!", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField txtName = new JTextField(club.getName());
        JTextArea txtMission = new JTextArea(club.getMission());
        JComboBox<String> comboCategory = new JComboBox<>(ClubDAO.getAllCategories().toArray(new String[0]));
        comboCategory.setSelectedItem(club.getCategory());

        styleInputComponent(txtName);
        styleTextArea(txtMission);
        styleComboBox(comboCategory);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createStyledLabel("Club Name:"), gbc);
        gbc.gridx = 1;
        panel.add(txtName, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(createStyledLabel("Category:"), gbc);
        gbc.gridx = 1;
        panel.add(comboCategory, gbc);
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(createStyledLabel("Mission:"), gbc);
        gbc.gridx = 1;
        panel.add(new JScrollPane(txtMission), gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Club", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String name = txtName.getText().trim();
            String category = (String) comboCategory.getSelectedItem();
            String mission = txtMission.getText().trim();

            if (name.isEmpty() || mission.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            club.setName(name);
            club.setCategory(category);
            club.setMission(mission);
            ClubDAO.updateClub(club);
            refreshMemberTable();
        }
    }

    private void createEvent() {
        JTextField txtTitle = new JTextField();
        JTextField txtDate = new JTextField("YYYY-MM-DD");
        JTextField txtTime = new JTextField("HH:MM");
        JTextField txtVenue = new JTextField();
        JTextField txtDescription = new JTextField();
        JTextField txtCapacity = new JTextField();
        JComboBox<String> comboType = new JComboBox<>(new String[]{"Meeting", "Seating", "Event"});
        JCheckBox chkBudget = new JCheckBox("Request Budget");
        JTextField txtBudgetAmount = new JTextField();

        styleInputComponent(txtTitle);
        styleInputComponent(txtDate);
        styleInputComponent(txtTime);
        styleInputComponent(txtVenue);
        styleInputComponent(txtDescription);
        styleInputComponent(txtCapacity);
        styleComboBox(comboType);
        styleInputComponent(txtBudgetAmount);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createStyledLabel("Title:"), gbc);
        gbc.gridx = 1;
        panel.add(txtTitle, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(createStyledLabel("Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        panel.add(txtDate, gbc);
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(createStyledLabel("Time (HH:MM):"), gbc);
        gbc.gridx = 1;
        panel.add(txtTime, gbc);
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(createStyledLabel("Venue:"), gbc);
        gbc.gridx = 1;
        panel.add(txtVenue, gbc);
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(createStyledLabel("Description:"), gbc);
        gbc.gridx = 1;
        panel.add(txtDescription, gbc);
        gbc.gridx = 0; gbc.gridy = 5;
        panel.add(createStyledLabel("Capacity:"), gbc);
        gbc.gridx = 1;
        panel.add(txtCapacity, gbc);
        gbc.gridx = 0; gbc.gridy = 6;
        panel.add(createStyledLabel("Type:"), gbc);
        gbc.gridx = 1;
        panel.add(comboType, gbc);
        gbc.gridx = 0; gbc.gridy = 7;
        panel.add(chkBudget, gbc);
        gbc.gridx = 0; gbc.gridy = 8;
        panel.add(createStyledLabel("Budget Amount:"), gbc);
        gbc.gridx = 1;
        panel.add(txtBudgetAmount, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Create Event", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String title = txtTitle.getText().trim();
                String dateStr = txtDate.getText().trim();
                String timeStr = txtTime.getText().trim();
                String venue = txtVenue.getText().trim();
                String description = txtDescription.getText().trim();
                int capacity = Integer.parseInt(txtCapacity.getText().trim());
                String type = (String) comboType.getSelectedItem();
                boolean budgetRequested = chkBudget.isSelected();
                double budgetAmount = budgetRequested ? Double.parseDouble(txtBudgetAmount.getText().trim()) : 0.0;

                if (title.isEmpty() || dateStr.isEmpty() || timeStr.isEmpty() || venue.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "All fields are required!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                Date date = Date.valueOf(LocalDate.parse(dateStr));
                Time time = Time.valueOf(LocalTime.parse(timeStr));
                int clubId = ClubDAO.getClubByLeader(user.getId()).getId();

                // Check for scheduling conflicts
                if (EventDAO.hasConflict(venue, date, time)) {
                    JOptionPane.showMessageDialog(this, "Venue is booked at this time!", "Conflict", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                Event event = new Event(clubId, title, date, time, venue, description, capacity, type, budgetRequested, budgetAmount);
                EventDAO.createEvent(event);
                refreshEventTable();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void markAttendance() {
        int row = tableEvents.getSelectedRow();
        if (row >= 0) {
            int eventId = (int) tableEvents.getValueAt(row, 0);
            List<Membership> members = MembershipDAO.getMembersByClub(ClubDAO.getClubByLeader(user.getId()).getId());
            DefaultTableModel attendanceModel = new DefaultTableModel(new Object[]{"User ID", "Username", "Attended"}, 0);
            for (Membership m : members) {
                User u = UserDAO.getUserById(m.getUserId());
                attendanceModel.addRow(new Object[]{u.getId(), u.getUsername(), false});
            }
            JTable attendanceTable = new JTable(attendanceModel);
            styleTable(attendanceTable);

            int result = JOptionPane.showConfirmDialog(this, new JScrollPane(attendanceTable), "Mark Attendance", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                for (int i = 0; i < attendanceModel.getRowCount(); i++) {
                    int userId = (int) attendanceModel.getValueAt(i, 0);
                    boolean attended = (boolean) attendanceModel.getValueAt(i, 2);
                    EventDAO.markAttendance(eventId, userId, attended);
                }
                JOptionPane.showMessageDialog(this, "Attendance marked!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Select an event first!", "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void sendNotification() {
        JTextArea txtMessage = new JTextArea(5, 20);
        styleTextArea(txtMessage);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createStyledLabel("Message:"), gbc);
        gbc.gridx = 1;
        panel.add(new JScrollPane(txtMessage), gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Send Notification", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String message = txtMessage.getText().trim();
            if (message.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Message is required!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            new Thread(() -> {
                List<Membership> members = MembershipDAO.getMembersByClub(ClubDAO.getClubByLeader(user.getId()).getId());
                for (Membership m : members) {
                    User u = UserDAO.getUserById(m.getUserId());
                    // Simulate email sending
                    System.out.println("Sending notification to " + u.getEmail() + ": " + message);
                }
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Notifications sent!", "Success", JOptionPane.INFORMATION_MESSAGE));
            }).start();
        }
    }

    private void exportAttendance() {
        int row = tableEvents.getSelectedRow();
        if (row >= 0) {
            int eventId = (int) tableEvents.getValueAt(row, 0);
            List<Membership> members = EventDAO.getAttendance(eventId);
            JTextField txtFilePath = new JTextField();
            styleInputComponent(txtFilePath);
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(Color.WHITE);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0; gbc.gridy = 0;
            panel.add(createStyledLabel("Enter CSV file path:"), gbc);
            gbc.gridx = 1;
            panel.add(txtFilePath, gbc);

            int result = JOptionPane.showConfirmDialog(this, panel, "Export Attendance", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String filePath = txtFilePath.getText().trim();
                if (filePath.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter a file path!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                try (FileWriter writer = new FileWriter(filePath)) {
                    writer.write("User ID,Username,Attended\n");
                    for (Membership m : members) {
                        User u = UserDAO.getUserById(m.getUserId());
                        writer.write(String.format("%d,%s,%b\n", m.getUserId(), u.getUsername(), m.getAttended()));
                    }
                    JOptionPane.showMessageDialog(this, "Attendance exported successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Select an event first!", "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void styleInputComponent(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 150, 150), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
    }

    private void styleTextArea(JTextArea area) {
        area.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        area.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 150, 150), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
    }

    private void styleComboBox(JComboBox<?> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setBackground(Color.WHITE);
        combo.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 1));
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(new Color(40, 40, 40));
        return label;
    }

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

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new ClubLeaderDashboard(new User("leader", "leader@example.com", "", "ClubLeader")));
    }
}