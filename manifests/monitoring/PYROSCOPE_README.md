# Java Profiling with Grafana Pyroscope and Alloy

This directory contains the necessary components for continuous Java profiling using Grafana Pyroscope and Grafana Alloy.

## Overview

The profiling setup includes:

1. **Pyroscope**: A continuous profiling backend for storing and querying profiling data
2. **Grafana Alloy**: Auto-instrumentation agent that discovers Java processes and profiles them using async-profiler
3. **Grafana Integration**: Datasource provisioning for viewing profiling data in Grafana

## Components

### Pyroscope Deployment (`pyroscope.yaml`)

Deploys Pyroscope as a single replica deployment with service exposure on port 4040.

### Grafana Alloy DaemonSet (`grafana-alloy.yaml`)

Deploys Grafana Alloy as a DaemonSet with:
- **RBAC**: ClusterRole and bindings for pod discovery
- **Security Context**: Privileged access for attaching to Java processes
- **Configuration**: Auto-discovery and profiling of Java processes

Key features:
- Discovers all Kubernetes pods
- Identifies Java processes automatically
- Profiles CPU, memory allocations, and locks
- Sends profiling data to Pyroscope

### Grafana Provisioning (`grafana-pyroscope-provisioning.yaml`)

ConfigMap for provisioning Pyroscope as a datasource in Grafana.

## Installation

### Quick Start

```bash
cd manifests/monitoring
./install_pyroscope.sh
```

### Manual Installation

1. Create the monitoring namespace:
   ```bash
   kubectl create namespace monitoring
   ```

2. Deploy Pyroscope:
   ```bash
   kubectl apply -f pyroscope.yaml -n monitoring
   ```

3. Deploy Grafana Alloy:
   ```bash
   kubectl apply -f grafana-alloy.yaml
   ```

4. Apply Grafana provisioning:
   ```bash
   kubectl apply -f grafana-pyroscope-provisioning.yaml
   ```

## Configuration

### Helm Values

The following configuration has been added to `values.yaml`:

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

### Environment Variables

When Pyroscope is enabled in Helm, the following environment variables are automatically configured for Java services:

- `PYROSCOPE_APPLICATION_NAME`: Service name for profiling data
- `PYROSCOPE_FORMAT`: Profile format (jfr)
- `PYROSCOPE_PROFILING_INTERVAL`: How often to sample (10ms)
- `PYROSCOPE_PROFILER_EVENT`: Profiler event type (itimer)
- `PYROSCOPE_PROFILER_LOCK`: Lock profiling interval (10ms)
- `PYROSCOPE_PROFILER_ALLOC`: Allocation profiling threshold (512k)
- `PYROSCOPE_UPLOAD_INTERVAL`: How often to upload profiles (15s)
- `PYROSCOPE_LOG_LEVEL`: Logging level
- `PYROSCOPE_SERVER_ADDRESS`: Pyroscope server URL
- `OTEL_JAVAAGENT_EXTENSIONS`: OpenTelemetry extension for Pyroscope integration

### Java Agent Configuration

The Java agents are configured with:
- `-javaagent:/otel-agent/pyroscope.jar`: Pyroscope Java agent
- `-javaagent:/otel-agent/otel-agent.jar`: OpenTelemetry Java agent v2.23.0
- Extension: `/otel-agent/pyroscope-otel.jar`: Pyroscope-OTEL integration

## Accessing Profiling Data

### Port Forward to Pyroscope

```bash
kubectl port-forward -n monitoring svc/pyroscope 4040:4040
```

Then open http://localhost:4040 in your browser.

### Via Grafana

If you have Grafana deployed with the provisioning ConfigMap:

1. Open Grafana
2. Navigate to Explore
3. Select "Pyroscope" as the datasource
4. Browse profiling data using the Profiles Explorer

## Monitoring

### Check Grafana Alloy Status

```bash
# View logs
kubectl logs -n monitoring -l app=grafana-alloy -f

# Check DaemonSet status
kubectl get daemonset -n monitoring grafana-alloy

# Check discovered targets
kubectl port-forward -n monitoring ds/grafana-alloy 12345:12345
# Open http://localhost:12345 for Alloy UI
```

### Check Pyroscope Status

```bash
# Check deployment status
kubectl get deployment -n monitoring pyroscope

# View logs
kubectl logs -n monitoring -l app=pyroscope -f
```

## Profiling Capabilities

Grafana Alloy profiles the following:

1. **CPU**: Using itimer event at 10ms intervals
2. **Memory Allocations**: Objects allocated larger than 512KB
3. **Lock Contention**: Lock waits longer than 10ms

## Architecture

```
┌─────────────────┐
│ Java Services   │
│  (Train Ticket) │
└────────┬────────┘
         │
    ┌────▼────────────────┐
    │  Grafana Alloy      │
    │  (DaemonSet)        │
    │  - Process Discovery│
    │  - Auto-profiling   │
    └────────┬────────────┘
             │
        ┌────▼──────┐
        │ Pyroscope │
        │  (Storage)│
        └────┬──────┘
             │
        ┌────▼────────┐
        │   Grafana   │
        │  (Viewing)  │
        └─────────────┘
```

## Troubleshooting

### Alloy not discovering Java processes

1. Check that Alloy has the required RBAC permissions:
   ```bash
   kubectl get clusterrolebinding grafana-alloy-binding
   ```

2. Verify Alloy is running with privileged security context:
   ```bash
   kubectl get pod -n monitoring -l app=grafana-alloy -o yaml | grep privileged
   ```

3. Check Alloy logs for discovery issues:
   ```bash
   kubectl logs -n monitoring -l app=grafana-alloy | grep discovery
   ```

### Profiling data not appearing

1. Verify Pyroscope is accessible:
   ```bash
   kubectl exec -n monitoring -it deployment/grafana-alloy -- wget -O- http://pyroscope.monitoring.svc.cluster.local:4040/healthz
   ```

2. Check Java application environment variables:
   ```bash
   kubectl exec -it <java-pod> -- env | grep PYROSCOPE
   ```

3. Verify Java agents are loaded:
   ```bash
   kubectl logs <java-pod> | grep javaagent
   ```

## References

- [Grafana Pyroscope Documentation](https://grafana.com/docs/pyroscope/latest/)
- [Grafana Alloy Java Profiling](https://grafana.com/docs/pyroscope/latest/configure-client/grafana-alloy/java/)
- [OpenTelemetry Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
- [Pyroscope Java Agent](https://github.com/grafana/pyroscope-java)
- [Example: Tracing with Java](https://github.com/grafana/pyroscope/tree/main/examples/tracing/java)
- [Example: Grafana Alloy Auto-instrumentation](https://github.com/grafana/pyroscope/tree/main/examples/grafana-alloy-auto-instrumentation/java/kubernetes)
