FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./

RUN npm ci

COPY . .

RUN npm run build -- --configuration=docker

FROM nginx:alpine
WORKDIR /usr/share/nginx/html

RUN rm -rf ./*

COPY --from=build /app/dist/web-ui /usr/share/nginx/html

RUN echo 'server { \
  listen 80; \
  server_name localhost; \
  root /usr/share/nginx/html; \
  index index.html; \
  \
  location / { \
  try_files $uri $uri/ /index.html; \
  } \
  \
  location /api { \
  proxy_pass http://backend:8090; \
  proxy_set_header Host $host; \
  proxy_set_header X-Real-IP $remote_addr; \
  proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; \
  proxy_set_header X-Forwarded-Proto $scheme; \
  } \
  }' > /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
