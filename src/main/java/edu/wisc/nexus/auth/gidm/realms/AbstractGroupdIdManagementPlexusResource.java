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
