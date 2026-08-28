FROM node:24-alpine AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG VITE_API_BASE_URL=/api/v1
ARG VITE_AMAP_JS_KEY=
ARG AMAP_H5_SECURITY_JS_CODE=
ARG AMAP_H5_SERVICE_HOST=
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_AMAP_JS_KEY=$VITE_AMAP_JS_KEY
ENV AMAP_H5_SECURITY_JS_CODE=$AMAP_H5_SECURITY_JS_CODE
ENV AMAP_H5_SERVICE_HOST=$AMAP_H5_SERVICE_HOST
RUN npm run build:h5

FROM nginx:1.30-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/build/h5 /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 CMD wget -q -O /dev/null http://127.0.0.1/healthz || exit 1
