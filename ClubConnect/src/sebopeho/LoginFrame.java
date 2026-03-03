package sebopeho;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import moetso.User;
import phethiso.UserDAO;
import thusang.PasswordUtils;

public class LoginFrame extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin, btnRegister, btnForgotPassword;
    private static Connection dbConnection;

    public LoginFrame() {
        initializeDatabase();
        setTitle("ClubConnect - Login");
        setSize(500, 350);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new GradientPanel();
        mainPanel.setLayout(new GridBagLayout());
        setContentPane(mainPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("ClubConnect", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(40, 40, 40));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        JLabel lblUsername = new JLabel("Username:");
        lblUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblUsername.setForeground(new Color(40, 40, 40));
        mainPanel.add(lblUsername, gbc);

        txtUsername = new JTextField(20);
        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtUsername.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 150, 150), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        gbc.gridx = 1;
        mainPanel.add(txtUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        JLabel lblPassword = new JLabel("Password:");
        lblPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblPassword.setForeground(new Color(40, 40, 40));
        mainPanel.add(lblPassword, gbc);

        txtPassword = new JPasswordField(20);
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 150, 150), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        gbc.gridx = 1;
        mainPanel.add(txtPassword, gbc);

        btnLogin = new CustomButton("Login");
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        mainPanel.add(btnLogin, gbc);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        bottomPanel.setOpaque(false);
        btnRegister = new CustomButton("Register");
        btnForgotPassword = new CustomButton("Forgot Password");
        bottomPanel.add(btnRegister);
        bottomPanel.add(btnForgotPassword);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        mainPanel.add(bottomPanel, gbc);

        btnLogin.addActionListener(e -> loginAction());
        btnRegister.addActionListener(e -> registerAction());
        btnForgotPassword.addActionListener(e -> forgotPasswordAction());

        // Export database on close
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                new Thread(() -> exportDatabase()).start();
            }
        });

        setVisible(true);
    }

    private void initializeDatabase() {
        try {
            dbConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/JohnDoe12345678?useSSL=false", "root", "");
            // Run SQL setup script
            try (InputStream is = Files.newInputStream(Paths.get("database_setup.sql"))) {
                String sql = new String(Files.readAllBytes(Paths.get("database_setup.sql")));
                for (String statement : sql.split(";")) {
                    if (!statement.trim().isEmpty()) {
                        dbConnection.createStatement().execute(statement);
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to initialize database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database connection failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportDatabase() {
        try {
            ProcessBuilder pb = new ProcessBuilder("mysqldump", "-u", "root", "--password=", "JohnDoe12345678", "--result-file", "backup.sql");
            pb.start();
        } catch (IOException e) {
            System.err.println("Database export failed: " + e.getMessage());
        }
    }

    private void loginAction() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your username.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtUsername.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your password.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtPassword.requestFocus();
            return;
        }
        if (username.length() < 3) {
            JOptionPane.showMessageDialog(this, "Username must be at least 3 characters.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtUsername.requestFocus();
            return;
        }
        if (password.length() < 4) {
            JOptionPane.showMessageDialog(this, "Password must be at least 4 characters.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtPassword.requestFocus();
            return;
        }

        User user = UserDAO.login(username, hashPassword(password));
        if (user != null) {
            JOptionPane.showMessageDialog(this, "Welcome back, " + user.getUsername() + "!");
            openDashboard(user);
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Invalid username or password.\nPlease try again.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            txtPassword.setText("");
            txtPassword.requestFocus();
        }
    }

    private void registerAction() {
        JTextField txtUsername = new JTextField();
        JTextField txtEmail = new JTextField();
        JPasswordField txtPassword = new JPasswordField();
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
        panel.add(createStyledLabel("Password:"), gbc);
        gbc.gridx = 1;
        panel.add(txtPassword, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Register", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String username = txtUsername.getText().trim();
            String email = txtEmail.getText().trim();
            String password = new String(txtPassword.getPassword());

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,6}$")) {
                JOptionPane.showMessageDialog(this, "Invalid email address!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String verificationCode = UUID.randomUUID().toString().substring(0, 6);
            // Simulate email sending (replace with actual email logic if needed)
            JOptionPane.showMessageDialog(this, "Verification code sent to " + email + ": " + verificationCode);
            String inputCode = JOptionPane.showInputDialog(this, "Enter verification code:");
            if (!verificationCode.equals(inputCode)) {
                JOptionPane.showMessageDialog(this, "Invalid verification code!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            User newUser = new User(username, email, hashPassword(password), "Member");
            boolean registered = UserDAO.register(newUser);
            if (registered) {
                JOptionPane.showMessageDialog(this, "Registration successful! You can now log in.");
            } else {
                JOptionPane.showMessageDialog(this, "Username or email already exists.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void forgotPasswordAction() {
        JTextField txtUsername = new JTextField();
        styleInputComponent(txtUsername);
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createStyledLabel("Username:"), gbc);
        gbc.gridx = 1;
        panel.add(txtUsername, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Forgot Password", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String username = txtUsername.getText().trim();
            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username is required!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            User user = UserDAO.getUserByUsername(username);
            if (user != null) {
                String tempPassword = PasswordUtils.generateTempPassword(8);
                UserDAO.updatePassword(user.getId(), hashPassword(tempPassword));
                JOptionPane.showMessageDialog(this, "Temporary password: " + tempPassword + "\nUse it to log in and change your password.");
            } else {
                JOptionPane.showMessageDialog(this, "User not found!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Password hashing failed: " + e.getMessage());
        }
    }

    private void openDashboard(User user) {
        switch (user.getRole()) {
            case "Admin":
                new AdminDashboard(user).setVisible(true);
                break;
            case "ClubLeader":
                new ClubLeaderDashboard(user).setVisible(true);
                break;
            case "Member":
                new MemberDashboard(user).setVisible(true);
                break;
            default:
                JOptionPane.showMessageDialog(this, "Unknown role: " + user.getRole());
        }
    }

    private void styleInputComponent(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 150, 150), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
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
        SwingUtilities.invokeLater(() -> new LoginFrame());
    }
}