package cn.ansteel.sc.db_mcp_server.service;

import cn.ansteel.sc.db_mcp_server.config.DatabaseConfigProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数据源配置文件存储服务
 */
@Slf4j
@Service
public class DatabaseConfigFileService {

    private static final String CONFIG_FILE_NAME = "database-configs.json";
    private final ObjectMapper objectMapper;
    private final Path configFilePath;

    public DatabaseConfigFileService() {
        this.objectMapper = new ObjectMapper();
        // 在项目根目录下创建配置文件
        this.configFilePath = Paths.get(System.getProperty("user.dir"), CONFIG_FILE_NAME);
        initializeConfigFile();
    }

    /**
     * 初始化配置文件
     */
    private void initializeConfigFile() {
        try {
            if (!Files.exists(configFilePath)) {
                log.error("配置文件被删除或损坏 停止程序: {}", configFilePath);
                System.exit(1);
            } else {
                log.info("使用现有配置文件: {}", configFilePath);
            }
        } catch (Exception e) {
            log.error("初始化配置文件失败", e);
            throw new RuntimeException("初始化配置文件失败", e);
        }
    }

    /**
     * 读取所有配置
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> readAllConfigs() {
        try {
            File configFile = configFilePath.toFile();
            if (!configFile.exists()) {
                log.error("配置文件被删除或损坏 停止程序: {}", configFilePath);
                System.exit(1);
            }

            return objectMapper.readValue(configFile, Map.class);
        } catch (IOException e) {
            log.error("读取配置文件失败", e);
            throw new RuntimeException("读取配置文件失败", e);
        }
    }

    /**
     * 写入所有配置
     */
    public void writeAllConfigs(Map<String, Object> configs) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configFilePath.toFile(), configs);
            log.info("配置文件更新成功");
        } catch (IOException e) {
            log.error("写入配置文件失败", e);
            throw new RuntimeException("写入配置文件失败", e);
        }
    }

    /**
     * 获取所有数据源配置
     */
    @SuppressWarnings("unchecked")
    public Map<String, DatabaseConfigProperties.ConnectionConfig> readProfiles() {
        Map<String, Object> allConfigs = readAllConfigs();
        Map<String, Object> profilesData = (Map<String, Object>) allConfigs.get("profiles");

        Map<String, DatabaseConfigProperties.ConnectionConfig> profiles = new LinkedHashMap<>();

        if (profilesData != null) {
            profilesData.forEach((name, configData) -> {
                try {
                    // 将Map转换为ConnectionConfig对象
                    Map<String, Object> configMap = (Map<String, Object>) configData;
                    DatabaseConfigProperties.ConnectionConfig config = objectMapper.convertValue(configMap, DatabaseConfigProperties.ConnectionConfig.class);
                    profiles.put(name, config);
                } catch (Exception e) {
                    log.error("转换数据源配置失败: {}", name, e);
                }
            });
        }

        return profiles;
    }

    /**
     * 获取活跃的数据源名称
     */
    public String readActiveProfile() {
        Map<String, Object> allConfigs = readAllConfigs();
        return (String) allConfigs.getOrDefault("activeProfile", "mysql");
    }

    /**
     * 设置活跃的数据源
     */
    public void writeActiveProfile(String profile) {
        Map<String, Object> allConfigs = readAllConfigs();
        allConfigs.put("activeProfile", profile);
        writeAllConfigs(allConfigs);
        log.info("活跃数据源已更新为: {}", profile);
    }

    /**
     * 添加或更新数据源配置
     */
    public void writeProfile(String profileName, DatabaseConfigProperties.ConnectionConfig config) {
        Map<String, Object> allConfigs = readAllConfigs();

        @SuppressWarnings("unchecked")
        Map<String, Object> profiles = (Map<String, Object>) allConfigs.computeIfAbsent("profiles", k -> new HashMap<>());

        // 将ConnectionConfig对象转换为Map
        Map<String, Object> configMap = objectMapper.convertValue(config, Map.class);
        profiles.put(profileName, configMap);

        writeAllConfigs(allConfigs);
        log.info("数据源配置已保存: {}", profileName);
    }

    /**
     * 删除数据源配置
     */
    public void deleteProfile(String profileName) {
        Map<String, Object> allConfigs = readAllConfigs();

        @SuppressWarnings("unchecked")
        Map<String, Object> profiles = (Map<String, Object>) allConfigs.get("profiles");

        if (profiles != null) {
            profiles.remove(profileName);

            // 如果删除的是当前活跃的配置，需要切换到其他配置
            String currentActive = (String) allConfigs.get("activeProfile");
            if (profileName.equals(currentActive)) {
                String newActive = profiles.isEmpty() ? null : profiles.keySet().iterator().next();
                allConfigs.put("activeProfile", newActive);
            }

            writeAllConfigs(allConfigs);
            log.info("数据源配置已删除: {}", profileName);
        }
    }

    /**
     * 检查配置文件是否存在
     */
    public boolean configFileExists() {
        return Files.exists(configFilePath);
    }

    /**
     * 获取配置文件路径
     */
    public String getConfigFilePath() {
        return configFilePath.toString();
    }
}