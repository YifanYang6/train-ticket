#!/bin/bash

# Install Pyroscope and Grafana Alloy for Java profiling

set -e

echo "Installing Pyroscope and Grafana Alloy components..."

# Create monitoring namespace if it doesn't exist
kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -

# Deploy Pyroscope
echo "Deploying Pyroscope..."
kubectl apply -f pyroscope.yaml -n monitoring

# Deploy Grafana Alloy with RBAC
echo "Deploying Grafana Alloy..."
kubectl apply -f grafana-alloy.yaml

# Deploy Grafana Pyroscope provisioning
echo "Deploying Grafana Pyroscope provisioning..."
kubectl apply -f grafana-pyroscope-provisioning.yaml

echo "Waiting for Pyroscope to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/pyroscope -n monitoring || true

echo ""
echo "Installation complete!"
echo ""
echo "To access Pyroscope:"
echo "  kubectl port-forward -n monitoring svc/pyroscope 4040:4040"
echo "  Open http://localhost:4040 in your browser"
echo ""
echo "Grafana Alloy is deployed as a DaemonSet and will automatically discover and profile Java processes."
echo ""
echo "To check Grafana Alloy logs:"
echo "  kubectl logs -n monitoring -l app=grafana-alloy -f"
