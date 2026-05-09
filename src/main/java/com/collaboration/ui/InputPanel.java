package com.collaboration.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * 底部输入面板
 */
public class InputPanel extends JPanel {

    private JTextArea messageArea;
    private JButton cancelPrivateButton;
    private JButton sendButton;
    private JLabel targetLabel;
    private String privateTarget = null;
    private List<BiConsumer<String, String>> sendListeners = new ArrayList<>();

    public InputPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 5));
        setBackground(UIColors.BG_WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UIColors.BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)));

        // === 输入框 ===
        messageArea = new JTextArea(3, 40);
        messageArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setBorder(null);

        // Ctrl+Enter 发送
        messageArea.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });

        // === 底部按钮栏 ===
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(UIColors.BG_WHITE);

        // 左侧：私聊目标 + 取消按钮
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setBackground(UIColors.BG_WHITE);

        targetLabel = new JLabel();
        targetLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        targetLabel.setForeground(UIColors.PRIMARY);
        leftPanel.add(targetLabel);

        cancelPrivateButton = new JButton("✕ 取消私聊");
        cancelPrivateButton.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        cancelPrivateButton.setForeground(UIColors.TEXT_SECONDARY);
        cancelPrivateButton.setBorder(new EmptyBorder(3, 8, 3, 8));
        cancelPrivateButton.setBackground(UIColors.BG_MAIN);
        cancelPrivateButton.setFocusPainted(false);
        cancelPrivateButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelPrivateButton.setVisible(false);
        cancelPrivateButton.addActionListener(e -> clearPrivateTarget());
        leftPanel.add(cancelPrivateButton);

        // 右侧：发送按钮
        sendButton = new JButton("发送  ↵");
        sendButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        sendButton.setForeground(UIColors.TEXT_WHITE);
        sendButton.setBackground(UIColors.PRIMARY);
        sendButton.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UIColors.PRIMARY_DARK, 1, true),
                new EmptyBorder(8, 20, 8, 20)));
        sendButton.setFocusPainted(false);
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendButton.setOpaque(true);
        sendButton.setContentAreaFilled(true);

        // 鼠标悬停效果
        sendButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                sendButton.setBackground(UIColors.PRIMARY_DARK);
            }
            public void mouseExited(MouseEvent e) {
                sendButton.setBackground(UIColors.PRIMARY);
            }
        });
        sendButton.addActionListener(e -> sendMessage());

        bottomPanel.add(leftPanel, BorderLayout.WEST);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * 设置私聊目标
     */
    public void setPrivateChatTarget(String username) {
        this.privateTarget = username;
        targetLabel.setText("📩 私聊对象: " + username);
        cancelPrivateButton.setVisible(true);
        messageArea.requestFocus();
    }

    /**
     * 取消私聊
     */
    private void clearPrivateTarget() {
        this.privateTarget = null;
        targetLabel.setText("");
        cancelPrivateButton.setVisible(false);
        messageArea.requestFocus();
    }

    /**
     * 发送消息
     */
    private void sendMessage() {
        System.out.println("[InputPanel] sendMessage 被调用！message=" + messageArea.getText());
        String message = messageArea.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        sendListeners.forEach(l -> l.accept(message, privateTarget));
        messageArea.setText("");
        messageArea.requestFocus();
    }

    /**
     * 添加发送监听器
     */
    public void addSendListener(BiConsumer<String, String> listener) {
        sendListeners.add(listener);
    }
}