package com.example.smarttestplatform.module.user.service.impl;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.module.user.dto.ProfileUpdateDTO;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.entity.Role;
import com.example.smarttestplatform.module.user.mapper.UserMapper;
import com.example.smarttestplatform.module.user.mapper.RoleMapper;
import com.example.smarttestplatform.module.user.mapper.UserRoleMapper;
import com.example.smarttestplatform.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User findById(Integer id) {
        return userMapper.findById(id);
    }

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public List<User> findAll() {
        return userMapper.findAll();
    }

    @Override
    @Transactional
    public void createUser(User user, List<Integer> roleIds) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
        if (roleIds != null) {
            for (Integer roleId : roleIds) {
                userRoleMapper.insertUserRole(user.getId(), roleId);
            }
        }
    }

    @Override
    @Transactional
    public void updateUser(User user, List<Integer> roleIds) {
        // 如果密码不为空且不是空字符串，则加密
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            // 如果密码为空，从数据库获取原有密码（避免覆盖）
            User existingUser = userMapper.findById(user.getId());
            if (existingUser != null) {
                user.setPassword(existingUser.getPassword());
            }
        }
        userMapper.update(user);
        // 更新角色：先删除旧角色，再添加新角色
        userRoleMapper.deleteByUserId(user.getId());
        if (roleIds != null) {
            for (Integer roleId : roleIds) {
                userRoleMapper.insertUserRole(user.getId(), roleId);
            }
        }
    }

    @Override
    @Transactional
    public void deleteUser(Integer id) {
        userRoleMapper.deleteByUserId(id);
        userMapper.deleteById(id);
    }

    @Override
    public List<Role> getUserRoles(Integer userId) {
        return roleMapper.findRolesByUserId(userId);
    }

    @Override
    @Transactional
    public void updateProfile(Integer userId, ProfileUpdateDTO dto) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setRealName(dto.getRealName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        userMapper.update(user);
    }

    @Override
    @Transactional
    public void changePassword(Integer userId, String oldPassword, String newPassword) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.update(user);
    }

    @Override
    public PageResult<User> pageQuery(PageRequest pageRequest) {
        Map<String, Object> conditions = pageRequest.getConditions();
        Integer page = pageRequest.getPage();
        Integer size = pageRequest.getSize();
        int offset = (page - 1) * size;
        // 分页查询用户
        List<User> records = userMapper.pageQuery(conditions, offset, size);
        // 查询总记录数
        Long total = userMapper.count(conditions);

        // 对于每个用户，加载其角色信息（可选，也可以在 SQL 中连表查询）
        for (User user : records) {
            List<Role> roles = roleMapper.findRolesByUserId(user.getId());
            user.setRoles(roles); // 需要在 User 实体中添加 transient 字段 roles
        }

        return new PageResult<>(records, total, page, size);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        // 如果数据库设置了级联删除，直接删除 user 即可
        // 否则需要先删除 user_role 关联
        userMapper.deleteBatch(ids);
    }
}