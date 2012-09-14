package edu.wisc.nexus.auth.gidm.realms;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.codehaus.plexus.component.annotations.Component;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.rest.repositories.AbstractRepositoryPlexusResource;
import org.sonatype.nexus.rest.repotargets.RepositoryTargetListPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

import edu.wisc.nexus.auth.gidm.GroupIdManager;
import edu.wisc.nexus.auth.gidm.om.v1_0_0.ManagedRepository;

@Component(role = PlexusResource.class, hint = "ManagedRepositoryPlexusResource")
@Path(RepositoryTargetListPlexusResource.RESOURCE_URI)
@Produces({ "application/xml", "application/json" })
@Consumes({ "application/xml", "application/json" })
public class ManagedRepositoryPlexusResource extends AbstractGroupdIdManagementPlexusResource {
    public static final String RESOURCE_URI = "/gidm/managed_repositories/{" + REPOSITORY_ID_KEY + "}";

    public ManagedRepositoryPlexusResource() {
        this.setModifiable(true);
    }

    @Override
    public Object getPayloadInstance() {
        return null;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection() {
        return new PathProtectionDescriptor("/gidm/managed_repositories/*", "authcBasic,perms[nexus:repositories]");
    }

    @Override
    public String getResourceUri() {
        return RESOURCE_URI;
    }

    @Override
    @GET
    @ResourceMethodSignature(pathParams = { @PathParam(AbstractRepositoryPlexusResource.REPOSITORY_ID_KEY) }, output = ManagedRepository.class)
    public Object get(Context context, Request request, Response response, Variant variant) throws ResourceException {
        final String repositoryId = getRepositoryId(request);
        this.getLogger().info("get managed repository: " + repositoryId);
        try {
            return getGroupIdManager().getAsManagedRepository(repositoryId);
        }
        catch (NoSuchRepositoryException e) {
            throw new ResourceException(503, e);
        }
    }

    @Override
    @PUT
    @ResourceMethodSignature(pathParams = { @PathParam(AbstractRepositoryPlexusResource.REPOSITORY_ID_KEY) }, input = ManagedRepository.class, output = ManagedRepository.class)
    public Object put(Context context, Request request, Response response, Object payload) throws ResourceException {
        final String repositoryId = getRepositoryId(request);
        this.getLogger().info("add managed repository: " + repositoryId);
        
        final GroupIdManager groupIdManager = this.getGroupIdManager();
        try {
            groupIdManager.addManagedRepository(repositoryId);
            return groupIdManager.getAsManagedRepository(repositoryId);
        }
        catch (NoSuchRepositoryException e) {
            throw new ResourceException(503, e);
        }
    }

    @Override
    @DELETE
    @ResourceMethodSignature(pathParams = { @PathParam(AbstractRepositoryPlexusResource.REPOSITORY_ID_KEY) })
    public void delete(Context context, Request request, Response response) {
        final String repositoryId = getRepositoryId(request);
        this.getLogger().info("delete managed repository: " + repositoryId);

        this.getGroupIdManager().removeManagedRepository(repositoryId);
    }
}
