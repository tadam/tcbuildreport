FROM openjdk:8-jre-alpine

COPY ./build/libs/backend-fat.jar /root/backend-fat.jar

WORKDIR /root

ENV JAVA_OPTS -server -Xms350m -Xmx350m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication

CMD java $JAVA_OPTS -jar backend-fat.jar