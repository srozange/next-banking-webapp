# syntax=docker/dockerfile:1
# Conteneur web : SPA construite sur Node, servie par nginx ; contexte de construction : racine du dépôt.
FROM node:24.21.0-alpine AS construction
WORKDIR /construction
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --silent
COPY frontend/ ./
RUN npm run build

FROM nginx:1.30.5-alpine
ENV TZ=Europe/Paris
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=construction /construction/dist /usr/share/nginx/html
EXPOSE 8080
