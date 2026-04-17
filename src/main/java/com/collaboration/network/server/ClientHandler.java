package main.java.com.collaboration.network.server;

import java.io.*;
import java.net.Socket;

public class ClientHandler {
    private final Socket socket;                 // 客户端套接字
    private String userId;                       // 用户ID（登录后赋值）
    private String username;                     // 用户名（登录后赋值）
    private BufferedReader in;                   // 读取客户端输入
    private PrintWriter out;                     // 向客户端输出
    private final Server server;                 // 服务端引用
    private boolean isAuthenticated;             // 是否已登录认证

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.isAuthenticated = false;
       // 初始化输入输出流（在run方法中或构造中）
    }

    public Socket getSocket() {
        return socket;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public BufferedReader getIn() {
        return in;
    }

    public void setIn(BufferedReader in) {
        this.in = in;
    }

    public PrintWriter getOut() {
        return out;
    }

    public void setOut(PrintWriter out) {
        this.out = out;
    }

    public Server getServer() {
        return server;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;
    }

    //线程执行体（核心）
    public void run() {
        try {
            //初始化 in 和 out（从 socket 获取流）
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            //循环执行：
            String command;
            while((command=in.readLine())!=null){
                //调用 in.readLine() 读取客户端发送的一行指令
                System.out.println("收到指令:"+command);
                System.out.println("来自:"+(username!=null ? username:"未登录)"));

                //调用 MessageDispatcher.dispatch(this, command) 处理指令
                server.getDispatcher().dispatch(this,command);
            }
        } catch (IOException e) {
            System.err.println("客户端通信异常: " + e.getMessage());
        }finally {
            //最终调用 close() 清理资源
            try {
                close();
            } catch (IOException e) {
                System.err.println("关闭客户端连接失败: " + e.getMessage());
            }
        }
    }

    //向客户端发送消息
    public void sendMessage(String msg){
        if (out != null) {
            out.println(msg);
            out.flush();//（确保立即发送）
        }
    }

    // 关闭连接
    public void close() throws IOException {
        //如果已登录（isAuthenticated == true）：
        if(isAuthenticated && username!=null){
            //从 server 移除该用户 server.removeClient(username)
            server.removeClient(username);
            //关闭 in
            if (in!=null){
                in.close();
            }
            //关闭 out
            if (out != null) {
                out.close();
            }
            //关闭 socket
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

        }
    }

}
