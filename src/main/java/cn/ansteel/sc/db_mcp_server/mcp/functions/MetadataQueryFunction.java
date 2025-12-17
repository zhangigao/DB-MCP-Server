package cn.ansteel.sc.db_mcp_server.mcp.functions;

import cn.ansteel.sc.db_mcp_server.constant.McpConstants;
import cn.ansteel.sc.db_mcp_server.constant.DatabaseConstant;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 元数据查询函数 - 真实数据库操作版本
 */
@Slf4j
public class MetadataQueryFunction implements java.util.function.Function<MetadataQueryFunction.Request, MetadataQueryFunction.Response> {

    private final DatabaseConnectionManager connectionManager;

    public MetadataQueryFunction(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public Response apply(Request request) {
        log.info(McpConstants.LogMessages.METADATA_QUERY, request.getOperation(), request.getTableName());

        try {
            String profile = request.getProfile() != null ? request.getProfile() : McpConstants.Defaults.DEFAULT_PROFILE;
            try (Connection conn = connectionManager.getConnection(profile)) {
                DatabaseMetaData metaData = conn.getMetaData();

                // 从连接URL中提取数据库名称
                String databaseName = extractDatabaseName(conn);
                log.info(McpConstants.LogMessages.USING_DATABASE, databaseName);

                return switch (request.getOperation()) {
                    case McpConstants.Operations.LIST_TABLES -> listTables(metaData, databaseName, request.getSchemaName());
                    case McpConstants.Operations.DESCRIBE_TABLE -> describeTable(metaData, databaseName, request.getSchemaName(), request.getTableName());
                    case McpConstants.Operations.LIST_DATABASES -> listDatabases(metaData);
                    case McpConstants.Operations.TABLE_INDEXES -> getTableIndexes(metaData, databaseName, request.getSchemaName(), request.getTableName());
                    case McpConstants.Operations.TABLE_CONSTRAINTS -> getTableConstraints(metaData, databaseName, request.getSchemaName(), request.getTableName());
                    case McpConstants.Operations.TABLE_STATISTICS -> getTableStatistics(conn, databaseName, request.getSchemaName(), request.getTableName());
                    case McpConstants.Operations.COLUMN_INFO -> getColumnInfo(metaData, databaseName, request.getSchemaName(), request.getTableName());
                    default -> Response.error(String.format(McpConstants.ErrorMessages.UNSUPPORTED_OPERATION, request.getOperation()));
                };
            }
        } catch (Exception e) {
            log.error("元数据查询失败: {}", e.getMessage(), e);
            return Response.error(String.format(McpConstants.ErrorMessages.QUERY_FAILED, e.getMessage()));
        }
    }

    private Response listTables(DatabaseMetaData metaData, String databaseName, String schemaName) throws Exception {
        List<Map<String, Object>> tables = new ArrayList<>();

        log.info(McpConstants.LogMessages.QUERY_TABLES, databaseName, schemaName);

        try (ResultSet rs = metaData.getTables(databaseName, schemaName, McpConstants.Defaults.WILDCARD, McpConstants.TableTypes.SUPPORTED_TYPES)) {
            while (rs.next()) {
                Map<String, Object> table = new LinkedHashMap<>();
                table.put(McpConstants.JsonFields.TABLE_NAME, rs.getString(McpConstants.DatabaseFields.TABLE_NAME));
                table.put(McpConstants.JsonFields.TABLE_TYPE, rs.getString(McpConstants.DatabaseFields.TABLE_TYPE));
                table.put(McpConstants.JsonFields.REMARKS, rs.getString(McpConstants.DatabaseFields.REMARKS));
                table.put(McpConstants.JsonFields.CATALOG, rs.getString(McpConstants.DatabaseFields.TABLE_CAT));
                table.put(McpConstants.JsonFields.SCHEMA, rs.getString(McpConstants.DatabaseFields.TABLE_SCHEM));
                tables.add(table);
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put(McpConstants.JsonFields.OPERATION, McpConstants.Operations.LIST_TABLES);
        data.put(McpConstants.JsonFields.TABLE_COUNT, tables.size());
        data.put(McpConstants.JsonFields.DATABASE_TYPE, getDatabaseType(metaData));
        data.put(McpConstants.JsonFields.TABLES, tables);

        return Response.success(McpConstants.SuccessMessages.TABLE_LIST_SUCCESS, data);
    }

    private Response describeTable(DatabaseMetaData metaData, String databaseName, String schemaName, String tableName) throws Exception {
        if (tableName == null || tableName.trim().isEmpty()) {
            return Response.error("表名不能为空");
        }

        List<Map<String, Object>> columns = new ArrayList<>();
        try (ResultSet rs = metaData.getColumns(databaseName, schemaName, tableName, "%")) {
            while (rs.next()) {
                Map<String, Object> column = new LinkedHashMap<>();
                column.put("name", rs.getString("COLUMN_NAME"));
                column.put("type", rs.getString("TYPE_NAME"));
                column.put("size", rs.getInt("COLUMN_SIZE"));
                column.put("nullable", rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.put("defaultValue", rs.getString("COLUMN_DEF"));
                column.put("remarks", rs.getString("REMARKS"));
                columns.add(column);
            }
        }

        // 获取主键信息
        List<Map<String, Object>> primaryKeys = new ArrayList<>();
        try (ResultSet rs = metaData.getPrimaryKeys(databaseName, schemaName, tableName)) {
            while (rs.next()) {
                Map<String, Object> pk = new LinkedHashMap<>();
                pk.put("columnName", rs.getString("COLUMN_NAME"));
                pk.put("keySeq", rs.getInt("KEY_SEQ"));
                pk.put("pkName", rs.getString("PK_NAME"));
                primaryKeys.add(pk);
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("operation", "describe_table");
        data.put("tableName", tableName);
        data.put("columns", columns);
        data.put("primaryKeys", primaryKeys);
        data.put("columnCount", columns.size());

        return Response.success("表结构查询成功", data);
    }

    private Response listDatabases(DatabaseMetaData metaData) throws Exception {
        List<Map<String, Object>> databases = new ArrayList<>();
        try (ResultSet rs = metaData.getCatalogs()) {
            while (rs.next()) {
                Map<String, Object> db = new LinkedHashMap<>();
                db.put("name", rs.getString("TABLE_CAT"));
                databases.add(db);
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("operation", "list_databases");
        data.put("databaseCount", databases.size());
        data.put("databases", databases);

        return Response.success("获取数据库列表成功", data);
    }

    private Response getTableIndexes(DatabaseMetaData metaData, String databaseName, String schemaName, String tableName) throws Exception {
        if (tableName == null || tableName.trim().isEmpty()) {
            return Response.error("表名不能为空");
        }

        List<Map<String, Object>> indexes = new ArrayList<>();
        try (ResultSet rs = metaData.getIndexInfo(databaseName, schemaName, tableName, false, true)) {
            while (rs.next()) {
                Map<String, Object> index = new LinkedHashMap<>();
                index.put("name", rs.getString("INDEX_NAME"));
                index.put("columnName", rs.getString("COLUMN_NAME"));
                index.put("nonUnique", rs.getBoolean("NON_UNIQUE"));
                index.put("type", rs.getString("TYPE"));
                index.put("ordinalPosition", rs.getInt("ORDINAL_POSITION"));
                indexes.add(index);
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("operation", "table_indexes");
        data.put("tableName", tableName);
        data.put("indexes", indexes);
        data.put("indexCount", indexes.size());

        return Response.success("索引信息查询成功", data);
    }

    private Response getTableConstraints(DatabaseMetaData metaData, String databaseName, String schemaName, String tableName) throws Exception {
        if (tableName == null || tableName.trim().isEmpty()) {
            return Response.error("表名不能为空");
        }

        List<Map<String, Object>> constraints = new ArrayList<>();
        try (ResultSet rs = metaData.getExportedKeys(databaseName, schemaName, tableName)) {
            while (rs.next()) {
                Map<String, Object> constraint = new LinkedHashMap<>();
                constraint.put("pkTable", rs.getString("PKTABLE_NAME"));
                constraint.put("pkColumn", rs.getString("PKCOLUMN_NAME"));
                constraint.put("fkTable", rs.getString("FKTABLE_NAME"));
                constraint.put("fkColumn", rs.getString("FKCOLUMN_NAME"));
                constraint.put("fkName", rs.getString("FK_NAME"));
                constraint.put("pkName", rs.getString("PK_NAME"));
                constraints.add(constraint);
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("operation", "table_constraints");
        data.put("tableName", tableName);
        data.put("constraints", constraints);
        data.put("constraintCount", constraints.size());

        return Response.success("约束信息查询成功", data);
    }

    private Response getTableStatistics(Connection conn, String databaseName, String schemaName, String tableName) throws Exception {
        if (tableName == null || tableName.trim().isEmpty()) {
            return Response.error("表名不能为空");
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        try (var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) as row_count FROM " + tableName)) {
            if (rs.next()) {
                stats.put("rowCount", rs.getLong("row_count"));
            }
        }

        // 获取表大小信息（MySQL特有）
        try (var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'size_mb' " +
                     "FROM information_schema.TABLES WHERE table_schema = DATABASE() AND table_name = '" + tableName + "'")) {
            if (rs.next()) {
                stats.put("sizeMB", rs.getDouble("size_mb"));
            }
        } catch (Exception e) {
            log.debug("获取表大小信息失败", e);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("operation", "table_statistics");
        data.put("tableName", tableName);
        data.put("statistics", stats);

        return Response.success("表统计信息查询成功", data);
    }

    private Response getColumnInfo(DatabaseMetaData metaData, String databaseName, String schemaName, String tableName) throws Exception {
        return describeTable(metaData, databaseName, schemaName, tableName);
    }

    private String getDatabaseType(DatabaseMetaData metaData) throws Exception {
        String productName = metaData.getDatabaseProductName();
        if (productName.toLowerCase().contains("mysql")) return "mysql";
        if (productName.toLowerCase().contains("oracle")) return "oracle";
        if (productName.toLowerCase().contains("postgresql")) return "postgresql";
        if (productName.toLowerCase().contains("sql server")) return "sqlserver";
        return "unknown";
    }

    /**
     * 从JDBC连接URL中提取数据库名称
     */
    private String extractDatabaseName(Connection conn) throws Exception {
        String url = conn.getMetaData().getURL();
        log.debug("JDBC URL: {}", url);

        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        // 处理不同数据库类型的URL格式
        if (url.startsWith("jdbc:mysql:")) {
            // MySQL格式: jdbc:mysql://host:port/database
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash != -1) {
                String databasePart = url.substring(lastSlash + 1);
                int questionMark = databasePart.indexOf('?');
                if (questionMark != -1) {
                    databasePart = databasePart.substring(0, questionMark);
                }
                return databasePart.isEmpty() ? null : databasePart;
            }
        } else if (url.startsWith("jdbc:postgresql:")) {
            // PostgreSQL格式: jdbc:postgresql://host:port/database
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash != -1) {
                String databasePart = url.substring(lastSlash + 1);
                int questionMark = databasePart.indexOf('?');
                if (questionMark != -1) {
                    databasePart = databasePart.substring(0, questionMark);
                }
                return databasePart.isEmpty() ? null : databasePart;
            }
        } else if (url.startsWith("jdbc:oracle:")) {
            // Oracle格式: jdbc:oracle:thin:@host:port:SID
            // 或者 jdbc:oracle:thin:@host:port/service_name
            int lastColon = url.lastIndexOf(':');
            if (lastColon != -1 && url.substring(0, lastColon).split(":").length >= 5) {
                return url.substring(lastColon + 1);
            }
        } else if (url.startsWith("jdbc:sqlserver:")) {
            // SQL Server格式: jdbc:sqlserver://host:port;databaseName=database
            String[] parts = url.split(";");
            for (String part : parts) {
                if (part.startsWith("databaseName=")) {
                    return part.substring("databaseName=".length());
                }
            }
        }

        log.warn("无法从URL中提取数据库名称: {}", url);
        return null;
    }

    @Data
    public static class Request {
        private String operation;
        private String tableName;
        private String schemaName;
        private String profile;
    }

    @Data
    public static class Response {
        private boolean success;
        private String message;
        private Map<String, Object> data;
        private long executionTime;

        public static Response success(String message, Map<String, Object> data) {
            Response response = new Response();
            response.setSuccess(true);
            response.setMessage(message);
            response.setData(data);
            response.setExecutionTime(System.currentTimeMillis());
            return response;
        }

        public static Response error(String message) {
            Response response = new Response();
            response.setSuccess(false);
            response.setMessage(message);
            return response;
        }
    }
}