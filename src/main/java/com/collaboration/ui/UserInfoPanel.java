package com.collaboration.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 用户信息面板（悬停展开）
 */
public class UserInfoPanel extends JPanel {

    private String username;
    private JPopupMenu popupMenu;
    private JLabel avatarLabel;
    private Runnable logoutListener;

    public UserInfoPanel(String username) {
        this.username = username;
        initUI();
    }

    private void initUI() {
        setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        setBackground(UIColors.BG_WHITE);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 用户名
        JLabel nameLabel = new JLabel(username);
        nameLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        nameLabel.setForeground(UIColors.TEXT_PRIMARY);

        // 头像
        avatarLabel = new JLabel("👤");
        avatarLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));

        add(nameLabel);
        add(avatarLabel);

        // 创建弹出菜单
        createPopupMenu();

        // 鼠标悬停展开
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                popupMenu.show(UserInfoPanel.this, -100, getHeight());
            }
        });
        avatarLabel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                popupMenu.show(avatarLabel, -100, avatarLabel.getHeight() + 5);
            }
        });
    }

    private void createPopupMenu() {
        popupMenu = new JPopupMenu();
        popupMenu.setBorder(new LineBorder(UIColors.BORDER));

        // 个人信息区域
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(new EmptyBorder(12, 15, 12, 15));
        infoPanel.setBackground(UIColors.BG_WHITE);

        JLabel avatarBig = new JLabel("👤");
        avatarBig.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        avatarBig.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLabel = new JLabel(username);
        nameLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 15));
        nameLabel.setForeground(UIColors.TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel roleLabel = new JLabel("普通用户");
        roleLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        roleLabel.setForeground(UIColors.TEXT_SECONDARY);
        roleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton logoutBtn = new JButton("退出登录");
        logoutBtn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        logoutBtn.setForeground(UIColors.DANGER);
        logoutBtn.setBackground(UIColors.BG_MAIN);
        logoutBtn.setBorder(new EmptyBorder(6, 15, 6, 15));
        logoutBtn.setFocusPainted(false);
        logoutBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> {
            if (logoutListener != null) {
                logoutListener.run();
            }
        });

        infoPanel.add(avatarBig);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(3));
        infoPanel.add(roleLabel);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(new JSeparator());
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(logoutBtn);

        popupMenu.add(infoPanel);
        popupMenu.pack();
    }

    public void addLogoutListener(Runnable listener) {
        this.logoutListener = listener;
    }
}