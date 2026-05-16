package com.example.smarttestplatform.module.autotest.util;

import com.example.smarttestplatform.module.autotest.config.BrowserDriverManager;
import com.example.smarttestplatform.module.autotest.dto.ExecuteResult;
import com.example.smarttestplatform.module.autotest.entity.AutoTestCase;
import com.example.smarttestplatform.module.autotest.entity.AutoTestStep;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class SeleniumExecutor {

    @Autowired
    private BrowserDriverManager driverManager;
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;
    public ExecuteResult execute(AutoTestCase testCase, List<AutoTestStep> steps, boolean headless) {
        WebDriver driver = null;
        StringBuilder log = new StringBuilder();
        boolean success = true;
        String screenshotUrl = null;
        try {
            log.append("=== 自动化测试执行开始 ===\n");
            log.append("用例名称：").append(testCase.getCaseName()).append("\n");
            log.append("浏览器：Chrome\n");
            log.append("运行模式：").append(headless ? "无头模式（后台运行）" : "有头模式（显示界面）").append("\n");
            log.append("正在启动 Chrome 浏览器...\n");

            driver = driverManager.createDriver(headless);
            log.append("Chrome 浏览器启动成功\n");

            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));

            if (!headless) {
                driver.manage().window().maximize();
            }

            String url = replaceUrlVariables(testCase.getUrl(), testCase);
            log.append("打开URL：").append(url).append("\n");
            driver.get(url);
            Thread.sleep(2000);

            for (int i = 0; i < steps.size(); i++) {
                AutoTestStep step = steps.get(i);
                log.append("\n===== 步骤 ").append(step.getStepOrder()).append(" =====");
                log.append("\n描述：").append(step.getDescription() != null ? step.getDescription() : "");
                log.append("\n操作：").append(step.getActionType());

                if (step.getWaitSeconds() != null && step.getWaitSeconds() > 0) {
                    log.append("\n等待 ").append(step.getWaitSeconds()).append(" 秒...");
                    Thread.sleep(step.getWaitSeconds() * 1000L);
                }

                try {
                    executeStep(driver, step, url);
                    log.append("\n✓ 执行成功\n");
                } catch (Exception e) {
                    log.append("\n✗ 执行失败: ").append(e.getMessage()).append("\n");
                    success = false;
                    break;
                }
            }

            log.append("\n=== 测试执行").append(success ? "成功" : "失败").append(" ===\n");

        } catch (Exception e) {
            success = false;
            log.append("\n执行异常：").append(e.getMessage()).append("\n");
            e.printStackTrace();
        } finally {
            if (driver != null) {
                try {
                    Thread.sleep(500);
                    // 进行截图
                    screenshotUrl = captureScreenshot(driver);
                    if (screenshotUrl != null) {
                        log.append("\n截图已保存: ").append(screenshotUrl).append("\n");
                    }
                    driver.quit();
                } catch (Exception e) {
                    log.append("\n关闭浏览器异常：").append(e.getMessage()).append("\n");
                }
            }
        }
        // 将截图URL附加到日志中，或单独存储
        return new ExecuteResult(success, log.toString(), screenshotUrl);
    }

    private String replaceUrlVariables(String url, AutoTestCase testCase) {
        if (url == null) return url;
        if (testCase.getProjectName() != null) {
            url = url.replace("{projectName}", testCase.getProjectName());
        }
        return url;
    }

    private void executeStep(WebDriver driver, AutoTestStep step, String baseUrl) {
        String actionType = step.getActionType().toLowerCase();

        // 处理不需要定位元素的操作
        switch (actionType) {
            case "open":
                String targetUrl = step.getLocatorValue() != null ? step.getLocatorValue() : "";
                if (!targetUrl.startsWith("http")) {
                    targetUrl = baseUrl + targetUrl;
                }
                driver.get(targetUrl);
                return;
            case "wait":
                return;
            case "asserttitle":
                String expectedTitle = step.getInputValue();
                String actualTitle = driver.getTitle();
                if (!actualTitle.equals(expectedTitle)) {
                    throw new AssertionError("标题断言失败，期望: " + expectedTitle + ", 实际: " + actualTitle);
                }
                return;
            case "switchtodefaultcontent":
                driver.switchTo().defaultContent();
                return;
            case "executescript":
                JavascriptExecutor js = (JavascriptExecutor) driver;
                Object jsResult = js.executeScript(step.getInputValue());
                System.out.println("JS执行结果: " + jsResult);
                return;
            case "back":
                driver.navigate().back();
                return;
            case "forward":
                driver.navigate().forward();
                return;
            case "refresh":
                driver.navigate().refresh();
                return;
            case "assertelementpresent":
                return;
        }

        // 需要定位元素的操作
        WebElement element = findElement(driver, step);

        switch (actionType) {
            case "click":
                element.click();
                break;
            case "input":
                element.clear();
                element.sendKeys(step.getInputValue());
                break;
            case "clear":
                // 方法1：尝试使用 clear()
                try {
                    element.clear();
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.out.println("clear() 失败: " + e.getMessage());
                }

                // 方法2：使用 Ctrl+A + Delete 清空
                try {
                    element.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                    element.sendKeys(Keys.DELETE);
                } catch (Exception e) {
                    System.out.println("Ctrl+A+Delete 清空失败: " + e.getMessage());
                }

                // 方法3：使用 JavaScript 清空（最可靠）
                try {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("arguments[0].value = '';", element);
                } catch (Exception e) {
                    System.out.println("JS清空失败: " + e.getMessage());
                }
                break;
            case "select":
                Select select = new Select(element);
                if (step.getInputValue() != null) {
                    try {
                        select.selectByVisibleText(step.getInputValue());
                    } catch (Exception e) {
                        try {
                            select.selectByValue(step.getInputValue());
                        } catch (Exception ex) {
                            select.selectByIndex(Integer.parseInt(step.getInputValue()));
                        }
                    }
                }
                break;
            case "hover":
                new Actions(driver).moveToElement(element).perform();
                break;
            case "doubleclick":
                new Actions(driver).doubleClick(element).perform();
                break;
            case "contextclick":
                new Actions(driver).contextClick(element).perform();
                break;
            case "asserttext":
                String actualText = element.getText();
                String expectedText = step.getInputValue();
                if (!actualText.equals(expectedText)) {
                    throw new AssertionError("文本断言失败，期望: " + expectedText + ", 实际: " + actualText);
                }
                break;
            case "assertelementvisible":
                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.visibilityOf(element));
                break;
            case "getattribute":
                String attrValue = element.getAttribute(step.getInputValue());
                System.out.println("属性 " + step.getInputValue() + " 的值为: " + attrValue);
                break;
            case "scrollintoview":
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
                break;
            case "switchtoframe":
                driver.switchTo().frame(element);
                break;
            case "switchtowindow":
                String targetTitle = step.getInputValue();
                if (targetTitle != null && !targetTitle.isEmpty()) {
                    for (String handle : driver.getWindowHandles()) {
                        driver.switchTo().window(handle);
                        if (driver.getTitle().contains(targetTitle)) {
                            break;
                        }
                    }
                }
                break;
            case "uploadfile":
                element.sendKeys(step.getInputValue());
                break;
            case "keypress":
                try {
                    element.sendKeys(Keys.valueOf(step.getInputValue().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    element.sendKeys(step.getInputValue());
                }
                break;
            default:
                throw new IllegalArgumentException("未知操作: " + step.getActionType());
        }
    }

    private WebElement findElement(WebDriver driver, AutoTestStep step) {
        String locatorType = step.getLocatorType();
        String locatorValue = step.getLocatorValue();

        if (locatorType == null || locatorValue == null) {
            throw new IllegalArgumentException("定位方式或定位值不能为空");
        }

        By by;
        switch (locatorType.toLowerCase()) {
            case "id":
                by = By.id(locatorValue);
                break;
            case "name":
                by = By.name(locatorValue);
                break;
            case "xpath":
                by = By.xpath(locatorValue);
                break;
            case "css":
            case "cssselector":
                by = By.cssSelector(locatorValue);
                break;
            case "linktext":
                by = By.linkText(locatorValue);
                break;
            case "partiallinktext":
                by = By.partialLinkText(locatorValue);
                break;
            case "tagname":
                by = By.tagName(locatorValue);
                break;
            case "classname":
                by = By.className(locatorValue);
                break;
            default:
                throw new IllegalArgumentException("不支持的定位方式: " + locatorType);
        }

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        return wait.until(ExpectedConditions.presenceOfElementLocated(by));
    }
    private String captureScreenshot(WebDriver driver) {
        if (driver == null) return null;
        try {
            if (driver instanceof TakesScreenshot) {
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                String fileName = "auto_" + System.currentTimeMillis() + ".png";
                Path targetPath = Paths.get(uploadDir, "screenshots", fileName);
                Files.createDirectories(targetPath.getParent());
                Files.write(targetPath, screenshot);
                return "/uploads/screenshots/" + fileName;
            }
        } catch (Exception e) {
            System.err.println("截图失败: " + e.getMessage());
        }
        return null;
    }
}