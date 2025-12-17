package cn.ansteel.sc.db_mcp_server.config;

import lombok.Data;

/**
 * 数据库配置数据模型类
 * 用于JSON文件存储和数据传输
 */
@Data
public class DatabaseConfigProperties {

    /**
     * 数据库连接配置
     */
    @Data
    public static class ConnectionConfig {
        private String driverClassName;
        private String url;
        private String username;
        private String password;
        private PoolConfig pool;

    }

    /**
     * 连接池配置
     */
    @Data
    public static class PoolConfig {
        private int maximumPoolSize;
        private int minimumIdle;
        private long connectionTimeout;
        private long idleTimeout;
        private long maxLifetime;

    }

}