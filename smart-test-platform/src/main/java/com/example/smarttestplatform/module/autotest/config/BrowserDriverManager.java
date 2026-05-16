package com.example.smarttestplatform.module.autotest.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;

@Component
public class BrowserDriverManager {

    @PostConstruct
    public void init() {
        setupMirrors();
    }

    private void setupMirrors() {
        System.setProperty("wdm.chromeDriverUrl", "https://npmmirror.com/mirrors/chromedriver/");
        System.setProperty("wdm.timeout", "60");
        System.setProperty("wdm.cachePath", System.getProperty("user.home") + "/.webdrivers");
        System.setProperty("wdm.avoidExport", "true");
        System.setProperty("wdm.avoidRootUpdate", "true");
    }

    public WebDriver createDriver(boolean headless) {
        return createChromeDriver(headless);
    }

    private WebDriver createChromeDriver(boolean headless) {
        // 优先使用本地驱动
        String localDriverPath = findLocalChromeDriver();
        if (localDriverPath != null) {
            try {
                System.setProperty("webdriver.chrome.driver", localDriverPath);
                System.out.println("使用本地 Chrome 驱动: " + localDriverPath);
            } catch (Exception e) {
                System.err.println("设置本地驱动失败: " + e.getMessage());
            }
        }

        try {
            // 如果本地没有，则使用 WebDriverManager 自动下载
            if (localDriverPath == null) {
                WebDriverManager.chromedriver().setup();
                System.out.println("使用 WebDriverManager 自动下载的 Chrome 驱动");
            }

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("--disable-extensions");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--remote-allow-origins=*");
            options.addArguments("--disable-search-engine-choice-screen");

            if (headless) {
                options.addArguments("--headless");
                options.addArguments("--window-size=1920,1080");
                options.addArguments("--disable-gpu");
            } else {
                options.addArguments("--start-maximized");
            }

            return new ChromeDriver(options);
        } catch (Exception e) {
            throw new RuntimeException("创建 Chrome 驱动失败: " + e.getMessage(), e);
        }
    }

    private String findLocalChromeDriver() {
        // 检查项目根目录
        File projectDir = new File(System.getProperty("user.dir"));
        File driverFile = new File(projectDir, "chromedriver.exe");
        if (driverFile.exists()) {
            return driverFile.getAbsolutePath();
        }

        // 检查 resources 目录
        File resourcesDir = new File(projectDir, "src/main/resources");
        driverFile = new File(resourcesDir, "chromedriver.exe");
        if (driverFile.exists()) {
            return driverFile.getAbsolutePath();
        }

        return null;
    }
}