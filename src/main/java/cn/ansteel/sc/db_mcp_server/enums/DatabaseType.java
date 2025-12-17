package cn.ansteel.sc.db_mcp_server.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 数据库类型枚举
 */
public enum DatabaseType {
    MYSQL("mysql", "com.mysql.cj.jdbc.Driver", "MySQL"),
    ORACLE("oracle", "oracle.jdbc.OracleDriver", "Oracle"),
    POSTGRESQL("postgresql", "org.postgresql.Driver", "PostgreSQL"),
    SQLSERVER("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "SQL Server");

    private final String code;
    private final String driverClassName;
    @JsonValue
    private final String displayName;

    DatabaseType(String code, String driverClassName, String displayName) {
        this.code = code;
        this.driverClassName = driverClassName;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 根据代码获取数据库类型
     */
    public static DatabaseType fromCode(String code) {
        for (DatabaseType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown database type: " + code);
    }

    /**
     * 根据驱动类名获取数据库类型
     */
    public static DatabaseType fromDriverClassName(String driverClassName) {
        for (DatabaseType type : values()) {
            if (type.driverClassName.equals(driverClassName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown driver class name: " + driverClassName);
    }
}