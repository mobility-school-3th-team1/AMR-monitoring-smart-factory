FROM node:20-alpine AS build

WORKDIR /app

ARG VITE_MQTT_URL=ws://localhost:1883
ENV VITE_MQTT_URL=$VITE_MQTT_URL

COPY package*.json ./
RUN npm install

COPY . ./
RUN npm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]