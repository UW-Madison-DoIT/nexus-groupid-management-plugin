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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.rest.repotargets.RepositoryTargetListPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

import edu.wisc.nexus.auth.gidm.config.GroupManagementPluginConfiguration;
import edu.wisc.nexus.auth.gidm.om.v1_0_0.ManagedGroupIds;

@Component(role = PlexusResource.class, hint = "ManagedGroupIdListPlexusResource")
@Path(RepositoryTargetListPlexusResource.RESOURCE_URI)
@Produces({ "application/xml", "application/json" })
@Consumes({ "application/xml", "application/json" })
public class ManagedGroupIdListPlexusResource extends AbstractGroupdIdManagementPlexusResource {
    public static final String RESOURCE_URI = "/gidm/managed_groupids";

    @Requirement
    private GroupManagementPluginConfiguration groupManagementPluginConfiguration;

    @Override
    public Object getPayloadInstance() {
        return null;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection() {
        return new PathProtectionDescriptor( getResourceUri(), "authcBasic,perms[nexus:targets]" );
    }

    @Override
    public String getResourceUri() {
        return RESOURCE_URI;
    }

    /**
     * Get the list of managed repositories
     */
    @Override
    @GET
    @ResourceMethodSignature( output = ManagedGroupIds.class )
    public Object get(Context context, Request request, Response response, Variant variant) throws ResourceException {
        return this.getGroupIdManager().getManagedGroupIds();
    }
}
