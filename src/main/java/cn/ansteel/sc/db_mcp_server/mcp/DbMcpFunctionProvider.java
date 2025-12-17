package cn.ansteel.sc.db_mcp_server.mcp;

import cn.ansteel.sc.db_mcp_server.factory.DataSourceFactory;
import cn.ansteel.sc.db_mcp_server.mcp.functions.DatabaseConnectionManager;
import cn.ansteel.sc.db_mcp_server.mcp.functions.MetadataQueryFunction;
import cn.ansteel.sc.db_mcp_server.mcp.functions.SqlExecutionFunction;
import cn.ansteel.sc.db_mcp_server.service.DatabaseConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

/**
 * MCP函数提供者 - 使用DatabaseConfigService配置
 */
@Slf4j
@Configuration
public class DbMcpFunctionProvider {

    @Bean
    public DatabaseConnectionManager databaseConnectionManager(
            DatabaseConfigService databaseConfigService,
            DataSourceFactory dataSourceFactory) {
        return new DatabaseConnectionManager(databaseConfigService, dataSourceFactory);
    }

    @Bean
    @Description("执行SQL查询并返回结果。支持SELECT、SHOW、DESCRIBE查询，自动限制结果集大小。")
    public Function<SqlExecutionFunction.Request, SqlExecutionFunction.Response> executeSql(DatabaseConnectionManager connectionManager) {
        return new SqlExecutionFunction(connectionManager);
    }

    @Bean
    @Description("查询数据库元数据信息，包括表结构、索引、约束等")
    public Function<MetadataQueryFunction.Request, MetadataQueryFunction.Response> queryMetadata(DatabaseConnectionManager connectionManager) {
        return new MetadataQueryFunction(connectionManager);
    }
}