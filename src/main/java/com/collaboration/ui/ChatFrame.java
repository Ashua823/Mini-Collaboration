package com.collaboration.ui;

import com.collaboration.network.client.Client;
import com.collaboration.ui.start.LoginFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ChatFrame extends JFrame {

    private String currentUsername;
    private String loginPassword;
    private OnlineUserPanel onlineUserPanel;
    private ChatPanel chatPanel;
    private InputPanel inputPanel;
    private UserInfoPanel userInfoPanel;
    private Client client;

    public ChatFrame(String username, String password) {
        this.currentUsername = username;
        this.loginPassword = password;
        initUI();
        connectToServer();
    }

    private void initUI() {
        setTitle("Mini-Collaboration - " + currentUsername);
        setSize(1000, 680);
        setMinimumSize(new Dimension(800, 550));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(UIColors.BG_MAIN);

        // === 顶部标题栏 ===
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(UIColors.BG_WHITE);
        titleBar.setBorder(new EmptyBorder(8, 15, 8, 15));

        JLabel titleLabel = new JLabel("Mini-Collaboration");
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        titleLabel.setForeground(UIColors.TEXT_PRIMARY);

        userInfoPanel = new UserInfoPanel(currentUsername);
        userInfoPanel.addLogoutListener(() -> {
            if (client != null) {
                try {
                    client.disconnect();
                } catch (Exception ignored) {}
            }
            dispose();
            new LoginFrame().setVisible(true);
        });

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setBackground(UIColors.BG_WHITE);
        rightPanel.add(userInfoPanel);

        titleBar.add(titleLabel, BorderLayout.WEST);
        titleBar.add(rightPanel, BorderLayout.EAST);
        mainPanel.add(titleBar, BorderLayout.NORTH);

        // === 中间内容区 ===
        JSplitPane contentPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        contentPane.setDividerSize(1);
        contentPane.setBorder(null);

        onlineUserPanel = new OnlineUserPanel();
        onlineUserPanel.addUserSelectListener(username -> {
            inputPanel.setPrivateChatTarget(username);
        });

        JPanel chatArea = new JPanel(new BorderLayout());
        chatArea.setBackground(UIColors.BG_MAIN);

        chatPanel = new ChatPanel();
        JScrollPane chatScroll = new JScrollPane(chatPanel);
        chatScroll.setBorder(null);
        chatScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);

        FilePanel filePanel = new FilePanel();

        inputPanel = new InputPanel();

        // ===== 只保留一个 addSendListener =====
        inputPanel.addSendListener((message, target) -> {
            System.out.println("[发送] 消息=" + message + " 目标=" + target + " client=" + client);
            if (client != null && client.isConnected()) {
                if (target != null && !target.isEmpty()) {
                    client.sendPrivateMessage(target, message);
                    chatPanel.addPrivateMessage(currentUsername, target, message);
                } else {
                    client.sendPublicMessage(message);
                    chatPanel.addPublicMessage(currentUsername, message);
                }
            } else {
                chatPanel.addPublicMessage("系统", "未连接到服务器");
            }
        });

        JPanel centerWithFile = new JPanel(new BorderLayout());
        centerWithFile.setBackground(UIColors.BG_MAIN);
        centerWithFile.add(filePanel, BorderLayout.NORTH);
        centerWithFile.add(chatScroll, BorderLayout.CENTER);

        chatArea.add(centerWithFile, BorderLayout.CENTER);
        chatArea.add(inputPanel, BorderLayout.SOUTH);

        contentPane.setLeftComponent(onlineUserPanel);
        contentPane.setRightComponent(chatArea);
        contentPane.setDividerLocation(200);

        mainPanel.add(contentPane, BorderLayout.CENTER);
        setContentPane(mainPanel);

        chatPanel.addPublicMessage("系统", "欢迎来到 Mini-Collaboration 协作平台！");
    }

    private void connectToServer() {
        try {
            client = new Client("localhost", 8888);

            if (!client.connect()) {
                chatPanel.addPublicMessage("系统", "无法连接到服务器");
                return;
            }

            System.out.println("[ChatFrame] 已连接，启动监听...");

            // 启动消息监听
            new Thread(() -> {
                client.listenForMessagesInternal();
            }).start();

            // 等监听线程启动
            try { Thread.sleep(300); } catch (InterruptedException e) {}

            // 登录
            String loginResponse = client.login(currentUsername, loginPassword);
            System.out.println("[ChatFrame] 登录响应: " + loginResponse);

            if (loginResponse != null && loginResponse.contains("成功")) {
                chatPanel.addPublicMessage("系统", "已连接到服务器");
            } else {
                chatPanel.addPublicMessage("系统", "登录失败: " + loginResponse);
                return;
            }

            // 持续接收消息
            new Thread(() -> {
                while (client.isConnected()) {
                    String message = client.receiveMessage();
                    if (message != null) {
                        System.out.println("[ChatFrame] 收到: " + message);
                        SwingUtilities.invokeLater(() -> handleServerMessage(message));
                    }
                }
                SwingUtilities.invokeLater(() -> {
                    chatPanel.addPublicMessage("系统", "与服务器断开连接");
                });
            }).start();

            // 定时刷新在线用户列表（每3秒）
            new Thread(() -> {
                while (client != null && client.isConnected()) {
                    try {
                        Thread.sleep(3000);
                        client.sendCommand("GET_ONLINE_USERS");
                        // 不需要处理响应，responseQueue 会被 receiveMessage 线程消费
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        System.out.println("[刷新在线用户] 失败: " + e.getMessage());
                    }
                }
            }).start();

        } catch (Exception e) {
            System.out.println("[ChatFrame] 连接异常: " + e.getMessage());
            chatPanel.addPublicMessage("系统", "连接失败: " + e.getMessage());
        }
    }

    private void handleServerMessage(String message) {
        if (message == null || message.isEmpty()) return;

        System.out.println("[GUI] 处理消息: " + message);

        if (message.startsWith("[公共消息]")) {
            String content = message.substring("[公共消息] ".length());
            int colonIndex = content.indexOf(": ");
            if (colonIndex > 0) {
                String sender = content.substring(0, colonIndex);
                String text = content.substring(colonIndex + 2);
                if (!sender.equals(currentUsername)) {
                    chatPanel.addPublicMessage(sender, text);
                }
            }
            return;
        }

        if (message.startsWith("[私聊]")) {
            String content = message.substring("[私聊]".length());
            if (content.contains("你对")) return; // 自己发的确认，跳过
            if (content.contains("发送信息:")) {
                String[] parts = content.split("发送信息:", 2);
                if (parts.length >= 2) {
                    chatPanel.addPrivateMessage(parts[0].trim(), currentUsername, parts[1].trim());
                }
            }
            return;
        }

        if (message.startsWith("在线用户列表:")) {
            String userListStr = message.substring("在线用户列表:".length()).trim();
            System.out.println("[GUI] 解析在线用户: " + userListStr);
            if (!userListStr.isEmpty() && !userListStr.equals("当前没有在线用户")) {
                String[] users = userListStr.split(",\\s*");
                java.util.List<String> others = new java.util.ArrayList<>();
                for (String u : users) {
                    if (!u.equals(currentUsername)) {
                        others.add(u);
                    }
                }
                onlineUserPanel.updateUsers(others.toArray(new String[0]));
            } else {
                onlineUserPanel.updateUsers(new String[0]);
            }
            return;
        }

        if (message.startsWith("登录成功") || message.startsWith("消息已发送")) {
            return;
        }

        chatPanel.addPublicMessage("系统", message);
    }
}