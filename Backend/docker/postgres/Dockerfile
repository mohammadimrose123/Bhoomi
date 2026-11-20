FROM postgis/postgis:18-3.6-alpine

COPY --chmod=755 docker/postgres/init/01-create-app-user.sh /docker-entrypoint-initdb.d/01-create-app-user.sh

RUN sed -i 's/\r$//' /docker-entrypoint-initdb.d/01-create-app-user.sh
