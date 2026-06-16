package com.example.smarttestplatform.module.message.service;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.module.message.entity.Message;

public interface MessageService {
    void sendMessage(Message message);
    PageResult<Message> getMessages(PageRequest pageRequest, Integer receiverId);
    void markAsRead(Integer messageId);
    void markAllAsRead(Integer receiverId);
    int countUnread(Integer receiverId);

    void complete(Integer messageId);
    void completeRelatedTodos(Integer relatedId, String relatedType);
}