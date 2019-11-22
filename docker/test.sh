#!/bin/bash

export GOSS_PATH=tests/goss-linux-amd64
export GOSS_OPTS="$GOSS_OPTS --format junit"
export GOSS_FILES_STRATEGY=cp
DOCKER_IMAGE=$1
DOCKER_FILE="${2:-$PWD/Dockerfile}"

cleanup() {
    set +e
    echo "Cleaning temporary data directory"
    rm -rf "$tmp_dir"
}

i=0

tmp_dir=$(mktemp -d /tmp/tmp.XXXXXXXXXX)
chmod 777 "$tmp_dir"
trap 'ret=$?;cleanup;exit $ret' EXIT
echo "Temp Data Directory: ${tmp_dir}"
ls $tmp_dir
mkdir -p ./reports

## Generate public/private key pair
# pipe password 123456 to orion
yes 123456 | docker run -i --rm --mount type=bind,source=$tmp_dir,target=/data $DOCKER_IMAGE -g /data/nodeKey

# Create password file
cat << EOF > $tmp_dir/passwordFile
123456
EOF

## Create orion configuration file
cat << EOF > $tmp_dir/orion.conf
nodeurl = "http://127.0.0.1:8080/"
nodeport = 8080
nodenetworkinterface = "0.0.0.0"
clienturl = "http://127.0.0.1:8888/"
clientport = 8888
clientnetworkinterface = "0.0.0.0"
publickeys = ["/data/nodeKey.pub"]
privatekeys = ["/data/nodeKey.key"]
passwords = "/data/passwordFile"
workdir = "/data"
tls = "off"
EOF

ls -l $tmp_dir/

# Test for normal startup with ports opened
GOSS_FILES_PATH=tests/01 \
bash tests/dgoss run --mount type=bind,source=$tmp_dir,target=/data $DOCKER_IMAGE /data/orion.conf \
> ./reports/01.xml || i=`expr $i + 1`

exit $i
