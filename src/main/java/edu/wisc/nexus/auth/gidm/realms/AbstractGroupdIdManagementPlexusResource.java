/**
 * Copyright 2012, Board of Regents of the University of
 * Wisconsin System. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Board of Regents of the University of Wisconsin
 * System licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package edu.wisc.nexus.auth.gidm.realms;

import org.codehaus.plexus.component.annotations.Requirement;
import org.restlet.data.Request;
import org.sonatype.plexus.rest.resource.AbstractPlexusResource;

import edu.wisc.nexus.auth.gidm.GroupIdManager;

public abstract class AbstractGroupdIdManagementPlexusResource extends AbstractPlexusResource {
    /** Key to store Repo with which we work against. */
    public static final String REPOSITORY_ID_KEY = "repositoryId";

    /** Key to store Repo Target with which we work against. */
    public static final String MANAGED_GROUPID_KEY = "repositoryTargetId";

    @Requirement
    private GroupIdManager groupIdManager;

    protected GroupIdManager getGroupIdManager() {
        return groupIdManager;
    }

    /**
     * Pull the repository Id out of the Request.
     */
    protected String getRepositoryId(Request request) {
        return request.getAttributes().get(REPOSITORY_ID_KEY).toString();
    }

    /**
     * Pull the managed groupId out of the Request.
     */
    protected String getManagedGroupId(Request request) {
        return request.getAttributes().get(MANAGED_GROUPID_KEY).toString();
    }
}
