package com.example.smarttestplatform.module.user.dto;

import lombok.Data;

@Data
public class UpdateUserInfoDTO {
    private String realName;
    private String email;
    private String phone;
}