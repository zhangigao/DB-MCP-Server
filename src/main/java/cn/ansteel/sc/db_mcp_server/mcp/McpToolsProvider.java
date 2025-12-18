package cn.ansteel.sc.db_mcp_server.mcp;

import cn.ansteel.sc.db_mcp_server.mcp.tool.DatabaseConnectionManager;
import cn.ansteel.sc.db_mcp_server.mcp.tool.MetadataQuery;
import cn.ansteel.sc.db_mcp_server.mcp.tool.SqlExecution;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringAI MCP工具提供者
 * 注册数据库操作工具到SpringAI MCP服务器
 */
@Configuration
public class McpToolsProvider {

    /**
     * 注册SpringAI MCP工具
     * SpringAI会自动扫描带有@Tool注解的方法并注册为MCP工具
     */
    @Bean
    public ToolCallbackProvider databaseToolCallbackProvider(
            SqlExecution sqlExecution,
            MetadataQuery metadataQuery) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(sqlExecution, metadataQuery)
                .build();
    }

    /**
     * SqlExecution工具Bean
     */
    @Bean
    public SqlExecution sqlExecution(DatabaseConnectionManager connectionManager) {
        return new SqlExecution(connectionManager);
    }

    /**
     * MetadataQuery工具Bean
     */
    @Bean
    public MetadataQuery metadataQuery(DatabaseConnectionManager connectionManager) {
        return new MetadataQuery(connectionManager);
    }
}
