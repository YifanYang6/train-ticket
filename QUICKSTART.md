# Quick Start: Java Profiling with Grafana Pyroscope

## What's Been Added

This implementation adds continuous profiling to all Java services in the train-ticket system using:
- **Pyroscope**: Official Helm chart for profiling data storage and querying
- **MinIO**: Object storage for persistent profile data (20Gi)
- **Push-based Profiling**: Java agents automatically send profiling data to Pyroscope
- **OpenTelemetry Java Agent v2.23.0**: Distributed tracing
- **Pyroscope Java Agent v2.1.2**: Continuous profiling
- **Pyroscope OTEL Extension v1.0.4**: Correlation between traces and profiles

## Quick Installation

### 1. Install Pyroscope on Kubernetes

**Note**: MinIO requires persistent storage. Ensure your cluster has a storage class:
```bash
kubectl get storageclass
```

If none exists, see troubleshooting section in `manifests/monitoring/PYROSCOPE_README.md`.

```bash
cd manifests/monitoring
./install_pyroscope.sh
```

This uses the official Grafana Helm chart to install:
- Pyroscope deployment in `monitoring` namespace
- MinIO object storage for persistent profiling data (requires storage class)
- Service exposed at `http://pyroscope.monitoring.svc.cluster.local:4040`

### 2. Rebuild the Java Agent Image

```bash
cd otel-java-agent
./build.sh
```

This builds a new image with:
- OpenTelemetry Java agent v2.23.0
- Pyroscope Java agent v2.1.2
- Pyroscope OTEL extension v1.0.4

### 3. Deploy or Update Train Ticket Services

```bash
cd manifests/helm/trainticket
helm upgrade --install trainticket . --namespace train-ticket --create-namespace
```

## What Happens Automatically

Once deployed:

1. **Java services** start with profiling agents loaded
2. They **automatically profile** CPU, memory allocations, and lock contention
3. Profiling data is sent to **Pyroscope** every 15 seconds
4. You can view the data in the **Pyroscope UI** or **Grafana**

## Access Profiling Data

### Option 1: Pyroscope UI

```bash
kubectl port-forward -n monitoring svc/pyroscope 4040:4040
```

Then open http://localhost:4040 in your browser.

### Option 2: Grafana

Add Pyroscope as a datasource in Grafana:
1. Open Grafana
2. Go to **Configuration > Data Sources**
3. Add Pyroscope datasource with URL: `http://pyroscope.monitoring.svc.cluster.local:4040`
4. Navigate to **Explore** and select Pyroscope datasource
5. Browse profiles by service, time range, and profile type

## Configuration

### Pyroscope Helm Chart Configuration

Edit `manifests/monitoring/pyroscope-values.yaml` to customize Pyroscope deployment:

```yaml
pyroscope:
  replicaCount: 1
  resources:
    limits:
      memory: 2Gi
      cpu: 1000m
  persistence:
    enabled: false  # Set to true for production
    size: 10Gi
```

After changing values, upgrade Pyroscope:

```bash
helm upgrade pyroscope grafana/pyroscope \
  --namespace monitoring \
  --values manifests/monitoring/pyroscope-values.yaml
```

### Train-Ticket Profiling Configuration

Edit `manifests/helm/trainticket/values.yaml`:

```yaml
pyroscope:
  enabled: true  # Set to false to disable profiling
  profilingInterval: "10ms"    # How often to sample (lower = more detail, more overhead)
  profilerAlloc: "512k"        # Memory allocation threshold
  profilerLock: "10ms"         # Lock contention threshold
  uploadInterval: "15s"        # How often to send data to Pyroscope
  logLevel: "info"             # Logging level (debug, info, warn, error)
```

After changing values, update the deployment:

```bash
helm upgrade trainticket manifests/helm/trainticket --namespace train-ticket
```

## What's Profiled

1. **CPU Time**: Where your code spends CPU cycles
2. **Memory Allocations**: Objects being allocated (> 512KB by default)
3. **Lock Contention**: Where threads wait for locks (> 10ms by default)

## Verify It's Working

### Check Pyroscope is running

```bash
kubectl get pods -n monitoring -l app.kubernetes.io/name=pyroscope
```

### Check a Java service has profiling enabled

```bash
# Pick any train-ticket pod
kubectl get pods -n train-ticket
kubectl exec -n train-ticket -it <pod-name> -- env | grep PYROSCOPE
```

You should see environment variables like:
- PYROSCOPE_APPLICATION_NAME
- PYROSCOPE_SERVER_ADDRESS
- OTEL_JAVAAGENT_EXTENSIONS

### Check Pyroscope has data

```bash
kubectl port-forward -n monitoring svc/pyroscope 4040:4040
```

Open http://localhost:4040 and you should see services listed.

## Troubleshooting

### No profiling data appearing

1. Check Pyroscope is running:
   ```bash
   kubectl get pods -n monitoring -l app.kubernetes.io/name=pyroscope
   ```

2. Check Pyroscope logs:
   ```bash
   kubectl logs -n monitoring -l app.kubernetes.io/name=pyroscope -f
   ```

3. Verify Java services can reach Pyroscope:
   ```bash
   kubectl exec -n train-ticket -it <pod-name> -- curl http://pyroscope.monitoring.svc.cluster.local:4040/healthz
   ```

4. Check Java agent logs in service pods:
   ```bash
   kubectl logs -n train-ticket <pod-name> | grep -i pyroscope
   ```

### Helm installation issues

1. Check Helm release:
   ```bash
   helm list -n monitoring
   helm status pyroscope -n monitoring
   ```

2. Reinstall if needed:
   ```bash
   helm uninstall pyroscope -n monitoring
   cd manifests/monitoring
   ./install_pyroscope.sh
   ```

### MinIO storage issues

If MinIO pods are pending due to PVC issues:

```bash
# Check storage classes
kubectl get storageclass

# If none exist, specify a storage class in pyroscope-values.yaml:
# minio:
#   persistence:
#     storageClass: "local-path"

# Or disable persistence for testing (not for production):
# minio:
#   persistence:
#     enabled: false

# Then upgrade:
helm upgrade pyroscope grafana/pyroscope \
  --namespace monitoring \
  --values manifests/monitoring/pyroscope-values.yaml
```

See `manifests/monitoring/PYROSCOPE_README.md` for detailed storage troubleshooting.

### High CPU/memory usage

Reduce profiling frequency:
```yaml
pyroscope:
  profilingInterval: "100ms"  # Increase from 10ms
  uploadInterval: "60s"       # Increase from 15s
```

### Java services not starting

Check if the javaagent files exist:
```bash
kubectl exec -n train-ticket -it <pod-name> -- ls -la /otel-agent/
```

Should show:
- otel-agent.jar
- pyroscope.jar
- pyroscope-otel.jar

## Next Steps

1. **Explore Profiles**: Use Pyroscope UI to find performance bottlenecks
2. **Set Up Alerts**: Configure alerts for high CPU usage or memory allocation rates
3. **Integrate with Grafana**: Set up dashboards showing profiles alongside metrics and traces
4. **Optimize**: Use profiling data to identify and fix performance issues

## Documentation

- Full implementation details: [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)
- Detailed guide: [manifests/monitoring/PYROSCOPE_README.md](manifests/monitoring/PYROSCOPE_README.md)

## Support

For issues or questions:
1. Check the troubleshooting sections in the documentation
2. Review Grafana Alloy logs for discovery issues
3. Verify network connectivity between components
4. Ensure RBAC permissions are correctly configured
