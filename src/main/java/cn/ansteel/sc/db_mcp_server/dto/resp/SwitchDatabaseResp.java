package cn.ansteel.sc.db_mcp_server.dto.resp;

import cn.ansteel.sc.db_mcp_server.enums.DatabaseType;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 15566
 * @version 0.0.1
 * @description: 切换配置源相应类
 * @date 2025/12/15 15:47
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SwitchDatabaseResp {

    private String database;
    private String activeProfile;
    private DatabaseType databaseType;
}
