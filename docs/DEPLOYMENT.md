# 阿里云 ECS 部署说明

## 1. 部署结构

本项目拆成两个部分：

| 部分 | 部署位置 | 说明 |
| --- | --- | --- |
| 静态作品集 | GitHub Pages | 托管 HTML、CSS 和 JavaScript |
| Spring Boot API | 阿里云 ECS | Docker 运行后端，通过 Nginx 提供 HTTPS API |

```text
GitHub Pages 静态页面
        |
        | HTTPS
        v
https://api.example.com
        |
        v
阿里云 ECS 安全组：80 / 443
        |
        v
Nginx：TLS 与反向代理
        |
        v
127.0.0.1:8080
        |
        v
Docker：portfolio-ai-assistant
        |
        v
智谱 AI
```

生产环境不将 Spring Boot 的 `8080` 端口暴露到公网。

## 2. 准备资源

### 2.1 ECS

个人作品集初期可从以下配置开始：

```text
中国内地地域：北京、上海或杭州等靠近主要访客的地域
操作系统：Alibaba Cloud Linux 3 / 4 或 Ubuntu LTS
实例规格：2 vCPU、2 GiB 内存起步
系统盘：40 GiB ESSD 起步
网络：分配公网 IPv4
```

### 2.2 域名与备案

准备一个 API 子域名：

```text
api.example.com
```

将 DNS `A` 记录解析到 ECS 公网 IP。

如果 ECS 位于中国内地，域名对外提供服务前需要完成 ICP 备案。备案成功并开通网站后，还需要按要求办理公安联网备案。

### 2.3 HTTPS 证书

申请 `api.example.com` 的 TLS 证书，并将证书文件部署到 ECS：

```text
/etc/nginx/ssl/api.example.com.pem
/etc/nginx/ssl/api.example.com.key
```

GitHub Pages 使用 HTTPS，因此浏览器侧 API 也必须使用 HTTPS。

## 3. 配置安全组

| 端口 | 来源 | 用途 |
| --- | --- | --- |
| `22/tcp` | 仅管理员固定 IP | SSH 运维 |
| `80/tcp` | `0.0.0.0/0` | HTTP 跳转 HTTPS，证书验证 |
| `443/tcp` | `0.0.0.0/0` | 对外 HTTPS API |
| `8080/tcp` | 不开放 | Spring Boot 仅供本机 Nginx 访问 |

## 4. 安装运行环境

登录 ECS 后安装：

```text
Git
Docker
Nginx
```

Docker 安装方式根据 ECS 操作系统选择，使用阿里云官方 Docker 安装文档中的对应命令。

## 5. 构建后端镜像

将代码部署到 ECS，例如：

```text
/opt/portfolio-site
```

进入仓库后构建镜像：

```bash
cd /opt/portfolio-site/server
docker build -t portfolio-ai-assistant:0.1.0 .
```

## 6. 配置环境变量

在 ECS 中创建：

```text
/opt/portfolio-ai-assistant/.env
```

内容：

```text
ZAI_API_KEY=你的智谱 API Key
ZAI_BASE_URL=https://open.bigmodel.cn/api/paas/v4/
ZAI_MODEL=上线前确认的可用模型
PORTFOLIO_ALLOWED_ORIGIN=https://你的 GitHub 用户名.github.io
```

如果 GitHub Pages 地址包含仓库路径，例如：

```text
https://你的 GitHub 用户名.github.io/portfolio-site/
```

`PORTFOLIO_ALLOWED_ORIGIN` 仍然只填写协议和域名，不包含 `/portfolio-site/`。

限制环境变量文件权限：

```bash
chmod 600 /opt/portfolio-ai-assistant/.env
```

禁止将 `.env` 和智谱 API Key 提交到 Git。

## 7. 运行后端容器

```bash
docker run -d \
  --name portfolio-ai-assistant \
  --restart unless-stopped \
  --env-file /opt/portfolio-ai-assistant/.env \
  -p 127.0.0.1:8080:8080 \
  portfolio-ai-assistant:0.1.0
```

先在 ECS 本机验证：

```bash
curl http://127.0.0.1:8080/actuator/health
```

预期响应：

```json
{
  "status": "UP"
}
```

## 8. 配置 Nginx

创建：

```text
/etc/nginx/conf.d/portfolio-ai-assistant.conf
```

内容：

```nginx
server {
    listen 80;
    server_name api.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.example.com;

    ssl_certificate /etc/nginx/ssl/api.example.com.pem;
    ssl_certificate_key /etc/nginx/ssl/api.example.com.key;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 75s;
    }

    location /api/v1/chat/stream {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_buffering off;
        proxy_cache off;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 75s;
    }
}
```

验证并加载配置：

```bash
nginx -t
systemctl reload nginx
```

公网验证：

```bash
curl https://api.example.com/actuator/health
```

## 9. 配置 GitHub Pages 前端

修改仓库根目录中的 `config.js`：

```js
window.PORTFOLIO_API_BASE_URL =
  window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1"
    ? "http://localhost:8080"
    : "https://api.example.com";
```

提交并推送代码后，在 GitHub 仓库中打开 `Settings > Pages`：

```text
Source: Deploy from a branch
Branch: main
Directory: / (root)
```

发布完成后，在 GitHub Pages 页面打开 AI 简历助手并发送问题。

## 10. CI/CD 设计

### 10.1 目标

第一版 CI/CD 以“简单、可回滚、业务密钥不出服务器”为原则：

- 静态作品集继续使用 GitHub Pages 自动发布。
- 后端在 GitHub Actions 中完成测试，测试通过后再允许发布。
- 生产环境镜像在 ECS 本机构建，避免将镜像仓库和云厂商权限过早引入。
- 智谱 API Key、模型配置和 CORS 白名单仍然保存在 ECS 的 `.env` 文件中，不进入 GitHub Secrets、镜像或任何构建产物。
- 每次发布使用 Git commit SHA 作为镜像版本，保留最近两个可用版本，异常时快速回滚。

### 10.2 流水线分层

| 层级 | 触发方式 | 负责内容 |
| --- | --- | --- |
| Frontend CD | 推送到 `main` | GitHub Pages 发布根目录静态文件 |
| Server CI | `server/**` 或 workflow 变更 | 使用 Java 17 执行 `mvn --batch-mode verify` |
| Server CD | 手动触发或推送 tag | SSH 到 ECS，拉取代码，构建 Docker 镜像并重启容器 |

当前仓库已有 `.github/workflows/server-ci.yml`，用于后端测试。下一步新增 CD workflow 时，不替换该 CI，而是在 CI 通过后追加一个独立的部署流程。

### 10.3 GitHub Pages 发布

静态页面没有构建步骤，保持 GitHub Pages 的分支发布模式即可：

```text
Source: Deploy from a branch
Branch: main
Directory: / (root)
```

`main` 分支变更会自动发布前端。生产环境上线前需要确认 `config.js` 中的 API 地址已经指向：

```text
https://api.example.com
```

### 10.4 后端 CI

后端 CI 触发范围：

```text
server/**
.github/workflows/server-ci.yml
```

执行步骤：

1. 检出代码。
2. 配置 Java 17。
3. 缓存 Maven 依赖。
4. 在 `server` 目录执行：

```bash
mvn --batch-mode verify
```

只有 CI 通过的 commit 才允许部署到 ECS。

### 10.5 后端 CD

推荐先采用手动触发的 GitHub Actions CD：

```text
workflow_dispatch
```

等发布节奏稳定后，再增加 tag 触发：

```text
push tags: v*
```

CD 执行步骤：

1. GitHub Actions 通过 SSH 登录 ECS。
2. 进入 `/opt/portfolio-site`。
3. 拉取指定 commit。
4. 在 `server` 目录构建镜像：

```bash
docker build -t portfolio-ai-assistant:${GITHUB_SHA} .
```

5. 停止并移除旧容器。
6. 使用 ECS 本机的 `.env` 启动新容器：

```bash
docker run -d \
  --name portfolio-ai-assistant \
  --restart unless-stopped \
  --env-file /opt/portfolio-ai-assistant/.env \
  -p 127.0.0.1:8080:8080 \
  portfolio-ai-assistant:${GITHUB_SHA}
```

7. 在 ECS 本机验证：

```bash
curl --fail http://127.0.0.1:8080/actuator/health
```

8. 通过公网域名验证：

```bash
curl --fail https://api.example.com/actuator/health
```

如果健康检查失败，CD 应立即回滚到上一个镜像版本，并让 workflow 失败。

### 10.6 GitHub Secrets

GitHub Actions CD 只需要 SSH 发布权限，不需要智谱 API Key。

建议配置以下 Secrets：

| Secret | 说明 |
| --- | --- |
| `ECS_HOST` | ECS 公网 IP 或可解析域名 |
| `ECS_USER` | 专用于发布的 Linux 用户 |
| `ECS_SSH_PRIVATE_KEY` | 发布用户的 SSH 私钥 |
| `ECS_SSH_PORT` | SSH 端口，默认可为 `22` |

ECS 上建议创建独立发布用户，并只授予必要权限：

- 读取 `/opt/portfolio-site`。
- 执行 Docker 构建和容器重启。
- 读取 `/opt/portfolio-ai-assistant/.env`。

不要把 `ZAI_API_KEY` 放入 GitHub Actions 日志、镜像层或仓库文件。

### 10.7 ECS 发布脚本

为了让 GitHub Actions 的 SSH 命令保持短小，建议在 ECS 保存一个发布脚本：

```text
/opt/portfolio-ai-assistant/deploy.sh
```

脚本职责：

1. 接收 commit SHA 或 tag 作为版本号。
2. 拉取代码并切换到指定版本。
3. 构建新 Docker 镜像。
4. 启动新容器。
5. 执行本机健康检查。
6. 健康检查失败时恢复上一个镜像。
7. 清理旧镜像，只保留最近两个可用版本。

这样 GitHub Actions 只需要执行：

```bash
/opt/portfolio-ai-assistant/deploy.sh "${GITHUB_SHA}"
```

### 10.8 回滚策略

第一版采用单机快速回滚：

- 每次发布前记录当前正在运行的镜像 tag。
- 新容器启动失败或健康检查失败时，重新启动上一版镜像。
- 发布成功后，保留当前版本和上一版本。
- 如果公网健康检查失败但本机健康检查通过，优先检查 Nginx、证书、DNS 和安全组。

手动回滚命令示例：

```bash
docker stop portfolio-ai-assistant
docker rm portfolio-ai-assistant
docker run -d \
  --name portfolio-ai-assistant \
  --restart unless-stopped \
  --env-file /opt/portfolio-ai-assistant/.env \
  -p 127.0.0.1:8080:8080 \
  portfolio-ai-assistant:上一版镜像tag
```

### 10.9 后续演进

当访问量或发布频率上来后，再考虑：

- 将镜像推送到阿里云容器镜像服务 ACR。
- 使用蓝绿发布或双容器端口切换，减少重启窗口。
- 将 Nginx upstream 切换做成原子操作。
- 接入阿里云云监控、日志服务和告警。
- 增加 smoke test，覆盖聊天接口的非流式或模拟模型调用路径。

## 11. 更新与回滚

发布新版本：

```bash
cd /opt/portfolio-site
git pull
cd server
docker build -t portfolio-ai-assistant:0.2.0 .
docker stop portfolio-ai-assistant
docker rm portfolio-ai-assistant
docker run -d \
  --name portfolio-ai-assistant \
  --restart unless-stopped \
  --env-file /opt/portfolio-ai-assistant/.env \
  -p 127.0.0.1:8080:8080 \
  portfolio-ai-assistant:0.2.0
curl http://127.0.0.1:8080/actuator/health
```

出现异常时，使用同样的运行命令重新启动上一个镜像版本：

```text
portfolio-ai-assistant:0.1.0
```

第一版保留最近两个可用镜像即可。接入 CI/CD 后，这组命令可以收敛到 ECS 上的 `deploy.sh`。

## 12. 本地运行

启动后端：

```bash
cd server
export ZAI_API_KEY=你的智谱 API Key
export PORTFOLIO_ALLOWED_ORIGIN=http://localhost:4173
/Users/qin/apache-maven-3.8.9/bin/mvn spring-boot:run
```

另开终端启动静态页面：

```bash
python3 -m http.server 4173
```

浏览器访问：

```text
http://localhost:4173
```

## 13. 上线检查

- ECS 安全组未向公网开放 `8080`。
- SSH `22` 端口仅允许管理员 IP。
- 域名、ICP备案和 HTTPS 证书已经准备完成。
- `.env` 权限为 `600`，且未提交到 Git。
- GitHub Actions 只保存 SSH 发布权限，不保存智谱 API Key。
- 后端 CI 已通过 `mvn --batch-mode verify`。
- Nginx 已关闭 SSE 路径的代理缓冲。
- `/actuator/health` 可通过 HTTPS 访问。
- GitHub Pages 的 `config.js` 指向正式 API 域名。
- 浏览器请求中不存在智谱 API Key。
- 智谱控制台已经设置预算告警。

## 14. 参考文档

- [阿里云 ECS 安全组规则](https://help.aliyun.com/zh/ecs/user-guide/add-a-security-group-rule)
- [阿里云 ICP 备案流程](https://help.aliyun.com/zh/icp-filing/basic-icp-service/user-guide/icp-filing-process)
- [阿里云公安联网备案说明](https://help.aliyun.com/zh/icp-filing/basic-icp-service/support/what-is-a-public-security-network-record)
- [在阿里云 ECS 中安装 Docker](https://help.aliyun.com/zh/ecs/use-cases/install-and-use-docker)
- [在 Nginx 服务器中安装证书](https://help.aliyun.com/zh/ssl-certificate/user-guide/install-an-ssl-certificate-on-a-nginx-server)
