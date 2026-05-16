package com.example.smarttestplatform.module.user.service;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.module.user.dto.ProfileUpdateDTO;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.entity.Role;

import java.util.List;

public interface UserService {
    User findById(Integer id);
    User findByUsername(String username);
    List<User> findAll();
    void createUser(User user, List<Integer> roleIds);
    void updateUser(User user, List<Integer> roleIds);
    void deleteUser(Integer id);
    List<Role> getUserRoles(Integer userId);

    void updateProfile(Integer userId, ProfileUpdateDTO dto);
    void changePassword(Integer userId, String oldPassword, String newPassword);
    PageResult<User> pageQuery(PageRequest pageRequest);

    void deleteBatch(List<Integer> ids);
}