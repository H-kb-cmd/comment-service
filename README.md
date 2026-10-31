# comment-service 评论服务（微服务版）

一个基于 **Spring Boot + Spring Cloud Alibaba** 的评论服务，从单体应用改造为微服务架构的练习项目。

改造覆盖了微服务落地时最核心的四件事：**服务注册发现、网关路由、服务间调用（Feign）、熔断降级（Sentinel）**。

---

## 架构

```
                     前端 / 测试请求
                          │
                          ▼
              ┌───────────────────────┐
              │  comment-gateway      │  9201  统一入口
              │  Spring Cloud Gateway │  /api/** → 去掉 /api 前缀
              └───────────┬───────────┘
                          │  lb://feidian-comment（按服务名负载均衡）
                          ▼
              ┌───────────────────────┐
              │  comment-service      │  8081  评论业务
              │  @FeignClient ────────┼──────┐
              └───────────┬───────────┘      │ POST /user/ids
                          │                  ▼
                          │      ┌───────────────────────┐
                          │      │  user-service         │  8082  用户数据
                          │      └───────────┬───────────┘
                          ▼                  ▼
                   comment 库             user_db 库
                   （comment 表）          （user 表）

              三个服务全部注册到 Nacos（127.0.0.1:8848）
```

| 服务 | 端口 | 服务名（Nacos） | 职责 | 数据库 |
|---|---|---|---|---|
| `comment-gateway` | 9201 | `comment-gateway` | 统一入口、路由、耗时统计过滤器 | 无 |
| `comment-service` | 8081 | `feidian-comment` | 评论的增删改查、分页、楼中楼组装 | `comment` |
| `user-service` | 8082 | `user-service` | 用户查询（单个 / 批量） | `user_db` |

> 本仓库只包含 `comment-service`。同系列的另外两个服务是独立工程，通过 Nacos 服务名互联，不依赖编译期耦合。
>
> 配套仓库：[user-service](https://github.com/H-kb-cmd/user-service) · [comment-gateway](https://github.com/H-kb-cmd/comment-gateway)
>
> 构建顺序：`user-service` 依赖本工程的 `comment-common` 模块，**先在本仓库执行 `mvn install`**，再构建 `user-service`。

---

## 模块结构

```
comment-service
├── pom.xml                                     # 聚合 POM，统一 Spring Cloud 版本
├── application                                 # 启动模块（Spring Boot 入口）
└── shared.all                                  # 共享模块聚合
    ├── comment-common                          # ★ 公共模块：Result / User 等跨服务复用的模型
    └── comment.shared
        ├── comment.api                         # 对外 HTTP 接口层（Controller）
        ├── comment.service                     # 服务契约 + 领域模型 + 用户服务 Feign 客户端
        └── comment.service.impl                # 业务实现 + DAO + MyBatis 映射
```

| 模块 | 职责 | 关键类 |
|---|---|---|
| `application` | 应用启动、Spring 扫描、Feign 客户端扫描 | `Application`（`@EnableFeignClients`） |
| `comment-common` | 跨服务复用的模型，被 `user-service` 以 Maven 依赖方式复用 | `Result`、`User` |
| `comment.api` | 暴露 REST 接口 | `CommentController` |
| `comment.service` | 定义服务契约 | `CommentService`、`UserService`（`@FeignClient`）、`UserFallbackService` |
| `comment.service.impl` | 业务实现、数据访问、DO 转换 | `CommentServiceImpl`、`CommentDAO`、`CommentDO` |

---

## 技术栈

| 组件 | 版本 | 说明 |
|---|---|---|
| Java | 11 | Spring Cloud Hoxton 系列的最低要求，**不要用 17/21** |
| Spring Boot | 2.3.0.RELEASE | |
| Spring Cloud | Hoxton.SR9 | 与 Boot 2.3.x 配套 |
| Spring Cloud Alibaba | 2.2.6.RELEASE | 与 Hoxton.SR9 配套，三者版本必须对齐 |
| Nacos | 1.x / 2.x | 注册中心 |
| OpenFeign | — | 声明式服务调用 |
| Sentinel | 1.8.x | 熔断降级 |
| MyBatis Spring Boot Starter | 2.1.3 | |
| PageHelper | 1.2.13 | 物理分页 |
| Druid | 1.1.23 | 连接池 + 监控页 |
| MySQL | 5.7+ | `utf8mb4` / InnoDB |

---

## 核心设计

### 1. 服务间调用：Feign + default 方法拆包

评论列表需要展示作者信息，而用户数据已经迁到 `user-service`。改造时用 Feign 声明式调用，**并且让业务代码一行不改**：

```java
@FeignClient(value = "user-service", fallback = UserFallbackService.class)
public interface UserService {

    /** Feign 只代理带 @RequestMapping 的抽象方法，返回原始 Result 包装 */
    @PostMapping("/user/ids")
    Result<List<User>> findByIdsRemote(@RequestBody List<Long> ids);

    /** default 方法不会被代理，在这里把 Result 拆开，对上层屏蔽包装细节 */
    default List<User> findByIds(List<Long> ids) {
        Result<List<User>> r = findByIdsRemote(ids);
        return r == null || r.getData() == null ? Collections.emptyList() : r.getData();
    }
}
```

好处：`CommentServiceImpl` 里原本的 `userService.findByIds(userIds)` 调用**完全不需要改**，微服务改造对业务代码透明。

**为什么用 POST 批量查**：唯一调用方是 Feign，`GET + List` 参数需要手动拼 `?ids=1,2,3`，`POST + @RequestBody` 更直接，也避免 URL 长度限制。

**N+1 问题**：分页查出 15 条评论后，先收集去重后的 `userId`，**一次** Feign 调用批量查回所有用户，再用 `Map<Long, User>` 建索引 O(n) 补全，而不是每条评论查一次。

### 2. 熔断降级：fallback 返回占位用户

`UserFallbackService` 是 `UserService` 的降级实现（`@Component` + `implements UserService`），当 `user-service` 不可用时，**不查任何数据库**，直接按 `userId` 返回占位用户：

```java
{
  "id": 1,
  "userName": "unknown",
  "nickName": "未命名用户"
}
```

设计要点：

- `fallback` 生效的前提是 `feign.sentinel.enabled = true`（否则 Sentinel 不接管 Feign）。
- 降级方法必须返回 `Result.ok(...)` 而不是 `Result.fail(...)`：如果返回失败，上层拿到 `data = null`，`author` 会变成 null，前端渲染直接崩；返回占位用户则页面仍能正常展示（**降级的目标是“降级但可用”，不是“报错”**）。
- 保留 `user_id` 是对的：评论到底是谁写的这个事实还在，只是名字暂时显示不出来。

实测熔断器的三状态流转（连续三次请求，日志/返回值如下）：

| 次数 | user-service 状态 | 观察到的 `author` | 熔断器状态 | 耗时 |
|---|---|---|---|---|
| 1 | 未启动 | `unknown / 未命名用户` | CLOSED（请求失败，开始计数） | ~1s（等连接超时） |
| 2 | 未启动 | `unknown / 未命名用户` | **OPEN**（已达阈值，直接拒绝） | ~10ms（不再等超时） |
| 3 | 手动启动 | `zhangsan / Zhang San` | HALF_OPEN → CLOSED（探测成功） | 正常 |

第 2 次请求从 ~1s 降到 ~10ms，是熔断器在起作用：**快速失败，不再让请求卡在超时上**，保护调用方线程池。

### 3. 数据所有权分离

改造前 `comment` 库里有 `user` 表，评论查询用 `left join user` 一次拿全。
改造后：

- 用户数据**只**属于 `user-service`（`user_db.user`），评论服务不再碰用户表；
- `CommentDAO.xml` 的 `findByRefId` 从 `left join user` 改回**单表查询**，作者信息在 Service 层通过 Feign 补齐；
- 代价是从 1 次 SQL 变成 2 次远程调用，收益是**表结构与服务边界对齐**——用户表怎么改都不会影响评论服务，这是微服务拆分的前提。

### 4. 网关统一入口

```yaml
routes:
  - id: comment_route
    uri: lb://feidian-comment      # lb:// = 从 Nacos 取实例列表做负载均衡
    predicates:
      - Path=/api/**
    filters:
      - StripPrefix=1              # 转发前去掉 /api 这一段
```

另有一个自定义 `GlobalFilter`（`RequestTimeFilter`）统计每个请求的耗时，属于网关层做统一横切逻辑的典型场景（鉴权、限流、日志都可以放这里）。

### 5. 分页：PageHelper 只拦截第一条 SQL

`PageHelper.startPage()` 的拦截是**一次性**的：它只会给接下来执行的第一条 SQL 拼上 `LIMIT`。因此：

- `startPage` 必须紧挨着 DAO 调用，中间不能插别的查询；
- 分页总数要立即用 `new PageInfo<>(dos)` 取出来，再做后续的补全作者等操作（补全会产生新的查询，可能消耗掉拦截）；
- 所以 `findPage()` 里是「startPage → DAO → PageInfo 取分页信息 → 补作者」的顺序。

---

## 接口

### 评论服务（`comment-service`，8081）

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/comments?pageNum=&pageSize=` | 分页查询（默认第 1 页、每页 15 条） |
| `POST` | `/comment` | 新增单条评论 |
| `POST` | `/comment/batchAdd` | 批量新增 |
| `POST` | `/comment/update` | 更新评论内容 |
| `GET` | `/comment/del?id=` | 按主键删除 |
| `GET` | `/comment/findByRefId?refId=` | 按业务 ID 查评论（楼中楼结构） |
| `GET` | `/comment/findUserIds?userIds=` | 按用户 ID 集合查评论 |

### 用户服务（`user-service`，8082）

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/user/{id}` | 查单个用户 |
| `POST` | `/user/ids` | 批量查用户，body 为 `[1,2,3]` |

### 通过网关访问（9201）

```bash
curl.exe "http://localhost:9201/api/comment/findByRefId?refId=1001"
#                 └─ /api 由网关剥离，实际打到 comment-service 的 /comment/findByRefId
```

---

## 配置

敏感信息全部走环境变量，配置文件里只保留本地默认值：

| 环境变量 | 说明 | 默认值 |
|---|---|---|
| `DB_URL` | MySQL 连接串 | `jdbc:mysql://localhost:3306/comment?serverTimezone=GMT%2B8` |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | 空（**必须设置，否则连接池初始化会一直等待**） |
| `NACOS_ADDR` | Nacos 地址 | `127.0.0.1:8848` |
| `DRUID_USERNAME` | Druid 监控页账号 | `admin` |
| `DRUID_PASSWORD` | Druid 监控页密码 | 空 |

```bash
# Windows PowerShell
$env:DB_PASSWORD = "your-password"
$env:NACOS_ADDR  = "127.0.0.1:8848"

# Linux / macOS
export DB_PASSWORD="your-password"
export NACOS_ADDR="127.0.0.1:8848"
```

---

## 启动

**顺序不能乱**：Nacos → MySQL → user-service → comment-service → gateway。

```bash
# 0. 基础设施（Docker）
docker start nacos mysql-comment

# 1. 初始化数据库（首次）
#    mysql -uroot -p < init-comment-db.sql

# 2. 编译安装（多模块项目，改了兄弟模块必须 install 而不是 compile）
mvn clean install -DskipTests

# 3. 启动评论服务
mvn -pl application spring-boot:run
```

**验证**：

```bash
# 服务是否注册到 Nacos（应返回 feidian-comment / user-service / comment-gateway）
curl.exe "http://localhost:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=20"

# 直接访问评论服务
curl.exe "http://localhost:8081/comment/findByRefId?refId=1001"

# 通过网关访问（统一入口）
curl.exe "http://localhost:9201/api/comment/findByRefId?refId=1001"
```

**降级演示**：停掉 `user-service` 再请求，`author` 应变成 `unknown / 未命名用户`；重新启动 `user-service`，等到熔断器半开探测成功后恢复正常。

---

## 数据库表结构

`comment` 库 `comment` 表：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT | 主键，自增 |
| `ref_id` | VARCHAR | 业务引用 ID（一篇文章 / 一个商品） |
| `user_id` | BIGINT | 评论者 ID，**只存 ID，不存用户信息** |
| `content` | VARCHAR | 评论内容（入库前做 HTML 转义） |
| `parent_id` | BIGINT | 父评论 ID，`0` 表示一级评论 |
| `gmt_created` | DATETIME | 创建时间 |
| `gmt_modified` | DATETIME | 更新时间 |

字符集统一 `utf8mb4`（`utf8` 存不了 emoji，评论场景很容易踩到）。

---

## 踩坑记录

改造过程中遇到的几个典型问题，记录下来备查：

| 现象 | 真正原因 | 结论 |
|---|---|---|
| 改了代码、重启了服务，行为还是旧的 | 多模块项目里应用依赖的是装进 `~/.m2` 的 jar，`mvn compile` 只更新 `target/classes` | 改兄弟模块必须 `mvn clean install`；用「进程启动时间 vs jar 安装时间」判断 |
| 启动失败，报 `No bean named '...importRegistry'` | 这是连带伤害。堆栈最底部真实的 `Caused by` 是 `No fallback instance of type UserFallbackService` | Spring 启动失败先看**最后一个** `Caused by` |
| `No fallback instance ... found for feign client` | `UserFallbackService` 忘了加 `@Component`，不是 Bean | fallback 类必须是 Spring Bean |
| 接口 500，堆栈里同一行重复几百次 | Service 实现类注入了自己（`@Autowired UserService` 在 `UserServiceImpl` 里），自己调自己 | 实现类只能调下一层（DAO） |
| 启动了但一调接口就 500 | 连接池是懒加载，启动时没连库；`DB_PASSWORD` 未设置导致连接一直等待 | 在哪启动服务就在哪个窗口设环境变量 |

---

## 说明

- 包名沿用 `com.youkeda`，未做重命名，以免破坏 MyBatis 映射与 Spring 扫描路径。
- 本项目为学习用途的微服务练习，重点在架构演进过程而非业务复杂度。
