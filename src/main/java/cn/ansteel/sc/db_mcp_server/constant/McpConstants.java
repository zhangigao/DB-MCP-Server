package cn.ansteel.sc.db_mcp_server.constant;

import java.sql.DatabaseMetaData;

/**
 * MCP相关常量类
 *
 * @author db_mcp_server
 * @version 1.0.0
 */
public final class McpConstants {

    private McpConstants() {
        // 工具类，禁止实例化
    }

    /**
     * MCP操作类型
     */
    public static final class Operations {
        public static final String LIST_TABLES = "list_tables";
        public static final String DESCRIBE_TABLE = "describe_table";
        public static final String LIST_DATABASES = "list_databases";
        public static final String TABLE_INDEXES = "table_indexes";
        public static final String TABLE_CONSTRAINTS = "table_constraints";
        public static final String TABLE_STATISTICS = "table_statistics";
        public static final String COLUMN_INFO = "column_info";
        public static final String EXECUTE_SQL = "execute_sql";

        private Operations() {}
    }

    /**
     * 数据库表类型
     */
    public static final class TableTypes {
        public static final String[] SUPPORTED_TYPES = {"TABLE", "VIEW"};

        private TableTypes() {}
    }

    /**
     * 数据库字段名称
     */
    public static final class DatabaseFields {
        // 表字段
        public static final String TABLE_NAME = "TABLE_NAME";
        public static final String TABLE_TYPE = "TABLE_TYPE";
        public static final String REMARKS = "REMARKS";
        public static final String TABLE_CAT = "TABLE_CAT";
        public static final String TABLE_SCHEM = "TABLE_SCHEM";

        // 列字段
        public static final String COLUMN_NAME = "COLUMN_NAME";
        public static final String TYPE_NAME = "TYPE_NAME";
        public static final String COLUMN_SIZE = "COLUMN_SIZE";
        public static final String NULLABLE = "NULLABLE";
        public static final String COLUMN_DEF = "COLUMN_DEF";
        public static final String ORDINAL_POSITION = "ORDINAL_POSITION";

        // 主键字段
        public static final String KEY_SEQ = "KEY_SEQ";
        public static final String PK_NAME = "PK_NAME";

        // 索引字段
        public static final String INDEX_NAME = "INDEX_NAME";
        public static final String COLUMN_NAME_IDX = "COLUMN_NAME";
        public static final String NON_UNIQUE = "NON_UNIQUE";
        public static final String TYPE = "TYPE";

        // 外键字段
        public static final String PKTABLE_NAME = "PKTABLE_NAME";
        public static final String PKCOLUMN_NAME = "PKCOLUMN_NAME";
        public static final String FKTABLE_NAME = "FKTABLE_NAME";
        public static final String FKCOLUMN_NAME = "FKCOLUMN_NAME";
        public static final String FK_NAME = "FK_NAME";

        private DatabaseFields() {}
    }

    /**
     * JSON字段名称
     */
    public static final class JsonFields {
        public static final String OPERATION = "operation";
        public static final String TABLE_NAME = "tableName";
        public static final String TABLE_TYPE = "tableType";
        public static final String TABLES = "tables";
        public static final String TABLE_COUNT = "tableCount";
        public static final String DATABASE_TYPE = "databaseType";
        public static final String DATABASES = "databases";
        public static final String DATABASE_COUNT = "databaseCount";
        public static final String COLUMNS = "columns";
        public static final String COLUMN_COUNT = "columnCount";
        public static final String PRIMARY_KEYS = "primaryKeys";
        public static final String INDEXES = "indexes";
        public static final String INDEX_COUNT = "indexCount";
        public static final String CONSTRAINTS = "constraints";
        public static final String CONSTRAINT_COUNT = "constraintCount";
        public static final String STATISTICS = "statistics";
        public static final String REMARKS = "remarks";
        public static final String CATALOG = "catalog";
        public static final String SCHEMA = "schema";

        private JsonFields() {}
    }

    /**
     * JDBC URL前缀
     */
    public static final class JdbcUrlPrefixes {
        public static final String MYSQL = "jdbc:mysql:";
        public static final String POSTGRESQL = "jdbc:postgresql:";
        public static final String ORACLE = "jdbc:oracle:";
        public static final String SQLSERVER = "jdbc:sqlserver:";

        private JdbcUrlPrefixes() {}
    }

    /**
     * 默认配置
     */
    public static final class Defaults {
        public static final String DEFAULT_PROFILE = "mysql";
        public static final String WILDCARD = "%";
        public static final int COLUMN_NULLABLE = DatabaseMetaData.columnNullable;

        private Defaults() {}
    }

    /**
     * 错误消息
     */
    public static class ErrorMessages {
        public static final String UNSUPPORTED_OPERATION = "不支持的操作: %s";
        public static final String TABLE_NAME_EMPTY = "表名不能为空";
        public static final String DATASOURCE_CONFIG_NOT_FOUND = "数据源配置不存在: %s";
        public static final String QUERY_FAILED = "查询失败: %s";
        public static final String METADATA_QUERY_FAILED = "元数据查询失败: %s";
        public static final String CONNECTION_FAILED = "获取数据库连接失败: profile=%s, error=%s";
        public static final String SQL_EXECUTION_FAILED = "SQL执行失败: %s";

        private ErrorMessages() {}
    }

    /**
     * 成功消息
     */
    public static class SuccessMessages {
        public static final String TABLE_LIST_SUCCESS = "获取表列表成功";
        public static final String TABLE_DESCRIBE_SUCCESS = "表结构查询成功";
        public static final String DATABASE_LIST_SUCCESS = "获取数据库列表成功";
        public static final String INDEX_INFO_SUCCESS = "索引信息查询成功";
        public static final String CONSTRAINT_INFO_SUCCESS = "约束信息查询成功";
        public static final String STATISTICS_SUCCESS = "表统计信息查询成功";
        public static final String SQL_EXECUTION_SUCCESS = "SQL执行成功";

        private SuccessMessages() {}
    }

    /**
     * 日志消息
     */
    public static class LogMessages {
        public static final String METADATA_QUERY = "查询元数据: operation={}, table={}";
        public static final String USING_DATABASE = "使用数据库: {}";
        public static final String QUERY_TABLES = "查询数据库 {} 中的表，schema: {}";
        public static final String GET_CONNECTION = "获取数据库连接: profile={}";
        public static final String CONNECTION_SUCCESS = "成功创建数据库连接: profile={}";
        public static final String ADD_DATASOURCE = "通过DatabaseConnectionManager添加数据源: profile={}";

        private LogMessages() {}
    }
}