package main.java.com.collaboration.domain;

import java.time.LocalDateTime;

public class Message {
    // 定义消息类型的常量或枚举
    public static final int TYPE_PUBLIC = 0;
    public static final int TYPE_PRIVATE = 1;

    //唯一标识
    private String messageId;
    //发送者ID(关联User)
    private String senderId;
    //昵称
    private String senderName;
    //接受者ID(null为公共消息)
    private String receiverID;
    //消息内容
    private String content;
    //公共/私聊/系统消息
    private Type type;
    //发送时间
    private String timestamp;
    //是否已读(私信)
    private boolean isRead;

    // 构造方法
    public Message() {
    }

    public Message(String messageId, String senderId, String senderName, String receiverID, String content, Type type, String  timestamp, boolean isRead) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverID = receiverID;
        this.content = content;
        this.type = type;
        this.timestamp =LocalDateTime.now().toString();
        this.isRead = isRead;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getReceiverID() {
        return receiverID;
    }

    public void setReceiverID(String receiverID) {
        this.receiverID = receiverID;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    // 辅助方法：判断是否为公共消息
    public boolean isPublicMessage() {
        return getType() == Type.PUBLIC;
    }

    public enum Type{
        PUBLIC,PRIVATE,SYSTEM
    }
}


