package com.collaboration.ui;

import com.collaboration.network.client.Client;
import com.collaboration.ui.start.LoginFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ChatFrame extends JFrame {

    private String currentUsername;
    private String loginPassword;
    private OnlineUserPanel onlineUserPanel;
    private ChatPanel chatPanel;
    private InputPanel inputPanel;
    private UserInfoPanel userInfoPanel;
    private Client client;
    private File pendingUploadFile;
    private StringBuilder fileListBuilder;  // 收集文件列表
    private boolean collectingFileList = false;
    private FilePanel filePanel;



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

        filePanel = new FilePanel();
        inputPanel = new InputPanel();
        filePanel.setUploadListener((file, type) -> {
            System.out.println("[上传] 开始上传: " + file.getAbsolutePath());
            if (client != null && client.isConnected()) {
                this.pendingUploadFile = file;
                client.sendUploadRequest(file.getAbsolutePath());
                chatPanel.addPublicMessage("系统", "📁 开始上传: " + file.getName());
            }
        });

        filePanel.setDownloadListener(fileId -> {
            if (client != null && client.isConnected()) {
                new Thread(() -> {
                    SwingUtilities.invokeLater(() -> {
                        JFileChooser chooser = new JFileChooser();
                        chooser.setSelectedFile(new File("downloaded_file"));
                        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                            String savePath = chooser.getSelectedFile().getAbsolutePath();
                            chatPanel.addPublicMessage("系统", "⬇ 开始下载...");
                            new Thread(() -> {
                                boolean ok = client.downloadFileSync(fileId, savePath);
                                SwingUtilities.invokeLater(() -> {
                                    if (ok) {
                                        chatPanel.addPublicMessage("系统", "✅ 下载完成: " + savePath);
                                        JOptionPane.showMessageDialog(this, "下载完成！", "成功", JOptionPane.INFORMATION_MESSAGE);
                                    } else {
                                        chatPanel.addPublicMessage("系统", "❌ 下载失败");
                                    }
                                });
                            }).start();
                        }
                    });
                }).start();
            }
        });
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

        // 文件列表按钮悬停展开
        filePanel.getFileListButton().addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (client != null && client.isConnected()) {
                    fileListBuilder = new StringBuilder();
                    collectingFileList = true;
                    client.sendCommand("GET_FILE_LIST");
                }
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
            client = new Client("192.168.100.200", 8888);

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
                // 请求历史消息
                new Thread(() -> {
                    try { Thread.sleep(600); } catch (InterruptedException _) {}
                    client.sendCommand("GET_MESSAGES|1|50");
                }).start();
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

        // ===== 文件列表收集模式 =====
        if (collectingFileList) {
            System.out.println("[文件列表收集] 收到: " + message + " collecting=" + collectingFileList);
            if (message.startsWith("========== 文件列表 ==========")) {
                return;
            }
            if (message.equals("========")) {
                collectingFileList = false;
                String list = fileListBuilder.toString().trim();
                System.out.println("[文件列表] 收集完成: " + list);

                if (!list.isEmpty()) {
                    String[] lines = list.split("\n");
                    List<String[]> fileData = new ArrayList<>();
                    for (String line : lines) {
                        // 格式: 上传用户 | 文件名 | 上传时间
                        String[] parts = line.split("\\|");
                        if (parts.length >= 4) {
                            fileData.add(new String[]{
                                    parts[1].trim(),  // 文件名
                                    parts[0].trim(),  // 上传用户
                                    parts[2].trim(),  // 上传时间
                                    parts[3].trim()   // 文件ID
                            });
                        }
                    }
                    if (!fileData.isEmpty()) {
                        SwingUtilities.invokeLater(() -> {

                            filePanel.showFileListPopup(filePanel.getFileListButton(), fileData);
                        });
                    }
                }
                return;
            }
            if (message.contains("|")) {
                fileListBuilder.append(message).append("\n");
            }
            return;
        }

        // ===== 上传就绪 =====
        if (message.startsWith("UPLOAD_READY|")) {
            handleUploadReady(message);
            return;
        }

        // ===== 历史消息 [公聊] =====
        if (message.startsWith("[公聊]")) {
            String content = message.substring("[公聊] ".length());
            int colonIndex = content.indexOf(": ");
            if (colonIndex > 0) {
                String sender = content.substring(0, colonIndex);
                String text = content.substring(colonIndex + 2);
                chatPanel.addPublicMessage(sender, text);
            }
            return;
        }

        // ===== 历史消息 [私聊]（带 ->） =====
        if (message.startsWith("[私聊]") && message.contains(" -> ")) {
            String content = message.substring("[私聊] ".length());
            String[] arrowParts = content.split(" -> ", 2);
            if (arrowParts.length >= 2) {
                String sender = arrowParts[0].trim();
                String rest = arrowParts[1];
                int colonIdx = rest.indexOf(": ");
                if (colonIdx > 0) {
                    String text = rest.substring(colonIdx + 2);
                    chatPanel.addPrivateMessage(sender, currentUsername, text);
                }
            }
            return;
        }
        // ===== 实时公聊 [公共消息] =====
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
        // ===== 实时私聊 [私聊]xxx发送信息:xxx =====
        if (message.startsWith("[私聊]")) {
            String content = message.substring("[私聊]".length());
            if (content.contains("你对")) return;
            if (content.contains("发送信息:")) {
                String[] msgParts = content.split("发送信息:", 2);
                if (msgParts.length >= 2) {
                    chatPanel.addPrivateMessage(msgParts[0].trim(), currentUsername, msgParts[1].trim());
                }
            }
            return;
        }
        // ===== 在线用户列表 =====
        if (message.startsWith("在线用户列表:")) {
            String userListStr = message.substring("在线用户列表:".length()).trim();
            if (!userListStr.isEmpty() && !userListStr.equals("当前没有在线用户")) {
                String[] users = userListStr.split(",\\s*");
                List<String> others = new ArrayList<>();
                for (String u : users) {
                    if (!u.equals(currentUsername)) others.add(u);
                }
                onlineUserPanel.updateUsers(others.toArray(new String[0]));
            } else {
                onlineUserPanel.updateUsers(new String[0]);
            }
            return;
        }
        // ===== 跳过 =====
        if (message.startsWith("登录成功") || message.startsWith("消息已发送")) {
            return;
        }
        // 未匹配的消息显示为系统消息
        chatPanel.addPublicMessage("系统", message);
    }
    private void handleUploadReady(String message) {
        String[] parts = message.split("\\|");
        if (parts.length < 3 || pendingUploadFile == null) return;

        String fileId = parts[1];
        int totalChunks = Integer.parseInt(parts[2]);
        String fileName = pendingUploadFile.getName();

        chatPanel.addPublicMessage("系统", "服务器就绪，共 " + totalChunks + " 块，开始传输...");

        new Thread(() -> {
            try {
                byte[] fileData = Files.readAllBytes(pendingUploadFile.toPath());
                long fileSize = pendingUploadFile.length();

                for (int i = 0; i < totalChunks; i++) {
                    final int chunkIndex = i;
                    int start = i * 1024 * 1024;
                    int end = (int) Math.min(start + 1024 * 1024, fileSize);
                    byte[] chunk = new byte[end - start];
                    System.arraycopy(fileData, start, chunk, 0, chunk.length);

                    String base64 = Base64.getEncoder().encodeToString(chunk);
                    client.sendCommand("UPLOAD_CHUNK|" + i + "|" + base64);

                    // 进度提示
                    int percent = (i + 1) * 100 / totalChunks;
                    int current = i + 1;
                    SwingUtilities.invokeLater(() -> {
                        chatPanel.addPublicMessage("系统", "上传进度: " + percent + "% (" + current + "/" + totalChunks + ")");
                    });

                    // 等待确认
                    String ack = client.waitForResponsePublic("CHUNK_OK|", 30000);
                    if (ack == null) {
                        SwingUtilities.invokeLater(() -> {
                            chatPanel.addPublicMessage("系统", "❌ 上传失败: 分块 " + chunkIndex + " 超时");
                        });
                        return;
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    chatPanel.addPublicMessage("系统", "✅ 上传完成: " + fileName);
                    JOptionPane.showMessageDialog(this, "文件上传成功！\n" + fileName, "上传完成", JOptionPane.INFORMATION_MESSAGE);
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    chatPanel.addPublicMessage("系统", "❌ 上传失败: " + e.getMessage());
                });
            }
        }).start();
    }





}