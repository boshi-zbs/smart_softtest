package com.example.smarttestplatform.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:./uploads}")  // 可在 application.yml 中配置
    private String uploadDir;

    /**
     * 保存文件，返回访问URL（相对路径形式）
     */
    public String store(MultipartFile file) throws IOException {
        System.out.println("FileStorageService.store called");
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String storedFileName = UUID.randomUUID().toString() + extension;

        // 按日期创建子目录，避免单目录文件过多
        String datePath = java.time.LocalDate.now().toString().replace("-", "/");
        Path targetDir = Paths.get(uploadDir, datePath);
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        Path targetPath = targetDir.resolve(storedFileName);
        file.transferTo(targetPath.toFile());

        // 返回可访问的相对路径（前端拼接后端静态资源映射前缀）
        return "/uploads/" + datePath + "/" + storedFileName;
    }
}