package cn.ansteel.sc.db_mcp_server.mcp.functions;

import cn.ansteel.sc.db_mcp_server.config.DatabaseConfigProperties;
import cn.ansteel.sc.db_mcp_server.factory.DataSourceFactory;
import cn.ansteel.sc.db_mcp_server.service.DatabaseConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;

/**
 * 数据库连接管理器 - 使用DatabaseConfigService配置
 */
@Slf4j
@RequiredArgsConstructor
public class DatabaseConnectionManager {

    private final DatabaseConfigService databaseConfigService;
    private final DataSourceFactory dataSourceFactory;

    public Connection getConnection(String profile) throws Exception {
        log.info("获取数据库连接: profile={}", profile);

        try {
            // 通过DatabaseConfigService获取配置
            DatabaseConfigProperties.ConnectionConfig config = databaseConfigService.getConnectionConfig(profile);
            if (config == null) {
                throw new IllegalArgumentException("数据源配置不存在: " + profile);
            }

            // 使用DataSourceFactory创建数据源并获取连接
            javax.sql.DataSource dataSource = dataSourceFactory.createDataSource(profile, config);
            Connection connection = dataSource.getConnection();
            log.info("成功创建数据库连接: profile={}", profile);

            return connection;
        } catch (Exception e) {
            log.error("获取数据库连接失败: profile={}, error={}", profile, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 添加数据源配置 - 委托给DatabaseConfigService
     */
    public void addDataSource(String profile, String driverClassName, String url, String username, String password) {
        log.info("通过DatabaseConnectionManager添加数据源: profile={}", profile);

        // 创建配置对象
        DatabaseConfigProperties.ConnectionConfig config = new DatabaseConfigProperties.ConnectionConfig();
        config.setDriverClassName(driverClassName);
        config.setUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        // 委托给DatabaseConfigService来添加配置
        databaseConfigService.addDataSource(profile, config);
    }
}