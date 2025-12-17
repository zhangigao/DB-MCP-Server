package cn.ansteel.sc.db_mcp_server.mcp;

import cn.ansteel.sc.db_mcp_server.mcp.functions.MetadataQueryFunction;
import cn.ansteel.sc.db_mcp_server.mcp.functions.SqlExecutionFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

/**
 * MCP HTTP/SSE端点控制器
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
@RequiredArgsConstructor
public class McpHttpController {

    private final Function<SqlExecutionFunction.Request, SqlExecutionFunction.Response> executeSql;
    private final Function<MetadataQueryFunction.Request, MetadataQueryFunction.Response> queryMetadata;

    /**
     * MCP工具列表 - SSE模式
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> toolsListSse(
            @RequestParam(required = false) String sessionId) {

        log.info("MCP SSE连接建立 - sessionId: {}", sessionId);

        return Flux.concat(
            // 发送初始化事件
            createSseEvent("initialize", Map.of(
                "sessionId", sessionId,
                "serverInfo", Map.of(
                    "name", "Database MCP Server",
                    "version", "1.0.0"
                )
            )),

            // 发送工具列表
            createSseEvent("tools/list", Map.of(
                "tools", getToolDefinitions()
            )),

            // 保持连接活跃
            Flux.interval(Duration.ofSeconds(30))
                .map(i -> ServerSentEvent.builder("ping").build())
        );
    }

    /**
     * MCP工具调用 - SSE模式
     */
    @PostMapping(value = "/sse/call", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> callToolSse(
            @RequestBody McpToolCallRequest request,
            @RequestParam(required = false) String sessionId) {

        log.info("MCP工具调用 - sessionId: {}, tool: {}", sessionId, request.name());

        // 参数验证
        if (request.name() == null || request.name().trim().isEmpty()) {
            log.warn("工具名称为空");
            return Flux.just(createErrorSseEvent("Tool name is required"));
        }

        if (request.arguments() == null) {
            log.warn("工具参数为空");
            return Flux.just(createErrorSseEvent("Tool arguments are required"));
        }

        try {
            Object result = switch (request.name()) {
                case "executeSql" -> {
                    String sql = (String) request.arguments().get("sql");
                    if (sql == null || sql.trim().isEmpty()) {
                        throw new IllegalArgumentException("SQL parameter is required for executeSql");
                    }

                    SqlExecutionFunction.Request sqlRequest = new SqlExecutionFunction.Request();
                    sqlRequest.setSql(sql);
                    sqlRequest.setProfile((String) request.arguments().get("profile"));
                    sqlRequest.setLimit((Integer) request.arguments().getOrDefault("limit", 100));
                    yield executeSql.apply(sqlRequest);
                }
                case "queryMetadata" -> {
                    String operation = (String) request.arguments().get("operation");
                    if (operation == null || operation.trim().isEmpty()) {
                        throw new IllegalArgumentException("Operation parameter is required for queryMetadata");
                    }

                    MetadataQueryFunction.Request metaRequest = new MetadataQueryFunction.Request();
                    metaRequest.setOperation(operation);
                    metaRequest.setTableName((String) request.arguments().get("table_name"));
                    metaRequest.setSchemaName((String) request.arguments().get("schema_name"));
                    metaRequest.setProfile((String) request.arguments().get("profile"));
                    yield queryMetadata.apply(metaRequest);
                }
                default -> throw new IllegalArgumentException("Unknown tool: " + request.name() +
                    ". Available tools: executeSql, queryMetadata");
            };

            return Flux.concat(
                createSseEvent("tool/start", Map.of("tool", request.name())),
                createSseEvent("tool/result", result),
                createSseEvent("tool/complete", Map.of("tool", request.name()))
            );

        } catch (Exception e) {
            log.error("工具调用失败: {}", e.getMessage(), e);
            return Flux.concat(
                createSseEvent("tool/error", Map.of(
                    "tool", request.name(),
                    "error", e.getMessage()
                )),
                Flux.just(createErrorSseEvent("Tool execution failed: " + e.getMessage()))
            );
        }
    }

    /**
     * Streamable HTTP模式
     */
    @PostMapping(value = "/streamable/call")
    public Flux<ServerSentEvent<String>> callToolStreamable(
            @RequestBody McpToolCallRequest request) {

        log.info("Streamable HTTP工具调用 - tool: {}", request.name());

        return callToolSse(request, null)
            .timeout(Duration.ofSeconds(30))
            .onErrorResume(e -> Flux.just(createErrorSseEvent(e.getMessage())));
    }

    /**
     * 传统HTTP模式
     */
    @PostMapping("/call")
    public Mono<McpHttpResponse> callTool(@RequestBody McpToolCallRequest request) {
        log.info("HTTP工具调用 - tool: {}", request.name());

        // 参数验证
        if (request.name() == null || request.name().trim().isEmpty()) {
            log.warn("工具名称为空");
            return Mono.just(McpHttpResponse.error("Tool name is required"));
        }

        if (request.arguments() == null) {
            log.warn("工具参数为空");
            return Mono.just(McpHttpResponse.error("Tool arguments are required"));
        }

        try {
            Object result = switch (request.name()) {
                case "executeSql" -> {
                    String sql = (String) request.arguments().get("sql");
                    if (sql == null || sql.trim().isEmpty()) {
                        throw new IllegalArgumentException("SQL parameter is required for executeSql");
                    }

                    SqlExecutionFunction.Request sqlRequest = new SqlExecutionFunction.Request();
                    sqlRequest.setSql(sql);
                    sqlRequest.setProfile((String) request.arguments().get("profile"));
                    sqlRequest.setLimit((Integer) request.arguments().getOrDefault("limit", 100));
                    yield executeSql.apply(sqlRequest);
                }
                case "queryMetadata" -> {
                    String operation = (String) request.arguments().get("operation");
                    if (operation == null || operation.trim().isEmpty()) {
                        throw new IllegalArgumentException("Operation parameter is required for queryMetadata");
                    }

                    MetadataQueryFunction.Request metaRequest = new MetadataQueryFunction.Request();
                    metaRequest.setOperation(operation);
                    metaRequest.setTableName((String) request.arguments().get("table_name"));
                    metaRequest.setSchemaName((String) request.arguments().get("schema_name"));
                    metaRequest.setProfile((String) request.arguments().get("profile"));
                    yield queryMetadata.apply(metaRequest);
                }
                default -> throw new IllegalArgumentException("Unknown tool: " + request.name() +
                    ". Available tools: executeSql, queryMetadata");
            };

            return Mono.just(McpHttpResponse.success(result));

        } catch (Exception e) {
            log.error("工具调用失败: {}", e.getMessage(), e);
            return Mono.just(McpHttpResponse.error(e.getMessage()));
        }
    }

    /**
     * MCP JSON-RPC 端点 - 完整的MCP协议实现
     */
    @PostMapping
    public Map<String, Object> handleMcpRequest(@RequestBody Map<String, Object> request) {
        String requestId = (request != null && request.containsKey("id")) ?
            String.valueOf(request.get("id")) : "unknown";
        String method = (String) request.get("method");

        log.info("MCP请求 - method: {}, id: {}", method, requestId);

        try {
            switch (method) {
                case "initialize":
                    return handleInitialize(request, requestId);
                case "tools/list":
                    return handleToolsList(request, requestId);
                case "tools/call":
                    return handleToolCall(request, requestId);
                default:
                    return createErrorResponse(requestId, -32601, "Method not found: " + method);
            }
        } catch (Exception e) {
            log.error("MCP请求处理失败: {}", e.getMessage(), e);
            return createErrorResponse(requestId, -32603, "Internal error: " + e.getMessage());
        }
    }

    /**
     * MCP tools/list 端点 - 标准MCP JSON-RPC格式
     */
    @PostMapping("/tools")
    public Map<String, Object> toolsList(@RequestBody(required = false) Map<String, Object> request) {
        // 如果请求体为空或缺少id，使用默认ID
        String requestId = (request != null && request.containsKey("id")) ?
            String.valueOf(request.get("id")) : "tools-list";

        return handleToolsList(request, requestId);
    }

    /**
     * 处理initialize方法
     */
    private Map<String, Object> handleInitialize(Map<String, Object> request, String requestId) {
        return Map.of(
            "jsonrpc", "2.0",
            "id", requestId,
            "result", Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(
                    "tools", Map.of(),
                    "logging", Map.of()
                ),
                "serverInfo", Map.of(
                    "name", "Database MCP Server",
                    "version", "1.0.0"
                )
            )
        );
    }

    /**
     * 处理tools/list方法
     */
    private Map<String, Object> handleToolsList(Map<String, Object> request, String requestId) {
        return Map.of(
            "jsonrpc", "2.0",
            "id", requestId,
            "result", Map.of(
                "tools", new Object[]{
                    Map.of(
                        "name", "executeSql",
                        "description", "执行SQL查询并返回结果。支持SELECT、SHOW、DESCRIBE查询，自动限制结果集大小。",
                        "inputSchema", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                "sql", Map.of(
                                    "type", "string",
                                    "description", "要执行的SQL语句"
                                ),
                                "profile", Map.of(
                                    "type", "string",
                                    "description", "数据库配置文件名称"
                                ),
                                "limit", Map.of(
                                    "type", "integer",
                                    "description", "最大结果行数，默认100"
                                )
                            ),
                            "required", new String[]{"sql"}
                        )
                    ),
                    Map.of(
                        "name", "queryMetadata",
                        "description", "查询数据库元数据信息，包括表结构、索引、约束等",
                        "inputSchema", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                "operation", Map.of(
                                    "type", "string",
                                    "description", "元数据查询操作类型",
                                    "enum", new String[]{"list_tables", "describe_table", "list_databases", "table_indexes", "table_constraints", "table_statistics", "column_info"}
                                ),
                                "table_name", Map.of(
                                    "type", "string",
                                    "description", "表名（当操作需要时）"
                                ),
                                "schema_name", Map.of(
                                    "type", "string",
                                    "description", "模式名（数据库需要时）"
                                ),
                                "profile", Map.of(
                                    "type", "string",
                                    "description", "数据库配置文件名称"
                                )
                            ),
                            "required", new String[]{"operation"}
                        )
                    )
                }
            )
        );
    }

    /**
     * 处理tools/call方法
     */
    private Map<String, Object> handleToolCall(Map<String, Object> request, String requestId) {
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        String toolName = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");

        log.info("MCP工具调用 - tool: {}, arguments: {}", toolName, arguments);

        try {
            Object result = switch (toolName) {
                case "executeSql" -> {
                    String sql = (String) arguments.get("sql");
                    if (sql == null || sql.trim().isEmpty()) {
                        throw new IllegalArgumentException("SQL parameter is required for executeSql");
                    }

                    SqlExecutionFunction.Request sqlRequest = new SqlExecutionFunction.Request();
                    sqlRequest.setSql(sql);
                    sqlRequest.setProfile((String) arguments.get("profile"));
                    sqlRequest.setLimit((Integer) arguments.getOrDefault("limit", 100));
                    yield executeSql.apply(sqlRequest);
                }
                case "queryMetadata" -> {
                    String operation = (String) arguments.get("operation");
                    if (operation == null || operation.trim().isEmpty()) {
                        throw new IllegalArgumentException("Operation parameter is required for queryMetadata");
                    }

                    MetadataQueryFunction.Request metaRequest = new MetadataQueryFunction.Request();
                    metaRequest.setOperation(operation);
                    metaRequest.setTableName((String) arguments.get("table_name"));
                    metaRequest.setSchemaName((String) arguments.get("schema_name"));
                    metaRequest.setProfile((String) arguments.get("profile"));
                    yield queryMetadata.apply(metaRequest);
                }
                default -> throw new IllegalArgumentException("Unknown tool: " + toolName +
                    ". Available tools: executeSql, queryMetadata");
            };

            return Map.of(
                "jsonrpc", "2.0",
                "id", requestId,
                "result", Map.of(
                    "content", new Object[]{
                        Map.of(
                            "type", "text",
                            "text", result.toString()
                        )
                    }
                )
            );

        } catch (Exception e) {
            log.error("工具调用失败: {}", e.getMessage(), e);
            return Map.of(
                "jsonrpc", "2.0",
                "id", requestId,
                "result", Map.of(
                    "content", new Object[]{
                        Map.of(
                            "type", "text",
                            "text", "Error: " + e.getMessage()
                        )
                    },
                    "isError", true
                )
            );
        }
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String id, int code, String message) {
        return Map.of(
            "jsonrpc", "2.0",
            "id", id,
            "error", Map.of(
                "code", code,
                "message", message
            )
        );
    }

    /**
     * 获取工具列表 - 兼容格式（GET请求）
     */
    @GetMapping("/tools")
    public McpToolsResponse getToolsCompat() {
        return new McpToolsResponse(getToolDefinitions());
    }

    private Map<String, Object> getToolDefinitions() {
        return Map.of(
            "executeSql", Map.of(
                "name", "executeSql",
                "description", "执行SQL查询并返回结果。支持SELECT、SHOW、DESCRIBE查询，自动限制结果集大小。",
                "inputSchema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "sql", Map.of(
                            "type", "string",
                            "description", "要执行的SQL语句"
                        ),
                        "profile", Map.of(
                            "type", "string",
                            "description", "数据库配置文件名称"
                        ),
                        "limit", Map.of(
                            "type", "integer",
                            "description", "最大结果行数，默认100"
                        )
                    ),
                    "required", new String[]{"sql"}
                )
            ),
            "queryMetadata", Map.of(
                "name", "queryMetadata",
                "description", "查询数据库元数据信息，包括表结构、索引、约束等",
                "inputSchema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "operation", Map.of(
                            "type", "string",
                            "description", "元数据查询操作类型",
                            "enum", new String[]{"list_tables", "describe_table", "list_databases", "table_indexes", "table_constraints", "table_statistics", "column_info"}
                        ),
                        "table_name", Map.of(
                            "type", "string",
                            "description", "表名（当操作需要时）"
                        ),
                        "schema_name", Map.of(
                            "type", "string",
                            "description", "模式名（数据库需要时）"
                        ),
                        "profile", Map.of(
                            "type", "string",
                            "description", "数据库配置文件名称"
                        )
                    ),
                    "required", new String[]{"operation"}
                )
            )
        );
    }

    private Flux<ServerSentEvent<String>> createSseEvent(String type, Object data) {
        return Flux.just(ServerSentEvent.builder(String.valueOf(data))
            .id(type)
            .event(type)
            .build());
    }

    private ServerSentEvent<String> createErrorSseEvent(String error) {
        return ServerSentEvent.builder(error)
            .id("error")
            .event("error")
            .build();
    }

    // 数据传输对象
    public record McpToolCallRequest(
        String name,
        Map<String, Object> arguments
    ) {}

    public record McpToolsResponse(
        Map<String, Object> tools
    ) {}

    public record McpHttpResponse(
        boolean success,
        String message,
        Object data
    ) {
        public static McpHttpResponse success(Object data) {
            return new McpHttpResponse(true, "Success", data);
        }

        public static McpHttpResponse error(String message) {
            return new McpHttpResponse(false, message, null);
        }
    }
}