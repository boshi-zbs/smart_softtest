package com.example.smarttestplatform.module.autotest.service.impl;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.module.autotest.entity.AutoTestCase;
import com.example.smarttestplatform.module.autotest.entity.AutoTestExecution;
import com.example.smarttestplatform.module.autotest.entity.AutoTestStep;
import com.example.smarttestplatform.module.autotest.mapper.AutoTestCaseMapper;
import com.example.smarttestplatform.module.autotest.mapper.AutoTestExecutionMapper;
import com.example.smarttestplatform.module.autotest.mapper.AutoTestStepMapper;
import com.example.smarttestplatform.module.autotest.service.AutoTestService;
import com.example.smarttestplatform.module.project.mapper.ProjectMapper;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.mapper.UserMapper;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;

@Service
public class AutoTestServiceImpl implements AutoTestService {

    @Autowired
    private AutoTestCaseMapper testCaseMapper;
    @Autowired
    private AutoTestStepMapper stepMapper;
    @Autowired
    private AutoTestExecutionMapper executionMapper;
    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private UserMapper userMapper;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(); // 简单单线程执行，避免并发问题

    @Override
    public AutoTestCase findTestCaseById(Integer id) {
        AutoTestCase testCase = testCaseMapper.findById(id);
        if (testCase != null) {
            testCase.setSteps(stepMapper.findByCaseId(id));
            if (testCase.getProjectId() != null) {
                testCase.setProjectName(projectMapper.findById(testCase.getProjectId()).getProjectName());
            }
            if (testCase.getCreateUserId() != null) {
                User user = userMapper.findById(testCase.getCreateUserId());
                if (user != null) testCase.setCreatorName(user.getRealName());
            }
        }
        return testCase;
    }

    @Override
    public PageResult<AutoTestCase> pageQueryTestCase(PageRequest pageRequest) {
        Map<String, Object> conditions = pageRequest.getConditions();
        int offset = (pageRequest.getPage() - 1) * pageRequest.getSize();
        List<AutoTestCase> records = testCaseMapper.pageQuery(conditions, offset, pageRequest.getSize());
        for (AutoTestCase tc : records) {
            if (tc.getProjectId() != null) {
                tc.setProjectName(projectMapper.findById(tc.getProjectId()).getProjectName());
            }
            // 查询最近一次执行记录
            AutoTestExecution latest = executionMapper.findLatestByCaseId(tc.getId());
            if (latest != null) {
                tc.setLatestStatus(latest.getStatus());
                tc.setLatestStartTime(latest.getStartTime());
            } else {
                tc.setLatestStatus("none");  // 表示从未执行过
            }
        }
        Long total = testCaseMapper.count(conditions);
        return new PageResult<>(records, total, pageRequest.getPage(), pageRequest.getSize());
    }

    @Override
    @Transactional
    public void createTestCase(AutoTestCase testCase, Integer creatorId, List<AutoTestStep> steps) {
        testCase.setCreateUserId(creatorId);
        testCase.setStatus(1);
        testCaseMapper.insert(testCase);
        if (steps != null) {
            for (AutoTestStep step : steps) {
                step.setCaseId(testCase.getId());
                stepMapper.insert(step);
            }
        }
    }

    @Override
    @Transactional
    public void updateTestCase(AutoTestCase testCase, List<AutoTestStep> steps) {
        testCaseMapper.update(testCase);
        stepMapper.deleteByCaseId(testCase.getId());
        if (steps != null) {
            for (AutoTestStep step : steps) {
                step.setCaseId(testCase.getId());
                stepMapper.insert(step);
            }
        }
    }

    @Override
    @Transactional
    public void deleteTestCase(Integer id) {
        stepMapper.deleteByCaseId(id);
        testCaseMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteTestCase(id);
        }
    }

    @Override
    public List<AutoTestStep> getStepsByCaseId(Integer caseId) {
        return stepMapper.findByCaseId(caseId);
    }

    @Override
    public AutoTestExecution executeTestCase(Integer caseId, Integer executorId, String baseUrlOverride) {
        AutoTestCase testCase = testCaseMapper.findById(caseId);
        if (testCase == null) throw new RuntimeException("测试用例不存在");
        List<AutoTestStep> steps = stepMapper.findByCaseId(caseId);

        // 创建执行记录
        AutoTestExecution execution = new AutoTestExecution();
        execution.setCaseId(caseId);
        execution.setExecutorId(executorId);
        execution.setStartTime(new Date());
        execution.setStatus("running");
        executionMapper.insert(execution);

        // 异步执行，避免阻塞HTTP请求
        CompletableFuture.runAsync(() -> {
            executeWithSelenium(testCase, steps, baseUrlOverride, execution);
        }, executor);

        return execution;
    }

    private void executeWithSelenium(AutoTestCase testCase, List<AutoTestStep> steps, String baseUrlOverride, AutoTestExecution execution) {
        WebDriver driver = null;
        StringBuilder log = new StringBuilder();
        boolean success = true;
        try {
            // 手动指定 ChromeDriver 路径（请根据实际路径修改）
            System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
//            // 配置WebDriver（这里使用Chrome，可根据需要配置）
            ChromeOptions options = new ChromeOptions();
//            options.addArguments("--headless"); // 无头模式，可选
            driver = new ChromeDriver(options);

            driver.manage().window().maximize();
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));

            String baseUrl = baseUrlOverride != null ? baseUrlOverride : testCase.getUrl();
            if (baseUrl == null || baseUrl.isEmpty()) {
                throw new RuntimeException("未指定基础URL");
            }
            // 打开基础URL
            driver.get(baseUrl);
            log.append("打开URL：").append(baseUrl).append("\n");
            for (AutoTestStep step : steps) {
                log.append("步骤").append(step.getStepOrder()).append(": ").append(step.getDescription()).append("\n");
                try {
                    executeStep(driver, step, baseUrl);
                    log.append("执行成功\n");
                } catch (Exception e) {
                    log.append("执行失败: ").append(e.getMessage()).append("\n");
                    success = false;
                    break;
                }
            }
        } catch (Exception e) {
            log.append("初始化失败: ").append(e.getMessage()).append("\n");
            success = false;
        } finally {
            // 调试阶段暂时不关闭浏览器，便于观察
            if (driver != null) {
                driver.quit();
            }
            // 更新执行记录
            execution.setEndTime(new Date());
            execution.setStatus(success ? "success" : "failed");
            execution.setResult(log.toString());
            executionMapper.update(execution);
        }
    }

    private void executeStep(WebDriver driver, AutoTestStep step, String baseUrl) {
        // 步骤自身的等待（如果配置了 waitSeconds）
        if (step.getWaitSeconds() != null && step.getWaitSeconds() > 0) {
            try {
                Thread.sleep(step.getWaitSeconds() * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 对于不需要定位元素的操作，直接执行
        if ("open".equalsIgnoreCase(step.getActionType())) {
            driver.get(baseUrl + (step.getLocatorValue() != null ? step.getLocatorValue() : ""));
            return;
        }
        if ("wait".equalsIgnoreCase(step.getActionType())) {
            // 等待已在上面处理，这里无需操作
            return;
        }
        if ("assertTitle".equalsIgnoreCase(step.getActionType())) {
            String title = driver.getTitle();
            if (!title.equals(step.getInputValue())) {
                throw new AssertionError("标题断言失败，期望: " + step.getInputValue() + ", 实际: " + title);
            }
            return;
        }
        if ("switchToDefaultContent".equalsIgnoreCase(step.getActionType())) {
            driver.switchTo().defaultContent();
            return;
        }
        if ("executeScript".equalsIgnoreCase(step.getActionType())) {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object result = js.executeScript(step.getInputValue());
            // 可将结果记录到日志，这里简单打印
            System.out.println("JS执行结果: " + result);
            return;
        }

        // 其他操作需要先定位元素
        WebElement element = findElement(driver, step);

        switch (step.getActionType().toLowerCase()) {
            case "click":
                element.click();
                break;
            case "input":
                element.sendKeys(step.getInputValue());
                break;
            case "clear":
                element.clear();
                break;
            case "select":
                Select select = new Select(element);
                select.selectByVisibleText(step.getInputValue());
                break;
            case "hover":
                Actions actions = new Actions(driver);
                actions.moveToElement(element).perform();
                break;
            case "doubleclick":
                Actions doubleClickAction = new Actions(driver);
                doubleClickAction.doubleClick(element).perform();
                break;
            case "contextclick":
                Actions contextClickAction = new Actions(driver);
                contextClickAction.contextClick(element).perform();
                break;
            case "asserttext":
                String actualText = element.getText();
                if (!actualText.equals(step.getInputValue())) {
                    throw new AssertionError("文本断言失败，期望: " + step.getInputValue() + ", 实际: " + actualText);
                }
                break;
            case "assertelementpresent":
                // 元素已经通过 findElement 找到，若不存在会抛出异常，这里无需额外操作
                break;
            case "assertelementvisible":
                WebDriverWait waitVisible = new WebDriverWait(driver, Duration.ofSeconds(10));
                waitVisible.until(ExpectedConditions.visibilityOf(element));
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
                // 简单实现：根据窗口标题切换，inputValue 为标题包含的字符串
                for (String handle : driver.getWindowHandles()) {
                    driver.switchTo().window(handle);
                    if (driver.getTitle().contains(step.getInputValue())) {
                        break;
                    }
                }
                break;
            case "uploadfile":
                element.sendKeys(step.getInputValue());
                break;
            case "keypress":
                element.sendKeys(Keys.valueOf(step.getInputValue().toUpperCase()));
                break;
            default:
                throw new IllegalArgumentException("未知操作: " + step.getActionType());
        }
    }

    private WebElement findElement(WebDriver driver, AutoTestStep step) {
        String type = step.getLocatorType();
        String value = step.getLocatorValue();
        By by;
        switch (type) {
            case "id":
                by = By.id(value);
                break;
            case "name":
                by = By.name(value);
                break;
            case "xpath":
                by = By.xpath(value);
                break;
            case "css":
                by = By.cssSelector(value);
                break;
            case "linkText":           // 注意前端传的是 "linkText"
                by = By.linkText(value);
                break;
            case "partialLinkText":    // 新增
                by = By.partialLinkText(value);
                break;
            case "tagName":            // 前端传的是 "tagName"
                by = By.tagName(value);
                break;
            case "className":           // 新增
                by = By.className(value);
                break;
            default:
                throw new IllegalArgumentException("不支持的定位方式: " + type);
        }
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        return wait.until(ExpectedConditions.presenceOfElementLocated(by));
    }
    @Override
    public PageResult<AutoTestExecution> pageQueryExecution(PageRequest pageRequest) {
        Map<String, Object> conditions = pageRequest.getConditions();
        int offset = (pageRequest.getPage() - 1) * pageRequest.getSize();
        List<AutoTestExecution> records = executionMapper.pageQuery(conditions, offset, pageRequest.getSize());
        for (AutoTestExecution exec : records) {
            if (exec.getCaseId() != null) {
                AutoTestCase tc = testCaseMapper.findById(exec.getCaseId());
                if (tc != null) exec.setCaseName(tc.getCaseName());
            }
            if (exec.getExecutorId() != null) {
                User user = userMapper.findById(exec.getExecutorId());
                if (user != null) exec.setExecutorName(user.getRealName());
            }
        }
        Long total = executionMapper.count(conditions);
        return new PageResult<>(records, total, pageRequest.getPage(), pageRequest.getSize());
    }
    @Override
    public AutoTestExecution findExecutionById(Integer id) {
        AutoTestExecution execution = executionMapper.findById(id);
        if (execution != null) {
            if (execution.getCaseId() != null) {
                AutoTestCase tc = testCaseMapper.findById(execution.getCaseId());
                if (tc != null) execution.setCaseName(tc.getCaseName());
            }
            if (execution.getExecutorId() != null) {
                User user = userMapper.findById(execution.getExecutorId());
                if (user != null) execution.setExecutorName(user.getRealName());
            }
        }
        return execution;
    }

    @Override
    public List<Integer> batchExecuteTestCase(List<Integer> caseIds, Integer executorId) {
        List<Integer> executionIds = new ArrayList<>();
        for (Integer caseId : caseIds) {
            // 调用已有的 executeTestCase，它返回 AutoTestExecution（立即返回，异步执行）
            AutoTestExecution execution = executeTestCase(caseId, executorId, null);
            executionIds.add(execution.getId());
        }
        return executionIds;
    }
    @Override
    public AutoTestExecution findLastFailedExecutionByCaseId(Integer caseId) {
        return executionMapper.findLastFailedByCaseId(caseId);
    }
}