#!/bin/bash
set +x

DOCKER_IMAGE=$1

cd ha
DOCKER_IMAGE="$DOCKER_IMAGE" docker-compose up -d
i=0
while [ $i -lt 10 ]
do
  if [[ $(docker ps | grep healthy | wc -l) -eq 2 ]]; then
    break
  fi
  echo "Wait for 5s"
  sleep 5
  i=$((i+1))
done

exit_code=0
if [[ $i -lt 10 ]]; then
  echo "Orion started successfully."
  curl -s http://localhost:8080/upcheck
  exit_code=$((exit_code+$?))

  peercount=$(curl http://localhost:8888/peercount)
  exit_code=$((exit_code+$?))
  knownnodes=$(curl http://localhost:8888/knownnodes)
  exit_code=$((exit_code+$?))
  echo "Peers: $peercount -- Known nodes: $knownnodes"

  curl -X POST http://localhost:8080/partyinfo -H "Content-Type: application/cbor" -d @partyinfo.cbor
  exit_code=$((exit_code+$?))
  sleep 5

  peercount=$(curl http://localhost:8888/peercount)
  exit_code=$((exit_code+$?))
  knownnodes=$(curl http://localhost:8888/knownnodes)
  exit_code=$((exit_code+$?))
  echo "Peers: $peercount -- Known nodes: $knownnodes"

  if [[ $peercount -ne 2 ]]; then
    echo "Should have had 2 peers"
    exit_code=$((exit_code+1))
  fi

  docker stop orion_2
  knownnodes_orion1=$(curl http://localhost:8888/knownnodes)
  exit_code=$((exit_code+$?))
  docker start orion_2
  docker stop orion_1
  sleep 10

  knownnodes_orion2=$(curl http://localhost:8888/knownnodes)
  exit_code=$((exit_code+$?))
  if [[ "$knownnodes_orion1" != "$knownnodes_orion2" ]]; then
    echo "Should have had the same known nodes, was:"
    echo $knownnodes_orion1
    echo $knownnodes_orion2
    exit_code=$((exit_code+1))
  fi
else
  echo "Orion failed to start successfully."
  exit_code=1
fi
DOCKER_IMAGE="$DOCKER_IMAGE" docker-compose down

exit $exit_code