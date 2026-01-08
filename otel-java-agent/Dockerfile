FROM alpine:3.18

# Install wget to download the files
RUN apk add --no-cache wget

# Download the OpenTelemetry Java agent version 2.23.0
RUN wget -O /javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.23.0/opentelemetry-javaagent.jar

# Download Pyroscope Java agent
RUN wget -O /pyroscope.jar https://github.com/grafana/pyroscope-java/releases/download/v2.1.2/pyroscope.jar

# Download Pyroscope OTEL extension
RUN wget -O /pyroscope-otel.jar https://github.com/grafana/otel-profiling-java/releases/download/v1.0.4/pyroscope-otel.jar

# Verify download and set permissions
RUN chmod 644 /javaagent.jar /pyroscope.jar /pyroscope-otel.jar