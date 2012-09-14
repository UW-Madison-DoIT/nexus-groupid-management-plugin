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
import org.sonatype.nexus.proxy.target.Target;
import org.sonatype.nexus.proxy.target.TargetRegistry;
import org.sonatype.nexus.rest.model.RepositoryTargetListResource;
import org.sonatype.nexus.rest.model.RepositoryTargetListResourceResponse;
import org.sonatype.nexus.rest.repotargets.AbstractRepositoryTargetPlexusResource;
import org.sonatype.nexus.rest.repotargets.RepositoryTargetListPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

import edu.wisc.nexus.auth.gidm.config.GroupManagementPluginConfiguration;

@Component(role = PlexusResource.class, hint = "ManagedGroupIdListPlexusResource")
@Path(RepositoryTargetListPlexusResource.RESOURCE_URI)
@Produces({ "application/xml", "application/json" })
@Consumes({ "application/xml", "application/json" })
public class ManagedGroupIdListPlexusResource extends AbstractRepositoryTargetPlexusResource {
    private static final String REPO_TARGET_PREFIX_GIDM = "GIDM_";

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
    @ResourceMethodSignature( output = RepositoryTargetListResourceResponse.class )
    public Object get(Context context, Request request, Response response, Variant variant) throws ResourceException {
        final TargetRegistry targetRegistry = this.getTargetRegistry();
        
        final RepositoryTargetListResourceResponse repositoryTargetListResponse = new RepositoryTargetListResourceResponse();
        for (final Target target : targetRegistry.getRepositoryTargets()) {
            if (target.getName().startsWith(REPO_TARGET_PREFIX_GIDM)) {
                final RepositoryTargetListResource res = new RepositoryTargetListResource();
                res.setId( target.getId() );
                res.setName( target.getName().substring(REPO_TARGET_PREFIX_GIDM.length()) );
                res.setContentClass( target.getContentClass().getId() );
                res.setResourceURI( this.createChildReference( request, this, target.getId() ).toString() );
                repositoryTargetListResponse.addData(res);
                
                //TODO verify all privs and roles are setup correctly
                
                this.getLogger().info("Including " + target.getName() + " as managed GroupId");
            }
            else {
                this.getLogger().info("Excluding " + target.getName() + " as managed GroupId");
            }
        }
     
        return repositoryTargetListResponse;
    }
}
