package cn.ansteel.sc.db_mcp_server.mcp.functions;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL执行函数 - 真实数据库操作版本
 */
@Slf4j
public class SqlExecutionFunction implements java.util.function.Function<SqlExecutionFunction.Request, SqlExecutionFunction.Response> {

    private final DatabaseConnectionManager connectionManager;

    public SqlExecutionFunction(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public Response apply(Request request) {
        log.info("执行SQL查询: {}", request.getSql());

        try {
            // 验证SQL安全性
            String sql = request.getSql().trim();
            if (!isSafeQuery(sql)) {
                return Response.error("仅支持SELECT、SHOW、DESCRIBE查询");
            }

            // 获取数据库连接
            String profile = request.getProfile() != null ? request.getProfile() : "mysql";
            try (Connection conn = connectionManager.getConnection(profile)) {

                // 设置查询限制
                int limit = request.getLimit() != null ? request.getLimit() : 100;
                String limitedSql = applyLimit(sql, limit);

                // 执行查询
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(limitedSql)) {

                    return processResultSet(rs, sql);
                }
            }
        } catch (Exception e) {
            log.error("SQL执行失败: {}", e.getMessage(), e);
            return Response.error("SQL执行失败: " + e.getMessage());
        }
    }

    private boolean isSafeQuery(String sql) {
        String upperSql = sql.toUpperCase();
        return upperSql.startsWith("SELECT") ||
               upperSql.startsWith("SHOW") ||
               upperSql.startsWith("DESCRIBE") ||
               upperSql.startsWith("EXPLAIN");
    }

    private String applyLimit(String sql, int limit) {
        String upperSql = sql.toUpperCase();
        if (upperSql.startsWith("SELECT") && !upperSql.contains(" LIMIT ")) {
            return sql + " LIMIT " + limit;
        }
        return sql;
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