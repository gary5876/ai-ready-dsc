#!/bin/bash
set -e

ZK_HOST=${ZOOKEEPER_HOST:-zookeeper.railway.internal}
ZK_PORT=${ZOOKEEPER_PORT:-2181}

echo "Waiting for Zookeeper at ${ZK_HOST}:${ZK_PORT}..."
until bash -c "echo > /dev/tcp/${ZK_HOST}/${ZK_PORT}" 2>/dev/null; do
  echo "Zookeeper not ready, retrying in 3s..."
  sleep 3
done
echo "Zookeeper is ready."

exec /etc/confluent/docker/run
