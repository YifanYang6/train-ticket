#!/bin/bash

# Install Pyroscope using official Helm chart
# Reference: https://github.com/grafana/pyroscope/tree/main/operations/pyroscope/helm/pyroscope

set -e

echo "Installing Pyroscope using official Helm chart..."

# Create monitoring namespace if it doesn't exist
kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -

# Add Grafana Helm repository
echo "Adding Grafana Helm repository..."
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

# Install Pyroscope with custom values
echo "Installing Pyroscope..."
helm upgrade --install pyroscope grafana/pyroscope \
  --namespace monitoring \
  --values pyroscope-values.yaml \
  --wait

echo ""
echo "Installation complete!"
echo ""
echo "Pyroscope has been installed with:"
echo "  - Pyroscope server for profile storage"
echo "  - Alloy for auto-discovery and profiling (disabled by default)"
echo ""
echo "To access Pyroscope UI:"
echo "  kubectl port-forward -n monitoring svc/pyroscope 4040:4040"
echo "  Open http://localhost:4040 in your browser"
echo ""
echo "To check deployment status:"
echo "  kubectl get pods -n monitoring -l app.kubernetes.io/name=pyroscope"
echo ""
echo "For Java profiling via push-based approach:"
echo "  The train-ticket services are configured with Pyroscope Java agents"
echo "  that will automatically send profiling data to Pyroscope."
echo ""
echo "For Alloy auto-discovery (optional):"
echo "  Enable alloy in pyroscope-values.yaml and run this script again."
