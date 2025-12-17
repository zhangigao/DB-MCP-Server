package cn.ansteel.sc.db_mcp_server.constant;

/**
 * 数据库相关常量类
 *
 * @author db_mcp_server
 * @version 1.0.0
 * @description: 数据库连接和配置相关常量
 * @date 2025/12/15 14:49
 */
public final class DatabaseConstant {

    private DatabaseConstant() {
        // 工具类，禁止实例化
    }

    /**
     * 连接池相关
     */
    public static final class ConnectionPool {
        public static final String PREFIX = "HikariPool-";

        private ConnectionPool() {}
    }

    /**
     * 数据库连接测试语句
     */
    public static final class TestStatements {
        public static final String MYSQL = "SELECT 1";
        public static final String ORACLE = "SELECT 1 FROM DUAL";
        public static final String POSTGRESQL = "SELECT 1";
        public static final String SQLSERVER = "SELECT 1";

        private TestStatements() {}
    }

    /**
     * SQL关键字
     */
    public static final class SqlKeywords {
        public static final String SELECT = "SELECT";
        public static final String SHOW = "SHOW";
        public static final String DESCRIBE = "DESCRIBE";
        public static final String EXPLAIN = "EXPLAIN";
        public static final String FROM = "FROM";
        public static final String WHERE = "WHERE";
        public static final String COUNT = "COUNT(*)";
        public static final String LIMIT = "LIMIT";

        private SqlKeywords() {}
    }

    /**
     * 数据库产品名称
     */
    public static final class ProductNames {
        public static final String MYSQL = "mysql";
        public static final String ORACLE = "oracle";
        public static final String POSTGRESQL = "postgresql";
        public static final String SQL_SERVER = "sql server";

        private ProductNames() {}
    }

    /**
     * 配置相关
     */
    public static final class Config {
        public static final String CONFIG_FILE_NAME = "database-configs.json";
        public static final String DEFAULT_PROFILE = "mysql";
        public static final String DATABASE_NAME_PARAM = "databaseName=";

        private Config() {}
    }

    /**
     * SQL查询模板
     */
    public static final class SqlTemplates {
        public static final String TABLE_SIZE_QUERY = "SELECT ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'size_mb' " +
                "FROM information_schema.TABLES WHERE table_schema = DATABASE() AND table_name = '%s'";
        public static final String ROW_COUNT_QUERY = "SELECT COUNT(*) as row_count FROM %s";

        private SqlTemplates() {}
    }
}
