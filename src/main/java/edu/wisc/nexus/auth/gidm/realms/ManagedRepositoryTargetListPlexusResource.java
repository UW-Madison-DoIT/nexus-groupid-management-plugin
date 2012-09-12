package edu.wisc.nexus.auth.gidm.realms;

import java.util.Collection;
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
import org.sonatype.nexus.proxy.target.Target;
import org.sonatype.nexus.proxy.target.TargetRegistry;
import org.sonatype.nexus.rest.model.RepositoryTargetListResource;
import org.sonatype.nexus.rest.model.RepositoryTargetListResourceResponse;
import org.sonatype.nexus.rest.repotargets.AbstractRepositoryTargetPlexusResource;
import org.sonatype.nexus.rest.repotargets.RepositoryTargetListPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

import edu.wisc.nexus.auth.gidm.config.GroupManagementPluginConfiguration;

@Component(role = PlexusResource.class, hint = "RepositoryTargetListPlexusResource")
@Path(RepositoryTargetListPlexusResource.RESOURCE_URI)
@Produces({ "application/xml", "application/json" })
@Consumes({ "application/xml", "application/json" })
public class ManagedRepositoryTargetListPlexusResource extends AbstractRepositoryTargetPlexusResource {
    public static final String RESOURCE_URI = "/managed_repo_targets";

    @Requirement
    private GroupManagementPluginConfiguration groupManagementPluginConfiguration;

    @Override
    public Object getPayloadInstance() {
        return null;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection() {
        return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:targets]");
    }

    @Override
    public String getResourceUri() {
        return RESOURCE_URI;
    }

    /**
     * Get the list of configuration repository targets.
     */
    @Override
    @GET
    @ResourceMethodSignature(output = RepositoryTargetListResourceResponse.class)
    public Object get(Context context, Request request, Response response, Variant variant) throws ResourceException {
        final RepositoryTargetListResourceResponse result = new RepositoryTargetListResourceResponse();
        
        final TargetRegistry targetRegistry = getTargetRegistry();
        final Collection<Target> targets = targetRegistry.getRepositoryTargets();
        
        final Set<String> managedRepositories = groupManagementPluginConfiguration.getManagedRepositories();

        for (Target target : targets) {
            if (managedRepositories.contains(target.getName())) {
                this.getLogger().info("Including " + target.getName() + " as managed repo");
                final RepositoryTargetListResource res = new RepositoryTargetListResource();
                res.setId(target.getId());
                res.setName(target.getName());
                res.setContentClass(target.getContentClass().getId());
                res.setResourceURI(this.createChildReference(request, this, target.getId()).toString());
                result.addData(res);
            }
            else {
                this.getLogger().info("Excluding " + target.getName() + " as managed repo");
            }
        }

        return result;
    }
}
