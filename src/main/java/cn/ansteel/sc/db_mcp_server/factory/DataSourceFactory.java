package cn.ansteel.sc.db_mcp_server.factory;

import cn.ansteel.sc.db_mcp_server.config.DatabaseConfigProperties;
import cn.ansteel.sc.db_mcp_server.constant.DatabaseConstant;
import cn.ansteel.sc.db_mcp_server.enums.DatabaseType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 数据源工厂 - 纯粹的数据源创建工厂，不依赖其他服务
 */
@Order(1)
@Slf4j
@Component
public class DataSourceFactory {

    /**
     * 缓存已创建的数据源
     */
    private final ConcurrentMap<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();

    private String activeProfile;

    public DataSourceFactory() {
    }

    /**
     * 获取当前活跃的数据源
     */
    public DataSource getActiveDataSource() {
        if (activeProfile == null) {
            throw new IllegalStateException("No active database profile configured");
        }
        return getDataSource(activeProfile);
    }

    /**
     * 根据配置名称获取数据源
     */
    public DataSource getDataSource(String profile) {
        return dataSourceCache.computeIfAbsent(profile, this::createDataSource);
    }

    /**
     * 根据配置创建数据源
     */
    public DataSource createDataSource(String profile, DatabaseConfigProperties.ConnectionConfig config) {
        try {
            log.info("Creating data source for profile: {}", profile);

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName(config.getDriverClassName());
            hikariConfig.setJdbcUrl(config.getUrl());
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(config.getPassword());

            // 设置连接池参数
            if (config.getPool() != null) {
                DatabaseConfigProperties.PoolConfig poolConfig = config.getPool();
                hikariConfig.setMaximumPoolSize(poolConfig.getMaximumPoolSize());
                hikariConfig.setMinimumIdle(poolConfig.getMinimumIdle());
                hikariConfig.setConnectionTimeout(poolConfig.getConnectionTimeout());
                hikariConfig.setIdleTimeout(poolConfig.getIdleTimeout());
                hikariConfig.setMaxLifetime(poolConfig.getMaxLifetime());
            }

            // 设置连接池名称
            hikariConfig.setPoolName(DatabaseConstant.ConnectionPool.PREFIX + profile);

            // 设置连接测试查询
            DatabaseType dbType = DatabaseType.fromDriverClassName(config.getDriverClassName());
            hikariConfig.setConnectionTestQuery(getTestQuery(dbType));

            // 设置连接验证
            hikariConfig.setValidationTimeout(3000);
            hikariConfig.setConnectionTimeout(30000);

            DataSource dataSource = new HikariDataSource(hikariConfig);
            dataSourceCache.put(profile, dataSource);
            return dataSource;

        } catch (Exception e) {
            log.error("Failed to create data source for profile: {}", profile, e);
            throw new RuntimeException("Failed to create data source for profile: " + profile, e);
        }
    }

    /**
     * 创建数据源（从缓存中获取配置）
     */
    private DataSource createDataSource(String profile) {
        // 这个方法现在不应该被直接调用，因为工厂不再存储配置
        throw new UnsupportedOperationException("Use createDataSource(String profile, DatabaseConfigProperties.ConnectionConfig config) instead");
    }

    /**
     * 获取数据库测试查询语句
     */
    private String getTestQuery(DatabaseType dbType) {
        return switch (dbType) {
            case MYSQL -> DatabaseConstant.TestStatements.MYSQL;
            case ORACLE -> DatabaseConstant.TestStatements.ORACLE;
            case POSTGRESQL -> DatabaseConstant.TestStatements.POSTGRESQL;
            case SQLSERVER -> DatabaseConstant.TestStatements.SQLSERVER;
            default -> throw new IllegalArgumentException("Unsupported database type: " + dbType);
        };
    }

    /**
     * 设置活跃的数据源配置名称
     */
    public void setActiveProfile(String activeProfile) {
        this.activeProfile = activeProfile;
        log.info("Active database profile set to: {}", activeProfile);
    }

    /**
     * 获取活跃的数据源配置名称
     */
    public String getActiveProfile() {
        return activeProfile;
    }

    /**
     * 关闭所有数据源
     */
    public void closeAllDataSources() {
        dataSourceCache.values().forEach(dataSource -> {
            if (dataSource instanceof HikariDataSource) {
                ((HikariDataSource) dataSource).close();
            }
        });
        dataSourceCache.clear();
        log.info("All data sources closed");
    }

    /**
     * 关闭指定的数据源
     */
    public void closeDataSource(String profile) {
        DataSource dataSource = dataSourceCache.remove(profile);
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
            log.info("Data source closed for profile: {}", profile);
        }
    }

    /**
     * 刷新数据源（重新创建）
     */
    public DataSource refreshDataSource(String profile, DatabaseConfigProperties.ConnectionConfig config) {
        closeDataSource(profile);
        return createDataSource(profile, config);
    }

    /**
     * 销毁阶段清空数据源
     */
    @PreDestroy
    public void destroy() {
        closeAllDataSources();
    }
}