#!/bin/bash
set -e

# shellcheck source=scripts/common.sh
source "$(dirname "$0")/common.sh"

buildName=$(cat artifactory-repo/build-info.json | jq -r '.buildInfo.name')
buildNumber=$(cat artifactory-repo/build-info.json | jq -r '.buildInfo.number')
version=$(cat artifactory-repo/build-info.json | jq -r '.buildInfo.modules[0].id' | sed 's/.*:.*:\(.*\)/\1/')

echo "Distributing ${buildName}/${buildNumber} to ${BINTRAY_DISTRIBUTION_REPO}"

curl \
  -s \
  --connect-timeout 240 \
  --max-time 2700 \
  -u "${ARTIFACTORY_USERNAME}":"${ARTIFACTORY_PASSWORD}" \
  -H "Content-type:application/json" \
  -d "{\"sourceRepos\": [\"libs-release-local\"], \"targetRepo\" : \"${BINTRAY_DISTRIBUTION_REPO}\", \"async\":\"true\"}" \
  -f \
  -X \
  POST "${ARTIFACTORY_SERVER}/api/build/distribute/${buildName}/${buildNumber}" >/dev/null || {
  echo "Failed to promote" >&2
  exit 1
}

echo "Waiting for artifacts to be distributed"

WAIT_TIME=20
WAIT_ATTEMPTS=120

artifacts_published=false
retry_counter=0
while [ $artifacts_published == "false" ] && [ $retry_counter -lt $WAIT_ATTEMPTS ]; do
  result=$(curl -s -f -u "${BINTRAY_USERNAME}":"${BINTRAY_API_KEY}" https://api.bintray.com/packages/"${BINTRAY_SUBJECT}"/"${BINTRAY_REPO}"/"${BINTRAY_PACKAGE}")
  if [ $? -eq 0 ]; then
    versions=$(echo "$result" | jq -r '.versions')
    exists=$(echo "$versions" | grep "$version" -o || true)
    if [ "$exists" = "$version" ]; then
      artifacts_published=true
    fi
  fi
  retry_counter=$((retry_counter + 1))
  sleep $WAIT_TIME
done
if [[ $artifacts_published == "false" ]]; then
  echo "Failed to ditribute"
  exit 1
fi

echo "Distribution complete"
echo $version >version/version
