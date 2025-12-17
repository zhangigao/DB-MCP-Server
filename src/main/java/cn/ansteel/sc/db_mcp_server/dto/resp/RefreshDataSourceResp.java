package cn.ansteel.sc.db_mcp_server.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 刷新数据源响应DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefreshDataSourceResp {

    private boolean success;
    private String message;
    private String profile;
    private String dataSource;

    public static RefreshDataSourceResp success(String profile, String dataSource) {
        return new RefreshDataSourceResp(true, "Data source refreshed successfully", profile, dataSource);
    }

    public static RefreshDataSourceResp failure(String profile, String message) {
        return new RefreshDataSourceResp(false, message, profile, null);
    }
}