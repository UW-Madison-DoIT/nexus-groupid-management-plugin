#!/bin/bash
#
# Copyright 2012, Board of Regents of the University of
# Wisconsin System. See the NOTICE file distributed with
# this work for additional information regarding copyright
# ownership. Board of Regents of the University of Wisconsin
# System licenses this file to you under the Apache License,
# Version 2.0 (the "License"); you may not use this file
# except in compliance with the License. You may obtain a
# copy of the License at:
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on
# an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.
#


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

