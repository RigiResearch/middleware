#!/bin/sh
echo "[1/3] Pushing jachinte/historian"
docker push jachinte/historian:latest

echo "[2/3] Pushing jachinte/coordinator"
docker push jachinte/coordinator:latest

echo "[3/3] Pushing jachinte/vmware"
docker push jachinte/vmware:latest
