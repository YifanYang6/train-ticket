# Quick Start: Java Profiling with Grafana Pyroscope

## What's Been Added

This implementation adds continuous profiling to all Java services in the train-ticket system using:
- **Pyroscope**: Profiling data storage and querying
- **Grafana Alloy**: Auto-instrumentation agent for discovering and profiling Java processes
- **OpenTelemetry Java Agent v2.23.0**: Distributed tracing
- **Pyroscope Java Agent v2.1.2**: Continuous profiling
- **Pyroscope OTEL Extension v1.0.4**: Correlation between traces and profiles

## Quick Installation

### 1. Install Pyroscope Components on Kubernetes

```bash
cd manifests/monitoring
./install_pyroscope.sh
```

This installs:
- Pyroscope deployment in `monitoring` namespace
- Grafana Alloy DaemonSet for auto-instrumentation
- RBAC permissions for pod discovery

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

1. **Grafana Alloy** runs as a DaemonSet on each node
2. It **discovers** all Java processes in Kubernetes pods
3. It **automatically profiles** CPU, memory allocations, and lock contention
4. Profiling data is sent to **Pyroscope** every 15 seconds
5. You can view the data in the **Pyroscope UI** or **Grafana**

## Access Profiling Data

### Option 1: Pyroscope UI

```bash
kubectl port-forward -n monitoring svc/pyroscope 4040:4040
```

Then open http://localhost:4040 in your browser.

### Option 2: Grafana (if configured with provisioning)

1. Open Grafana
2. Go to **Explore**
3. Select **Pyroscope** datasource
4. Browse profiles by service, time range, and profile type

## Configuration

### Enable/Disable Profiling

Edit `manifests/helm/trainticket/values.yaml`:

```yaml
pyroscope:
  enabled: true  # Set to false to disable profiling
```

### Adjust Profiling Parameters

```yaml
pyroscope:
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

### Check Grafana Alloy is discovering processes

```bash
kubectl logs -n monitoring -l app=grafana-alloy -f
```

Look for messages about discovered Java processes.

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
   kubectl get pods -n monitoring -l app=pyroscope
   ```

2. Check Alloy logs for errors:
   ```bash
   kubectl logs -n monitoring -l app=grafana-alloy -f
   ```

3. Verify Java services can reach Pyroscope:
   ```bash
   kubectl exec -n train-ticket -it <pod-name> -- curl http://pyroscope.monitoring.svc.cluster.local:4040/healthz
   ```

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
