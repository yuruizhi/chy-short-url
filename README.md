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

## 百万级并发支持

要实现百万级并发短链接服务，除了短链接生成策略外，还需要在以下方面进行资源配置和优化：

### 1. 硬件资源配置

#### 服务器配置
- **应用服务器**: 建议至少10台高性能服务器组成集群
  - 每台配置: 16+ 核心CPU，64GB+ 内存，高速SSD
  - 网络带宽: 至少10Gbps网络接口
- **Redis集群**: 至少3主3从的集群配置
  - 每节点配置: 8+ 核心CPU，32GB+ 内存
  - 建议使用Redis Cluster而非单实例
- **数据库服务器**: 主从架构或分片集群
  - 主服务器: 16+ 核心CPU，64GB+ 内存，高性能SSD
  - 至少配置2个从库用于读操作分流

#### 负载均衡
- 至少2台高性能负载均衡器(如F5、LVS或Nginx)
- 配置会话保持和健康检查机制
- 考虑使用DNS轮询实现地理级负载均衡

### 2. 系统参数优化

#### JVM配置
```bash
# 示例配置 - 根据实际服务器内存调整
JAVA_OPTS="-Xms16g -Xmx16g -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+ParallelRefProcEnabled"
```

#### Linux系统参数
```bash
# 最大文件描述符数量
sysctl -w fs.file-max=1000000

# TCP连接参数优化
sysctl -w net.ipv4.tcp_max_syn_backlog=65536
sysctl -w net.core.somaxconn=65536
sysctl -w net.ipv4.tcp_fin_timeout=30
sysctl -w net.ipv4.tcp_tw_reuse=1

# 本地端口范围扩大
sysctl -w net.ipv4.ip_local_port_range="1024 65535"
```

#### Tomcat/Undertow配置
```yaml
server:
  tomcat:
    max-threads: 1000                # 最大工作线程数
    max-connections: 20000           # 最大连接数
    accept-count: 2000               # 等待队列长度
    connection-timeout: 3000         # 连接超时时间(ms)
    processor-cache: 1000            # 处理器缓存大小
  undertow:                          # 如使用Undertow
    io-threads: 16                   # IO线程数，通常设置为CPU核心数
    worker-threads: 1000             # 工作线程数
    buffer-size: 1024                # 缓冲区大小
```

### 3. 网络架构优化

#### CDN加速
- 使用全球分布的CDN网络缓存热门短链接
- 配置适当的缓存策略和TTL
- 实现CDN回源流控，避免雪崩效应

#### 多区域部署
- 在多个地理区域部署服务实例
- 实现就近接入，降低访问延迟
- 容灾备份，提高系统可用性

### 4. 数据库优化

#### 分库分表策略
```
# 按照短码哈希分片，例如:
short_url_0, short_url_1, ..., short_url_N
```

#### 读写分离
- 写操作路由到主库
- 读操作分发到多个从库
- 实现动态数据源切换

#### 批量操作优化
```java
// 批量插入示例
@Transactional
public void batchInsert(List<UrlMapping> mappings) {
    // 每批100条数据
    int batchSize = 100;
    for (int i = 0; i < mappings.size(); i += batchSize) {
        int endIndex = Math.min(i + batchSize, mappings.size());
        List<UrlMapping> subList = mappings.subList(i, endIndex);
        urlMappingMapper.insertBatch(subList);
    }
}
```

### 5. 缓存架构优化

#### 多级缓存策略
- 浏览器缓存: 设置合理的HTTP缓存头
- CDN缓存: 全球加速热门链接
- 应用层缓存: Caffeine本地缓存
- 分布式缓存: Redis集群
- 数据库缓存: 利用数据库查询缓存

#### Redis集群配置
```yaml
spring:
  redis:
    cluster:
      nodes:
        - redis-node1:6379
        - redis-node2:6379
        - redis-node3:6379
        - redis-node4:6379
        - redis-node5:6379
        - redis-node6:6379
      max-redirects: 3
    lettuce:
      pool:
        max-active: 1000
        max-idle: 100
        min-idle: 50
        max-wait: 200ms
    timeout: 1000ms
```

### 6. 限流熔断

#### API限流配置
```yaml
resilience4j:
  ratelimiter:
    instances:
      shortenUrl:
        limit-for-period: 1000
        limit-refresh-period: 1s
        timeout-duration: 100ms
      redirectUrl:
        limit-for-period: 10000
        limit-refresh-period: 1s
        timeout-duration: 100ms
```

#### 熔断器配置
```yaml
resilience4j:
  circuitbreaker:
    instances:
      urlService:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 5000ms
        permitted-number-of-calls-in-half-open-state: 10
        sliding-window-size: 100
        sliding-window-type: COUNT_BASED
```

### 7. 监控告警

#### 指标收集
- JVM监控: 内存、GC、线程
- 系统监控: CPU、内存、磁盘I/O、网络
- 应用监控: QPS、响应时间、错误率
- 缓存监控: 命中率、过期率、内存占用

#### 预警配置
```yaml
# Prometheus告警规则示例
groups:
- name: ShortUrl
  rules:
  - alert: HighErrorRate
    expr: sum(rate(http_server_requests_seconds_count{status="5xx"}[1m])) / sum(rate(http_server_requests_seconds_count[1m])) > 0.01
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "高错误率告警"
      description: "服务错误率超过1%，当前值: {{ $value }}"
```

### 8. 消息队列引入

#### 异步处理架构
```
[请求] --> [生成/重定向服务] --> [消息队列] --> [统计处理服务]
```

#### Kafka配置
```yaml
spring:
  kafka:
    bootstrap-servers: kafka1:9092,kafka2:9092,kafka3:9092
    producer:
      retries: 3
      acks: 1
      batch-size: 16384
      buffer-memory: 33554432
    consumer:
      group-id: short-url-stats-group
      auto-offset-reset: latest
      max-poll-records: 500
```

### 9. 部署方案

#### 容器编排
```yaml
# Kubernetes部署配置示例
apiVersion: apps/v1
kind: Deployment
metadata:
  name: short-url-service
spec:
  replicas: 10
  selector:
    matchLabels:
      app: short-url
  template:
    metadata:
      labels:
        app: short-url
    spec:
      containers:
      - name: short-url-app
        image: short-url:latest
        resources:
          requests:
            memory: "4Gi"
            cpu: "2"
          limits:
            memory: "8Gi"
            cpu: "4"
        ports:
        - containerPort: 8080
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
```

#### 自动扩缩容
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: short-url-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: short-url-service
  minReplicas: 5
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

以上资源配置结合使用，可以有效支持百万级并发的短链接服务。实际配置应根据具体业务负载特性和可用资源进行调整优化。

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

本系统支持多种短链接生成策略，可以根据业务需求选择最合适的方案：

### 1. 随机生成策略 (RandomShortUrlStrategy)

- 随机生成指定长度的字符串作为短码
- 优点：实现简单，均匀分布
- 缺点：可能存在冲突，需要重试机制
- 适用场景：通用短链接生成

### 2. 雪花算法策略 (SnowflakeShortUrlStrategy)

- 基于Twitter的Snowflake算法生成唯一ID，然后转为62进制作为短码
- 优点：保证唯一性，有序递增，支持分布式生成
- 缺点：依赖时钟，如果时钟回拨可能出现问题
- 适用场景：高并发分布式环境

### 3. MD5哈希策略 (Md5ShortUrlStrategy)

- 基于URL内容计算MD5哈希值，并取其中一部分作为短码
- 优点：固定URL生成固定短码，计算效率高
- 缺点：理论上存在哈希碰撞可能性
- 适用场景：对URL内容稳定性要求高的场景

### 4. MurmurHash3策略 (Murmur3ShortUrlStrategy)

- 使用MurmurHash3算法生成哈希值，哈希分布更均匀
- 优点：比MD5更快，哈希质量高，碰撞概率低
- 缺点：同样存在碰撞可能
- 适用场景：高性能要求场景

### 5. 自增计数器策略 (CounterShortUrlStrategy)

- 基于Redis全局计数器实现自增ID，支持分布式环境
- 优点：实现简单，短码连续可预测，生成性能高
- 缺点：短码可被预测，且长度随计数增长
- 适用场景：对短码安全性要求不高，但要求稳定生成的场景

### 6. Base64编码策略 (Base64ShortUrlStrategy)

- 使用Base64编码方式，可将URL信息编码为短码
- 优点：支持信息编码，可实现可逆转换
- 缺点：生成的短码相对较长
- 适用场景：需要在短码中包含特定信息的场景

可以通过配置文件选择短链接生成策略：

```yaml
shorturl:
  strategy: RANDOM  # 可选值: RANDOM, SNOWFLAKE, MD5, MURMUR3, COUNTER, BASE64
```

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
