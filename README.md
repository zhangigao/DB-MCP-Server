# 数据库 MCP 服务器 🚀

一个基于Spring AI MCP的企业级数据库查询和元数据访问服务器，支持多种数据库类型，提供安全、高效的数据库访问能力。

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.12-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.java.net/)
[![MCP](https://img.shields.io/badge/MCP-Model%20Context%20Protocol-purple.svg)](https://modelcontextprotocol.io/)

## ✨ 功能特性

### 🔧 MCP功能
- **SQL执行工具** (`executeSql`): 安全执行SELECT、SHOW、DESCRIBE、EXPLAIN查询
- **元数据查询工具** (`queryMetadata`): 全面的数据库元数据访问能力
- **数据库作用域限制**: 自动将查询限制在指定数据库范围内

### 🗄️ 支持的数据库
- **MySQL** 8.0+
- **PostgreSQL** 12+
- **Oracle** 19c+
- **SQL Server** 2019+

### 🛡️ 安全特性
- **只读查询**: 只允许SELECT、SHOW、DESCRIBE、EXPLAIN查询
- **自动LIMIT限制**: 防止大数据量查询
- **数据库作用域**: 查询自动限制在配置的数据库内
- **连接池管理**: HikariCP高性能连接池
- **配置文件加密**: 敏感信息保护

## 🏗️ 项目架构

```
db_mcp_server/
├── src/main/java/cn/ansteel/sc/db_mcp_server/
│   ├── constant/                    # 常量定义
│   │   ├── McpConstants.java       # MCP相关常量
│   │   └── DatabaseConstant.java   # 数据库相关常量
│   ├── config/                      # 配置类
│   ├── controller/                  # REST控制器
│   ├── dto/                         # 数据传输对象
│   ├── enums/                       # 枚举类
│   ├── factory/                     # 工厂类
│   ├── mcp/                         # MCP核心功能
│   │   ├── DbMcpFunctionProvider.java
│   │   ├── McpHttpController.java
│   │   └── functions/              # MCP功能实现
│   │       ├── MetadataQueryFunction.java
│   │       ├── SqlExecutionFunction.java
│   │       └── DatabaseConnectionManager.java
│   └── service/                     # 服务层
└── src/main/resources/
    ├── application.yml              # 应用配置
    └── database-configs.json       # 数据库配置
```

## 🚀 快速开始

### 1. 环境要求
- Java 21+
- Maven 3.6+
- 对应的数据库驱动

### 2. 启动服务器
```bash
# 编译并启动
mvn clean compile
mvn spring-boot:run

# 或者指定端口
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8080
```

### 3. 配置数据库连接

编辑 `database-configs.json`:

```json
{
  "activeProfile": "mysql",
  "profiles": {
    "mysql": {
      "driverClassName": "com.mysql.cj.jdbc.Driver",
      "url": "jdbc:mysql://localhost:3306/your_database",
      "username": "your_username",
      "password": "your_password",
      "pool": null
    },
    "postgresql": {
      "driverClassName": "org.postgresql.Driver",
      "url": "jdbc:postgresql://localhost:5432/your_database",
      "username": "your_username",
      "password": "your_password",
      "pool": null
    }
  }
}
```

## 🔧 MCP功能使用

### SQL执行工具

**函数名**: `executeSql`

**参数**:
- `sql` (必需): SQL查询语句
- `profile` (可选): 数据库配置名称，默认使用当前活跃配置
- `limit` (可选): 最大结果行数，默认100

**支持的SQL类型**:
- `SELECT` - 查询数据
- `SHOW` - 显示数据库信息
- `DESCRIBE` - 描述表结构
- `EXPLAIN` - 查询执行计划

**示例**:
```json
{
  "sql": "SELECT * FROM users WHERE status = 'active' ORDER BY created_at DESC",
  "profile": "mysql",
  "limit": 50
}
```

**响应格式**:
```json
{
  "success": true,
  "message": "SQL执行成功",
  "data": {
    "type": "SELECT",
    "sql": "SELECT * FROM users WHERE status = 'active' ORDER BY created_at DESC LIMIT 50",
    "rowCount": 25,
    "executionTimeMs": 15,
    "columns": ["id", "name", "email", "status", "created_at"],
    "data": [
      {"id": 1, "name": "张三", "email": "zhangsan@example.com", "status": "active", "created_at": "2024-01-15T10:30:00"},
      {"id": 2, "name": "李四", "email": "lisi@example.com", "status": "active", "created_at": "2024-01-16T09:20:00"}
    ]
  }
}
```

### 元数据查询工具

**函数名**: `queryMetadata`

**参数**:
- `operation` (必需): 操作类型
  - `list_tables`: 列出指定数据库中的所有表
  - `describe_table`: 描述表结构（列信息、主键等）
  - `list_databases`: 列出所有数据库
  - `table_indexes`: 查询表索引信息
  - `table_constraints`: 查询表约束信息
  - `table_statistics`: 查询表统计信息（行数、大小等）
  - `column_info`: 查询列详细信息
- `table_name` (部分操作需要): 表名
- `schema_name` (可选): 模式名
- `profile` (可选): 数据库配置名称

**示例**:
```json
{
  "operation": "describe_table",
  "table_name": "users",
  "profile": "mysql"
}
```

**响应格式**:
```json
{
  "success": true,
  "message": "表结构查询成功",
  "data": {
    "operation": "describe_table",
    "tableName": "users",
    "columnCount": 6,
    "columns": [
      {"name": "id", "type": "int", "size": 11, "nullable": false, "defaultValue": null, "remarks": "用户ID"},
      {"name": "name", "type": "varchar", "size": 100, "nullable": false, "defaultValue": null, "remarks": "用户姓名"},
      {"name": "email", "type": "varchar", "size": 150, "nullable": false, "defaultValue": null, "remarks": "邮箱地址"},
      {"name": "status", "type": "varchar", "size": 20, "nullable": false, "defaultValue": "active", "remarks": "用户状态"},
      {"name": "created_at", "type": "datetime", "size": 19, "nullable": false, "defaultValue": "CURRENT_TIMESTAMP", "remarks": "创建时间"}
    ],
    "primaryKeys": [
      {"columnName": "id", "keySeq": 1, "pkName": "PRIMARY"}
    ]
  }
}
```

## 🌐 REST API接口

服务器提供REST API用于配置管理和监控：

### 配置管理
- `GET /api/database/config/active` - 获取当前活跃配置
- `GET /api/database/config/profiles` - 获取所有可用配置
- `GET /api/database/config/status` - 获取配置状态
- `POST /api/database/config/switch/{profile}` - 切换数据库配置
- `POST /api/database/config/test/{profile}` - 测试数据库连接
- `POST /api/database/config/refresh` - 刷新配置

### 系统监控
- `GET /api/database/config/validation` - 获取配置验证结果
- `GET /actuator/health` - 健康检查
- `GET /actuator/info` - 应用信息

## 📝 使用场景

### 1. 数据分析
```json
{
  "sql": "SELECT category, COUNT(*) as count, AVG(price) as avg_price FROM products GROUP BY category ORDER BY count DESC",
  "limit": 20
}
```

### 2. 表结构探索
```json
{
  "operation": "list_tables",
  "profile": "mysql"
}
```

### 3. 性能分析
```json
{
  "operation": "table_statistics",
  "table_name": "orders",
  "profile": "mysql"
}
```

### 4. 索引分析
```json
{
  "operation": "table_indexes",
  "table_name": "orders",
  "profile": "mysql"
}
```

## ⚠️ 错误处理

所有MCP函数都包含完整的错误处理：

```json
{
  "success": false,
  "message": "查询失败: Table 'test.users' doesn't exist",
  "data": null
}
```

常见错误：
- 🚫 表不存在
- 🚫 SQL语法错误
- 🚫 连接失败
- 🚫 权限不足
- 🚫 不支持的操作类型
- 🚫 数据库配置不存在

## 🔍 监控和日志

### 启用详细日志
```yaml
logging:
  level:
    cn.ansteel.sc.db_mcp_server: DEBUG
    com.zaxxer.hikari: DEBUG
```

### 日志示例
```
2024-12-17 12:18:04.462  INFO 29864 --- [db_mcp_server] c.a.s.d.DbMcpServerApplication : Started DbMcpServerApplication
2024-12-17 12:18:05.746  INFO 29864 --- [db_mcp_server] c.a.s.d.s.DatabaseConfigFileService : 使用现有配置文件
2024-12-17 12:18:06.132  INFO 29864 --- [db_mcp_server] o.s.a.m.s.a.McpServerAutoConfiguration : Enable tools capabilities, notification: true
2024-12-17 12:18:06.536  INFO 29864 --- [db_mcp_server] o.s.b.web.embedded.netty.NettyWebServer : Netty started on port 8089
```

## 🔧 扩展开发

### 添加新的数据库支持

1. **更新枚举**: 在 `DatabaseType` 中添加新类型
```java
public enum DatabaseType {
    MYSQL("mysql", "com.mysql.cj.jdbc.Driver", "MySQL"),
    ORACLE("oracle", "oracle.jdbc.OracleDriver", "Oracle"),
    POSTGRESQL("postgresql", "org.postgresql.Driver", "PostgreSQL"),
    SQLSERVER("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "SQL Server"),
    // 新增数据库类型
    NEWDB("newdb", "com.newdb.jdbc.Driver", "NewDatabase");
}
```

2. **更新常量**: 在 `DatabaseConstant.TestStatements` 中添加测试语句
```java
public static final class TestStatements {
    public static final String MYSQL = "SELECT 1";
    public static final String NEWDB = "SELECT 1 FROM DUAL";  // 新增
}
```

3. **更新URL解析**: 在 `MetadataQueryFunction.extractDatabaseName` 中添加URL解析逻辑

4. **添加依赖**: 在 `pom.xml` 中添加JDBC驱动

### 自定义MCP功能

1. **创建功能类**: 实现 `Function<T, R>` 接口
2. **注册Bean**: 在 `DbMcpFunctionProvider` 中添加 `@Bean` 注解
3. **添加描述**: 使用 `@Description` 注解描述功能

## 🏛️ 技术栈

### 核心框架
- **Spring Boot 3.4.12** - 应用框架
- **Spring AI MCP** - MCP协议支持
- **Spring WebFlux** - 响应式Web框架

### 数据库相关
- **HikariCP** - 高性能连接池
- **多数据库驱动** - MySQL, PostgreSQL, Oracle, SQL Server

### 开发工具
- **Lombok** - 代码简化
- **Jackson** - JSON处理
- **SLF4J + Logback** - 日志框架

### 监控运维
- **Spring Boot Actuator** - 应用监控
- **Spring Boot Configuration Processor** - 配置处理

## 🛠️ 配置说明

### 应用配置 (application.yml)
```yaml
server:
  port: 8080

spring:
  application:
    name: db_mcp_server

logging:
  level:
    cn.ansteel.sc.db_mcp_server: INFO
    com.zaxxer.hikari: INFO

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### 数据库配置 (database-configs.json)
```json
{
  "activeProfile": "mysql",
  "profiles": {
    "mysql": {
      "driverClassName": "com.mysql.cj.jdbc.Driver",
      "url": "jdbc:mysql://localhost:3306/sc_db",
      "username": "root",
      "password": "password",
      "pool": {
        "maximumPoolSize": 10,
        "minimumIdle": 2,
        "connectionTimeout": 30000,
        "idleTimeout": 600000,
        "maxLifetime": 1800000
      }
    }
  }
}
```

## 🚀 性能优化

### 连接池优化
- 默认最大连接数：10
- 最小空闲连接：2
- 连接超时：30秒
- 空闲超时：10分钟
- 连接生命周期：30分钟

### 查询优化
- 自动LIMIT限制，防止大数据量查询
- 只允许只读SQL操作
- 使用预编译语句防止SQL注入

### 内存优化
- 使用Stream处理大量数据
- 及时释放数据库连接
- 合理的结果集大小限制

## 🧪 测试

### 单元测试
```bash
mvn test
```

### 集成测试
```bash
mvn verify -P integration-test
```

### 手动测试
使用提供的HTML页面进行功能测试：
```bash
mvn spring-boot:run
# 访问 http://localhost:8080
```

## 📋 版本历史

### v1.0.0 (2024-12-17)
- ✨ 基础MCP功能实现
- 🔧 支持MySQL、PostgreSQL、Oracle、SQL Server
- 🛡️ 数据库作用域限制功能
- 📝 完整的常量管理和代码规范化
- 🌐 REST API配置管理接口

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🆘 支持

如果您遇到问题或有疑问：

1. 📖 查看 [FAQ](docs/FAQ.md)
2. 🐛 提交 [Issue](https://github.com/your-repo/issues)
3. 📧 发送邮件至 zhanghongjun0228@gmail.com

---

**⭐ 如果这个项目对您有帮助，请给我们一个Star！**
