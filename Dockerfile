FROM openjdk:8-jre-alpine

RUN mkdir /red
WORKDIR /red

COPY ./target/scala-2.12/theia.jar theia.jar

ADD theia.sv.conf /etc/supervisor/conf.d/

RUN mkdir -p bin/
COPY ./bin/eveUniverseGraph.bin bin/eveUniverseGraph.bin

RUN apk update
RUN apk add supervisor

CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/theia.sv.conf"]