package sebopeho;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.FileWriter;
import java.io.IOException;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import moetso.Club;
import moetso.Membership;
import moetso.User;
import phethiso.ClubDAO;
import phethiso.MembershipDAO;
import phethiso.UserDAO;
import moetso.AttendanceReport;

public class AdminDashboard extends JFrame {

    private JTable tableClubs, tableMembers;
    private DefaultTableModel clubTableModel, memberTableModel;
    private JButton btnCreateClub, btnEditClub, btnDeleteClub, btnApproveClub, btnExportMembers, btnSearchMembers, btnApproveBudget, btnGenerateReport;
    private JTextField txtSearch;

    public AdminDashboard(User user) {
        setTitle("ClubConnect - Admin Dashboard");
        setSize(950, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new GradientPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        setContentPane(mainPanel);

        JLabel titleLabel = new JLabel("Admin Dashboard", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(40, 40, 40));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Member search
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setOpaque(false);
        txtSearch = new JTextField(20);
        styleInputComponent(txtSearch);
        btnSearchMembers = new CustomButton("Search Members");
        searchPanel.add(createStyledLabel("Search Members:"));
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearchMembers);

        // Club table
        clubTableModel = new DefaultTableModel(new Object[]{"ID", "Name", "Category", "Mission", "Status", "Leader"}, 0);
        tableClubs = new JTable(clubTableModel);
        styleTable(tableClubs);
        refreshClubTable();

        // Member table
        memberTableModel = new DefaultTableModel(new Object[]{"ID", "Username", "Email", "Role", "Clubs"}, 0);
        tableMembers = new JTable(memberTableModel);
        styleTable(tableMembers);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tableClubs), new JScrollPane(tableMembers));
        splitPane.setDividerLocation(300);
        splitPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(splitPane, BorderLayout.CENTER);

        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panelButtons.setOpaque(false);
        btnCreateClub = new CustomButton("Create Club");
        btnEditClub = new CustomButton("Edit Club");
        btnDeleteClub = new CustomButton("Archive Club");
        btnApproveClub = new CustomButton("Approve Club");
        btnApproveBudget = new CustomButton("Approve Budget");
        btnExportMembers = new CustomButton("Export Members CSV");
        btnGenerateReport = new CustomButton("Generate Report");

        panelButtons.add(btnCreateClub);
        panelButtons.add(btnEditClub);
        panelButtons.add(btnDeleteClub);
        panelButtons.add(btnApproveClub);
        panelButtons.add(btnApproveBudget);
        panelButtons.add(btnExportMembers);
        panelButtons.add(btnGenerateReport);

        mainPanel.add(panelButtons, BorderLayout.SOUTH);
        mainPanel.add(searchPanel, BorderLayout.NORTH);

        btnCreateClub.addActionListener(e -> createClub());
        btnEditClub.addActionListener(e -> editClub());
        btnDeleteClub.addActionListener(e -> deleteClub());
        btnApproveClub.addActionListener(e -> approveClub());
        btnApproveBudget.addActionListener(e -> approveBudget());
        btnExportMembers.addActionListener(e -> exportMembersCSV());
        btnGenerateReport.addActionListener(e -> generateReport());
        btnSearchMembers.addActionListener(e -> searchMembers());

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

    private void refreshClubTable() {
        clubTableModel.setRowCount(0);
        List<Club> clubs = ClubDAO.getAllClubs();
        for (Club c : clubs) {
            clubTableModel.addRow(new Object[]{
                c.getId(), c.getName(), c.getCategory(), c.getMission(), c.getStatus(), c.getLeaderUsername()
            });
        }
    }

    private void refreshMemberTable(List<User> users) {
        memberTableModel.setRowCount(0);
        for (User u : users) {
            String clubs = String.join(", ", MembershipDAO.getClubsByUser(u.getId()));
            memberTableModel.addRow(new Object[]{u.getId(), u.getUsername(), u.getEmail(), u.getRole(), clubs});
        }
    }

    private void createClub() {
        JTextField txtName = new JTextField();
        JTextArea txtMission = new JTextArea(3, 15);
        JComboBox<String> comboCategory = new JComboBox<>(getExistingCategories());
        JComboBox<String> comboLeader = new JComboBox<>(getAvailableLeaders());

        styleInputComponent(txtName);
        styleTextArea(txtMission);
        styleComboBox(comboCategory);
        styleComboBox(comboLeader);

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
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(createStyledLabel("Leader:"), gbc);
        gbc.gridx = 1;
        panel.add(comboLeader, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Create Club", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String name = txtName.getText().trim();
            String category = (String) comboCategory.getSelectedItem();
            String mission = txtMission.getText().trim();
            String leaderUsername = (String) comboLeader.getSelectedItem();

            if (name.isEmpty() || mission.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int leaderId = UserDAO.getUserIdByUsername(leaderUsername);
            Club club = new Club(name, category, mission, leaderId);
            ClubDAO.createClub(club);
            refreshClubTable();
        }
    }

    private void editClub() {
        int row = tableClubs.getSelectedRow();
        if (row >= 0) {
            int clubId = (int) tableClubs.getValueAt(row, 0);
            JTextField txtName = new JTextField((String) tableClubs.getValueAt(row, 1));
            JTextArea txtMission = new JTextArea((String) tableClubs.getValueAt(row, 3));
            JComboBox<String> comboCategory = new JComboBox<>(getExistingCategories());
            JComboBox<String> comboLeader = new JComboBox<>(getAvailableLeaders());
            comboCategory.setSelectedItem(tableClubs.getValueAt(row, 2));
            comboLeader.setSelectedItem(tableClubs.getValueAt(row, 5));

            styleInputComponent(txtName);
            styleTextArea(txtMission);
            styleComboBox(comboCategory);
            styleComboBox(comboLeader);

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
            gbc.gridx = 0; gbc.gridy = 3;
            panel.add(createStyledLabel("Leader:"), gbc);
            gbc.gridx = 1;
            panel.add(comboLeader, gbc);

            int result = JOptionPane.showConfirmDialog(this, panel, "Edit Club", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String name = txtName.getText().trim();
                String category = (String) comboCategory.getSelectedItem();
                String mission = txtMission.getText().trim();
                String leaderUsername = (String) comboLeader.getSelectedItem();
                int leaderId = UserDAO.getUserIdByUsername(leaderUsername);

                if (name.isEmpty() || mission.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "All fields are required!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                Club club = new Club(clubId, name, category, mission, leaderId, leaderUsername, "Pending");
                ClubDAO.updateClub(club);
                refreshClubTable();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Select a club first!", "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void deleteClub() {
        int row = tableClubs.getSelectedRow();
        if (row >= 0) {
            int clubId = (int) tableClubs.getValueAt(row, 0);
            ClubDAO.deleteClub(clubId);
            refreshClubTable();
        } else {
            JOptionPane.showMessageDialog(this, "Select a club first!", "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void approveClub() {
        int row = tableClubs.getSelectedRow();
        if (row >= 0) {
            int clubId = (int) tableClubs.getValueAt(row, 0);
            ClubDAO.approveClub(clubId);
            refreshClubTable();
        } else {
            JOptionPane.showMessageDialog(this, "Select a club first!", "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void approveBudget() {
        int row = tableClubs.getSelectedRow();
        if (row >= 0) {
            int clubId = (int) tableClubs.getValueAt(row, 0);
            List<Event> pendingBudgets = ClubDAO.getPendingBudgetEvents(clubId);
            if (pendingBudgets.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No pending budget requests!", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            DefaultTableModel budgetModel = new DefaultTableModel(new Object[]{"Event ID", "Title", "Budget Amount"}, 0);
            for (Event e : pendingBudgets) {
                budgetModel.addRow(new Object[]{e.getId(), e.getTitle(), e.getBudgetAmount()});
            }
            JTable budgetTable = new JTable(budgetModel);
            styleTable(budgetTable);

            int result = JOptionPane.showConfirmDialog(this, new JScrollPane(budgetTable), "Approve Budgets", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION && budgetTable.getSelectedRow() >= 0) {
                int eventId = (int) budgetTable.getValueAt(budgetTable.getSelectedRow(), 0);
                ClubDAO.approveBudget(eventId);
                JOptionPane.showMessageDialog(this, "Budget approved!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Select a club first!", "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void exportMembersCSV() {
        int row = tableClubs.getSelectedRow();
        if (row >= 0) {
            int clubId = (int) tableClubs.getValueAt(row, 0);
            List<Membership> members = MembershipDAO.getMembersByClub(clubId);
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

            int result = JOptionPane.showConfirmDialog(this, panel, "Export Members", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String filePath = txtFilePath.getText().trim();
                if (filePath.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter a file path!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                try (FileWriter writer = new FileWriter(filePath)) {
                    writer.write("User ID,Username,Email,Status\n");
                    for (Membership m : members) {
                        User u = UserDAO.getUserById(m.getUserId());
                        writer.write(String.format("%d,%s,%s,%s\n", m.getUserId(), u.getUsername(), u.getEmail(), m.getStatus()));
                    }
                    JOptionPane.showMessageDialog(this, "Members exported successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Select a club first!", "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void searchMembers() {
        String query = txtSearch.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a search term!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<User> users = UserDAO.searchMembers(query);
        refreshMemberTable(users);
    }

    private void generateReport() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtFilePath = new JTextField();
        styleInputComponent(txtFilePath);
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createStyledLabel("Enter PDF file path:"), gbc);
        gbc.gridx = 1;
        panel.add(txtFilePath, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Generate Report", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String filePath = txtFilePath.getText().trim();
            if (filePath.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a file path!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                PdfWriter writer = new PdfWriter(filePath);
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);
                document.add(new Paragraph("ClubConnect Financial Report"));
                for (Club c : ClubDAO.getAllClubs()) {
                    document.add(new Paragraph("Club: " + c.getName()));
                    List<Budget> budgets = ClubDAO.getBudgets(c.getId());
                    for (Budget b : budgets) {
                        document.add(new Paragraph(String.format("%s: %.2f (%s)", b.getDescription(), b.getAmount(), b.getType())));
                    }
                }
                document.close();
                JOptionPane.showMessageDialog(this, "Report generated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Report generation failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String[] getExistingCategories() {
        List<String> categories = ClubDAO.getAllCategories();
        return categories.toArray(new String[0]);
    }

    private String[] getAvailableLeaders() {
        List<String> leaders = UserDAO.getAllLeaderUsernames();
        return leaders.toArray(new String[0]);
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
        SwingUtilities.invokeLater(() -> new AdminDashboard(new User("admin", "admin@example.com", "", "Admin")));
    }
}