FROM nodered/node-red:latest

USER root
RUN npm install --omit=dev node-red-contrib-modbus@~5.45.2 mysql2@~3.11.0
USER node-red
