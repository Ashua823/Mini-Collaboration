package main.java.com.collaboration.network.common;

public class Protocol {
    // 指令类型
    public static final String CMD_LOGIN = "LOGIN";
    public static final String CMD_LOGOUT = "LOGOUT";
    public static final String CMD_PUBLIC_MSG = "PUBLIC_MSG";
    public static final String CMD_PRIVATE_MSG = "PRIVATE_MSG";
    public static final String CMD_GET_ONLINE_USERS = "GET_ONLINE_USERS";

    // 分隔符
    public static final String DELIMITER = "|";

    // 响应状态
    public static final String RESP_OK = "OK";
    public static final String RESP_ERROR = "ERROR";
}