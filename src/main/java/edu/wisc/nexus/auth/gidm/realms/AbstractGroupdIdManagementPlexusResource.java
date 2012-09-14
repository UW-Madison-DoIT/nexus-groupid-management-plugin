package edu.wisc.nexus.auth.gidm.realms;

import org.codehaus.plexus.component.annotations.Requirement;
import org.restlet.data.Request;
import org.sonatype.plexus.rest.resource.AbstractPlexusResource;

import edu.wisc.nexus.auth.gidm.GroupIdManager;

public abstract class AbstractGroupdIdManagementPlexusResource extends AbstractPlexusResource {
    /** Key to store Repo with which we work against. */
    public static final String REPOSITORY_ID_KEY = "repositoryId";

    @Requirement
    private GroupIdManager groupIdManager;

    protected GroupIdManager getGroupIdManager() {
        return groupIdManager;
    }

    /**
     * Pull the repository Id out of the Request.
     * 
     * @param request
     * @return
     */
    protected String getRepositoryId(Request request) {
        return request.getAttributes().get(REPOSITORY_ID_KEY).toString();
    }
}
