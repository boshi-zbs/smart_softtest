package com.example.smarttestplatform.module.mytodo.service;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.module.mytodo.dto.TodoItemDTO;

public interface MyTodoService {
    PageResult<TodoItemDTO> getMyTodos(Integer userId, PageRequest pageRequest);
}