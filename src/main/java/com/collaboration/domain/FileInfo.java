package main.java.com.collaboration.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

/**
 * 文件信息实体类
 * 用于记录文件的元数据信息
 */
public class FileInfo {

    // ========== 属性定义 ==========

    private String fileId;           // 文件唯一标识（UUID）
    private String name;             // 原始文件名
    private long fileSize;           // 文件大小（字节）
    private String uploaderId;       // 上传者ID
    private String uploaderName;     // 上传者昵称（用于显示）
    private String uploadTime;       // 上传时间（String类型，避免Jackson问题）
    private String savePath;         // 存储路径（相对路径，如 files/xxx.jpg）
    private String fileType;         // 文件扩展名（如 .jpg, .txt, .pdf）
    private int totalChunks;         // 总分块数（用于断点续传模拟）
    private long transferredBytes;   // 已传输字节数
    private boolean isComplete;      // 是否传输完成


    // ========== 构造方法 ==========

    /**
     * 无参构造方法（Jackson JSON反序列化需要）
     */
    public FileInfo() {
    }

    /**
     * 全参构造方法
     */
    public FileInfo(String fileId, String name, long fileSize,
                    String uploaderId, String uploaderName,
                    String uploadTime, String savePath, String fileType,
                    int totalChunks, long transferredBytes, boolean isComplete) {
        this.fileId = fileId;
        this.name = name;
        this.fileSize = fileSize;
        this.uploaderId = uploaderId;
        this.uploaderName = uploaderName;
        this.uploadTime = uploadTime;
        this.savePath = savePath;
        this.fileType = fileType;
        this.totalChunks = totalChunks;
        this.transferredBytes = transferredBytes;
        this.isComplete = isComplete;
    }


    // ========== Getter 和 Setter 方法 ==========

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

    public String getUploaderName() {
        return uploaderName;
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }

    public String getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(String uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
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

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }



    // ========== 辅助方法 ==========

    /**
     * 获取传输进度百分比
     * 实现步骤：
     * 1. 判断 fileSize 是否大于 0
     * 2. 如果 fileSize == 0，返回 0
     * 3. 计算 (transferredBytes * 100) / fileSize
     * 4. 返回 int 类型的百分比
     */
    @JsonIgnore
    public int getProgressPercent() {
        if (fileSize <= 0) {
            return 0;
        }
        return (int) ((transferredBytes * 100) / fileSize);
    }

    /**
     * 格式化显示文件大小（如 1.5 MB, 1024 B, 2.3 GB）
     * 实现步骤：
     * 1. 如果 fileSize < 1024，返回 fileSize + " B"
     * 2. 如果 fileSize < 1024 * 1024，返回 (fileSize / 1024.0) + " KB"
     * 3. 如果 fileSize < 1024 * 1024 * 1024，返回 (fileSize / 1024.0 / 1024.0) + " MB"
     * 4. 否则返回 (fileSize / 1024.0 / 1024.0 / 1024.0) + " GB"
     * 5. 使用 String.format("%.2f", value) 格式化小数位
     */
    @JsonIgnore
    public String getFormattedSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            double kb = fileSize / 1024.0;
            return String.format("%.2f KB", kb);
        } else if (fileSize < 1024 * 1024 * 1024) {
            double mb = fileSize / (1024.0 * 1024.0);
            return String.format("%.2f MB", mb);
        } else {
            double gb = fileSize / (1024.0 * 1024.0 * 1024.0);
            return String.format("%.2f GB", gb);
        }
    }

    /**
     * 获取文件状态描述
     * 实现步骤：
     * 1. 如果 isComplete 为 true，返回 "✓ 已完成"
     * 2. 否则返回 "⏳ 传输中 (" + getProgressPercent() + "%)"
     */
    @JsonIgnore
    public String getStatusDesc() {
        if (isComplete) {
            return "✓ 已完成";
        } else {
            return "⏳ 传输中 (" + getProgressPercent() + "%)";
        }
    }
}