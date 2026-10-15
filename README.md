# comment-service 评论服务

一个基于 Spring Boot + Spring Cloud Alibaba 的微服务示例项目，实现了一套完整的**评论（Comment）服务**：支持分页查询、发表、批量新增、修改、删除，以及按业务 ID / 用户 ID 检索。服务通过 Nacos 完成注册与配置，数据层使用 MyBatis + PageHelper 操作 MySQL。

## 模块结构

```
j9-7-5-1
├── pom.xml                                     # 聚合 POM，统一依赖版本管理
├── application                                 # 启动模块（Spring Boot 入口）
└── shared.all                                  # 共享模块聚合
    └── comment.shared                          # 评论共享模块聚合
        ├── comment.api                         # 对外 HTTP 接口层
        ├── comment.service                     # 服务接口 + 领域模型
        └── comment.service.impl                # 服务实现 + DAO + MyBatis 映射
```

| 模块 | 职责 | 关键类 |
|---|---|---|
| `application` | 应用启动、Spring 扫描配置 | `com.youkeda.exercise.Application` |
| `comment.api` | 暴露 REST 接口 | `CommentController`、`CommentAPI` |
| `comment.service` | 定义服务契约与模型 | `CommentService`、`Comment`、`Paging`、`Result` |
| `comment.service.impl` | 业务实现、数据访问、DO 转换 | `CommentServiceImpl`、`CommentDAO`、`CommentDO`、`CommentDOUtil` |

## 技术栈

| 组件 | 版本 |
|---|---|
| Java | 11 |
| Spring Boot | 2.3.0.RELEASE |
| Spring Cloud Alibaba | 2.2.6.RELEASE |
| MyBatis Spring Boot Starter | 2.1.3 |
| PageHelper | 1.2.13 |
| Druid | 1.1.23 |
| MySQL Connector | 随 Spring Boot 父 POM 管理 |
| Nacos | 注册中心 + 配置中心 |

## 接口一览

服务默认监听 `8081` 端口。

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/comments?pageNum=&pageSize=` | 分页查询评论，默认第 1 页、每页 15 条 |
| `POST` | `/comment` | 新增单条评论 |
| `POST` | `/comment/batchAdd` | 批量新增评论 |
| `POST` | `/comment/update` | 更新评论 |
| `GET` | `/comment/del?id=` | 按主键删除评论 |
| `GET` | `/comment/findByRefId?refId=` | 按业务引用 ID 查询评论 |
| `GET` | `/comment/findUserIds?userIds=` | 按用户 ID 集合查询评论 |

## 配置说明

为保证仓库中不出现任何真实凭证，所有敏感配置均通过**环境变量**注入，配置文件内只保留本地开发用的默认值：

| 环境变量 | 说明 | 默认值 |
|---|---|---|
| `DB_URL` | MySQL 连接串 | `jdbc:mysql://localhost:3306/comment?serverTimezone=GMT%2B8` |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | 空 |
| `NACOS_ADDR` | Nacos 地址（注册 + 配置中心） | `127.0.0.1:8848` |
| `DRUID_USERNAME` | Druid 监控页用户名 | `admin` |
| `DRUID_PASSWORD` | Druid 监控页密码 | 空 |

启动前请按需设置，例如：

```bash
export DB_URL="jdbc:mysql://127.0.0.1:3306/comment?serverTimezone=GMT%2B8"
export DB_USERNAME="root"
export DB_PASSWORD="your-password"
export NACOS_ADDR="127.0.0.1:8848"
```

## 构建与运行

```bash
# 编译
mvn clean compile

# 打包
mvn clean package

# 启动
mvn -pl application spring-boot:run
```

## 数据库表结构

服务依赖 `comment` 库中的评论主表，核心字段如下：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT | 主键，自增 |
| `ref_id` | VARCHAR | 业务引用 ID |
| `user_id` | BIGINT | 评论者用户 ID |
| `content` | VARCHAR | 评论内容 |
| `parent_id` | BIGINT | 父评论 ID，用于盖楼 |
| `gmt_created` | DATETIME | 创建时间 |
| `gmt_modified` | DATETIME | 更新时间 |

> 注：`Comment` 模型中的 `author` 字段由 `UserService` 提供，依赖外部的 `user-spring-boot-starter` 构件。

## 说明

- 项目中的 `com.youkeda` 包名沿用作历史结构，未做重命名，以免破坏 MyBatis 映射与 Spring 扫描路径。
- 本仓库为学习用途的微服务练习项目。
