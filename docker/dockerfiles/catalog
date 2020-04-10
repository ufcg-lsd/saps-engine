FROM postgres:10

LABEL maintainer="wesley.monte@ccc.ufcg.edu.br"

ENV POSTGRES_USER=admin
ENV POSTGRES_PASSWORD=admin
ENV POSTGRES_DB=saps

COPY ./docker/context/catalog/init-catalog.sh /docker-entrypoint-initdb.d/init-catalog.sh

EXPOSE 5432