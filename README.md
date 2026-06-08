# Qin Dongyun Portfolio

个人作品介绍网页，基于简历内容整理。静态页面无需构建工具，可直接部署到 GitHub Pages、Netlify 或 Vercel。

仓库包含一个可选的 AI 简历助手：后端使用 Spring Boot、LangChain4j 和智谱 AI OpenAI 兼容接口，前端通过 SSE 接收流式回答。

## 本地预览

只预览静态页面：

```bash
python3 -m http.server 4173
```

浏览器访问 `http://localhost:4173`。

AI 助手的本地启动方式见 [部署说明](docs/DEPLOYMENT.md)。

## GitHub Pages

仓库推送后，在 GitHub 仓库中打开 `Settings > Pages`，将部署来源设置为 `Deploy from a branch`，选择 `main` 分支和 `/ (root)` 目录。

GitHub Pages 只能托管静态页面，不能运行 Spring Boot 服务。生产环境中的 AI API 设计为单独部署到阿里云 ECS。

## 开发文档

- [AI 简历助手开发文档](docs/AI_RESUME_ASSISTANT_DEVELOPMENT.md)
- [部署说明](docs/DEPLOYMENT.md)
