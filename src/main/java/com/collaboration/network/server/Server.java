package main.java.com.collaboration.network.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    //职责
    //启动 Socket 服务器
    //监听端口，接受客户端连接
    //为每个连接创建 ClientHandler 线程
    //管理在线客户端

    private ServerSocket serverSocket;           // 服务器套接字
    private final int port;                      // 端口号
    private volatile boolean isRunning;          // 服务运行状态（volatile保证线程可见性）
    private final Map<String, ClientHandler> onlineClients;  // 在线用户ID → ClientHandler
    private final ExecutorService threadPool;    // 线程池
    private final MessageDispatcher dispatcher;

    public Server(int port) {
        this.port = port;
        this.onlineClients = new ConcurrentHashMap<>();
        this.threadPool = Executors.newCachedThreadPool();
        this.dispatcher = new MessageDispatcher(this);
    }

    // 提供一个获取 dispatcher 的方法
    public MessageDispatcher getDispatcher() {
        return dispatcher;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public Map<String, ClientHandler> getOnlineClients() {
        return onlineClients;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    //启动服务器接收连接
    public void start() throws IOException {
        //创建 ServerSocket 绑定端口
        this.serverSocket=new ServerSocket(port);
        System.out.println("服务器启动成功,监听窗口:"+port);

        //设置 isRunning = true
        isRunning=true;

        //循环执行：
        while(isRunning){
            try {
                //调用 serverSocket.accept() 等待客户端连接（阻塞）
                //接收到连接后，创建 ClientHandler 对象
                Socket socket =serverSocket.accept();
                //将 handler 提交到线程池执行
                ClientHandler handler=new ClientHandler(socket,this);

                threadPool.submit(()->{
                    handler.run();
                });

            } catch (IOException e) {
                if(isRunning){
                    System.err.println("接受连接失败: " + e.getMessage());
                }
            }
        }
    }

    //关闭服务器
    public void stop() throws IOException {
        //设置 isRunning = false
        isRunning=false;
        //关闭 serverSocket(不为空且开启状态才可以关闭)
        if(serverSocket !=null && !serverSocket.isClosed()){
            serverSocket.close();
        }
        //关闭线程池 threadPool.shutdown()
        threadPool.shutdown();
        //遍历所有在线客户端，调用每个 ClientHandler 的 close() 方法
        for (ClientHandler user : onlineClients.values()) {
            try {
                user.close();
            } catch (IOException e) {
                System.err.println("关闭客户端连接失败: " + e.getMessage());            }
        }
        //清空 onlineClients 集合
        onlineClients.clear();
        System.out.println("服务器已关闭");
    }

    //添加在线用户
    public void addClient(String username, ClientHandler handler){
        onlineClients.put(username, handler);
        //可选：广播用户上线通知
        System.out.println("用户上线: " + username + "，当前在线人数: " + onlineClients.size());
    }

    //	移除离线用户
    public void removeClient(String username){
        onlineClients.remove(username);
        //可选：广播用户下线通知
        System.out.println("用户下线: " + username + "，当前在线人数: " + onlineClients.size());
    }

    //获取指定用户的处理器
    public ClientHandler getClientHandler(String username){
        return onlineClients.get(username);

    }

    //广播消息给所有在线用户
    public void broadcastMessage(String message){
        //遍历 onlineClients.values()
        for (ClientHandler handler : onlineClients.values()) {
            //捕获单个发送失败异常，不影响其他客户端
            try {
                //对每个 handler 调用 sendMessage(message)
                handler.sendMessage(message);
            } catch (Exception e) {
                System.err.println("广播消息给 " + handler.getUsername() + " 失败: " + e.getMessage());
            }
        }
    }

    //获取在线用户列表
    public List<String> getOnlineUserList(){
        return new ArrayList<>(onlineClients.keySet());
    }

    //判断用户是否在线
    public boolean isUsernameOnline(String username){
        return onlineClients.containsKey(username);
    }

    // ========== 主方法（用于测试）==========
    public static void main(String[] args) {
        Server server = new Server(8888);
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("服务器启动失败: " + e.getMessage());
        }
    }




}
