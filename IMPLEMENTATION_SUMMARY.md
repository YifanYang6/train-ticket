# Java Profiling Implementation Summary

## Overview
This implementation adds continuous Java profiling capabilities to the train-ticket system using Grafana Pyroscope and Grafana Alloy, following the reference architectures from:
- https://github.com/grafana/pyroscope/tree/main/examples/tracing/java
- https://github.com/grafana/pyroscope/tree/main/examples/grafana-alloy-auto-instrumentation/java/kubernetes

## Changes Made

### 1. Kubernetes Components for Pyroscope

#### New Files Created:
- **manifests/monitoring/pyroscope.yaml**
  - Pyroscope deployment (1 replica)
  - Service exposure on port 4040
  - Resource limits configured

- **manifests/monitoring/grafana-alloy.yaml**
  - DaemonSet for auto-instrumentation
  - RBAC (ClusterRole, ServiceAccount, ClusterRoleBinding)
  - ConfigMap with Alloy configuration for:
    - Kubernetes pod discovery
    - Process discovery
    - Java process filtering
    - Automatic profiling configuration
  - Security context with required privileges (SYS_PTRACE, SYS_ADMIN, etc.)
  - hostPID: true for cross-pod process access

- **manifests/monitoring/grafana-pyroscope-provisioning.yaml**
  - ConfigMap for Grafana datasource provisioning
  - Pyroscope plugin configuration

- **manifests/monitoring/install_pyroscope.sh**
  - Installation script for easy deployment
  - Creates monitoring namespace
  - Applies all manifests
  - Provides usage instructions

- **manifests/monitoring/PYROSCOPE_README.md**
  - Comprehensive documentation
  - Installation instructions
  - Configuration details
  - Troubleshooting guide
  - Architecture diagram

### 2. Java Agent Updates

#### Updated: otel-java-agent/Dockerfile
Changed from:
- Elastic OTEL agent v1.4.1

To:
- OpenTelemetry Java agent v2.23.0
- Pyroscope Java agent v2.1.2
- Pyroscope OTEL extension v1.0.4

#### Updated: All Java Service Dockerfiles (40 services)
Added COPY commands to include:
- /pyroscope.jar
- /pyroscope-otel.jar

Services updated:
- ts-admin-basic-info-service
- ts-admin-order-service
- ts-admin-route-service
- ts-admin-travel-service
- ts-admin-user-service
- ts-assurance-service
- ts-auth-service
- ts-basic-service
- ts-cancel-service
- ts-config-service
- ts-consign-price-service
- ts-consign-service
- ts-contacts-service
- ts-delivery-service
- ts-execute-service
- ts-food-delivery-service
- ts-food-service
- ts-gateway-service
- ts-inside-payment-service
- ts-notification-service
- ts-order-other-service
- ts-order-service
- ts-payment-service
- ts-preserve-other-service
- ts-preserve-service
- ts-price-service
- ts-rebook-service
- ts-route-plan-service
- ts-route-service
- ts-seat-service
- ts-security-service
- ts-station-food-service
- ts-station-service
- ts-train-food-service
- ts-train-service
- ts-travel-plan-service
- ts-travel-service
- ts-travel2-service
- ts-user-service
- ts-verification-code-service
- ts-wait-order-service

### 3. Helm Configuration

#### Updated: manifests/helm/trainticket/values.yaml
Added new section:
```yaml
pyroscope:
  enabled: true
  serverAddress: "http://pyroscope.monitoring.svc.cluster.local:4040"
  profilingInterval: "10ms"
  profilerEvent: "itimer"
  profilerLock: "10ms"
  profilerAlloc: "512k"
  uploadInterval: "15s"
  logLevel: "info"
  addProfileUrl: "false"
  addProfileBaselineUrl: "false"
  startProfiling: "true"
```

#### Updated: manifests/helm/trainticket/templates/deployment.yaml
Added environment variables for Java services:
- PYROSCOPE_APPLICATION_NAME
- PYROSCOPE_FORMAT
- PYROSCOPE_PROFILING_INTERVAL
- PYROSCOPE_PROFILER_EVENT
- PYROSCOPE_PROFILER_LOCK
- PYROSCOPE_PROFILER_ALLOC
- PYROSCOPE_UPLOAD_INTERVAL
- PYROSCOPE_LOG_LEVEL
- PYROSCOPE_SERVER_ADDRESS
- OTEL_JAVAAGENT_EXTENSIONS
- OTEL_PYROSCOPE_ADD_PROFILE_URL
- OTEL_PYROSCOPE_ADD_PROFILE_BASELINE_URL
- OTEL_PYROSCOPE_START_PROFILING

Updated JAVA_TOOL_OPTIONS:
- Added: -javaagent:/otel-agent/pyroscope.jar
- Updated: -javaagent:/otel-agent/otel-agent.jar (now v2.23.0)
- Extension configured: /otel-agent/pyroscope-otel.jar

## Deployment Architecture

```
┌──────────────────────────────────────────────────┐
│           Kubernetes Cluster                      │
│                                                   │
│  ┌─────────────────────────────────────────┐    │
│  │  Train Ticket Java Services              │    │
│  │  (40 microservices)                      │    │
│  │  - OpenTelemetry agent v2.23.0           │    │
│  │  - Pyroscope agent v2.1.2                │    │
│  │  - Pyroscope OTEL extension v1.0.4       │    │
│  └──────────────┬──────────────────────────┘    │
│                 │                                 │
│  ┌──────────────▼──────────────────────────┐    │
│  │  Grafana Alloy (DaemonSet)               │    │
│  │  - Auto-discovers Java processes         │    │
│  │  - Profiles CPU, memory, locks           │    │
│  │  - Sends data to Pyroscope               │    │
│  └──────────────┬──────────────────────────┘    │
│                 │                                 │
│  ┌──────────────▼──────────────────────────┐    │
│  │  Pyroscope (Deployment)                  │    │
│  │  - Stores profiling data                 │    │
│  │  - Provides query API                    │    │
│  │  - Exposes UI on port 4040               │    │
│  └──────────────┬──────────────────────────┘    │
│                 │                                 │
│  ┌──────────────▼──────────────────────────┐    │
│  │  Grafana                                 │    │
│  │  - Pyroscope datasource configured       │    │
│  │  - Profiles Explorer available           │    │
│  └──────────────────────────────────────────┘    │
│                                                   │
└──────────────────────────────────────────────────┘
```

## Profiling Capabilities

The implementation profiles the following:

1. **CPU Profiling**
   - Event: itimer
   - Interval: 10ms
   - Sample rate: 100

2. **Memory Allocation Profiling**
   - Threshold: 512KB
   - Tracks object allocations

3. **Lock Contention Profiling**
   - Threshold: 10ms
   - Tracks lock waits

4. **Format**: JFR (Java Flight Recorder)

5. **Upload Interval**: 15 seconds

## Installation Instructions

### Prerequisites
- Kubernetes cluster
- Helm 3.x
- kubectl configured

### Step 1: Install Pyroscope Components
```bash
cd manifests/monitoring
./install_pyroscope.sh
```

Or manually:
```bash
kubectl create namespace monitoring
kubectl apply -f manifests/monitoring/pyroscope.yaml -n monitoring
kubectl apply -f manifests/monitoring/grafana-alloy.yaml
kubectl apply -f manifests/monitoring/grafana-pyroscope-provisioning.yaml
```

### Step 2: Rebuild and Deploy Java Services
```bash
# Rebuild the otel-java-agent image
cd otel-java-agent
./build.sh

# Rebuild and deploy services
# (Follow your existing CI/CD pipeline)
```

### Step 3: Deploy with Helm
```bash
cd manifests/helm/trainticket
helm upgrade --install trainticket . --namespace train-ticket
```

## Verification

### Check Pyroscope
```bash
kubectl port-forward -n monitoring svc/pyroscope 4040:4040
# Open http://localhost:4040
```

### Check Grafana Alloy
```bash
kubectl logs -n monitoring -l app=grafana-alloy -f
```

### Check Java Services
```bash
# Pick any Java service pod
kubectl exec -it <pod-name> -- env | grep PYROSCOPE
kubectl logs <pod-name> | grep javaagent
```

## Benefits

1. **Zero-Code Instrumentation**: Grafana Alloy automatically discovers and profiles Java processes
2. **Continuous Profiling**: Always-on profiling with minimal overhead
3. **Production-Safe**: Async-profiler is production-grade and battle-tested
4. **Integration with Observability Stack**: 
   - Traces from OpenTelemetry
   - Profiles from Pyroscope
   - Correlation via OTEL extension
5. **Performance Optimization**: Identify CPU hotspots, memory leaks, and lock contention
6. **Flexible Configuration**: Easily tune profiling parameters via Helm values

## Troubleshooting

See PYROSCOPE_README.md for detailed troubleshooting steps.

Common issues:
- Alloy not discovering processes: Check RBAC permissions and security context
- No profiling data: Verify connectivity to Pyroscope service
- High overhead: Adjust profiling intervals in values.yaml

## References

- [Grafana Pyroscope Documentation](https://grafana.com/docs/pyroscope/latest/)
- [Grafana Alloy Java Profiling](https://grafana.com/docs/pyroscope/latest/configure-client/grafana-alloy/java/)
- [OpenTelemetry Java](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
- [Example: Tracing with Java](https://github.com/grafana/pyroscope/tree/main/examples/tracing/java)
- [Example: Grafana Alloy Auto-instrumentation](https://github.com/grafana/pyroscope/tree/main/examples/grafana-alloy-auto-instrumentation/java/kubernetes)
