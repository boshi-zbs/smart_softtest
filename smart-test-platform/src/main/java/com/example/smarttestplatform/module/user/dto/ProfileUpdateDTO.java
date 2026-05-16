package com.example.smarttestplatform.module.user.dto;

import lombok.Data;

@Data
public class ProfileUpdateDTO {
    private String realName;
    private String email;
    private String phone;
}