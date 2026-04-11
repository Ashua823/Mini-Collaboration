package main.java.com.collaboration.domain;

import java.time.LocalDateTime;

public class FileInfo {
    //唯一标识
    private String fileId;
    //原始文件名
    private String name;
    //文件大小
    private long fileSize;
    //上传者ID
    private String uploaderId;
    //上传时间
    private LocalDateTime uploadTime;
    //存储路径
    private String savePath;
    //总分块数
    private int totalChunks; 
    private long transferredBytes;

    public FileInfo(String fileId, String name, long fileSize, String uploaderId, LocalDateTime uploadTime, String savePath, int totalChunks, long transferredBytes) {
        this.fileId = fileId;
        this.name = name;
        this.fileSize = fileSize;
        this.uploaderId = uploaderId;
        this.uploadTime = uploadTime;
        this.savePath = savePath;
        this.totalChunks = totalChunks;
        this.transferredBytes = transferredBytes;
    }

    public FileInfo() {
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(String uploaderId) {
        this.uploaderId = uploaderId;
    }


    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public long getTransferredBytes() {
        return transferredBytes;
    }

    public void setTransferredBytes(long transferredBytes) {
        this.transferredBytes = transferredBytes;
    }





}
