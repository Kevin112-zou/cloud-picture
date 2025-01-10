

<p align="center">
    <a href="" target="_blank">
      <img src="https://s21.ax1x.com/2025/01/09/pECWXWR.png" width="280px"/>
    </a>
</p>



<h1 align="center">SHARE-云图库</h1>
<p align="center"><strong>一个免费分享壁纸、表情包、海报等素材的网站。<br>支持从bing图库导入海量图片~支持AI生成精美图片~<em>持续更新 ing～</em></strong></p>

<div align="center">
    <a href="https://github.com/Kevin112-zou/cloud-picture"><img src="https://img.shields.io/badge/github-项目地址-yellow.svg?style=plasticr"></a>
    <a href="https://github.com/Kevin112-zou/cloud-picture"><img src="https://img.shields.io/badge/码云-项目地址-orange.svg?style=plasticr"></a>
    <a href="https://github.com/Kevin112-zou/cloud-picture-web"><img src="https://img.shields.io/badge/前端-项目地址-blueviolet.svg?style=plasticr"></a>
</div>




## 项目导航

- **快速体验地址：**[SHARE-云图库]()

## 项目介绍

SHARE-云图库是素材网站，基于vue3 + spring boot + COS对象存储 + websocket 的 **企业级在线共享图库平台**。

平台总共分为了**公共图库**、**私有图库**、**共享图库**三大模块。

- 所有用户可以在平台上公开上传图片素材，并且支持按图片的名称、类型以及标签检索你想要的素材。

- 管理员支持上传、批量抓取、审核和管理图片，并对系统内的图片进行分析。

- 对于个人用户，可将图片上传至私有空间进行批量管理、检索、编辑和分析，用作个人网盘、个人相册、作品集等。

- 对于企业，可开通团队空间并邀请成员，共享图片并 **实时协同编辑图片**，提高团队协作效率。可用于提供商业服务，如企业活动相册、企业内部素材库等：

该项目功能丰富，涉及文件存管、内容检索、权限控制、实时协同等企业主流业务场景，并运用多种编程思想、架构设计方法和优化策略来保证项目的高速迭代和稳定运行。


### 项目展示


1）第一阶段，开发公共的图库平台。

> 成果：可用作表情包网站、设计素材网站、壁纸网站等

![](https://s21.ax1x.com/2025/01/10/pECbPTx.png)

![](https://s21.ax1x.com/2025/01/10/pECbFk6.png)

![](https://s21.ax1x.com/2025/01/10/pECbktK.png)

![](https://s21.ax1x.com/2025/01/10/pECbAfO.png)

2）第二阶段，对项目 C 端功能进行大量扩展。用户可开通私有空间，并对空间图片进行多维检索、扫码分享、批量管理、快速编辑、用量分析。

> 成果：可用作个人网盘、个人相册、作品集等

3）第三阶段，对项目 B 端功能进行大量扩展。企业可开通团队空间，邀请和管理空间成员，团队内共享图片并实时协同编辑图片。

> 成果：可用于提供商业服务，如企业活动相册、企业内部素材库等

项目架构设计图：


<img src="https://pic.yupi.icu/1/1732691889100-e562c709-cffa-477d-9329-1dc5ac1d35c8-20241204144304741-20241204145344935-20241204145354234.png" style="zoom:50%;" />



## 环境搭建

如需使用对象存储COS服务，需先开通，详情请参考 [腾讯云官方文档](https://cloud.tencent.com/document/product/436/10199)

在项目目录下的`application.yml`中填写自己的环境配置。
1. 数据库配置
```yml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/your_database_name
    username: your_username
    password: your_password
```
2. Redis配置
```yml
spring:
  redis:
    database: 0
    host: your_redis_host
    port: 6379
    password: your_redis_password 
    timeout: 5000
```
3. 对象存储COS配置
```yml
# 对象存储配置（需要从腾讯云获取）
cos:
  client:
    host: xxx  # COS 域名
    secretId: xxx #  开通对象存储的ID
    secretKey: xxx # 开通对象存储的秘钥
    region: xxx # 地域，例如 ap-beijing
    bucket: 1234567890 # 桶名
```
## 技术选型

#### 后端

|         技术         | 说明                      | 官网                                                 |
| :------------------: | ------------------------- | ---------------------------------------------------- |
|      SpringBoot      | java必备框架              | https://spring.io/projects/spring-boot               |
|        MySql         | 关系型数据库              | https://dev.mysql.com/doc/                           |
| MyBatisPlus+MyBatisX | 自带分页插件，自动生成sql | https://baomidou.com/                                |
|        Redis         | 分布式缓存，提供高效性    | https://redis.io                                     |
|       Caffeine       | 本地缓存                  | http://caffe.berkeleyvision.org/                     |
|       Sa-Token       | 轻量级登录认证，权限认证  | https://sa-token.cc/v/v1.11.0/doc/index.html#/       |
|        Jsoup         | 数据抓取                  | https://jsoup.org/                                   |
|         COS          | 腾讯云对象存储            | https://cloud.tencent.com/document/product/436/10199 |
|      Disruptor       | 高性能无锁队列            | https://github.com/LMAX-Exchange/disruptor           |
|        Lombok        | 简化代码                  | https://projectlombok.org                            |
|        Hutool        | Java工具类库              | https://github.com/looly/hutool                      |
|       Knife4j        | API文档生成工具           | https://doc.xiaominfo.com/docs/quick-start           |


#### 前端

|      技术      |           说明            |                        官网                         |
| :------------: | :-----------------------: | :-------------------------------------------------: |
|      vue3      |     前端流行开发框架      |    [https://cn.vuejs.org](https://cn.vuejs.org/)    |
|     Pinia      |     全局状态管理框架      | [https://pinia.vuejs.org](https://pinia.vuejs.org/) |
| Ant Design Vue |     轻松上手的组件库      |    https://www.antdv.com/components/overview-cn/    |
|      Vite      |       前端打包工具        |   [https://cn.vitejs.dev](https://cn.vitejs.dev/)   |
|     Axios      | 基于Promise 的HTTP 请求库 |          https://axios-http.com/docs/intro          |
|   TypeScript   |    让 JS 具备类型声明     |           https://www.typescriptlang.org/           |
|     ESLint     |       前端代码修复        |                 https://eslint.org/                 |
|    Prettier    |      前端代码格式化       |                https://prettier.io/                 |
|    OpenAPI     |      自动代码生成器       |  https://github.com/OpenAPITools/openapi-generator  |


