package cn.ansteel.sc.db_mcp_server.dto.resp;

import cn.ansteel.sc.db_mcp_server.config.DatabaseConfigProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置详情响应DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileConfigResp {

    private boolean success;
    private String message;
    private String profile;
    private boolean active;
    private boolean valid;
    private boolean canConnect;
    private DatabaseConfigProperties.ConnectionConfig config;
    private boolean hasPassword;

    public static ProfileConfigResp success(String profile, boolean isActive, boolean isValid,
                                          boolean canConnect, DatabaseConfigProperties.ConnectionConfig config) {
        ProfileConfigResp resp = new ProfileConfigResp();
        resp.setSuccess(true);
        resp.setMessage("Success");
        resp.setProfile(profile);
        resp.setActive(isActive);
        resp.setValid(isValid);
        resp.setCanConnect(canConnect);
        resp.setConfig(config);
        resp.setHasPassword(config.getPassword() != null && !config.getPassword().isEmpty());
        return resp;
    }

    public static ProfileConfigResp failure(String profile, String message) {
        ProfileConfigResp resp = new ProfileConfigResp();
        resp.setSuccess(false);
        resp.setMessage(message);
        resp.setProfile(profile);
        resp.setActive(false);
        resp.setValid(false);
        resp.setCanConnect(false);
        resp.setConfig(null);
        resp.setHasPassword(false);
        return resp;
    }
}