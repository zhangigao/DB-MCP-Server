package cn.ansteel.sc.db_mcp_server.dto.resp;

import cn.ansteel.sc.db_mcp_server.config.DatabaseConfigProperties;
import cn.ansteel.sc.db_mcp_server.enums.DatabaseType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 活跃配置响应DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActiveConfigResp {

    private String activeProfile;
    private DatabaseType databaseType;
    private DatabaseConfigProperties.ConnectionConfig connectionConfig;
}