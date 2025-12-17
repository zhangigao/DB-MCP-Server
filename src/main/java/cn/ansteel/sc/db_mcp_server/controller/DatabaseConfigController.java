package cn.ansteel.sc.db_mcp_server.controller;

import cn.ansteel.sc.db_mcp_server.config.DatabaseConfigProperties;
import cn.ansteel.sc.db_mcp_server.dto.Result;
import cn.ansteel.sc.db_mcp_server.dto.resp.*;
import cn.ansteel.sc.db_mcp_server.service.DatabaseConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/database")
@RequiredArgsConstructor
public class DatabaseConfigController {

    private final DatabaseConfigService databaseConfigService;

    /**
     * 前端页面API - 获取所有数据源配置
     */
    @GetMapping("/datasources")
    public Map<String, Object> getAllDataSources() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, DatabaseConfigProperties.ConnectionConfig> configs = databaseConfigService.getAllConnectionConfigs();
            String activeProfile = databaseConfigService.getActiveProfile();

            Map<String, Object> dataSources = new HashMap<>();

            configs.forEach((profile, config) -> {
                Map<String, Object> dsInfo = new HashMap<>();
                dsInfo.put("name", profile);
                dsInfo.put("type", config.getDriverClassName().contains("mysql") ? "mysql" :
                        config.getDriverClassName().contains("oracle") ? "oracle" :
                                config.getDriverClassName().contains("postgresql") ? "postgresql" :
                                        config.getDriverClassName().contains("sqlserver") ? "sqlserver" : "unknown");

                // 从URL解析连接信息
                String url = config.getUrl();
                dsInfo.put("host", extractHostFromUrl(url));
                dsInfo.put("port", extractPortFromUrl(url));
                dsInfo.put("database", extractDatabaseFromUrl(url));
                dsInfo.put("username", config.getUsername());
                dsInfo.put("password", "***"); // 不返回实际密码

                dsInfo.put("active", profile.equals(activeProfile));

                // 默认状态为未连接，需要手动测试
                dsInfo.put("connected", false);

                dataSources.put(profile, dsInfo);
            });

            response.put("success", true);
            response.put("data", dataSources);
            response.put("active", activeProfile);

        } catch (Exception e) {
            log.error("获取数据源列表失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return response;
    }

    /**
     * 前端页面API - 切换活跃数据源
     */
    @PostMapping("/switch")
    public Map<String, Object> switchDataSource(@RequestParam String profile) {
        Map<String, Object> response = new HashMap<>();
        try {
            DataSource dataSource = databaseConfigService.switchDatabaseProfile(profile);

            response.put("success", true);
            response.put("message", "成功切换到数据源: " + profile);
            response.put("activeProfile", profile);
            response.put("databaseType", databaseConfigService.getCurrentDatabaseType());

        } catch (Exception e) {
            log.error("切换数据源失败", e);
            response.put("success", false);
            response.put("message", "切换失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 前端页面API - 测试数据源连接
     */
    @PostMapping("/validate")
    public Map<String, Object> validateConnection(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String profile = request.get("profile");

        try {
            boolean canConnect = databaseConfigService.testConnection(profile);

            response.put("success", canConnect);
            response.put("message", canConnect ? "连接测试成功" : "连接测试失败");

        } catch (Exception e) {
            log.error("测试连接失败", e);
            response.put("success", false);
            response.put("message", "连接测试失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 前端页面API - 获取当前活跃的数据库配置
     */
    @GetMapping("/config/active")
    public Result<ActiveConfigResp> getActiveConfig() {
        ActiveConfigResp resp = new ActiveConfigResp();
        resp.setActiveProfile(databaseConfigService.getActiveProfile());
        resp.setDatabaseType(databaseConfigService.getCurrentDatabaseType());
        resp.setConnectionConfig(databaseConfigService.getActiveConnectionConfig());
        return Result.success(resp);
    }

    /**
     * 获取所有可用的数据库配置
     */
    @GetMapping("/profiles")
    public Result<ProfilesResp> getAvailableProfiles() {
        ProfilesResp resp = new ProfilesResp();
        resp.setAvailableProfiles(databaseConfigService.getAvailableProfiles());
        resp.setActive(databaseConfigService.getActiveProfile());
        return Result.success(resp);
    }

    /**
     * 获取所有数据库配置状态
     */
    @GetMapping("/status")
    public Result<ConfigStatusResp> getConfigStatus() {
        ConfigStatusResp resp = new ConfigStatusResp(databaseConfigService.getConfigStatus());
        return Result.success(resp);
    }

    /**
     * 切换数据库配置
     */
    @PostMapping("/switch/{profile}")
    public Result<SwitchDatabaseResp> switchDatabase(
            @PathVariable String profile,
            @RequestParam(required = false, defaultValue = "false") boolean testConnection) {

        try {
            // 测试连接（如果要求）
            if (testConnection && !databaseConfigService.testConnection(profile)) {
                return Result.fail("Test connection failed");
            }
            DataSource dataSource = databaseConfigService.switchDatabaseProfile(profile);
            SwitchDatabaseResp resp = new SwitchDatabaseResp();
            resp.setActiveProfile(profile);
            resp.setDatabaseType(databaseConfigService.getCurrentDatabaseType());
            resp.setDatabase(dataSource.getClass().getSimpleName());
            return Result.success(resp);

        } catch (IllegalArgumentException e) {
            return Result.fail("Invalid profile: " + profile);
        } catch (Exception e) {
            return Result.fail("Error while switching database profile: " + profile);
        }
    }

    /**
     * 测试数据库连接
     */
    @PostMapping("/test/{profile}")
    public Result<String> testConnection(@PathVariable String profile) {
        try {
            return databaseConfigService.testConnection(profile) ? Result.success(profile) : Result.fail("Test connection failed");
        } catch (Exception e) {
            return Result.fail("Error while testing connection: " + profile);
        }
    }

    /**
     * 测试当前活跃的数据库连接
     */
    @PostMapping("/test")
    public Result<String> testActiveConnection() {
        return testConnection(databaseConfigService.getActiveProfile());
    }

    /**
     * 验证数据库配置
     */
    @PostMapping("/validate/{profile}")
    public Result<ValidationResp> validateProfile(@PathVariable String profile) {
        try {
            boolean isValid = databaseConfigService.validateProfile(profile);
            ValidationResp resp = isValid ?
                    ValidationResp.valid(profile) :
                    ValidationResp.invalid(profile, "Configuration is invalid");
            return Result.success(resp);
        } catch (Exception e) {
            ValidationResp resp = ValidationResp.invalid(profile, "Validation error: " + e.getMessage());
            return Result.success(resp);
        }
    }

    /**
     * 刷新数据库数据源
     */
    @PostMapping("/refresh/{profile}")
    public Result<RefreshDataSourceResp> refreshDataSource(@PathVariable String profile) {
        try {
            DataSource dataSource = databaseConfigService.refreshDataSource(profile);
            RefreshDataSourceResp resp = RefreshDataSourceResp.success(profile, dataSource.getClass().getSimpleName());
            return Result.success(resp);
        } catch (Exception e) {
            RefreshDataSourceResp resp = RefreshDataSourceResp.failure(profile, "Failed to refresh data source: " + e.getMessage());
            return Result.fail(resp.getMessage());
        }
    }

    /**
     * 获取数据库配置详情
     */
    @GetMapping("/profile/{profile}")
    public Result<ProfileConfigResp> getProfileConfig(@PathVariable String profile) {
        try {
            DatabaseConfigProperties.ConnectionConfig config = databaseConfigService.getConnectionConfig(profile);
            boolean isActive = profile.equals(databaseConfigService.getActiveProfile());
            boolean isValid = databaseConfigService.validateProfile(profile);
            boolean canConnect = false;

            if (isValid) {
                try {
                    canConnect = databaseConfigService.testConnection(profile);
                } catch (Exception e) {
                    // 连接测试失败，保持canConnect为false
                }
            }

            ProfileConfigResp resp = ProfileConfigResp.success(profile, isActive, isValid, canConnect, config);
            return Result.success(resp);
        } catch (Exception e) {
            ProfileConfigResp resp = ProfileConfigResp.failure(profile, "Failed to get profile config: " + e.getMessage());
            return Result.fail(resp.getMessage());
        }
    }

    /**
     * 从JDBC URL中提取主机地址
     */
    private String extractHostFromUrl(String url) {
        try {
            // jdbc:mysql://8.130.142.3:3306/sc_db
            if (url.contains("://")) {
                String afterProtocol = url.split("://")[1];
                if (afterProtocol.contains(":")) {
                    return afterProtocol.split(":")[0];
                }
                return afterProtocol.split("/")[0];
            }
            return "localhost";
        } catch (Exception e) {
            return "localhost";
        }
    }

    /**
     * 从JDBC URL中提取端口号
     */
    private int extractPortFromUrl(String url) {
        try {
            if (url.contains("://")) {
                String afterProtocol = url.split("://")[1];
                if (afterProtocol.contains(":") && afterProtocol.contains("/")) {
                    String portStr = afterProtocol.split(":")[1].split("/")[0];
                    return Integer.parseInt(portStr);
                }
            }

            // 默认端口
            if (url.contains("mysql")) return 3306;
            if (url.contains("oracle")) return 1521;
            if (url.contains("postgresql")) return 5432;
            if (url.contains("sqlserver")) return 1433;

            return 3306;
        } catch (Exception e) {
            return 3306;
        }
    }

    /**
     * 从JDBC URL中提取数据库名
     */
    private String extractDatabaseFromUrl(String url) {
        try {
            if (url.contains("://")) {
                String afterProtocol = url.split("://")[1];
                if (afterProtocol.contains("/")) {
                    String dbPart = afterProtocol.split("/", 2)[1];
                    if (dbPart.contains("?")) {
                        return dbPart.split("\\?")[0];
                    }
                    return dbPart;
                }
            }
            return "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 添加新的数据源配置
     */
    @PostMapping("/add")
    public Map<String, Object> addDataSource(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String profile = (String) request.get("profile");
            @SuppressWarnings("unchecked")
            Map<String, Object> configData = (Map<String, Object>) request.get("config");

            if (profile == null || profile.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "数据源名称不能为空");
                return response;
            }

            // 检查是否已存在
            if (databaseConfigService.getAllConnectionConfigs().containsKey(profile)) {
                response.put("success", false);
                response.put("message", "数据源 '" + profile + "' 已存在");
                return response;
            }

            // 创建配置对象
            DatabaseConfigProperties.ConnectionConfig config = new DatabaseConfigProperties.ConnectionConfig();
            config.setDriverClassName((String) configData.get("driverClassName"));
            config.setUrl((String) configData.get("url"));
            config.setUsername((String) configData.get("username"));
            config.setPassword((String) configData.get("password"));

            // 验证配置
            String driverClassName = config.getDriverClassName();
            try {
                Class.forName(driverClassName);
            } catch (ClassNotFoundException e) {
                response.put("success", false);
                response.put("message", "不支持的数据库驱动: " + driverClassName);
                return response;
            }

            // 添加配置
            databaseConfigService.addDataSource(profile, config);

            response.put("success", true);
            response.put("message", "数据源 '" + profile + "' 添加成功");
            response.put("profile", profile);

        } catch (Exception e) {
            log.error("添加数据源失败", e);
            response.put("success", false);
            response.put("message", "添加失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 更新数据源配置
     */
    @PostMapping("/update")
    public Map<String, Object> updateDataSource(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String profile = (String) request.get("profile");
            @SuppressWarnings("unchecked")
            Map<String, Object> configData = (Map<String, Object>) request.get("config");

            if (profile == null || profile.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "数据源名称不能为空");
                return response;
            }

            // 检查是否存在
            if (!databaseConfigService.getAllConnectionConfigs().containsKey(profile)) {
                response.put("success", false);
                response.put("message", "数据源 '" + profile + "' 不存在");
                return response;
            }

            // 创建配置对象
            DatabaseConfigProperties.ConnectionConfig config = new DatabaseConfigProperties.ConnectionConfig();
            config.setDriverClassName((String) configData.get("driverClassName"));
            config.setUrl((String) configData.get("url"));
            config.setUsername((String) configData.get("username"));
            config.setPassword((String) configData.get("password"));

            // 验证配置
            String driverClassName = config.getDriverClassName();
            try {
                Class.forName(driverClassName);
            } catch (ClassNotFoundException e) {
                response.put("success", false);
                response.put("message", "不支持的数据库驱动: " + driverClassName);
                return response;
            }

            // 更新配置
            databaseConfigService.updateDataSource(profile, config);

            response.put("success", true);
            response.put("message", "数据源 '" + profile + "' 更新成功");
            response.put("profile", profile);

        } catch (Exception e) {
            log.error("更新数据源失败", e);
            response.put("success", false);
            response.put("message", "更新失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 删除数据源配置
     */
    @PostMapping("/delete")
    public Map<String, Object> deleteDataSource(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String profile = request.get("profile");

        try {
            if (profile == null || profile.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "数据源名称不能为空");
                return response;
            }

            // 检查是否存在
            if (!databaseConfigService.getAllConnectionConfigs().containsKey(profile)) {
                response.put("success", false);
                response.put("message", "数据源 '" + profile + "' 不存在");
                return response;
            }

            // 不允许删除最后一个数据源
            if (databaseConfigService.getAllConnectionConfigs().size() <= 1) {
                response.put("success", false);
                response.put("message", "不能删除最后一个数据源");
                return response;
            }

            // 删除配置
            databaseConfigService.deleteDataSource(profile);

            response.put("success", true);
            response.put("message", "数据源 '" + profile + "' 删除成功");
            response.put("deletedProfile", profile);

        } catch (Exception e) {
            log.error("删除数据源失败", e);
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 获取单个数据源配置详情
     */
    @GetMapping("/profile/{profile}/detail")
    public Map<String, Object> getProfileDetail(@PathVariable String profile) {
        Map<String, Object> response = new HashMap<>();
        try {
            DatabaseConfigProperties.ConnectionConfig config = databaseConfigService.getConnectionConfig(profile);
            if (config == null) {
                response.put("success", false);
                response.put("message", "数据源 '" + profile + "' 不存在");
                return response;
            }

            Map<String, Object> configData = new HashMap<>();
            configData.put("driverClassName", config.getDriverClassName());
            configData.put("url", config.getUrl());
            configData.put("username", config.getUsername());
            configData.put("password", "***"); // 不返回实际密码

            response.put("success", true);
            response.put("profile", profile);
            response.put("config", configData);

        } catch (Exception e) {
            log.error("获取数据源详情失败", e);
            response.put("success", false);
            response.put("message", "获取详情失败: " + e.getMessage());
        }

        return response;
    }

  }