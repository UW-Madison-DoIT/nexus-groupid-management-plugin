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
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.rest.repositories.AbstractRepositoryPlexusResource;
import org.sonatype.nexus.rest.repotargets.RepositoryTargetListPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

import edu.wisc.nexus.auth.gidm.om.v1_0_0.ManagedGroupId;
import edu.wisc.nexus.auth.gidm.om.v1_0_0.ManagedGroupIds;

@Component(role = PlexusResource.class, hint = "ManagedGroupIdPlexusResource")
@Path(RepositoryTargetListPlexusResource.RESOURCE_URI)
@Produces({ "application/xml", "application/json" })
@Consumes({ "application/xml", "application/json" })
public class ManagedGroupIdPlexusResource extends AbstractGroupdIdManagementPlexusResource {
    public static final String RESOURCE_URI = "/gidm/managed_groupids/{" + MANAGED_GROUPID_KEY + "}";

    public ManagedGroupIdPlexusResource() {
        this.setModifiable(true);
    }

    @Override
    public Object getPayloadInstance() {
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
    @ResourceMethodSignature(pathParams = { @PathParam(AbstractRepositoryPlexusResource.REPOSITORY_ID_KEY) }, output = ManagedGroupId.class)
    public Object get(Context context, Request request, Response response, Variant variant) throws ResourceException {
        final String managedGroupId = getManagedGroupId(request);
        this.getLogger().info("get " + managedGroupId);
        
        final ManagedGroupIds managedGroupIds = this.getGroupIdManager().getManagedGroupIds();
        if (managedGroupIds.getManagedGroupIds().contains(managedGroupId)) {
            final ManagedGroupId result = new ManagedGroupId();
            result.setGroupId(managedGroupId);
            return result;
        }
        
        //requested groupId doesn't exist
        return null;
    }

    @Override
    @PUT
    @ResourceMethodSignature(pathParams = { @PathParam(AbstractRepositoryPlexusResource.REPOSITORY_ID_KEY) }, input = ManagedGroupId.class, output = ManagedGroupId.class)
    public Object put(Context context, Request request, Response response, Object payload) throws ResourceException {
        final String managedGroupId = getManagedGroupId(request);
        this.getLogger().info("add managed groupId: " + managedGroupId);
        
        try {
            this.getGroupIdManager().addManagedGroupId(managedGroupId);
        }
        catch (Exception e) {
            this.getLogger().error("Failed to add managed GroupId '" + managedGroupId + "'", e);
            throw new ResourceException(Status.CLIENT_ERROR_CONFLICT, "Failed to add managed GroupId '" + managedGroupId + "'", e);
        }

        final ManagedGroupId result = new ManagedGroupId();
        result.setGroupId(managedGroupId);
        return result;
    }

    @Override
    @DELETE
    @ResourceMethodSignature(pathParams = { @PathParam(AbstractRepositoryPlexusResource.REPOSITORY_ID_KEY) })
    public void delete(Context context, Request request, Response response) throws ResourceException {
        final String managedGroupId = getManagedGroupId(request);
        this.getLogger().info("delete managed groupId: " + managedGroupId);

        try {
            this.getGroupIdManager().removeManagedGroupId(managedGroupId);
        }
        catch (Exception e) {
            this.getLogger().error("Failed to delete managed GroupId '" + managedGroupId + "'", e);
            throw new ResourceException(Status.CLIENT_ERROR_CONFLICT, "Failed to delete managed GroupId '" + managedGroupId + "'", e);
        }
    }
}
