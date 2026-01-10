# Java Profiling with Grafana Pyroscope

This directory contains the configuration for continuous Java profiling using Grafana Pyroscope.

## Overview

The profiling setup uses:

1. **Pyroscope**: Official Helm chart for continuous profiling backend
2. **Push-based Profiling**: Java agents in train-ticket services send profiling data to Pyroscope
3. **Optional Alloy**: Can be enabled for auto-discovery and pull-based profiling

## Installation

### Quick Start (Recommended)

```bash
cd manifests/monitoring
./install_pyroscope.sh
```

This script will:
- Add the Grafana Helm repository
- Install Pyroscope using the official Helm chart
- Configure it for push-based profiling from Java services

### Manual Installation with Helm

1. Add the Grafana Helm repository:
   ```bash
   helm repo add grafana https://grafana.github.io/helm-charts
   helm repo update
   ```

2. Create the monitoring namespace:
   ```bash
   kubectl create namespace monitoring
   ```

3. Install Pyroscope:
   ```bash
   helm install pyroscope grafana/pyroscope \
     --namespace monitoring \
     --values pyroscope-values.yaml
   ```

### Using Official Helm Chart

We now use the official Pyroscope Helm chart from Grafana:
- **Repository**: https://github.com/grafana/pyroscope/tree/main/operations/pyroscope/helm/pyroscope
- **Chart**: `grafana/pyroscope`
- **Values**: See `pyroscope-values.yaml` for our configuration

## Configuration

### Pyroscope Values (`pyroscope-values.yaml`)

Key configuration options:

```yaml
pyroscope:
  replicaCount: 1          # Single instance for simple deployment
  resources:
    requests:
      memory: 256Mi
      cpu: 100m
    limits:
      memory: 2Gi
      cpu: 1000m
  service:
    type: ClusterIP
    port: 4040
  structuredConfig:
    storage:
      backend: s3          # Using MinIO object storage
      s3:
        endpoint: "minio.monitoring.svc.cluster.local:9000"
        bucket_name: "grafana-pyroscope-data"

minio:
  enabled: true            # MinIO enabled for persistent object storage
  persistence:
    size: 20Gi

alloy:
  enabled: false           # Disabled by default (using push-based profiling)
```

### Train-Ticket Helm Values

The following configuration has been added to `manifests/helm/trainticket/values.yaml`:

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
- `-javaagent:/otel-agent/pyroscope.jar`: Pyroscope Java agent v2.1.2
- `-javaagent:/otel-agent/otel-agent.jar`: OpenTelemetry Java agent v2.23.0
- Extension: `/otel-agent/pyroscope-otel.jar`: Pyroscope-OTEL integration v1.0.4

## Accessing Profiling Data

### Port Forward to Pyroscope

```bash
kubectl port-forward -n monitoring svc/pyroscope 4040:4040
```

Then open http://localhost:4040 in your browser.

### Via Grafana

You can add Pyroscope as a datasource in Grafana:

1. Open Grafana
2. Navigate to Configuration > Data Sources
3. Add Pyroscope datasource with URL: `http://pyroscope.monitoring.svc.cluster.local:4040`
4. Navigate to Explore and select "Pyroscope" datasource
5. Browse profiling data using the Profiles Explorer

## Monitoring

### Check Pyroscope Status

```bash
# Check Helm release
helm list -n monitoring

# Check deployment status
kubectl get pods -n monitoring -l app.kubernetes.io/name=pyroscope

# View logs
kubectl logs -n monitoring -l app.kubernetes.io/name=pyroscope -f
```

### Upgrade Pyroscope

```bash
helm upgrade pyroscope grafana/pyroscope \
  --namespace monitoring \
  --values pyroscope-values.yaml
```

## Profiling Capabilities

The Java services profile the following:

1. **CPU Time**: Using itimer event at 10ms intervals
2. **Memory Allocations**: Objects allocated larger than 512KB
3. **Lock Contention**: Lock waits longer than 10ms

## Architecture

### Push-Based Profiling with MinIO Storage (Default)

```
┌─────────────────┐
│ Java Services   │
│  (Train Ticket) │
│  - Pyroscope    │
│    Java Agent   │
└────────┬────────┘
         │ Push profiles
         │ every 15s
         ↓
    ┌────────────┐
    │ Pyroscope  │
    │  (Helm)    │
    └────┬───────┘
         │ Store profiles
         ↓
    ┌────────────┐
    │   MinIO    │
    │  (Object   │
    │  Storage)  │
    └────┬───────┘
         │
         ↓
    ┌────────────┐
    │  Grafana   │
    │  (Viewing) │
    └────────────┘
```

**MinIO Configuration:**
- Deployed as part of Pyroscope Helm chart
- 20Gi persistent storage
- S3-compatible API for profile storage
- Automatic bucket creation (`grafana-pyroscope-data`)
- Provides durable storage for profiling data

### Optional: Pull-Based with Alloy

If you enable Alloy in `pyroscope-values.yaml`:

```
┌─────────────────┐
│ Java Services   │
└────────┬────────┘
         │
    ┌────▼────────────────┐
    │  Grafana Alloy      │
    │  (Auto-discovery)   │
    └────────┬────────────┘
             │
        ┌────▼──────┐
        │ Pyroscope │
        └────┬──────┘
             │
        ┌────▼────────┐
        │   Grafana   │
        └─────────────┘
```

## Troubleshooting

### Profiling data not appearing

1. Verify Pyroscope is running:
   ```bash
   kubectl get pods -n monitoring -l app.kubernetes.io/name=pyroscope
   ```

2. Check Pyroscope logs:
   ```bash
   kubectl logs -n monitoring -l app.kubernetes.io/name=pyroscope -f
   ```

3. Verify Pyroscope is accessible from train-ticket namespace:
   ```bash
   kubectl run -it --rm debug --image=nicolaka/netshoot --restart=Never -n train-ticket -- \
     curl http://pyroscope.monitoring.svc.cluster.local:4040/healthz
   ```

4. Check Java application environment variables:
   ```bash
   kubectl exec -it -n train-ticket <java-pod> -- env | grep PYROSCOPE
   ```

5. Verify Java agents are loaded:
   ```bash
   kubectl logs -n train-ticket <java-pod> | grep javaagent
   ```

### Helm installation issues

1. Check Helm release status:
   ```bash
   helm list -n monitoring
   helm status pyroscope -n monitoring
   ```

2. View Helm values:
   ```bash
   helm get values pyroscope -n monitoring
   ```

3. Uninstall and reinstall if needed:
   ```bash
   helm uninstall pyroscope -n monitoring
   ./install_pyroscope.sh
   ```

## Enabling Alloy (Optional)

To enable Grafana Alloy for auto-discovery and pull-based profiling:

1. Edit `pyroscope-values.yaml`:
   ```yaml
   alloy:
     enabled: true
   ```

2. Upgrade the Helm release:
   ```bash
   helm upgrade pyroscope grafana/pyroscope \
     --namespace monitoring \
     --values pyroscope-values.yaml
   ```

Note: Alloy requires privileged security context and may not work in all cluster configurations.

## References

- [Pyroscope Official Helm Chart](https://github.com/grafana/pyroscope/tree/main/operations/pyroscope/helm/pyroscope)
- [Grafana Pyroscope Documentation](https://grafana.com/docs/pyroscope/latest/)
- [Grafana Alloy Java Profiling](https://grafana.com/docs/pyroscope/latest/configure-client/grafana-alloy/java/)
- [OpenTelemetry Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
- [Pyroscope Java Agent](https://github.com/grafana/pyroscope-java)
- [Example: Tracing with Java](https://github.com/grafana/pyroscope/tree/main/examples/tracing/java)
- [Example: Grafana Alloy Auto-instrumentation](https://github.com/grafana/pyroscope/tree/main/examples/grafana-alloy-auto-instrumentation/java/kubernetes)
