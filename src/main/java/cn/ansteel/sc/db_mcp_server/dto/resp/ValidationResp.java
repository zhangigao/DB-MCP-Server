package cn.ansteel.sc.db_mcp_server.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置验证响应DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationResp {

    private String profile;
    private boolean valid;
    private String message;

    public static ValidationResp valid(String profile) {
        return new ValidationResp(profile, true, "Configuration is valid");
    }

    public static ValidationResp invalid(String profile, String message) {
        return new ValidationResp(profile, false, message);
    }
}