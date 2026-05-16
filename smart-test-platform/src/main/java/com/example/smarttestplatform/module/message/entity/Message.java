package com.example.smarttestplatform.module.message.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Message {
    private Integer id;
    private Integer senderId;
    private Integer receiverId;
    private String title;
    private String content;
    private String type;        // 通知、待办、提醒
    private Integer relatedId;
    private String relatedType;
    private Boolean isRead;
    private Date readTime;
    private Date createTime;
    private String status;

    // 非数据库字段
    private String senderName;
    private String receiverName;
}