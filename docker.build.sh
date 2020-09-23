#!/bin/sh
echo "[1/3] Building jachinte/historian"
docker build . -f Dockerfile.historian -t jachinte/historian:latest

echo "[2/3] Building jachinte/coordinator"
docker build . -f Dockerfile.coordinator -t jachinte/coordinator:latest

echo "[3/3] Building jachinte/vmware"
docker build . -f Dockerfile.vmware -t jachinte/vmware:latest
