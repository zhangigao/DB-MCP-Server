package cn.ansteel.sc.db_mcp_server.dto.resp;

import cn.ansteel.sc.db_mcp_server.config.DatabaseConfigProperties;
import lombok.Data;

import java.util.Map;

/**
 * @author 15566
 * @version 0.0.1
 * @description: 配置响应类
 * @date 2025/12/15 15:31
 */
@Data
public class ProfilesResp {

    private Map<String, DatabaseConfigProperties.ConnectionConfig> availableProfiles;
    private String active;
}
