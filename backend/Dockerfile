FROM openjdk:8-jre-alpine

COPY ./build/libs/tcbuildreport-backend.jar /root/tcbuildreport-backend.jar

WORKDIR /root

CMD ["java", "-server", "-Xms200m", "-Xmx200m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "tcbuildreport-backend.jar"]