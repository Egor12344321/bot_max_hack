FROM node:22-bookworm-slim AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ARG VITE_API_MODE=real
ARG VITE_API_URL=/v1
ARG VITE_MAX_MODE=real

ENV VITE_API_MODE=$VITE_API_MODE
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_MAX_MODE=$VITE_MAX_MODE

RUN npm run build

FROM nginx:stable-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
