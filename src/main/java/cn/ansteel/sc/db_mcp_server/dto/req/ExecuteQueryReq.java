package cn.ansteel.sc.db_mcp_server.dto.req;

import lombok.Data;

/**
 * @author 15566
 * @version 0.0.1
 * @description: 执行查询入参
 * @date 2025/12/22 9:00
 */
@Data
public class ExecuteQueryReq {

    private String sql;
    private Integer limit;
}
