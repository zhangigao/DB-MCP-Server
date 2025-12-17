package cn.ansteel.sc.db_mcp_server.dto.resp;

import cn.ansteel.sc.db_mcp_server.service.DatabaseConfigService;
import lombok.Data;

import java.util.Map;

/**
 * 配置状态响应DTO
 */
@Data
public class ConfigStatusResp {

    private Map<String, DatabaseConfigService.ConfigStatus> statuses;

    public ConfigStatusResp(Map<String, DatabaseConfigService.ConfigStatus> statuses) {
        this.statuses = statuses;
    }
}