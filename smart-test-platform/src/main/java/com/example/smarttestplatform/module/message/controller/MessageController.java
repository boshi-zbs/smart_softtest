package com.example.smarttestplatform.module.message.controller;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.common.utils.Result;
import com.example.smarttestplatform.module.message.entity.Message;
import com.example.smarttestplatform.module.message.service.MessageService;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;
    @Autowired
    private UserService userService;

    // 获取当前用户的消息列表（分页）
    @GetMapping
    public Result<PageResult<Message>> listMessages(@AuthenticationPrincipal UserDetails userDetails,
                                                    @RequestParam Map<String, String> allParams) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }
        PageRequest pageRequest = new PageRequest();
        String pageStr = allParams.getOrDefault("page", "1");
        String sizeStr = allParams.getOrDefault("size", "10");
        try {
            pageRequest.setPage(Integer.parseInt(pageStr));
            pageRequest.setSize(Integer.parseInt(sizeStr));
        } catch (NumberFormatException e) {
            pageRequest.setPage(1);
            pageRequest.setSize(10);
        }
        allParams.remove("page");
        allParams.remove("size");
        Map<String, Object> conditions = new HashMap<>(allParams);
        pageRequest.setConditions(conditions);
        PageResult<Message> pageResult = messageService.getMessages(pageRequest, user.getId());
        return Result.success(pageResult);
    }

    // 获取未读消息数量
    @GetMapping("/unread/count")
    public Result<Integer> countUnread(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }
        int count = messageService.countUnread(user.getId());
        return Result.success(count);
    }

    // 标记单条消息为已读
    @PutMapping("/{id}/read")
    public Result<String> markAsRead(@PathVariable Integer id) {
        messageService.markAsRead(id);
        return Result.success("标记成功");
    }

    // 标记所有消息为已读
    @PutMapping("/read-all")
    public Result<String> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }
        messageService.markAllAsRead(user.getId());
        return Result.success("标记成功");
    }

    @PutMapping("/{id}/complete")
    public Result<String> complete(@PathVariable Integer id) {
        messageService.complete(id);
        return Result.success("已完成");
    }
}