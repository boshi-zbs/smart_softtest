package com.example.smarttestplatform.module.user.service.impl;

import com.example.smarttestplatform.module.user.entity.Role;
import com.example.smarttestplatform.module.user.mapper.RoleMapper;
import com.example.smarttestplatform.module.user.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleMapper roleMapper;

    @Override
    public Role findById(Integer id) {
        return roleMapper.findById(id);
    }

    @Override
    public Role findByCode(String roleCode) {
        return roleMapper.findByCode(roleCode);
    }

    @Override
    public List<Role> findAll() {
        return roleMapper.findAll();
    }

    @Override
    @Transactional
    public void createRole(Role role) {
        roleMapper.insert(role);
    }

    @Override
    @Transactional
    public void updateRole(Role role) {
        roleMapper.update(role);
    }

    @Override
    @Transactional
    public void deleteRole(Integer id) {
        roleMapper.deleteById(id);
    }

    @Override
    public List<Role> findRolesByUserId(Integer userId) {
        return roleMapper.findRolesByUserId(userId);
    }
}