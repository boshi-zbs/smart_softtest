package com.example.smarttestplatform.module.mytodo.controller;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.common.utils.Result;
import com.example.smarttestplatform.module.mytodo.dto.TodoItemDTO;
import com.example.smarttestplatform.module.mytodo.service.MyTodoService;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mytodo")
@PreAuthorize("hasAnyRole('DEV', 'ADMIN')")
public class MyTodoController {

    @Autowired
    private MyTodoService myTodoService;
    @Autowired
    private UserService userService;

    @GetMapping
    public Result<PageResult<TodoItemDTO>> getMyTodos(@AuthenticationPrincipal UserDetails userDetails,
                                                      PageRequest pageRequest) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }
        PageResult<TodoItemDTO> pageResult = myTodoService.getMyTodos(user.getId(), pageRequest);
        return Result.success(pageResult);
    }
}