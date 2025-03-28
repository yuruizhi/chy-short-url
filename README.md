# 高性能短链接服务 (CHY Short URL)

一个基于Java实现的高性能短链接服务，支持百万级并发访问。

## 技术栈

- Java 21
- Spring Boot 3.2
- Redis (分布式缓存)
- Caffeine (本地缓存)
- MySQL (存储层)
- MyBatis-Plus (ORM)
- Lombok
- Redisson (分布式锁)
- Logstash Logback Encoder (JSON日志格式化)

## 系统架构

系统采用多层架构设计，包括：
- **应用层**: 提供API接口，处理短链接的生成和重定向请求
- **服务层**: 实现核心业务逻辑，包括短链接生成策略和访问统计
- **缓存层**: 采用多级缓存架构，Caffeine本地缓存 + Redis分布式缓存
- **持久层**: 使用MySQL存储链接映射关系

## 高性能设计

本系统能够支持百万级并发，主要通过以下技术手段实现：

1. **三级缓存架构**
   - Caffeine本地缓存作为一级缓存，提供纳秒级访问速度
   - Redis分布式缓存作为二级缓存，保证集群一致性
   - MySQL数据库作为三级存储，保证数据持久化

2. **短链接生成策略**
   - 提供多种短链接生成策略：随机生成、雪花ID算法
   - 可根据业务需求动态切换生成策略

3. **异步处理与线程池优化**
   - 访问计数采用异步处理，不影响主流程性能
   - 优化的线程池参数，提高吞吐量
   - 定时批量同步访问统计，减少数据库写操作
   - 线程池监控，实时掌握系统负载情况

4. **Tomcat性能优化**
   - 增大最大线程数和最大连接数
   - 适当调整连接超时时间

5. **数据库优化**
   - 建立合适的索引
   - 使用连接池优化数据库连接

6. **分布式部署**
   - 支持集群水平扩展
   - 使用Redisson实现分布式锁，保证短码唯一性

## 缓存设计

### 1. Caffeine本地缓存
- **shortUrlLocalCache**: 缓存短链接映射关系，默认容量10000，过期时间3600秒
- **metadataLocalCache**: 缓存元数据(如访问统计)，默认容量2000，过期时间1800秒
- 优点：纳秒级访问速度，减轻Redis负担
- 应用场景：高频访问的热门短链接

### 2. Redis分布式缓存
- 缓存所有有效的短链接映射
- 支持集群部署，保证缓存一致性
- 应用场景：多实例间共享数据，负载均衡环境

### 3. 缓存监控
- 提供`/actuator/cache/stats`接口实时监控缓存状态
- 统计指标：缓存大小、命中率、未命中率、加载时间等

## 线程池优化

系统使用专门优化的线程池处理异步任务：

```java
ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
// 核心线程数
executor.setCorePoolSize(10);
// 最大线程数
executor.setMaxPoolSize(50);
// 队列容量
executor.setQueueCapacity(2000);
// 允许核心线程超时，提高资源利用率
executor.setAllowCoreThreadTimeOut(true);
// 拒绝策略：由调用线程处理任务
executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
```

### 线程池监控
- 后台守护线程定期监控线程池状态
- 记录活跃线程数、任务队列大小、已完成任务数等指标
- 监控周期可通过配置调整

## 短链接生成方案

### 1. 随机生成策略 (RandomShortUrlStrategy)

- 随机生成指定长度的字符串作为短码
- 优点：实现简单，均匀分布
- 缺点：可能存在冲突，需要重试机制

### 2. 雪花算法策略 (SnowflakeShortUrlStrategy)

- 基于Twitter的Snowflake算法生成唯一ID，然后转为62进制作为短码
- 优点：保证唯一性，有序递增
- 缺点：依赖时钟，如果时钟回拨可能出现问题

## 访问统计优化

### 1. 异步统计
- 使用CompletableFuture异步更新访问计数
- 线程池处理异步任务，避免创建过多线程

### 2. 批量处理
- 使用本地缓存暂存访问计数
- 定时任务每分钟批量同步数据到数据库
- 显著减少数据库写操作频率

## 性能测试方案

### 测试工具

- Apache JMeter
- wrk (HTTP基准测试工具)

### 测试场景

1. **短链接生成性能测试**
   - 并发用户数：100, 500, 1000, 5000
   - 测试时长：5分钟
   - 指标：TPS, 响应时间, 错误率

2. **短链接访问性能测试**
   - 并发用户数：1000, 5000, 10000, 50000
   - 测试时长：10分钟
   - 指标：QPS, 响应时间, 错误率

3. **持续负载测试**
   - 并发用户数：逐渐递增至最大值
   - 测试时长：30分钟
   - 指标：系统稳定性, 资源占用情况

### 测试指标

- **QPS(每秒查询率)**: 衡量系统处理读请求的能力
- **TPS(每秒事务数)**: 衡量系统处理写请求的能力
- **响应时间**: 请求从发送到接收响应的时间
- **错误率**: 请求失败的比例
- **资源使用率**: CPU, 内存, 网络I/O, 磁盘I/O
- **缓存命中率**: 本地缓存和Redis缓存的命中情况

## 请求链路追踪

### 1. 请求ID生成和传递

- 使用过滤器为每个请求自动生成请求ID (RequestIdFilter)
- 通过MDC机制在日志中自动关联请求ID
- 请求ID在响应头中返回，供客户端调试使用

### 2. JSON格式日志

- 使用Logstash Logback Encoder将日志输出为JSON格式
- 每条日志包含请求ID、时间戳、日志级别等标准字段
- 便于日志系统采集和分析

### 3. 参数日志注解

- 使用`@LogParam`注解自动记录方法入参和出参
- 支持敏感信息脱敏和日志级别控制
- 参数化配置，灵活适应不同场景需求

```java
// 简单用法
@LogParam(desc = "接口描述")
public Result method(Param param) {
    // 方法实现
}

// 完整用法
@LogParam(
    desc = "接口描述",
    printRequest = true,      // 是否打印请求参数
    printResponse = true,     // 是否打印响应结果
    printHeaders = false,     // 是否打印请求头
    printException = true,    // 是否打印异常堆栈
    hideSensitive = true,     // 是否隐藏敏感信息
    responseMaxLength = 1000, // 响应结果最大长度
    level = LogLevel.INFO     // 日志级别
)
public Result method(Param param) {
    // 方法实现
}
```

### 4. 日志格式示例

```json
{
  "timestamp": "2024-04-27 10:15:23.345",
  "level": "INFO",
  "thread": "http-nio-8080-exec-1",
  "class": "com.chy.shorturl.controller.ShortUrlController",
  "message": "[生成短链接] 开始执行, 请求参数: {\"url\":\"https://example.com\",\"phone\":\"138****1234\"}, requestId: a1b2c3d4e5f6g7h8",
  "request_id": "a1b2c3d4e5f6g7h8",
  "stack_trace": null
}
```

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

### 构建项目

```bash
# 克隆项目
git clone https://github.com/yuruizhi/chy-short-url.git
cd chy-short-url

# 编译打包
mvn clean package

# 运行应用
java -jar target/shorturl-0.0.1-SNAPSHOT.jar
```

### 配置说明

通过修改`application.yml`文件定制以下配置：

```yaml
shorturl:
  domain: http://localhost:8080
  cache-expire: 86400  # Redis缓存过期时间（秒）
  length: 6  # 短链接长度
  
  # 本地缓存配置
  cache:
    local:
      shortUrl:
        size: 10000
        expire-seconds: 3600
      metadata:
        size: 2000
        expire-seconds: 1800
    
  # 线程池配置
  thread:
    core-size: 10
    max-size: 50
    queue-capacity: 2000
    keep-alive-seconds: 60
    monitor:
      period-seconds: 60
```

### API 使用

#### 1. 生成短链接

```http
POST /api/url/shorten
Content-Type: application/json

{
    "url": "https://www.example.com/very/long/url/that/needs/to/be/shortened",
    "expireTime": 86400
}
```

#### 2. 访问短链接

```http
GET /{shortCode}
```

#### 3. 查看缓存统计

```http
GET /actuator/cache/stats
```

## 贡献者

- [Henry.Yu](https://github.com/yuruizhi)

## 许可证

本项目使用 MIT 许可证
