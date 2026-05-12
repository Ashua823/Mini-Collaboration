package com.collaboration.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 文件传输按钮面板
 */
public class FilePanel extends JPanel {

    private BiConsumer<File, String> uploadListener;
    private Consumer<String> downloadListener;
    private JPopupMenu fileListPopup;
    private DefaultTableModel tableModel;

    public FilePanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 4));
        setBackground(UIColors.BG_CHAT);
        setBorder(new EmptyBorder(5, 15, 5, 15));

        add(createFileButton("📎 发送文件"));
        add(createFileButton("📋 文件列表"));

        // 文件列表弹窗（表格形式）
        fileListPopup = new JPopupMenu();
        fileListPopup.setBorder(new LineBorder(UIColors.BORDER));
    }

    public void setUploadListener(BiConsumer<File, String> listener) {
        this.uploadListener = listener;
    }

    public void setDownloadListener(Consumer<String> listener) {
        this.downloadListener = listener;
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

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(UIColors.PRIMARY_LIGHT);
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(235, 245, 255));
            }
        });

        btn.addActionListener(e -> handleButtonClick(text));
        return btn;
    }

    private void handleButtonClick(String action) {
        if (action.contains("发送文件")) {
            sendFile();
        } else if (action.contains("文件列表")) {
            // 触发外部刷新（ChatFrame 会调用 showFileList）
            if (fileListPopup.isShowing()) {
                fileListPopup.setVisible(false);
            }
        }
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择要发送的文件");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (uploadListener != null) {
                uploadListener.accept(file, "file");
            }
        }
    }

    /**
     * 显示文件列表弹窗（由 ChatFrame 调用，传入解析好的数据）
     */
    public void showFileListPopup(Component invoker, java.util.List<String[]> fileData) {
        fileListPopup.removeAll();

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIColors.BG_WHITE);

        // 列宽
        int[] colWidths = {180, 100, 100, 70};
        String[] headers = {"文件名", "上传用户", "上传时间", "操作"};

        // 表头
        JPanel headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setBackground(UIColors.PRIMARY);
        headerPanel.setBorder(new EmptyBorder(8, 5, 8, 5));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;

        for (int i = 0; i < headers.length; i++) {
            gbc.gridx = i;
            gbc.weightx = 1;
            JLabel lbl = new JLabel(headers[i], SwingConstants.CENTER);
            lbl.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
            lbl.setForeground(Color.WHITE);
            lbl.setPreferredSize(new Dimension(colWidths[i], 25));
            headerPanel.add(lbl, gbc);
        }
        panel.add(headerPanel, BorderLayout.NORTH);

        // 数据行
        JPanel dataPanel = new JPanel();
        dataPanel.setLayout(new BoxLayout(dataPanel, BoxLayout.Y_AXIS));
        dataPanel.setBackground(UIColors.BG_WHITE);

        for (String[] row : fileData) {
            JPanel rowPanel = new JPanel(new GridBagLayout());
            rowPanel.setBackground(UIColors.BG_WHITE);
            rowPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIColors.BORDER));
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

            for (int i = 0; i < 3; i++) {
                gbc.gridx = i;
                gbc.weightx = 1;
                String text = row[i];
                // 文件名过长时截断并加省略号
                if (i == 0 && text.length() > 15) {
                    text = text.substring(0, 14) + "...";
                }
                JLabel cell = new JLabel(text, SwingConstants.CENTER);
                cell.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
                cell.setForeground(UIColors.TEXT_PRIMARY);
                cell.setPreferredSize(new Dimension(colWidths[i], 30));
                rowPanel.add(cell, gbc);
            }

            // 下载按钮
            gbc.gridx = 3;
            JButton downloadBtn = new JButton("下载");
            downloadBtn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
            downloadBtn.setForeground(Color.WHITE);
            downloadBtn.setBackground(UIColors.SUCCESS);
            downloadBtn.setBorder(new EmptyBorder(3, 8, 3, 8));
            downloadBtn.setFocusPainted(false);
            downloadBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            downloadBtn.setPreferredSize(new Dimension(65, 28));

            String fileId = row[3];
            downloadBtn.addActionListener(e -> {
                if (downloadListener != null) {
                    downloadListener.accept(fileId);
                }
                fileListPopup.setVisible(false);
            });

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            btnPanel.setBackground(UIColors.BG_WHITE);
            btnPanel.add(downloadBtn);
            rowPanel.add(btnPanel, gbc);

            dataPanel.add(rowPanel);
        }

        JScrollPane scrollPane = new JScrollPane(dataPanel);
        scrollPane.setPreferredSize(new Dimension(470, Math.min(300, fileData.size() * 37 + 5)));
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scrollPane, BorderLayout.CENTER);

        fileListPopup.add(panel);
        fileListPopup.pack();
        fileListPopup.show(invoker, 0, invoker.getHeight());
    }

    /**
     * 获取文件列表按钮，供 ChatFrame 绑定悬停事件
     */
    public JButton getFileListButton() {
        return (JButton) getComponent(1);  // 第二个按钮
    }
}