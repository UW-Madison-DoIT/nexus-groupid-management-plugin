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
import org.codehaus.plexus.component.annotations.Requirement;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.rest.model.RepositoryResourceResponse;
import org.sonatype.nexus.rest.repositories.AbstractRepositoryPlexusResource;
import org.sonatype.nexus.rest.repotargets.RepositoryTargetListPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

import edu.wisc.nexus.auth.gidm.config.GroupManagementPluginConfiguration;

@Component(role = PlexusResource.class, hint = "ManagedRepositoryPlexusResource")
@Path(RepositoryTargetListPlexusResource.RESOURCE_URI)
@Produces({ "application/xml", "application/json" })
@Consumes({ "application/xml", "application/json" })
public class ManagedRepositoryPlexusResource extends AbstractRepositoryPlexusResource {
    public static final String RESOURCE_URI = "/gidm/managed_repositories/{" + REPOSITORY_ID_KEY + "}";

    @Requirement
    private GroupManagementPluginConfiguration groupManagementPluginConfiguration;

    public ManagedRepositoryPlexusResource() {
        this.setModifiable(true);
    }

    @Override
    public Object getPayloadInstance() {
        //TODO needed?
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
    @ResourceMethodSignature(pathParams = { @PathParam(AbstractRepositoryPlexusResource.REPOSITORY_ID_KEY) }, output = RepositoryResourceResponse.class)
    public Object get(Context context, Request request, Response response, Variant variant) throws ResourceException {
        this.getLogger().info("get");
        return this.getRepositoryResourceResponse(request, getRepositoryId(request));
    }

    @Override
    @PUT
    @ResourceMethodSignature(pathParams = { @PathParam(AbstractRepositoryPlexusResource.REPOSITORY_ID_KEY) }, input = RepositoryResourceResponse.class, output = RepositoryResourceResponse.class)
    public Object put(Context context, Request request, Response response, Object payload) throws ResourceException {
        final String repositoryId = getRepositoryId(request);
        this.getLogger().info("put " + repositoryId);
        this.getLogger().info("put " + context);
        this.getLogger().info("put " + payload);
        
        this.groupManagementPluginConfiguration.addManagedRepository(repositoryId);

        return this.getRepositoryResourceResponse(request, repositoryId);
    }

    @Override
    @DELETE
    @ResourceMethodSignature(pathParams = { @PathParam(AbstractRepositoryPlexusResource.REPOSITORY_ID_KEY) })
    public void delete(Context context, Request request, Response response) {
        final String repositoryId = getRepositoryId(request);
        this.getLogger().info("delete managed repository: " + repositoryId);

        this.groupManagementPluginConfiguration.removeManagedRepository(repositoryId);
    }
}
