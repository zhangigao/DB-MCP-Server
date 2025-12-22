package cn.ansteel.sc.db_mcp_server.dto.req;

import lombok.Data;

/**
 * @author 15566
 * @version 0.0.1
 * @description: TODO
 * @date 2025/12/22 9:05
 */
@Data
public class MetadataQueryReq {

    /**
     * 操作类型：
     * list_tables,
     * describe_table,
     * list_databases,
     * table_indexes,
     * table_constraints,
     * table_statistics,
     * column_info
     */
    private String operation;

    private String tableName;
    private String schemaName;
}
