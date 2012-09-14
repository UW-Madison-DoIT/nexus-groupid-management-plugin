#!/bin/bash

set -e

NEXUS_VERSION=2.0.6
NEXUS_BASE=/home/edalquist/Downloads/nexus
NEXUS_INST=${NEXUS_BASE}/nexus-${NEXUS_VERSION}
NEXUS_WORK=${NEXUS_BASE}/sonatype-work

PLUGIN_NAME=nexus-groupid-management-plugin
PLUGIN_VERSION=1.0.0-SNAPSHOT

# Stop Nexus
$NEXUS_INST/bin/nexus stop

# Clean out old plugin
rm -Rf $NEXUS_WORK/nexus/plugin-repository/${PLUGIN_NAME}-*

# Install new plugin
mkdir -p $NEXUS_WORK/nexus/plugin-repository/${PLUGIN_NAME}-${PLUGIN_VERSION}
cp target/${PLUGIN_NAME}-${PLUGIN_VERSION}.jar $NEXUS_WORK/nexus/plugin-repository/${PLUGIN_NAME}-${PLUGIN_VERSION}/

# Start Nexus
$NEXUS_INST/bin/nexus start

