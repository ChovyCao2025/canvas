# Build stage compiles the Vite SPA with Node 20, matching the Vite runtime requirement.
FROM node:20-alpine AS builder
WORKDIR /build

# Install from lockfile first so dependency layers are stable across source-only changes.
COPY package*.json ./
RUN npm ci

COPY . .
ARG VITE_API_BASE_URL=/
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

# Runtime stage serves static assets through unprivileged nginx.
FROM nginxinc/nginx-unprivileged:1.27-alpine AS runtime
# Default upstream points at the in-cluster backend service and can be overridden by Helm.
ENV CANVAS_API_UPSTREAM=http://canvas-engine:8080

COPY --from=builder /build/dist /usr/share/nginx/html
# nginx.conf is used as a template so CANVAS_API_UPSTREAM is substituted at container start.
COPY nginx.conf /etc/nginx/templates/default.conf.template

EXPOSE 8080
