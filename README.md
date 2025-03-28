# 高性能短链接服务 (CHY Short URL)

一个基于Java实现的高性能短链接服务，支持百万级并发访问。

## 技术栈

- Java 21
- Spring Boot 3.2
- Redis (缓存层)
- MySQL (存储层)
- MyBatis-Plus (ORM)
- Lombok
- Redisson (分布式锁)

## 系统架构

系统采用多层架构设计，包括：
- **应用层**: 提供API接口，处理短链接的生成和重定向请求
- **服务层**: 实现核心业务逻辑，包括短链接生成策略和访问统计
- **缓存层**: 使用Redis缓存热门短链接，提高访问速度，降低数据库压力
- **持久层**: 使用MySQL存储链接映射关系

## 高性能设计

本系统能够支持百万级并发，主要通过以下技术手段实现：

1. **多级缓存架构**
   - Redis缓存热门短链接，减轻数据库压力
   - 本地缓存作为一级缓存，进一步提高响应速度

2. **短链接生成策略**
   - 提供多种短链接生成策略：随机生成、雪花ID算法
   - 可根据业务需求动态切换生成策略

3. **异步处理**
   - 访问计数采用异步处理，不影响主流程性能
   - 使用线程池处理异步任务，提高吞吐量

4. **Tomcat性能优化**
   - 增大最大线程数和最大连接数
   - 适当调整连接超时时间

5. **数据库优化**
   - 建立合适的索引
   - 使用HikariCP连接池优化数据库连接

6. **分布式部署**
   - 支持集群水平扩展
   - 使用Redisson实现分布式锁，保证短码唯一性

## 短链接生成方案

### 1. 随机生成策略 (RandomShortUrlStrategy)

- 随机生成指定长度的字符串作为短码
- 优点：实现简单，均匀分布
- 缺点：可能存在冲突，需要重试机制

### 2. 雪花算法策略 (SnowflakeShortUrlStrategy)

- 基于Twitter的Snowflake算法生成唯一ID，然后转为62进制作为短码
- 优点：保证唯一性，有序递增
- 缺点：依赖时钟，如果时钟回拨可能出现问题

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

- 数据库连接信息
- Redis连接信息
- 短链接生成策略
- 短链接长度
- 线程池参数

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

## 贡献者

- [Henry.Yu](https://github.com/yuruizhi)

## 许可证

本项目使用 MIT 许可证
