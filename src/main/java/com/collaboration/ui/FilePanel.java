package com.collaboration.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;

/**
 * 文件传输按钮面板
 */
public class FilePanel extends JPanel {

    public FilePanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 4));
        setBackground(UIColors.BG_CHAT);
        setBorder(new EmptyBorder(5, 15, 5, 15));

        add(createFileButton("📎 发送文件"));
        add(createFileButton("🖼 发送图片"));
        add(createFileButton("📋 文件列表"));
    }

    private JButton createFileButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        btn.setForeground(UIColors.PRIMARY);
        btn.setBackground(new Color(235, 245, 255));
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UIColors.PRIMARY_LIGHT, 1, true),
                new EmptyBorder(4, 10, 4, 10)));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 鼠标悬停效果
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(UIColors.PRIMARY_LIGHT);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(235, 245, 255));
            }
        });

        // 点击事件
        btn.addActionListener(e -> handleButtonClick(text));

        return btn;
    }

    /**
     * 处理按钮点击
     */
    private void handleButtonClick(String action) {
        if (action.contains("发送文件")) {
            sendFile();
        } else if (action.contains("发送图片")) {
            sendImage();
        } else if (action.contains("文件列表")) {
            showFileList();
        }
    }

    /**
     * 发送文件 - 打开文件选择器
     */
    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择要发送的文件");
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            JOptionPane.showMessageDialog(this,
                    "已选择文件: " + file.getName() + "\n大小: " + formatFileSize(file.length()),
                    "文件选择", JOptionPane.INFORMATION_MESSAGE);
            // TODO: 调用 FileService.uploadFile(file)
        }
    }

    /**
     * 发送图片
     */
    private void sendImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择要发送的图片");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                String name = f.getName().toLowerCase();
                return f.isDirectory() || name.endsWith(".png")
                        || name.endsWith(".jpg") || name.endsWith(".jpeg")
                        || name.endsWith(".gif") || name.endsWith(".bmp");
            }
            public String getDescription() {
                return "图片文件 (*.png, *.jpg, *.gif, *.bmp)";
            }
        });
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            JOptionPane.showMessageDialog(this,
                    "已选择图片: " + file.getName(),
                    "图片选择", JOptionPane.INFORMATION_MESSAGE);
            // TODO: 调用 FileService.uploadImage(file)
        }
    }

    /**
     * 显示文件列表
     */
    private void showFileList() {
        // TODO: 调用 FileService.getFileList() 获取真实文件列表
        String[] testFiles = {"会议纪要.docx (256KB)", "项目计划.xlsx (128KB)",
                "截图.png (512KB)", "需求文档.pdf (1.2MB)"};

        JList<String> list = new JList<>(testFiles);
        list.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(300, 200));

        JOptionPane.showMessageDialog(this,
                scrollPane,
                "文件列表",
                JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024));
    }
}