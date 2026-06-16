package com.example.smarttestplatform.module.message.service.impl;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.module.message.entity.Message;
import com.example.smarttestplatform.module.message.mapper.MessageMapper;
import com.example.smarttestplatform.module.message.service.MessageService;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public void sendMessage(Message message) {
        message.setIsRead(false);
        message.setCreateTime(new Date());
        // 如果前端没有传 status，默认 null 或 pending 根据需要
        if (message.getStatus() == null && "待办".equals(message.getType())) {
            message.setStatus("pending");
        }
        messageMapper.insert(message);
    }

    @Override
    public PageResult<Message> getMessages(PageRequest pageRequest, Integer receiverId) {
        Map<String, Object> conditions = pageRequest.getConditions();
        conditions.put("receiverId", receiverId);
        int offset = (pageRequest.getPage() - 1) * pageRequest.getSize();
        List<Message> records = messageMapper.pageQuery(conditions, offset, pageRequest.getSize());
        // 填充发送者姓名
        for (Message msg : records) {
            if (msg.getSenderId() != null) {
                User sender = userMapper.findById(msg.getSenderId());
                if (sender != null) {
                    msg.setSenderName(sender.getRealName() != null ? sender.getRealName() : sender.getUsername());
                }
            }
        }
        Long total = messageMapper.count(conditions);
        return new PageResult<>(records, total, pageRequest.getPage(), pageRequest.getSize());
    }

    @Override
    @Transactional
    public void markAsRead(Integer messageId) {
        messageMapper.markAsRead(messageId);
    }

    @Override
    @Transactional
    public void markAllAsRead(Integer receiverId) {
        messageMapper.markAllAsRead(receiverId);
    }

    @Override
    public int countUnread(Integer receiverId) {
        return messageMapper.countUnread(receiverId);
    }

    @Override
    @Transactional
    public void complete(Integer messageId) {
        int rows = messageMapper.complete(messageId);
        if (rows == 0) {
            throw new RuntimeException("消息不存在或已完成");
        }
    }

    @Override
    @Transactional
    public void completeRelatedTodos(Integer relatedId, String relatedType) {
        // 更新所有关联且状态为 pending 的待办消息为 completed
        messageMapper.completeByRelated(relatedId, relatedType);
    }
}