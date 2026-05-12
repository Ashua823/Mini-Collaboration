package com.collaboration.network.client;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Client {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean isConnected;
    private String host;
    private int port;
    private final BlockingQueue<String> syncQueue = new LinkedBlockingQueue<>();

    // 响应队列（用于同步等待服务器响应）
    private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();

    // 文件传输相关
    private static final int CHUNK_SIZE = 1024 * 1024;  // 1MB 每块

    public Client() {
    }

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            isConnected = true;
            System.out.println("已连接到服务器:" + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("连接服务器失败: " + e.getMessage());
            return false;
        }
    }

    public void sendCommand(String command) {
        if (out != null) {
            out.println(command);
            out.flush();
        }
    }

    /**
     * 监听服务端响应（独立线程）
     * 将所有收到的消息放入队列，供同步方法使用
     */
    private void listenForMessages() {
        String message;
        try {
            while (isConnected && (message = in.readLine()) != null) {
                // 放入队列供同步等待使用
                responseQueue.offer(message);
                syncQueue.offer(message);      // ← 加这行

                // 非文件传输消息才打印
                if (!message.startsWith("UPLOAD_READY|") &&
                        !message.startsWith("CHUNK_OK|") &&
                        !message.startsWith("CHUNK_DATA|") &&
                        !message.startsWith("FILE_DETAIL|")) {
                    System.out.println("[服务器] " + message);
                }
            }
        } catch (IOException e) {
            if (isConnected) {
                System.out.println("与服务端断开连接");
            }
        } finally {
            isConnected = false;
        }
    }

    /**
     * 同步等待特定前缀的响应
     * @param prefix 期望的响应前缀
     * @param timeoutMs 超时时间（毫秒）
     * @return 匹配的响应消息，超时返回 null
     */
    private String waitForResponse(String prefix, int timeoutMs) {
        long startTime = System.currentTimeMillis();
        try {
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                String msg = syncQueue.poll(100, TimeUnit.MILLISECONDS);
                if (msg != null) {
                    if (msg.startsWith(prefix)) {
                        return msg;
                    }
                    // 不匹配的放回 syncQueue
                    syncQueue.offer(msg);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    /**
     * 同步等待并解析文件详情
     * @param fileId 文件ID
     * @return 文件信息数组 [fileId, name, size, formattedSize, uploaderName, totalChunks]
     */
    private String[] getFileInfo(String fileId) {
        sendCommand("FILE_INFO|" + fileId);
        String response = waitForResponse("FILE_DETAIL|", 5000);
        if (response == null) {
            System.out.println("获取文件信息超时");
            return null;
        }
        return response.split("\\|");
    }

    /**
     * 处理文件上传
     * 指令格式: UPLOAD_FILE|文件路径
     */
    private void handleUploadFile(String input) {
        String[] parts = input.split("\\|", 2);
        if (parts.length < 2) {
            System.out.println("格式错误，正确格式: UPLOAD_FILE|文件路径");
            return;
        }

        String filePath = parts[1];
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            System.out.println("文件不存在: " + filePath);
            return;
        }

        try {
            String fileName = file.getName();
            long fileSize = file.length();

            // 1. 发送上传请求（包含文件名和大小）
            String initCmd = String.format("UPLOAD_FILE|%s|%d", fileName, fileSize);
            sendCommand(initCmd);

            // 2. 等待服务器返回 UPLOAD_READY
            String readyResponse = waitForResponse("UPLOAD_READY|", 5000);
            if (readyResponse == null) {
                System.out.println("服务器未响应，上传失败");
                return;
            }

            // 解析返回的 fileId 和 totalChunks
            String[] readyParts = readyResponse.split("\\|");
            if (readyParts.length < 3) {
                System.out.println("服务器响应格式错误");
                return;
            }
            String fileId = readyParts[1];
            int totalChunks = Integer.parseInt(readyParts[2]);

            // 3. 分块上传文件
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));

            for (int i = 0; i < totalChunks; i++) {
                int start = i * CHUNK_SIZE;
                int end = (int) Math.min(start + CHUNK_SIZE, fileSize);
                byte[] chunkData = new byte[end - start];
                System.arraycopy(fileData, start, chunkData, 0, chunkData.length);

                // 将字节数据转为 Base64 编码发送
                String chunkBase64 = Base64.getEncoder().encodeToString(chunkData);
                String chunkCmd = String.format("UPLOAD_CHUNK|%d|%s", i, chunkBase64);
                sendCommand(chunkCmd);

                // 等待分块确认
                String chunkResponse = waitForResponse("CHUNK_OK|", 10000);
                if (chunkResponse == null) {
                    System.out.println("分块 " + i + " 上传失败，无响应");
                    return;
                }

                System.out.printf("上传进度: %.1f%% (%d/%d)%n",
                        (i + 1) * 100.0 / totalChunks, i + 1, totalChunks);
            }

            System.out.println("文件上传完成: " + fileName);

        } catch (Exception e) {
            System.out.println("文件上传失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理文件下载
     * 指令格式: DOWNLOAD_FILE|文件ID
     */
    private void handleDownloadFile(String input) {
        String[] parts = input.split("\\|", 2);
        if (parts.length < 2) {
            System.out.println("格式错误，正确格式: DOWNLOAD_FILE|文件ID");
            return;
        }

        String fileId = parts[1];

        // 1. 获取文件信息
        String[] fileInfo = getFileInfo(fileId);
        if (fileInfo == null) {
            System.out.println("获取文件信息失败");
            return;
        }

        // 解析 FILE_DETAIL|fileId|name|size|formattedSize|uploaderName|totalChunks
        if (fileInfo.length < 7) {
            System.out.println("文件信息格式错误");
            return;
        }

        String fileName = fileInfo[2];
        long fileSize = Long.parseLong(fileInfo[3]);
        int totalChunks = Integer.parseInt(fileInfo[6]);

        System.out.println("开始下载文件: " + fileName + " (" + fileSize + " 字节, " + totalChunks + " 块)");

        try {
            // 2. 创建输出文件
            String outputFileName = "downloaded_" + fileName;
            FileOutputStream fos = new FileOutputStream(outputFileName);

            // 3. 分块下载
            for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
                String chunkCmd = String.format("DOWNLOAD_CHUNK|%s|%d", fileId, chunkIndex);
                sendCommand(chunkCmd);

                // 等待分块数据
                String chunkResponse = waitForResponse("CHUNK_DATA|", 10000);
                if (chunkResponse == null) {
                    System.out.println("分块 " + chunkIndex + " 下载失败，无响应");
                    fos.close();
                    return;
                }

                // 解析 CHUNK_DATA|chunkIndex|base64Data
                String[] chunkParts = chunkResponse.split("\\|", 3);
                if (chunkParts.length < 3) {
                    System.out.println("分块数据格式错误");
                    continue;
                }

                int receivedIndex = Integer.parseInt(chunkParts[1]);
                if (receivedIndex != chunkIndex) {
                    System.out.println("分块索引不匹配，期望 " + chunkIndex + "，收到 " + receivedIndex);
                }

                byte[] chunkData = Base64.getDecoder().decode(chunkParts[2]);
                fos.write(chunkData);

                System.out.printf("下载进度: %.1f%% (%d/%d)%n",
                        (chunkIndex + 1) * 100.0 / totalChunks, chunkIndex + 1, totalChunks);
            }

            fos.close();
            System.out.println("文件下载完成: " + outputFileName);

        } catch (Exception e) {
            System.out.println("文件下载失败: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void disconnect() throws IOException {
        isConnected = false;
        try {
            if (out != null) {
                sendCommand("LOGOUT");
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("断开连接失败: " + e.getMessage());
        }
        System.out.println("已断开连接");
    }
    /**
     * 登录
     * @param username 用户名
     * @param password 密码
     * @return 服务器响应
     */
    public String login(String username, String password) {
        sendCommand("LOGIN|" + username + "|" + password);
        return waitForResponse("登录", 5000);  // ← 改成匹配"登录"
    }

    /**
     * 发送公共消息
     */
    public void sendPublicMessage(String content) {
        sendCommand("PUBLIC_MSG|" + content);
    }

    /**
     * 发送私聊消息
     */
    public void sendPrivateMessage(String target, String content) {
        sendCommand("PRIVATE_MSG|" + target + "|" + content);
    }

    /**
     * 获取在线用户
     */
    public String getOnlineUsers() {
        sendCommand("GET_ONLINE_USERS");
        return waitForResponse("在线用户列表", 5000);  // ← 改成匹配"在线用户列表"
    }

    /**
     * 接收消息（供 GUI 轮询使用）
     * 从响应队列中取出一条非文件传输的消息
     * @return 消息内容，无消息时返回 null
     */
    public String receiveMessage() {
        try {
            return responseQueue.poll(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * 公开的监听方法（供外部线程调用）
     */
    public void listenForMessagesInternal() {
        listenForMessages();
    }

    /**
     * GUI 调用的文件上传
     * @param filePath 文件路径
     * @param progressCallback 进度回调 (当前块, 总块数) -> void
     * @return 成功返回文件ID，失败返回 null
     */
    public String uploadFile(String filePath, java.util.function.BiConsumer<Integer, Integer> progressCallback) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.out.println("文件不存在: " + filePath);
            return null;
        }

        try {
            String fileName = file.getName();
            long fileSize = file.length();

            // 1. 发送上传请求
            String initCmd = String.format("UPLOAD_FILE|%s|%d", fileName, fileSize);
            sendCommand(initCmd);

            // 2. 等待服务器返回 UPLOAD_READY
            String readyResponse = waitForResponse("UPLOAD_READY|", 10000);
            if (readyResponse == null) {
                System.out.println("服务器未响应");
                return null;
            }

            String[] readyParts = readyResponse.split("\\|");
            if (readyParts.length < 3) return null;
            String fileId = readyParts[1];
            int totalChunks = Integer.parseInt(readyParts[2]);

            // 3. 分块上传
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));
            for (int i = 0; i < totalChunks; i++) {
                int start = i * CHUNK_SIZE;
                int end = (int) Math.min(start + CHUNK_SIZE, fileSize);
                byte[] chunkData = new byte[end - start];
                System.arraycopy(fileData, start, chunkData, 0, chunkData.length);

                String chunkBase64 = Base64.getEncoder().encodeToString(chunkData);
                sendCommand(String.format("UPLOAD_CHUNK|%d|%s", i, chunkBase64));

                String chunkResponse = waitForResponse("CHUNK_OK|", 10000);
                if (chunkResponse == null) {
                    System.out.println("分块 " + i + " 上传失败");
                    return null;
                }

                if (progressCallback != null) {
                    progressCallback.accept(i + 1, totalChunks);
                }
            }

            System.out.println("文件上传完成: " + fileName);
            return fileId;

        } catch (Exception e) {
            System.out.println("文件上传失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * GUI 调用的文件下载
     * @param fileId 文件ID
     * @param savePath 保存路径
     * @param progressCallback 进度回调
     * @return 成功返回 true
     */
    public boolean downloadFile(String fileId, String savePath,
                                java.util.function.BiConsumer<Integer, Integer> progressCallback) {
        String[] fileInfo = getFileInfo(fileId);
        if (fileInfo == null || fileInfo.length < 7) return false;

        String fileName = fileInfo[2];
        int totalChunks = Integer.parseInt(fileInfo[6]);

        try {
            FileOutputStream fos = new FileOutputStream(savePath);
            for (int i = 0; i < totalChunks; i++) {
                sendCommand(String.format("DOWNLOAD_CHUNK|%s|%d", fileId, i));
                String chunkResponse = waitForResponse("CHUNK_DATA|", 10000);
                if (chunkResponse == null) {
                    fos.close();
                    return false;
                }
                String[] parts = chunkResponse.split("\\|", 3);
                if (parts.length < 3) continue;

                byte[] chunkData = Base64.getDecoder().decode(parts[2]);
                fos.write(chunkData);

                if (progressCallback != null) {
                    progressCallback.accept(i + 1, totalChunks);
                }
            }
            fos.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取文件列表（返回原始响应供 GUI 解析）
     */
    public String getFileList() {
        sendCommand("GET_FILE_LIST");

        StringBuilder sb = new StringBuilder();
        long start = System.currentTimeMillis();

        try {
            while (System.currentTimeMillis() - start < 3000) {
                String msg = responseQueue.poll(300, TimeUnit.MILLISECONDS);
                if (msg == null) continue;

                if (!msg.contains("|") && !msg.startsWith("暂无文件")) {
                    responseQueue.offer(msg);
                    continue;
                }

                if (msg.equals("========")) {
                    break;
                }

                if (msg.contains("|")) {
                    sb.append(msg).append("\n");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String result = sb.toString().trim();
        System.out.println("[文件列表] 收集结果: " + result);
        return result;
    }

    /**
     * 发送上传请求（不等待响应，响应由 receiveMessage 处理）
     */
    public void sendUploadRequest(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) return;

        String fileName = file.getName();
        long fileSize = file.length();
        sendCommand(String.format("UPLOAD_FILE|%s|%d", fileName, fileSize));
    }

    public String waitForResponsePublic(String prefix, int timeoutMs) {
        return waitForResponse(prefix, timeoutMs);
    }
    /**
     * GUI 同步下载文件
     */
    public boolean downloadFileSync(String fileId, String savePath) {
        // 获取文件信息
        sendCommand("FILE_INFO|" + fileId);
        String detailResp = waitForResponse("FILE_DETAIL|", 5000);
        if (detailResp == null) return false;

        String[] fileInfo = detailResp.split("\\|");
        if (fileInfo.length < 7) return false;
        int totalChunks = Integer.parseInt(fileInfo[6]);

        try {
            FileOutputStream fos = new FileOutputStream(savePath);
            for (int i = 0; i < totalChunks; i++) {
                sendCommand("DOWNLOAD_CHUNK|" + fileId + "|" + i);
                String chunkResp = waitForResponse("CHUNK_DATA|", 10000);
                if (chunkResp == null) {
                    fos.close();
                    return false;
                }
                String[] parts = chunkResp.split("\\|", 3);
                if (parts.length < 3) continue;
                byte[] data = Base64.getDecoder().decode(parts[2]);
                fos.write(data);
            }
            fos.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}