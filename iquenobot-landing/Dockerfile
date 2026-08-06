# -- Build stage
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .

ARG VITE_CRM_LOGIN_URL=https://iquenobot.onrender.com/login
ENV VITE_CRM_LOGIN_URL=$VITE_CRM_LOGIN_URL

RUN npm run build

# -- Runtime stage
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s \
    CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1
