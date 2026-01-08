FROM docker.io/opspai/autoinstrumentation-java:elastic as agent
FROM  eclipse-temurin:17-jre
WORKDIR /app
RUN /bin/cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone
COPY --from=agent /javaagent.jar /otel-agent/otel-agent.jar
COPY --from=agent /pyroscope.jar /otel-agent/pyroscope.jar
COPY --from=agent /pyroscope-otel.jar /otel-agent/pyroscope-otel.jar
COPY ./target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
