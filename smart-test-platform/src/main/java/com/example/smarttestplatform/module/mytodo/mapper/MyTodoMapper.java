package com.example.smarttestplatform.module.mytodo.mapper;

import com.example.smarttestplatform.module.mytodo.dto.TodoItemDTO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface MyTodoMapper {
    List<TodoItemDTO> queryMyTodos(@Param("userId") Integer userId,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);
    Long countMyTodos(@Param("userId") Integer userId);
}