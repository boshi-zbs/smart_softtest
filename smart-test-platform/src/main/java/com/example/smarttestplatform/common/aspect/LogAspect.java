package com.example.smarttestplatform.common.aspect;

import com.example.smarttestplatform.common.annotation.Log;
import com.example.smarttestplatform.module.operationlog.entity.OperationLog;
import com.example.smarttestplatform.module.operationlog.service.OperationLogService;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class LogAspect {

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskExecutor taskExecutor;

    @Pointcut("@annotation(com.example.smarttestplatform.common.annotation.Log)")
    public void logPointcut() {}

    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long beginTime = System.currentTimeMillis();
        Object result = null;
        String resultStatus = "成功";
        Exception businessException = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            resultStatus = "失败";
            businessException = e;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - beginTime;
            final String finalResultStatus = resultStatus;
            final long finalDuration = duration;
            final Exception finalException = businessException;
            final Object finalResult = result;

            taskExecutor.execute(() -> {
                try {
                    recordLog(joinPoint, finalResult, finalResultStatus, finalDuration, finalException);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void recordLog(ProceedingJoinPoint joinPoint, Object result, String resultStatus,
                           long duration, Exception exception) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = null;
        Integer userId = null;
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            username = userDetails.getUsername();
            User user = userService.findByUsername(username);
            userId = user != null ? user.getId() : null;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Log logAnnotation = method.getAnnotation(Log.class);
        if (logAnnotation == null) return;

        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setModule(logAnnotation.module());
        log.setOperation(logAnnotation.operation());
        log.setDescription(logAnnotation.description());
        log.setResult(resultStatus);
        log.setIpAddress(request != null ? getIpAddress(request) : null);
        log.setUserAgent(request != null ? request.getHeader("User-Agent") : null);
        log.setCreateTime(new Date());
        // 如果有 durationMs 字段，可以设置：log.setDurationMs((int) duration);

        // 记录请求参数（过滤敏感信息）
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                String params = filterSensitiveParams(args);
                log.setRequestParams(params.length() > 2000 ? params.substring(0, 2000) : params);
            }
        } catch (Exception e) {
            // ignore
        }

        Integer targetId = extractTargetId(joinPoint);
        if (targetId != null) {
            log.setTargetId(targetId);
        }

        operationLogService.saveLog(log);
    }

    private String filterSensitiveParams(Object[] args) {
        try {
            Map<String, Object> paramMap = new HashMap<>();
            for (int i = 0; i < args.length; i++) {
                paramMap.put("arg" + i, args[i]);
            }
            String json = objectMapper.writeValueAsString(paramMap);
            json = json.replaceAll("(?i)\"(password|oldPassword|newPassword|confirmPassword)\"\\s*:\\s*\"[^\"]*\"", "\"$1\":\"******\"");
            return json;
        } catch (Exception e) {
            return "参数序列化失败";
        }
    }

    private Integer extractTargetId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();

        for (int i = 0; i < paramNames.length; i++) {
            if ("id".equals(paramNames[i]) && args[i] instanceof Integer) {
                return (Integer) args[i];
            }
        }

        if (args.length > 0 && args[0] != null) {
            Object firstArg = args[0];
            if (firstArg instanceof Integer) {
                return (Integer) firstArg;
            }
            try {
                Method getIdMethod = firstArg.getClass().getMethod("getId");
                Object idValue = getIdMethod.invoke(firstArg);
                if (idValue instanceof Integer) {
                    return (Integer) idValue;
                }
            } catch (Exception e) {
                try {
                    Field idField = firstArg.getClass().getDeclaredField("id");
                    idField.setAccessible(true);
                    Object idValue = idField.get(firstArg);
                    if (idValue instanceof Integer) {
                        return (Integer) idValue;
                    }
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
        return null;
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}