package com.example.smarttestplatform.module.user.service;

import com.example.smarttestplatform.module.user.entity.Role;
import java.util.List;

public interface RoleService {
    Role findById(Integer id);
    Role findByCode(String roleCode);
    List<Role> findAll();
    void createRole(Role role);
    void updateRole(Role role);
    void deleteRole(Integer id);
    List<Role> findRolesByUserId(Integer userId);
}