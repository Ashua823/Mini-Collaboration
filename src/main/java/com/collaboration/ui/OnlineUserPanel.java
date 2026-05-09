package com.collaboration.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 在线用户列表面板（可折叠）
 */
public class OnlineUserPanel extends JPanel {

    private JPanel userListPanel;
    private boolean expanded = true;
    private JLabel toggleLabel;
    private JLabel countLabel;
    private List<Consumer<String>> selectListeners = new ArrayList<>();

    private String[] onlineUsers = new String[0];

    public OnlineUserPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(UIColors.BG_WHITE);
        setPreferredSize(new Dimension(200, 0));
        setBorder(new EmptyBorder(0, 0, 0, 0));

        // === 标题栏 ===
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UIColors.PRIMARY_LIGHT);
        headerPanel.setBorder(new EmptyBorder(12, 15, 10, 10));
        headerPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        toggleLabel = new JLabel("▼ 在线用户");
        toggleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        toggleLabel.setForeground(UIColors.TEXT_PRIMARY);

        countLabel = new JLabel("(" + onlineUsers.length + ")");
        countLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        countLabel.setForeground(UIColors.TEXT_SECONDARY);

        headerPanel.add(toggleLabel, BorderLayout.WEST);
        headerPanel.add(countLabel, BorderLayout.EAST);

        // 点击折叠/展开
        headerPanel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                toggleUserList();
            }
        });

        add(headerPanel, BorderLayout.NORTH);

        // === 用户列表 ===
        userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        userListPanel.setBackground(UIColors.BG_WHITE);

        refreshUserList();

        JScrollPane scrollPane = new JScrollPane(userListPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void toggleUserList() {
        expanded = !expanded;
        userListPanel.setVisible(expanded);
        toggleLabel.setText((expanded ? "▼" : "▶") + " 在线用户");
        revalidate();
    }

    private void refreshUserList() {
        userListPanel.removeAll();

        for (String user : onlineUsers) {
            JPanel userItem = createUserItem(user);
            userListPanel.add(userItem);
            userListPanel.add(Box.createVerticalStrut(2));
        }

        userListPanel.add(Box.createVerticalGlue());
        userListPanel.revalidate();
        userListPanel.repaint();
    }

    private JPanel createUserItem(String username) {
        JPanel item = new JPanel(new BorderLayout(10, 0));
        item.setBackground(UIColors.BG_WHITE);
        item.setBorder(new EmptyBorder(8, 15, 8, 10));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 头像 + 用户名
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        leftPanel.setBackground(UIColors.BG_WHITE);

        // 头像（用圆形色块代替）
        JLabel avatar = new JLabel(getAvatarText(username));
        avatar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        avatar.setForeground(getAvatarColor(username));

        JLabel nameLabel = new JLabel(username);
        nameLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        nameLabel.setForeground(UIColors.TEXT_PRIMARY);

        leftPanel.add(avatar);
        leftPanel.add(nameLabel);

        // 在线状态点
        JLabel statusDot = new JLabel("●");
        statusDot.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 10));
        statusDot.setForeground(UIColors.SUCCESS);

        item.add(leftPanel, BorderLayout.WEST);
        item.add(statusDot, BorderLayout.EAST);

        // 鼠标悬停效果
        item.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                item.setBackground(UIColors.PRIMARY_LIGHT);
                leftPanel.setBackground(UIColors.PRIMARY_LIGHT);
            }
            public void mouseExited(MouseEvent e) {
                item.setBackground(UIColors.BG_WHITE);
                leftPanel.setBackground(UIColors.BG_WHITE);
            }
            public void mouseClicked(MouseEvent e) {
                // 点击用户 → 设置私聊对象
                selectListeners.forEach(l -> l.accept(username));
            }
        });

        return item;
    }

    private String getAvatarText(String username) {
        return "🔵";
    }

    private Color getAvatarColor(String username) {
        Color[] colors = {UIColors.PRIMARY, new Color(240, 147, 43),
                new Color(155, 89, 182), new Color(46, 204, 113),
                new Color(241, 196, 15)};
        return colors[Math.abs(username.hashCode()) % colors.length];
    }

    /**
     * 更新在线用户列表（由 ChatFrame 调用）
     */
    public void updateUsers(String[] users) {
        System.out.println("[OnlineUserPanel] 更新用户列表: " + java.util.Arrays.toString(users));
        this.onlineUsers = users;
        countLabel.setText("(" + users.length + ")");
        refreshUserList();
    }

    /**
     * 添加用户选择监听器
     */
    public void addUserSelectListener(Consumer<String> listener) {
        selectListeners.add(listener);
    }
}