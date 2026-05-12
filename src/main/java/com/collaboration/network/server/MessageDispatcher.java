package com.collaboration.network.server;






import com.collaboration.domain.FileInfo;
import com.collaboration.domain.Message;
import com.collaboration.domain.Response;
import com.collaboration.domain.User;
import com.collaboration.service.FileService;
import com.collaboration.service.MessageService;
import com.collaboration.service.UserService;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class MessageDispatcher {
    private final UserService userService;       // 用户服务
    private final MessageService messageService;
    // private final MessageService messageService; // 消息服务（后续添加）
    private final Server server ;                 // 服务端引用
    private final FileService fileService;

    public MessageDispatcher(Server server) {
        this.server = server;
        this.userService = new UserService();
        this.messageService = new MessageService("data/messages.json");
        this.fileService = new FileService("data/files/", "data/files-metadata.json");
    }

    /**
     * 分发指令
     * @param handler 客户端处理器
     * @param command 原始指令字符串
     */
    public void dispatch(ClientHandler handler, String command) {
        //判断 command 是否为空，是则返回
        if(command==null || command.trim().isEmpty()){
            return;
        }

        //按分隔符 "\\|" 拆分指令（注意正则转义）
        String[] parts = command.split("\\|");
        //获取指令类型 parts[0]
        String cmdType= parts[0];
        //用 switch判断：
        switch (cmdType){
            case "LOGIN"-> handleLogin(handler, parts);
            case "LOGOUT"->handleLogout(handler);
            case "PUBLIC_MSG"->handlePublicMsg(handler, parts);
            case "PRIVATE_MSG"->handlePrivateMsg(handler, parts);
            case "GET_ONLINE_USERS"->handleGetOnlineUsers(handler);
            case "GET_MESSAGES" -> handleGetMessages(handler, parts);
            case "GET_MY_MESSAGES" -> handleGetMyMessages(handler);
            case "UPLOAD_FILE" -> handleUploadFile(handler, parts);
            case "UPLOAD_CHUNK" -> handleUploadChunk(handler, parts);
            case "DOWNLOAD_FILE" -> handleDownloadFile(handler, parts);
            case "DOWNLOAD_CHUNK" -> handleDownloadChunk(handler, parts);
            case "GET_FILE_LIST" -> handleGetFileList(handler);
            case "GET_MY_FILES" -> handleGetMyFiles(handler);
            case "FILE_INFO" -> handleFileInfo(handler, parts);
            //默认 → 发送未知指令错误
            default -> handler.sendMessage("未知指令: " + cmdType);
        }
    }

    /**
     * 处理登录
     * 指令格式: LOGIN|用户名|密码
     */
    private void handleLogin(ClientHandler handler, String[] parts){
        //检查 parts.length >= 3，否则发送错误
        if(parts.length <3){
            handler.sendMessage("登录指令格式错误，正确格式: LOGIN|用户名|密码");
            return;
        }

        //获取 username 和 password
        String username = parts[1];
        String password = parts[2];

        //调用 UserService.response(username, password)
        Response response = userService.login(username,password);

        //处理登录结果
        if(response.getCode()==200){
            //成功
            //从 Response 中获取 User 对象
            User user=(User)response.getData();

            //设置 handler 的 userId、username、isAuthenticated = true
            handler.setUserId(user.getUserId());
            handler.setUsername(user.getUsername());
            handler.setAuthenticated(true);

            //添加到在线用户列表
            //调用 server.addClient(username, handler)
            server.addClient(username,handler);

            //发送成功响应
            handler.sendMessage("登录成功,欢迎:"+username);
        }else {
            //登录失败
            handler.sendMessage("登录失败:"+response.getMessage());
        }
    }

    //处理登出
    private  void handleLogout(	ClientHandler handler)  {

        //如果 handler.isAuthenticated()：
        if(handler.isAuthenticated()){
            //调用 server.removeClient(handler.getUsername())
            String username =handler.getUsername();
            server.removeClient(username);
            //设置 handler.isAuthenticated = false
            handler.setAuthenticated(false);
            //发送登出成功响应
            handler.sendMessage("登出成功");
        }else {
            handler.sendMessage("没有登录");
        }
    }

    //处理公共聊天消息
    // 发送公聊消息时，不仅要广播，还要保存到消息队列和文件。
    private void handlePublicMsg(ClientHandler handler, String[] parts){
        //广播消息
        //检查 handler.isAuthenticated()，未登录则拒绝
        if(!handler.isAuthenticated()){
            handler.sendMessage("未登录");
            return;
        }

        //检查 parts.length >= 2，内容不能为空
        if(parts.length<2 || parts[1].trim().isEmpty()){
            handler.sendMessage("消息内容不能为空");
            return;
        }

        //获取消息内容
        //组装广播格式：[公聊] 用户名: 内容
        String rawContent =parts[1];

        //广播给所有在线用户
        //调用 server.broadcastMessage(组装的消息)
        // 广播时加上格式：[公共消息] 用户名: 内容
        String broadcastMsg = "[公共消息] " + handler.getUsername() + ": " + rawContent;
        server.broadcastMessage(broadcastMsg);

        //保存消息
        //创建 Message 对象
        Message message=new Message(
                UUID.randomUUID().toString(),
                handler.getUserId(),
                handler.getUsername(),
                null,
                rawContent,
                Message.Type.PUBLIC,
                LocalDateTime.now().toString(),
                false
                );
        //messageId：用 UUID.randomUUID().toString() 生成唯一ID
        //senderId：从 handler.getUserId() 获取
        //senderName：从 handler.getUsername() 获取
        //receiverID：公聊消息设为 null
        //content：消息内容
        //type：使用 Message.Type.PUBLIC
        //timestamp：LocalDateTime.now()
        //isRead：false
        //调用 messageService.saveMessage(message) 保存消息
        messageService.saveMessage(message);
        handler.sendMessage("消息已发送并保存");



    }

    /**
     * 处理私聊消息
     * 指令格式: PRIVATE_MSG|目标用户名|消息内容
     */
    private void handlePrivateMsg(ClientHandler handler, String[] parts){
        //检查 handler.isAuthenticated()
        if(!handler.isAuthenticated()){
            handler.sendMessage("请先登录");
            return;
        }

        //检查 parts.length >= 3
        if(parts.length<3){
            handler.sendMessage("私聊指令格式错误，正确格式: PRIVATE_MSG|目标用户名|消息内容");
        }

        //获取目标用户名和目标消息内容
        String targetUsername = parts[1];
        String content = parts[2];

        //调用 server.getClientHandler(targetUsername) 获取目标handler
        ClientHandler targetHandle=server.getClientHandler(targetUsername);
        //如果目标不在线或不存在：
        if(targetHandle==null){
            handler.sendMessage("用户"+targetUsername+"不在线或不存在");
            return;
        }
        //发送消息给目标用户
        //组装格式：[私聊] 用户名: 内容
        String privateMsg="[私聊]"+handler.getUsername()+"发送信息:"+content;
        //调用目标handler的 sendMessage()
        targetHandle.sendMessage(privateMsg);

        //同时给发送者发送确认
        handler.sendMessage("[私聊]"+"你对"+targetUsername+"发送信息:"+content);
        // === 保存私聊消息到文件 ===
        Message privateMessage = new Message(
                UUID.randomUUID().toString(),   // messageId
                handler.getUserId(),            // senderId（发送者）
                handler.getUsername(),          // senderName
                targetHandle.getUserId(),       // receiverID（接收者）
                content,                        // 消息内容
                Message.Type.PRIVATE,           // 类型：私聊
                LocalDateTime.now().toString(), // 时间戳
                false                           // isRead
        );
        messageService.saveMessage(privateMessage);
    }

    //获取在线用户
    private  void handleGetOnlineUsers(ClientHandler handler){

        //检查 handler.isAuthenticated()
        if(!handler.isAuthenticated()){
            handler.sendMessage("请先登录");
            return;
        }

        //调用 server.getOnlineUserList()
        List<String> onlineUserList = server.getOnlineUserList();
        if(onlineUserList.isEmpty()){
            handler.sendMessage("当前没有在线用户");
        }else {
            //将列表格式化为字符串发送给客户端
            handler.sendMessage("在线用户列表: " + String.join(", ", onlineUserList));
        }
    }


    private void handleGetMessages(ClientHandler handler, String[] parts) {
        if (!handler.isAuthenticated()) {
            handler.sendMessage("请先登录");
            return;
        }
        if (parts.length < 3) {
            handler.sendMessage("指令格式错误，正确格式: GET_MESSAGES|页码|每页数量");
            return;
        }
        try {
            int page = Integer.parseInt(parts[1]);
            int size = Integer.parseInt(parts[2]);

            List<Message> messages = messageService.getMessages(page, size);
            if (messages == null || messages.isEmpty()) {
                handler.sendMessage("列表为空");
            } else {
                for (Message message : messages) {
                    String formatted;
                    if (message.getType() == Message.Type.PRIVATE) {
                        // 私聊消息加上 [私聊] 前缀
                        formatted = String.format("[私聊] %s -> %s: %s",
                                message.getSenderName(),
                                message.getReceiverID() != null ? "我" : "?",
                                message.getContent());
                    } else {
                        // 公聊消息保持原格式
                        formatted = String.format("[公聊] %s: %s",
                                message.getSenderName(),
                                message.getContent());
                    }
                    handler.sendMessage(formatted);
                }
            }
        } catch (NumberFormatException e) {
            handler.sendMessage("页码或每页数量必须是数字");
        }
    }

    private void handleGetMyMessages(ClientHandler handler) {
        //检查 handler.isAuthenticated()
        if(!handler.isAuthenticated()){
            handler.sendMessage("请先登录");
            return;
        }
        List<Message> messages = messageService.getMessagesByUser(handler.getUsername());
        if(messages==null || messages.isEmpty()){
            handler.sendMessage("暂无相关消息");
        }else {
            for (Message message : messages) {
                String formatted;
                String currentUser = handler.getUsername();

                if (currentUser.equals(message.getSenderName())) {
                    // 我发送的消息
                    String receiver = message.getReceiverID();
                    if (receiver == null) {
                        receiver = "所有人";  // 公聊
                    }
                    formatted = "[我 → " + receiver + "]: " + message.getContent();
                } else {
                    // 别人发给我的消息
                    formatted = "[" + message.getSenderName() + " → 我]: " + message.getContent();
                }
                handler.sendMessage(formatted);
            }
        }


    }
    /**
     * 处理文件上传初始化
     * 指令格式: UPLOAD_FILE|文件名|文件大小
     */
    private void handleUploadFile(ClientHandler handler, String[] parts) {

        if (!handler.isAuthenticated()) {
            handler.sendMessage("请先登录");
            return;
        }

        if (parts.length < 3) {
            handler.sendMessage("格式错误: UPLOAD_FILE|文件名|文件大小");
            return;
        }

        String fileName = parts[1];
        long fileSize = Long.parseLong(parts[2]);

        FileInfo fileInfo = fileService.initUpload(
                fileName, fileSize,
                handler.getUserId(),
                handler.getUsername()
        );
        handler.setCurrentUploadFileId(fileInfo.getFileId());
        handler.sendMessage("UPLOAD_READY|" + fileInfo.getFileId() + "|" + fileInfo.getTotalChunks());
    }
    /**
     * 处理上传分块
     * 指令格式: UPLOAD_CHUNK|分块索引|Base64数据
     */
    private void handleUploadChunk(ClientHandler handler, String[] parts) {
        if (!handler.isAuthenticated()) {
            handler.sendMessage("请先登录");
            return;
        }

        if (parts.length < 3) {
            handler.sendMessage("格式错误: UPLOAD_CHUNK|分块索引|数据");
            return;
        }

        String fileId = handler.getCurrentUploadFileId();
        if (fileId == null) {
            handler.sendMessage("请先初始化上传");
            return;
        }
        int chunkIndex = Integer.parseInt(parts[1]);
        byte[] chunkData = Base64.getDecoder().decode(parts[2]);

        // 需要知道 fileId，可以通过缓存的方式
        // 简化：假设用户正在上传的文件ID存储在 handler 中
        // 这里需要你设计一个机制来关联当前上传的文件
        // 建议：在 handler 中添加一个 currentUploadFileId 字段

        boolean success = fileService.uploadChunk(fileId, chunkIndex, chunkData);

        if (success) {
            handler.sendMessage("CHUNK_OK|" + chunkIndex);
        } else {
            handler.sendMessage("CHUNK_ERROR|" + chunkIndex);
        }
    }
    /**
     * 获取所有已完成文件列表
     */
    private void handleGetFileList(ClientHandler handler) {
        if (!handler.isAuthenticated()) {
            handler.sendMessage("请先登录");
            return;
        }

        List<FileInfo> files = fileService.getAllCompletedFiles();

        if (files.isEmpty()) {
            handler.sendMessage("暂无文件");
            return;
        }

        handler.sendMessage("========== 文件列表 ==========");
        for (FileInfo file : files) {
            String uploadTime = file.getUploadTime();
            if (uploadTime != null && uploadTime.length() >= 10) {
                uploadTime = uploadTime.substring(0, 10);
            } else {
                uploadTime = "未知时间";
            }
            // 加上 fileId 作为第4个字段
            String line = String.format("%s | %s | %s | %s",
                    file.getUploaderName(),
                    file.getName(),
                    uploadTime,
                    file.getFileId()
            );
            handler.sendMessage(line);
        }
        handler.sendMessage("========");
    }
    /**
     * 获取当前用户上传的文件列表
     */
    private void handleGetMyFiles(ClientHandler handler) {
        if (!handler.isAuthenticated()) {
            handler.sendMessage("请先登录");
            return;
        }

        List<FileInfo> files = fileService.getFilesByUser(handler.getUsername());

        if (files.isEmpty()) {
            handler.sendMessage("您还没有上传过文件");
            return;
        }

        handler.sendMessage("========== 我的文件 ==========");
        for (FileInfo file : files) {
            String line = String.format("ID: %s | %s | %s | %s",
                    file.getFileId(),
                    file.getName(),
                    file.getFormattedSize(),
                    file.isComplete() ? "已完成" : "未完成"
            );
            handler.sendMessage(line);
        }
        handler.sendMessage("================================");
    }
    /**
     * 处理下载请求
     * 指令格式: DOWNLOAD_FILE|文件ID
     */
    private void handleDownloadFile(ClientHandler handler, String[] parts) {
        if (!handler.isAuthenticated()) {
            handler.sendMessage("请先登录");
            return;
        }

        if (parts.length < 2) {
            handler.sendMessage("格式错误: DOWNLOAD_FILE|文件ID");
            return;
        }

        String fileId = parts[1];
        FileInfo fileInfo = fileService.getFileInfo(fileId);

        if (fileInfo == null || !fileInfo.isComplete()) {
            handler.sendMessage("文件不存在或未完成");
            return;
        }

        // 发送文件信息
        handler.sendMessage("FILE_INFO|" + fileId + "|" + fileInfo.getName() + "|" + fileInfo.getFileSize() + "|" + fileInfo.getTotalChunks());
    }
    /**
     * 处理下载分块
     * 指令格式: DOWNLOAD_CHUNK|文件ID|分块索引
     */
    private void handleDownloadChunk(ClientHandler handler, String[] parts) {
        if (!handler.isAuthenticated()) {
            handler.sendMessage("请先登录");
            return;
        }

        if (parts.length < 3) {
            handler.sendMessage("格式错误: DOWNLOAD_CHUNK|文件ID|分块索引");
            return;
        }

        String fileId = parts[1];
        int chunkIndex = Integer.parseInt(parts[2]);

        byte[] chunkData = fileService.downloadChunk(fileId, chunkIndex);

        if (chunkData == null) {
            handler.sendMessage("CHUNK_ERROR|" + chunkIndex);
            return;
        }

        String chunkBase64 = Base64.getEncoder().encodeToString(chunkData);
        handler.sendMessage("CHUNK_DATA|" + chunkIndex + "|" + chunkBase64);

        // 如果是最后一块，发送完成消息
        FileInfo fileInfo = fileService.getFileInfo(fileId);
        if (fileInfo != null && chunkIndex == fileInfo.getTotalChunks() - 1) {
            handler.sendMessage("DOWNLOAD_COMPLETE|" + fileId);
        }
    }
    /**
     * 获取文件详细信息
     * 指令格式: FILE_INFO|文件ID
     */
    private void handleFileInfo(ClientHandler handler, String[] parts) {
        if (!handler.isAuthenticated()) {
            handler.sendMessage("请先登录");
            return;
        }

        if (parts.length < 2) {
            handler.sendMessage("格式错误: FILE_INFO|文件ID");
            return;
        }

        String fileId = parts[1];
        FileInfo fileInfo = fileService.getFileInfo(fileId);

        if (fileInfo == null) {
            handler.sendMessage("文件不存在: " + fileId);
            return;
        }

        // 发送文件详细信息
        String info = String.format("FILE_DETAIL|%s|%s|%d|%s|%s|%d",
                fileInfo.getFileId(),
                fileInfo.getName(),
                fileInfo.getFileSize(),
                fileInfo.getFormattedSize(),
                fileInfo.getUploaderName(),
                fileInfo.getTotalChunks()
        );
        handler.sendMessage(info);
    }
}
