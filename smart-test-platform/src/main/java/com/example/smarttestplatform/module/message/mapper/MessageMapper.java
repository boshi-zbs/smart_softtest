package com.example.smarttestplatform.module.message.mapper;

import com.example.smarttestplatform.module.message.entity.Message;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface MessageMapper {

    @Insert("INSERT INTO message(sender_id, receiver_id, title, content, type, related_id, related_type, is_read) " +
            "VALUES(#{senderId}, #{receiverId}, #{title}, #{content}, #{type}, #{relatedId}, #{relatedType}, #{isRead})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Message message);

    @Update("UPDATE message SET is_read = 1, read_time = NOW() WHERE id = #{id} AND is_read = 0")
    int markAsRead(@Param("id") Integer id);

    @Update("UPDATE message SET is_read = 1, read_time = NOW() WHERE receiver_id = #{receiverId} AND is_read = 0")
    int markAllAsRead(@Param("receiverId") Integer receiverId);

    @Select("SELECT * FROM message WHERE id = #{id}")
    Message findById(@Param("id") Integer id);

    List<Message> pageQuery(@Param("conditions") Map<String, Object> conditions,
                            @Param("offset") int offset,
                            @Param("size") int size);

    Long count(@Param("conditions") Map<String, Object> conditions);

    @Select("SELECT COUNT(*) FROM message WHERE receiver_id = #{receiverId} AND is_read = 0")
    int countUnread(@Param("receiverId") Integer receiverId);

    @Update("UPDATE message SET status = 'completed' WHERE id = #{id} AND status = 'pending'")
    int complete(Integer id);

    void completeByRelated(Integer relatedId, String relatedType);
}