# 社区医疗诊断管理系统

专科毕设 —— 社区医疗诊断管理系统

一个基于 **Spring Boot + Vue2** 前后端分离架构的社区医疗诊断管理平台，围绕社区诊所日常诊疗场景，覆盖患者档案、就诊病历、用药记录、费用账单等核心业务，提供基于 RBAC 的三角色权限控制（管理员/医生/患者）、JWT 认证、图形验证码与实时消息推送，并支持 MySQL / SQLite 双数据源自动切换。

## 一、项目简介

社区诊所就诊人数多、流转快，患者档案、病历书写、用药登记与收费记账若依赖纸质台账，既易丢失又难以追溯。本系统面向诊所管理员、医生与患者三类用户，提供一体化的线上管理能力，实现"患者可建档、病历可追溯、用药可查询、账单可核对外加权限可配置"。

## 二、技术栈

| 端 | 技术 |
| --- | --- |
| 后端框架 | Spring Boot 2.6.13（单模块 Maven 工程） |
| 持久层 | MyBatis-Plus 3.5.5 + Druid 连接池 |
| 数据库 | MySQL 5.7+（主数据源）/ SQLite 3.45（备用数据源，自动降级） |
| 缓存 | Redis（验证码存取等） |
| 安全认证 | Spring Security + JWT（JJWT 0.9.1）+ Kaptcha 验证码 + RSA 密码加密 |
| 实时通信 | WebSocket（消息实时推送） |
| 接口文档 | Swagger 2.9.2 |
| 工具库 | Hutool 5.3.3、Commons-Lang3、Commons-IO、Commons-Codec |
| 前端框架 | Vue 2.6 + Vue CLI 5 |
| UI / 样式 | Element UI 2.15 |
| 路由 | Vue Router 3 |
| HTTP | Axios（统一封装 token 注入） |
| 加密 | JSEncrypt（登录密码 RSA 前端加密） |
| 截图导出 | html2canvas（病历/账单页面截图导出） |

## 三、功能模块

| 模块 | 主要功能 |
| --- | --- |
| 登录注册 | 图形验证码校验、RSA 前端密码加密、JWT 登录态保持（7 天） |
| 患者管理（Patient） | 医生端患者档案列表、新增建档、信息维护 |
| 就诊管理（MedicalRecord） | 病历登记与查看：主诉、现病史、既往史、家族史、体格检查、诊断、治疗方案、就诊/出院日期 |
| 用药管理（MedicationRecord） | 患者用药记录登记与查询 |
| 费用管理（BillingRecord） | 诊疗费用账单登记、查询与核对，支持 html2canvas 截图导出 |
| 用户与角色（User/Role） | 管理员管理用户、分配角色；角色-权限（Authority）灵活配置 |
| 患者视图 | 患者登录后仅可查看本人档案及关联诊疗、用药、账单记录 |
| 实时通知（WebSocket） | 后端事件（如建档、挂号）通过 WebSocket 实时推送前端 |

权限采用 RBAC 模型（用户-角色-权限三级），前端按角色动态渲染 Tab 页签（`hasDoctorRole` / `hasAdminRole` / `hasPatientRole`），后端接口双重校验。

## 四、项目结构

```
社区医疗诊断管理系统/
├── UserLogin/                             # Spring Boot 后端工程
│   ├── data/
│   │   └── community_diagnosis_db.db      # SQLite 数据库文件（首次启动自动生成）
│   ├── src/main/java/com/example/userlogin/
│   │   ├── config/                        # 动态数据源/Security/Redis/Swagger/WebSocket/Kaptcha 配置
│   │   ├── controller/                    # 8 个控制器（用户/角色/患者/病历/用药/账单/验证码/WS推送）
│   │   ├── filter/                        # CaptchaFilter、CustomAuthenticationFilter、JwtAuthenticationFilter
│   │   ├── handler/                       # JWT 异常处理、登出成功处理、密码编码器
│   │   ├── mapper/                        # 9 个 MyBatis-Plus Mapper
│   │   ├── model/                         # 实体类 + Result 统一响应封装
│   │   ├── service/                       # 业务接口 + Impl 实现（业务逻辑与事务）
│   │   └── utils/                         # JwtUtils、RSAUtils、RedisUtil、DatabaseInitializer
│   └── src/main/resources/
│       ├── application.yaml               # 端口 8082、双数据源、Redis、JWT 配置
│       └── sql/                           # MySQL / SQLite 建表脚本
│           ├── schema-tables-mysql.sql
│           └── schema-tables-sqlite.sql
└── vue123/                                # Vue2 前端工程
    ├── src/
    │   ├── views/                         # Login / Register / user（主业务页，1900+ 行）
    │   ├── utils/api.js                   # axios 统一封装（baseUrl 指向后端 8082）
    │   ├── router/                        # 路由与登录守卫
    │   └── App.vue / main.js
    └── vue.config.js                      # devServer 端口 8081、host 0.0.0.0（局域网可访问）
```

后端分层规范：Controller 只承担 HTTP 职责，业务逻辑与事务（`@Transactional`）位于 ServiceImpl，Mapper 仅做持久化。

## 五、环境要求

| 依赖 | 版本要求 |
| --- | --- |
| JDK | 1.8（高版本 JDK 与 Lombok 可能不兼容） |
| Maven | 3.6+ |
| MySQL | 5.7 及以上（可选，不配置则自动降级 SQLite） |
| Redis | 任意稳定版 |
| Node.js | 14+ |

## 六、快速开始

### 1. 启动后端（默认 SQLite，零配置开箱即用）

系统内置 `DatabaseInitializer`，首次启动自动建库建表，无需手工导入 SQL：

```bash
cd UserLogin
mvn spring-boot:run
```

后端启动成功后监听 **8082** 端口，Swagger 文档地址：`http://localhost:8082/swagger-ui.html`

如需切换 MySQL 主数据源，编辑 `UserLogin/src/main/resources/application.yaml`，修改 `database.init` 下的建库账户与密码：

```yaml
database:
  init:
    username: root
    password: <你的数据库密码>
```

### 2. 启动 Redis

验证码校验依赖 Redis 存取，请先启动本机 Redis 服务（默认 6379）。

### 3. 启动前端

在 `vue123` 目录下执行：

```bash
npm install
npm run serve
```

开发服务器运行在 **8081** 端口，浏览器访问：

```
http://localhost:8081
```

> 前端通过 `http://${window.location.hostname}:8082` 直连后端，跨域由后端 `WebMvcConfig` 统一放行，无需额外配置。`vue.config.js` 中 `host: '0.0.0.0'` 允许局域网内其他设备访问，适合演示演示场景。

### 4. 生产构建

```bash
cd vue123
npm run build   # 产物输出至 dist/，可由 Nginx 托管并将 /api 反向代理至后端 8082
```

## 七、技术亮点

1. **前后端分离**：统一 `Result` 响应格式，前端 axios 层集中封装 token 注入与登录态跳转；
2. **双数据源架构**：MySQL 为主、SQLite 为备，`DynamicDataSourceConfig` + `DatabaseInitializer` 实现"无环境也能跑"，首次启动自动建库建表；
3. **安全体系**：Spring Security 过滤器链 + JWT 无状态认证 + Kaptcha 图形验证码 + RSA 前后端密码加密；
4. **实时推送**：基于 WebSocket 的消息实时下发，无需轮询刷新；
5. **RBAC 权限**：用户-角色-权限三级模型，管理员可在"角色管理"页动态配置角色权限，前端按角色渲染功能页签；
6. **贴心细节**：html2canvas 支持病历/账单页面一键截图导出，方便患者留存纸质凭证。

## 八、常见问题

| 问题 | 解决方案 |
| --- | --- |
| 后端启动报 Redis 连接失败 | 先启动本机 Redis 服务（默认 6379） |
| Lombok 编译报错 | 确认使用 JDK 8，并安装 IDE 的 Lombok 插件 |
| 登录页验证码不显示 | 检查后端 8082 是否启动、Redis 是否可写（验证码存于 Redis） |
| 局域网设备无法访问前端 | 检查 Windows 防火墙是否放行 8081 端口（专用网络入站规则） |
| 想切换 MySQL 但连不上 | 确认 MySQL 已启动，且 `application.yaml` 中 `database.init.username/password` 具备建库权限 |
