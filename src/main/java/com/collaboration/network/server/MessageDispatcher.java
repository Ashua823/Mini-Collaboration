package main.java.com.collaboration.network.server;


import main.java.com.collaboration.domain.Response;
import main.java.com.collaboration.domain.User;
import main.java.com.collaboration.service.UserService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MessageDispatcher {
    private final UserService userService;       // 用户服务
    // private final MessageService messageService; // 消息服务（后续添加）
    private final Server server ;                 // 服务端引用

    public MessageDispatcher(Server server) {
        this.server = server;
        this.userService = new UserService();
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
            //LOGIN → 调用 handleLogin(handler, parts)
            case "LOGIN"-> handleLogin(handler, parts);
            //LOGOUT → 调用 handleLogout(handler)
            case "LOGOUT"->handleLogout(handler);
            //PUBLIC_MSG → 调用 handlePublicMsg(handler, parts)
            case "PUBLIC_MSG"->handlePublicMsg(handler, parts);
            //PRIVATE_MSG → 调用 handlePrivateMsg(handler, parts)
            case "PRIVATE_MSG"->handlePrivateMsg(handler, parts);
            //GET_ONLINE_USERS → 调用 handleGetOnlineUsers(handler)
            case "GET_ONLINE_USERS"->handleGetOnlineUsers(handler);
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
    private void handlePublicMsg(ClientHandler handler, String[] parts){
        //检查 handler.isAuthenticated()，未登录则拒绝
        if(!handler.isAuthenticated()){
            Response.error("未登录");
            return;
        }

        //检查 parts.length >= 2，内容不能为空
        if(parts.length<2 || parts[1].trim().isEmpty()){
            Response.error("消息内容不能为空");
            return;
        }

        //获取消息内容
        //组装广播格式：[公聊] 用户名: 内容
        String message = "[公共消息]"+handler.getUsername()+":"+parts[1];

        //广播给所有在线用户
        //调用 server.broadcastMessage(组装的消息)
        server.broadcastMessage(message);
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

}
