package main.java.com.collaboration.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.java.com.collaboration.domain.Message;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class MessageService {
    //消息业务逻辑层，负责消息的存储、查询、队列管理
    private final LinkedList<Message> messageQueue;     // 消息队列（内存缓存）
    private final String storagePath;                   // 存储文件路径，如 "data/messages.json"
    private final ObjectMapper objectMapper;            // Jackson JSON 解析器
    private final ReentrantReadWriteLock lock;          // 读写锁（保证线程安全）

    public MessageService(String storagePath)  {
        messageQueue=new LinkedList<>();
        objectMapper=new ObjectMapper();
        // 忽略 JSON 中未知的字段（防止旧数据导致读取失败）
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.storagePath=storagePath;
        lock=new ReentrantReadWriteLock();
        loadMessagesFromFile();
    }

    //从文件加载信息
    private void loadMessagesFromFile() {
        File file = new File(storagePath);
        if (!file.exists()) {
            return;
        }

        List<Message> messages;
        try {
            // 读取文件时不需要持有锁
            messages = objectMapper.readValue(file, new TypeReference<List<Message>>() {});
        } catch (IOException e) {
            System.err.println("读取消息文件失败: " + e.getMessage());
            return;
        }

        // 只在操作内存队列时持有锁
        lock.writeLock().lock();
        try {
            messageQueue.clear();
            messageQueue.addAll(messages);
        } finally {
            lock.writeLock().unlock();
        }
    }

    //保存单条消息
    public void saveMessage(Message message){
        //获取写锁 lock.writeLock().lock()
        lock.writeLock().lock();
        try {
            //将消息添加到 messageQueue 末尾（messageQueue.addLast(message)）
            messageQueue.addLast(message);
            //调用 persistToFile() 方法将整个队列持久化到 JSON 文件
            persistToFile();
        } catch (Exception e) {
            throw new RuntimeException("保存消息失败", e);
        }finally {
            //在 finally 块中释放写锁
            lock.writeLock().unlock();
        }
        //注意：每次保存都全量写入文件（简单实现），生产环境可优化为增量写入
    }

    /**
     * 持久化到文件
     * ⚠️ 注意：调用此方法前必须已持有写锁（writeLock）
     */
    private void persistToFile(){
        try {

            //创建 File file = new File(storagePath)
            File file=new File(storagePath);
            //确保父目录存在：file.getParentFile().mkdirs()
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();  // 使用 mkdirs() 创建多级目录
            }

            //使用 objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, messageQueue) 写入
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file,messageQueue);
        } catch (IOException e) {
            //捕获 IO 异常并打印
            throw new RuntimeException(e);
        }

    }

    //分页查询消息（Stream流）
    public List<Message> getMessages(int page,int size){
        //参数校验：page < 1 时默认为 1，size < 1 时默认为 10
        if(page<1){
            page=1;
        }
        if(size<1){
            size=10;
        }
        //获取读锁 lock.readLock().lock()
        lock.readLock().lock();
        try {
            //使用 Stream 流操作：
            //messageQueue.stream() 创建流
            //.skip((page - 1) * size) 跳过前面的记录
            //.limit(size) 限制返回数量
            //.collect(Collectors.toList()) 收集为列表
            List<Message> list = messageQueue.stream()
                    .skip((long) (page - 1) * size)
                    .limit(size)
                    .toList();

            //返回结果列表
            return list;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            //在 finally 块中释放读锁
            lock.readLock().unlock();
        }

    }

    //按用户名筛选消息
    public List<Message> getMessagesByUser(String username){
        //参数校验：username 为 null 或空字符串时返回空列表
        if(username==null || username.trim().isEmpty()){
            return new ArrayList<>();
        }
        //获取读锁 lock.readLock().lock()
        lock.readLock().lock();
        try {
            //使用 Stream 流操作：
            //messageQueue.stream() 创建流
            //.filter(msg -> username.equals(msg.getSender()) || username.equals(msg.getReceiver())) 筛选
            //.collect(Collectors.toList()) 收集为列表
            //在 finally 块中释放读锁
            //返回结果列表
            return   messageQueue.stream()
                    .filter(msg -> username.equals(msg.getSenderName()) || username.equals(msg.getReceiverID()))
                    .toList();


        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            lock.readLock().unlock();
        }
    }

    //获取消息总数
    public int getMessageCount(){
        //获取读锁
        lock.readLock().lock();
        try {
            //返回 messageQueue.size()
            return messageQueue.size();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            //释放读锁
            lock.readLock().unlock();
        }
    }

    /**
     * 按时间范围查询消息（扩展功能）
     * @param start 开始时间（包含）
     * @param end   结束时间（包含）
     * @return 时间范围内的消息列表（按时间升序）
     */
    public List<Message> getMessagesByTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return new ArrayList<>();
        }

        String startStr = start.toString();
        String endStr = end.toString();

        lock.readLock().lock();
        try {
            return messageQueue.stream()
                    .filter(msg -> {
                        String timestamp = msg.getTimestamp();
                        return timestamp != null &&
                                timestamp.compareTo(startStr) >= 0 &&
                                timestamp.compareTo(endStr) <= 0;
                    })
                    .sorted((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }






}
