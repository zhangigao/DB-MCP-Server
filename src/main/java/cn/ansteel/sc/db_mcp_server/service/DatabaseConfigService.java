package cn.ansteel.sc.db_mcp_server.service;

import cn.ansteel.sc.db_mcp_server.config.DatabaseConfigProperties;
import cn.ansteel.sc.db_mcp_server.enums.DatabaseType;
import cn.ansteel.sc.db_mcp_server.factory.DataSourceFactory;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据库配置服务 - 使用文件存储
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseConfigService {

    private final DatabaseConfigFileService configFileService;
    private final DataSourceFactory dataSourceFactory;

    // 缓存当前配置，避免频繁读取文件
    private volatile String cachedActiveProfile;
    private volatile Map<String, DatabaseConfigProperties.ConnectionConfig> cachedProfiles;

    /**
     * 刷新缓存配置
     */
    private void refreshCache() {
        this.cachedActiveProfile = configFileService.readActiveProfile();
        this.cachedProfiles = configFileService.readProfiles();
    }

    /**
     * 获取当前活跃的数据库配置名称
     */
    public String getActiveProfile() {
        if (cachedActiveProfile == null) {
            refreshCache();
        }
        return cachedActiveProfile;
    }

    /**
     * 获取所有可用的数据库配置名称
     */
    public Map<String, DatabaseConfigProperties.ConnectionConfig> getAvailableProfiles() {
        if (cachedProfiles == null) {
            refreshCache();
        }
        return cachedProfiles;
    }

    /**
     * 获取所有数据源配置（用于前端API）
     */
    public Map<String, DatabaseConfigProperties.ConnectionConfig> getAllConnectionConfigs() {
        return getAvailableProfiles();
    }

    /**
     * 获取当前数据库类型
     */
    public DatabaseType getCurrentDatabaseType() {
        DatabaseConfigProperties.ConnectionConfig activeConfig = getActiveConnectionConfig();
        if (activeConfig == null) {
            throw new IllegalStateException("No active database connection configured");
        }
        String driverClassName = activeConfig.getDriverClassName();
        return DatabaseType.fromDriverClassName(driverClassName);
    }

    /**
     * 切换数据库配置
     */
    public DataSource switchDatabaseProfile(String profile) {
        Map<String, DatabaseConfigProperties.ConnectionConfig> profiles = getAvailableProfiles();
        if (!profiles.containsKey(profile)) {
            throw new IllegalArgumentException("Database profile '" + profile + "' not found");
        }

        log.info("Switching database profile from '{}' to '{}'", getActiveProfile(), profile);

        // 更新文件中的活跃配置
        configFileService.writeActiveProfile(profile);

        // 刷新缓存
        refreshCache();

        // 获取配置并创建数据源
        DatabaseConfigProperties.ConnectionConfig config = profiles.get(profile);
        DataSource dataSource = dataSourceFactory.createDataSource(profile, config);
        dataSourceFactory.setActiveProfile(profile);

        return dataSource;
    }

    /**
     * 测试数据库连接
     */
    public boolean testConnection(String profile) {
        try {
            DatabaseConfigProperties.ConnectionConfig config = getConnectionConfig(profile);
            if (config == null) {
                log.warn("Configuration not found for profile: {}", profile);
                return false;
            }

            // 临时创建数据源进行测试
            DataSource tempDataSource = dataSourceFactory.createDataSource("test_" + profile, config);
            try (Connection connection = tempDataSource.getConnection()) {
                boolean isValid = connection.isValid(5);
                log.info("Connection test for profile '{}' result: {}", profile, isValid);

                // 测试完成后关闭临时数据源
                dataSourceFactory.closeDataSource("test_" + profile);

                return isValid;
            }
        } catch (SQLException e) {
            log.error("Connection test failed for profile '{}': {}", profile, e.getMessage());
            return false;
        }
    }

    /**
     * 测试当前活跃的数据库连接
     */
    public boolean testActiveConnection() {
        return testConnection(getActiveProfile());
    }

    /**
     * 获取数据库配置信息
     */
    public DatabaseConfigProperties.ConnectionConfig getConnectionConfig(String profile) {
        Map<String, DatabaseConfigProperties.ConnectionConfig> profiles = getAvailableProfiles();
        return profiles.get(profile);
    }

    /**
     * 获取当前活跃的数据库配置信息
     */
    public DatabaseConfigProperties.ConnectionConfig getActiveConnectionConfig() {
        return getConnectionConfig(getActiveProfile());
    }

    /**
     * 验证数据库配置是否有效
     */
    public boolean validateProfile(String profile) {
        try {
            DatabaseConfigProperties.ConnectionConfig config = getConnectionConfig(profile);
            if (config == null) {
                log.warn("Configuration not found for profile: {}", profile);
                return false;
            }

            // 检查必要字段
            if (config.getDriverClassName() == null || config.getDriverClassName().trim().isEmpty()) {
                log.warn("Driver class name is missing for profile: {}", profile);
                return false;
            }

            if (config.getUrl() == null || config.getUrl().trim().isEmpty()) {
                log.warn("JDBC URL is missing for profile: {}", profile);
                return false;
            }

            if (config.getUsername() == null || config.getUsername().trim().isEmpty()) {
                log.warn("Username is missing for profile: {}", profile);
                return false;
            }

            // 检查驱动类是否存在
            try {
                Class.forName(config.getDriverClassName());
            } catch (ClassNotFoundException e) {
                log.warn("Driver class '{}' not found for profile: {}", config.getDriverClassName(), profile);
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Validation failed for profile '{}': {}", profile, e.getMessage());
            return false;
        }
    }

  
    /**
     * 刷新数据库配置（重新创建数据源）
     */
    public DataSource refreshDataSource(String profile) {
        DatabaseConfigProperties.ConnectionConfig config = getConnectionConfig(profile);
        if (config == null) {
            throw new IllegalArgumentException("Database profile '" + profile + "' not found");
        }
        return dataSourceFactory.refreshDataSource(profile, config);
    }

    /**
     * 获取指定配置的数据源
     */
    public DataSource getDataSource(String profile) {
        DatabaseConfigProperties.ConnectionConfig config = getConnectionConfig(profile);
        if (config == null) {
            throw new IllegalArgumentException("Database profile '" + profile + "' not found");
        }
        return dataSourceFactory.createDataSource(profile, config);
    }

    /**
     * 关闭指定配置的数据源
     */
    public void closeDataSource(String profile) {
        dataSourceFactory.closeDataSource(profile);
    }

    /**
     * 添加新的数据源配置
     */
    public void addDataSource(String profile, DatabaseConfigProperties.ConnectionConfig config) {
        log.info("Adding new data source profile: {}", profile);
        configFileService.writeProfile(profile, config);
        refreshCache();
    }

    /**
     * 更新数据源配置
     */
    public void updateDataSource(String profile, DatabaseConfigProperties.ConnectionConfig config) {
        log.info("Updating data source profile: {}", profile);
        configFileService.writeProfile(profile, config);
        refreshCache();

        // 如果更新的是当前活跃的数据源，需要刷新数据源
        if (profile.equals(getActiveProfile())) {
            dataSourceFactory.refreshDataSource(profile, config);
        }
    }

    /**
     * 删除数据源配置
     */
    public void deleteDataSource(String profile) {
        log.info("Deleting data source profile: {}", profile);

        // 先关闭数据源
        dataSourceFactory.closeDataSource(profile);

        // 删除配置文件
        configFileService.deleteProfile(profile);

        // 刷新缓存
        refreshCache();
    }

    /**
     * 获取所有数据库类型的配置状态
     */
    public Map<String, ConfigStatus> getConfigStatus() {
        Map<String, DatabaseConfigProperties.ConnectionConfig> profiles = getAvailableProfiles();
        return profiles.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            String profile = entry.getKey();
                            boolean isActive = profile.equals(getActiveProfile());
                            boolean isValid = validateProfile(profile);
                            boolean canConnect = false;

                            if (isValid) {
                                try {
                                    canConnect = testConnection(profile);
                                } catch (Exception e) {
                                    log.warn("Connection test failed for profile '{}': {}", profile, e.getMessage());
                                }
                            }

                            return new ConfigStatus(isActive, isValid, canConnect);
                        }
                ));
    }

    /**
     * 配置状态信息
     */
    @Data
    @AllArgsConstructor
    public static class ConfigStatus {
        private final boolean active;
        private final boolean valid;
        private final boolean connected;
    }
}