package com.collaboration.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 聊天消息显示面板
 */
public class ChatPanel extends JPanel {

    private JPanel messageContainer;

    public ChatPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(UIColors.BG_CHAT);

        messageContainer = new JPanel();
        messageContainer.setLayout(new BoxLayout(messageContainer, BoxLayout.Y_AXIS));
        messageContainer.setBackground(UIColors.BG_CHAT);
        messageContainer.setBorder(new EmptyBorder(10, 15, 10, 15));

        JScrollPane scrollPane = new JScrollPane(messageContainer);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * 添加公共消息
     */
    public void addPublicMessage(String sender, String content) {
        JPanel msgPanel = createMessageBubble(sender, content, UIColors.BG_WHITE);
        messageContainer.add(msgPanel);
        messageContainer.add(Box.createVerticalStrut(8));
        refreshScroll();
    }

    /**
     * 添加私聊消息（不同背景色）
     */
    public void addPrivateMessage(String sender, String receiver, String content) {
        JPanel msgPanel = createMessageBubble(sender, "[私聊] " + content, UIColors.PRIVATE_MSG);
        messageContainer.add(msgPanel);
        messageContainer.add(Box.createVerticalStrut(8));
        refreshScroll();
    }

    /**
     * 创建一条消息气泡
     */
    private JPanel createMessageBubble(String sender, String content, Color bgColor) {
        JPanel bubble = new JPanel(new BorderLayout(5, 3));
        bubble.setBackground(bgColor);
        bubble.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(8, 12, 8, 12),
                javax.swing.BorderFactory.createLineBorder(UIColors.BORDER, 1, true)));
        bubble.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        bubble.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 发送者名字（加粗 + 彩色）
        JLabel senderLabel = new JLabel(sender);
        senderLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        senderLabel.setForeground(UIColors.PRIMARY);

        // 消息内容
        JLabel contentLabel = new JLabel("<html><p style='width:500px'>" + content + "</p></html>");
        contentLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        contentLabel.setForeground(UIColors.TEXT_PRIMARY);

        bubble.add(senderLabel, BorderLayout.NORTH);
        bubble.add(contentLabel, BorderLayout.CENTER);

        return bubble;
    }

    private void refreshScroll() {
        SwingUtilities.invokeLater(() -> {
            messageContainer.revalidate();
            messageContainer.repaint();
            // 滚动到底部
            Container parent = messageContainer.getParent();
            if (parent instanceof JViewport) {
                JScrollPane scrollPane = (JScrollPane) parent.getParent();
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            }
        });
    }
}