FROM node:20-alpine AS builder
WORKDIR /build

COPY package*.json ./
RUN npm ci

COPY . .
ARG VITE_API_BASE_URL=/
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine AS runtime
ENV CANVAS_API_UPSTREAM=http://canvas-engine:8080

COPY --from=builder /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/templates/default.conf.template

EXPOSE 8080
