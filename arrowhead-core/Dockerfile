FROM eclipse-temurin:17-jre-alpine

WORKDIR /opt/arrowhead-core

COPY target/arrowhead-*.jar ./
COPY run.sh run.sh

CMD ["/opt/arrowhead-core/run.sh"]
