package edu.wisc.nexus.auth.gidm.realms;

import java.util.Iterator;
import java.util.Set;

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
import org.sonatype.nexus.rest.model.RepositoryListResource;
import org.sonatype.nexus.rest.model.RepositoryListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryTargetListResourceResponse;
import org.sonatype.nexus.rest.repositories.AbstractRepositoryPlexusResource;
import org.sonatype.nexus.rest.repotargets.RepositoryTargetListPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

import edu.wisc.nexus.auth.gidm.config.GroupManagementPluginConfiguration;

@Component(role = PlexusResource.class, hint = "ManagedRepositoryListPlexusResource")
@Path(RepositoryTargetListPlexusResource.RESOURCE_URI)
@Produces({ "application/xml", "application/json" })
@Consumes({ "application/xml", "application/json" })
public class ManagedRepositoryListPlexusResource extends AbstractRepositoryPlexusResource {
    public static final String RESOURCE_URI = "/gidm/managed_repositories";

    @Requirement
    private GroupManagementPluginConfiguration groupManagementPluginConfiguration;

    @Override
    public Object getPayloadInstance() {
        return null;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection() {
        return new PathProtectionDescriptor( getResourceUri(), "authcBasic,perms[nexus:repositories]" );
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
    @ResourceMethodSignature(output = RepositoryTargetListResourceResponse.class)
    public Object get(Context context, Request request, Response response, Variant variant) throws ResourceException {
        return getManagedRepositoryResourceResponse(request);
    }

    private RepositoryListResourceResponse getManagedRepositoryResourceResponse(Request request)
            throws ResourceException {
        final Set<String> managedRepositories = groupManagementPluginConfiguration.getManagedRepositories();
        
        final RepositoryListResourceResponse repositories = this.listRepositories(request, false, false);
        
        for (final Iterator<RepositoryListResource> repoIterator = repositories.getData().iterator(); repoIterator.hasNext();) {
            final RepositoryListResource repository = repoIterator.next();
            if (managedRepositories.contains(repository.getId())) {
                this.getLogger().info("Including " + repository.getId() + " as managed repo");
            }
            else {
                repoIterator.remove();
                this.getLogger().info("Excluding " + repository.getId() + " as managed repo");
            }
        }
        return repositories;
    }
}
