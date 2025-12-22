package cn.ansteel.sc.db_mcp_server.mcp.tool;

import cn.ansteel.sc.db_mcp_server.constant.DatabaseConstant;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL执行函数 - 使用SpringAI注解的真实数据库操作版本
 */
@Slf4j
public class SqlExecution {

    private final DatabaseConnectionManager connectionManager;

    public SqlExecution(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Tool(name = "sql执行器", description = "执行SQL查询语句，支持SELECT、SHOW、DESCRIBE、EXPLAIN等只读操作")
    public Response executeSql(
            @ToolParam(description = "SQL查询语句") String sql,
            @ToolParam(description = "数据库连接配置名称") String profile,
            @ToolParam(description = "查询结果限制条数",required = false) Integer limit) {

        log.info("执行SQL查询: {}", sql);

        try {
            // 验证SQL安全性
            if (!isSafeQuery(sql)) {
                return Response.error("仅支持SELECT、SHOW、DESCRIBE查询");
            }

            // 设置默认值
            if (profile == null || profile.trim().isEmpty()) {
                profile = "mysql";
            }

            // 获取数据库连接
            try {
                Connection conn = connectionManager.getConnection(profile);
                // 清理SQL语句，移除可能的无效字符
                String cleanedSql = cleanSql(sql);

                String finalSql;
                if (limit != null && limit > 0) {
                    // 如果设置了limit，则应用限制
                    finalSql = applyLimit(cleanedSql, limit, conn);
                    log.info("应用limit限制: {}", limit);
                } else {
                    // 如果没有设置limit，则查询全量数据
                    finalSql = cleanedSql;
                    log.info("查询全量数据，无limit限制");
                }

                log.info("原始SQL: {}", sql);
                log.info("清理后SQL: {}", cleanedSql);
                log.info("最终执行SQL: {}", finalSql);

                // 执行查询
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(finalSql);
                SqlExecution.Response response = processResultSet(rs, sql);
                rs.close();
                stmt.close();
                conn.close();
                return response;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            log.error("SQL执行失败: {}", e.getMessage(), e);
            return Response.error("SQL执行失败: " + e.getMessage());
        }
    }

    private String cleanSql(String sql) {
        if (sql == null) {
            return null;
        }
        // 移除末尾的分号和多余空格
        return sql.trim().replaceAll(";$", "");
    }

    private boolean isSafeQuery(String sql) {
        String upperSql = sql.toUpperCase();
        return upperSql.startsWith("SELECT") ||
                upperSql.startsWith("SHOW") ||
                upperSql.startsWith("DESCRIBE") ||
                upperSql.startsWith("EXPLAIN");
    }

    private String applyLimit(String sql, int limit, Connection conn) {
        String upperSql = sql.toUpperCase();
        if (!upperSql.startsWith("SELECT") || upperSql.contains(" LIMIT ") || upperSql.contains(" ROWNUM ") ||
            upperSql.contains(" FETCH FIRST ")) {
            return sql;
        }

        try {
            DatabaseMetaData metaData = conn.getMetaData();
            String databaseProductName = metaData.getDatabaseProductName().toLowerCase();

            if (databaseProductName.contains(DatabaseConstant.ProductNames.ORACLE)) {
                // Oracle使用ROWNUM语法，更兼容旧版本
                // 将原查询包装在子查询中并添加ROWUM限制
                return "SELECT * FROM (" + sql + ") WHERE ROWNUM <= " + limit;
            } else if (databaseProductName.contains(DatabaseConstant.ProductNames.SQL_SERVER)) {
                // SQL Server syntax: TOP N
                return sql.replaceFirst("(?i)SELECT\\s+", "SELECT TOP " + limit + " ");
            } else if (databaseProductName.contains(DatabaseConstant.ProductNames.POSTGRESQL) ||
                       databaseProductName.contains(DatabaseConstant.ProductNames.MYSQL)) {
                // MySQL/PostgreSQL syntax: LIMIT N
                return sql + " LIMIT " + limit;
            } else {
                // Default to MySQL syntax for unknown databases
                return sql + " LIMIT " + limit;
            }
        } catch (Exception e) {
            log.warn("无法获取数据库类型，使用默认LIMIT语法: {}", e.getMessage());
            return sql + " LIMIT " + limit;
        }
    }

    private Response processResultSet(ResultSet rs, String originalSql) throws Exception {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // 获取列信息
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            columns.add(metaData.getColumnLabel(i));
        }

        // 获取数据行
        List<Map<String, Object>> rows = new ArrayList<>();
        int rowCount = 0;
        while (rs.next() && rowCount < 1000) { // 额外安全限制
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                Object value = rs.getObject(i);
                row.put(columns.get(i - 1), value);
            }
            rows.add(row);
            rowCount++;
        }

        return Response.success(originalSql, columns, rows, rowCount);
    }

    @Data
    public static class Request {
        private String sql;
        private String profile;
        private Integer limit;
    }

    @Data
    public static class Response {
        private boolean success;
        private String message;
        private String sql;
        private List<String> columns;
        private List<Map<String, Object>> rows;
        private int rowCount;
        private long executionTime;

        public static Response success(String sql, List<String> columns, List<Map<String, Object>> rows, int rowCount) {
            Response response = new Response();
            response.setSuccess(true);
            response.setMessage("查询执行成功");
            response.setSql(sql);
            response.setColumns(columns);
            response.setRows(rows);
            response.setRowCount(rowCount);
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