package com.example.smarttestplatform.module.mytodo.service.impl;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.module.mytodo.dto.TodoItemDTO;
import com.example.smarttestplatform.module.mytodo.mapper.MyTodoMapper;
import com.example.smarttestplatform.module.mytodo.service.MyTodoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MyTodoServiceImpl implements MyTodoService {

    @Autowired
    private MyTodoMapper myTodoMapper;

    @Override
    public PageResult<TodoItemDTO> getMyTodos(Integer userId, PageRequest pageRequest) {
        int offset = (pageRequest.getPage() - 1) * pageRequest.getSize();
        List<TodoItemDTO> list = myTodoMapper.queryMyTodos(userId, offset, pageRequest.getSize());
        Long total = myTodoMapper.countMyTodos(userId);
        return new PageResult<>(list, total, pageRequest.getPage(), pageRequest.getSize());
    }
}