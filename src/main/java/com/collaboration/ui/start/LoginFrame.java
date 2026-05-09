package com.collaboration.ui.start;

import com.collaboration.domain.Response;
import com.collaboration.ui.ChatFrame;
import com.collaboration.ui.UIColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.collaboration.service.UserService;

/**
 * 登录界面
 */
public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel registerLink;
    private JLabel statusLabel;
    private UserService userService;

    public LoginFrame() {
        this.userService = new UserService();  // 初始化 UserService
        initUI();
    }

    private void initUI() {
        // 设置窗口基本属性
        setTitle("Mini-Collaboration 协作平台");
        setSize(500, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        // 去除原生标题栏以获得更好外观
        setUndecorated(false);

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(UIColors.BG_MAIN);
        mainPanel.setBorder(new EmptyBorder(40, 50, 30, 50));

        // 顶部标题
        mainPanel.add(createTitlePanel(), BorderLayout.NORTH);

        // 中间输入区域
        mainPanel.add(createInputPanel(), BorderLayout.CENTER);

        // 底部注册链接
        mainPanel.add(createBottomPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
        setLocationRelativeTo(null);
    }

    /**
     * 创建顶部标题区
     */
    private JPanel createTitlePanel() {
        JPanel panel = new JPanel();
        panel.setBackground(UIColors.BG_MAIN);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // 应用图标（使用文字代替，也可以放图标）
        JLabel iconLabel = new JLabel("💬");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 应用名称
        JLabel titleLabel = new JLabel("Mini-Collaboration");
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 26));
        titleLabel.setForeground(UIColors.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 副标题
        JLabel subtitleLabel = new JLabel("轻量级团队协作平台");
        subtitleLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        subtitleLabel.setForeground(UIColors.TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(Box.createVerticalStrut(10));
        panel.add(iconLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(subtitleLabel);
        panel.add(Box.createVerticalStrut(25));

        return panel;
    }

    /**
     * 创建中间输入区
     */
    private JPanel createInputPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(UIColors.BG_MAIN);
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);

        // === 用户名输入框（带圆角和内边距） ===
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UIColors.BORDER, 1, true),
                new EmptyBorder(10, 15, 10, 15)));

        // 设置提示文字
        usernameField.setForeground(UIColors.TEXT_PRIMARY);

        JLabel userIcon = new JLabel("👤");
        userIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));

        JPanel userPanel = new JPanel(new BorderLayout(10, 0));
        userPanel.setBackground(UIColors.BG_WHITE);
        userPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UIColors.BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        userPanel.add(userIcon, BorderLayout.WEST);
        userPanel.add(usernameField, BorderLayout.CENTER);
        usernameField.setBorder(null);

        // === 密码输入框 ===
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));

        passwordField.setForeground(UIColors.TEXT_PRIMARY);

        JLabel lockIcon = new JLabel("🔒");
        lockIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));

        JPanel passPanel = new JPanel(new BorderLayout(10, 0));
        passPanel.setBackground(UIColors.BG_WHITE);
        passPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UIColors.BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        passPanel.add(lockIcon, BorderLayout.WEST);
        passPanel.add(passwordField, BorderLayout.CENTER);
        passwordField.setBorder(null);

        // === 登录按钮 ===
        loginButton = new JButton("登  录");
        loginButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 15));
        loginButton.setForeground(UIColors.TEXT_PRIMARY);
        loginButton.setBackground(UIColors.PRIMARY);
        loginButton.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UIColors.PRIMARY_DARK, 1, true),  // 加边框，和背景区分
                new EmptyBorder(12, 0, 12, 0)));
        loginButton.setFocusPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // 去掉按钮默认的边框和填充
        loginButton.setOpaque(true);
        loginButton.setContentAreaFilled(true);

        // 鼠标悬停效果
        loginButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                loginButton.setBackground(UIColors.PRIMARY_DARK);
                loginButton.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(UIColors.PRIMARY_DARK.darker(), 2, true),
                        new EmptyBorder(11, 0, 11, 0)));
            }

            public void mouseExited(MouseEvent e) {
                loginButton.setBackground(UIColors.PRIMARY);
                loginButton.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(UIColors.PRIMARY_DARK, 1, true),
                        new EmptyBorder(12, 0, 12, 0)));
            }
        });

        // 登录事件
        loginButton.addActionListener(e -> handleLogin());

        // === 状态提示标签 ===
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        statusLabel.setForeground(UIColors.DANGER);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 布局
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 0, 10, 0);
        panel.add(userPanel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(5, 0, 15, 0);
        panel.add(passPanel, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(10, 0, 5, 0);
        panel.add(loginButton, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(5, 0, 0, 0);
        panel.add(statusLabel, gbc);

        return panel;
    }

    /**
     * 创建底部注册区
     */
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(UIColors.BG_MAIN);

        JLabel hintLabel = new JLabel("还没有账号？");
        hintLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        hintLabel.setForeground(UIColors.TEXT_SECONDARY);

        registerLink = new JLabel("<html><u>立即注册</u></html>");
        registerLink.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        registerLink.setForeground(UIColors.PRIMARY);
        registerLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                handleRegister();
            }
        });

        panel.add(hintLabel);
        panel.add(registerLink);

        return panel;
    }

    /**
     * 处理登录
     */
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("请输入用户名和密码");
            return;
        }

        try {
            // UserService.login() 返回的是 Response 对象
            Response response = userService.login(username, password);
            if (response.getCode()==200) {
                statusLabel.setText("");
                // LoginFrame.java 的 handleLogin()
                SwingUtilities.invokeLater(() -> {
                    ChatFrame chatFrame = new ChatFrame(username, password);  // 传入密码
                    chatFrame.setVisible(true);
                    LoginFrame.this.dispose();
                });
            } else {
                statusLabel.setText(response.getMessage());
            }
        } catch (Exception e) {
            statusLabel.setText(e.getMessage());
        }
    }

        /**
         * 处理注册
         */
        private void handleRegister() {
            String username = JOptionPane.showInputDialog(this,
                    "请输入用户名:", "注册新账号", JOptionPane.PLAIN_MESSAGE);
            if (username != null && !username.trim().isEmpty()) {
                String password = JOptionPane.showInputDialog(this,
                        "请输入密码:", "设置密码", JOptionPane.PLAIN_MESSAGE);
                if (password != null && !password.trim().isEmpty()) {
                    try {
                        Response response = userService.register(username, password);
                        if (response.getCode()==200) {
                            JOptionPane.showMessageDialog(this,
                                    "注册成功！请登录", "成功", JOptionPane.INFORMATION_MESSAGE);
                            usernameField.setText(username);
                            passwordField.setText("");
                        } else {
                            JOptionPane.showMessageDialog(this,
                                    "注册失败: " + response.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this,
                                "注册失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }


        // ========== 入口 ==========
        static void main(){
            // 设置系统外观
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            });
        }

}