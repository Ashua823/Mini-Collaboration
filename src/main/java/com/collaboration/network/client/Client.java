package main.java.com.collaboration.network.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private Socket socket;                       // 服务端连接
    private BufferedReader in;                   // 读取服务端响应
    private PrintWriter out;                     // 向服务端发送指令
    private BufferedReader userInput;             // 读取用户控制台输入
    private volatile boolean isConnected;        // 连接状态
    private String host;                         // 服务器地址
    private int port;                            // 服务器端口

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

    //连接服务器
    public boolean connect(){
        try {
            //创建 Socket 连接到指定 host 和 port
            socket=new Socket(host,port);

            //初始化 in（new BufferedReader(new InputStreamReader(socket.getInputStream()))）
            in=new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //初始化 out（new PrintWriter(socket.getOutputStream(), true)）
            out=new PrintWriter(socket.getOutputStream(),true);

            //初始化 userInput（new BufferedReader(new InputStreamReader(System.in))）
            userInput=new BufferedReader(new InputStreamReader(System.in));

            //设置 isConnected = true
            isConnected=true;

            System.out.println("已连接到服务器:"+host+":"+port);

            //返回 true；异常时返回 false
            return true;
        } catch (IOException e) {
            System.err.println("连接服务器失败: " + e.getMessage());
            return false;
        }

    }

    //发送指令
    public void sendCommand(String command){
        if (out != null) {
            out.println(command);
            out.flush();
        }

    }

    // 监听服务端响应（独立线程）
    private void listenForMessages()  {

        String message;
            try {
                while(isConnected && (message=in.readLine())!=null){
                    System.out.println(message);
                }
            } catch (IOException e) {
                if(isConnected){
                    System.out.println("与服务端断开连接");
                }
            }finally {
                isConnected=false;
            }
    }

    //启动客户端主循环
    public void start() throws IOException {
       //连接服务器
        if(!connect()){
            return;
        }

        //指令提示
        printHelp();

        //启动监听线程
        new Thread(this::listenForMessages).start();

        //主循环:读取用户输入并发送
        String input;
        try {
            while(isConnected && (input=userInput.readLine())!=null){
                if(input.trim().isEmpty()){
                    continue;
                }
                if(input.equals("/quit")){
                    break;
                }
                sendCommand(input);
            }
        } catch (IOException e) {
            System.err.println("读取输入失败: " + e.getMessage());
        }finally {
            disconnect();

        }

    }

    //指令提示
    private void printHelp() {
        System.out.println("\n========== 指令帮助 ==========");
        System.out.println("登录:        LOGIN|用户名|密码");
        System.out.println("公聊:        PUBLIC_MSG|消息内容");
        System.out.println("私聊:        PRIVATE_MSG|目标用户名|消息内容");
        System.out.println("在线列表:    GET_ONLINE_USERS");
        System.out.println("登出:        LOGOUT");
        System.out.println("退出程序:    /quit");
        System.out.println("==============================\n");
    }

    //断开连接
    public void disconnect() throws IOException {
        //设置 isConnected = false
        isConnected=false;
        try {
            //发送登出指令 sendCommand("LOGOUT")
            if(out !=null){
                sendCommand("LOGOUT");
            }
            if(in!=null){
                in.close();
            }
            if(out!=null){
                out.close();
            }
            //关闭 in、out、socket
            if(socket!=null && !socket.isClosed()){
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("断开连接失败: " + e.getMessage());
        }
        System.out.println("已断开连接");
    }


    // ========== 主方法 ==========
    public static void main(String[] args) throws IOException {
        Client client = new Client("localhost", 8888);
        client.start();
    }



}



