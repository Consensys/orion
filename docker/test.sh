#!/bin/bash

export GOSS_PATH=tests/goss-linux-amd64
export GOSS_OPTS="$GOSS_OPTS --format junit"
export GOSS_FILES_STRATEGY=cp
DOCKER_IMAGE=$1
DOCKER_FILE="${2:-$PWD/Dockerfile}"

i=0

## Generate public/private key pair
tmp_dir=$(mktemp -d -t ci-XXXXXXXXXX)

GOSS_FILES_PATH=tests/00 \
yes 123456 | bash tests/dgoss run $DOCKER_IMAGE \
--mount type=bind,source=$tmp_dir,target=/data -g /data/nodeKey
> ./reports/00.xml || i=`expr $i + 1`
ls -l $tmp_dir/
# fail fast if we dont pass static checks
if [[ $i != 0 ]]; then exit $i; fi

# Test for normal startup with ports opened
# we test that things listen on the right interface/port, not what interface the advertise
# hence we dont set p2p-host=0.0.0.0 because this sets what its advertising to devp2p; the important piece is that it defaults to listening on all interfaces
#GOSS_FILES_PATH=tests/01 \
#bash tests/dgoss run $DOCKER_IMAGE \
#--network=dev \
#--rpc-http-enabled \
#--rpc-ws-enabled \
#--graphql-http-enabled \
#> ./reports/01.xml || i=`expr $i + 1`

exit $i
