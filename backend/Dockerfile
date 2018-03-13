FROM openjdk:8-jre-alpine

COPY ./build/libs/backend-fat.jar /root/backend-fat.jar

WORKDIR /root

CMD ["java", "-server", "-Xms200m", "-Xmx200m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "backend-fat.jar"]