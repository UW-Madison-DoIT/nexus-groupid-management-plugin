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
import org.sonatype.nexus.proxy.target.Target;
import org.sonatype.nexus.rest.model.RepositoryResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryTargetResource;
import org.sonatype.nexus.rest.repositories.AbstractRepositoryPlexusResource;
import org.sonatype.nexus.rest.repotargets.AbstractRepositoryTargetPlexusResource;
import org.sonatype.nexus.rest.repotargets.RepositoryTargetListPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

import edu.wisc.nexus.auth.gidm.config.GroupManagementPluginConfiguration;

@Component(role = PlexusResource.class, hint = "ManagedGroupIdPlexusResource")
@Path(RepositoryTargetListPlexusResource.RESOURCE_URI)
@Produces({ "application/xml", "application/json" })
@Consumes({ "application/xml", "application/json" })
public class ManagedGroupIdPlexusResource extends AbstractRepositoryTargetPlexusResource {
    public static final String REPOSITORY_TARGET_ID_KEY = "repositoryTargetId";
    public static final String RESOURCE_URI = "/gidm/managed_groupids/{" + REPOSITORY_TARGET_ID_KEY + "}";

    @Requirement
    private GroupManagementPluginConfiguration groupManagementPluginConfiguration;

    public ManagedGroupIdPlexusResource() {
        this.setModifiable(true);
    }

    @Override
    public Object getPayloadInstance() {
        //TODO needed?
        return null;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection() {
        return new PathProtectionDescriptor("/gidm/managed_groupids/*", "authcBasic,perms[nexus:targets]" );
    }

    @Override
    public String getResourceUri() {
        return RESOURCE_URI;
    }

    @Override
    @GET
    @ResourceMethodSignature(pathParams = { @PathParam(AbstractRepositoryPlexusResource.REPOSITORY_ID_KEY) }, output = RepositoryTargetResource.class)
    public Object get(Context context, Request request, Response response, Variant variant) throws ResourceException {
        this.getLogger().info("get");
        final String repositoryTargetId = getRepositoryTargetId(request);
        final Target repositoryTarget = this.getTargetRegistry().getRepositoryTarget(repositoryTargetId);
        return this.getNexusToRestResource(repositoryTarget, request);
    }

    @Override
    @PUT
    @ResourceMethodSignature(pathParams = { @PathParam(AbstractRepositoryPlexusResource.REPOSITORY_ID_KEY) }, input = RepositoryResourceResponse.class, output = RepositoryResourceResponse.class)
    public Object put(Context context, Request request, Response response, Object payload) throws ResourceException {
        final String repositoryId = getRepositoryTargetId(request);
        this.getLogger().info("add managed groupId: " + repositoryId);

        //TODO add new groupId

        return null;
    }

    @Override
    @DELETE
    @ResourceMethodSignature(pathParams = { @PathParam(AbstractRepositoryPlexusResource.REPOSITORY_ID_KEY) })
    public void delete(Context context, Request request, Response response) {
        final String repositoryId = getRepositoryTargetId(request);
        this.getLogger().info("delete managed groupId: " + repositoryId);

        //TODO delete groupId
    }
    
    
    protected String getRepositoryTargetId( Request request )
    {
        return request.getAttributes().get( REPOSITORY_TARGET_ID_KEY ).toString();
    }
}
