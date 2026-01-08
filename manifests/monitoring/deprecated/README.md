# Deprecated Manifests

The files in this directory are **deprecated** and kept only for reference.

## Why Deprecated?

We now use the official Pyroscope Helm chart from Grafana instead of raw Kubernetes manifests. This provides:
- Better maintenance and updates
- Official support from Grafana
- More configuration options
- Easier upgrades

## Migration

If you previously installed Pyroscope using these manifests:

1. Uninstall the old resources:
   ```bash
   kubectl delete -f pyroscope.yaml -n monitoring
   kubectl delete -f grafana-alloy.yaml
   kubectl delete -f grafana-pyroscope-provisioning.yaml
   ```

2. Install using Helm:
   ```bash
   cd ..
   ./install_pyroscope.sh
   ```

## Files

- **pyroscope.yaml**: Raw Kubernetes deployment (replaced by Helm chart)
- **grafana-alloy.yaml**: DaemonSet with manual configuration (now optional via Helm)
- **grafana-pyroscope-provisioning.yaml**: Grafana datasource provisioning (can be added manually to Grafana)

## Current Approach

See `../pyroscope-values.yaml` and `../install_pyroscope.sh` for the current Helm-based installation.
