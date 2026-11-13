# =====================================================================
# oj-web 多阶段构建（论文 5.8）：构建产物装入 Nginx 镜像
# 构建上下文为项目根目录（compose 中已配置）
# =====================================================================
FROM node:18-alpine AS build
WORKDIR /app

COPY oj-web/package.json ./
RUN npm install

COPY oj-web/ ./
RUN npm run build

FROM nginx:1.25-alpine
COPY deploy/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
