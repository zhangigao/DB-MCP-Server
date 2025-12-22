package cn.ansteel.sc.db_mcp_server.controller;

import cn.ansteel.sc.db_mcp_server.dto.Result;
import cn.ansteel.sc.db_mcp_server.dto.req.ExecuteQueryReq;
import cn.ansteel.sc.db_mcp_server.dto.req.MetadataQueryReq;
import cn.ansteel.sc.db_mcp_server.mcp.tool.MetadataQuery;
import cn.ansteel.sc.db_mcp_server.mcp.tool.SqlExecution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 数据库查询控制器
 */

@Slf4j
@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
public class DatabaseQueryController {

    private final MetadataQuery metadataQuery;
    private final SqlExecution sqlExecution;

    @PostMapping("/{profile}")
    public Result<SqlExecution.Response> executeQuery(@PathVariable String profile,
                                                      @RequestBody ExecuteQueryReq req) {
        SqlExecution.Response resp = sqlExecution.executeSql(req.getSql(), profile, req.getLimit());
        return Result.success(resp);
    }

    @PostMapping("/{profile}/metadata")
    public Result<MetadataQuery.Response> executeMetadataQuery(@PathVariable String profile,
                                                               @RequestBody MetadataQueryReq req) {
        MetadataQuery.Response resp = metadataQuery.queryMetadata(req.getOperation(), req.getTableName(), req.getSchemaName(), profile);
        return Result.success(resp);
    }
}
