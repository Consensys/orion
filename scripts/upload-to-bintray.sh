#!/bin/sh

# Script to upload a file to Bintray

BT_USER=${1}    # Bintray username
BT_KEY=${2}     # Bintray API key
FILE=${3}       # Path to the file that will be uploaded (e.g. build/distributions/orion.zip)
ORG=${4}        # Organisation name (e.g. consensys)
REPO=${5}       # Repository name (e.g. binaries)
PACKAGE=${6}    # Package name (e.g. orion)
VERSION=${7}    # Version name (e.g. 1.0.0)
DIR_PATH=${8}   # Path on server that the file will be uploaded to (e.g. dir/)

echo "Uploading file ${FILE} to repository ${REPO}\n"

RESPONSE=$(curl -T ${FILE} -u${BT_USER}:${BT_KEY} https://api.bintray.com/content/${ORG}/${REPO}/${PACKAGE}/${VERSION}/${DIR_PATH}?publish=0&override=1)

if [ "${RESPONSE}" != '{"message":"success"}' ]; then
  echo "Error uploading file to Bintray: ${RESPONSE}/n"
  exit 1
fi

echo "File has been uploaded successfully"