package edu.wisc.nexus.auth.gidm;

import java.io.IOException;
import java.util.Set;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.security.authorization.NoSuchAuthorizationManagerException;
import org.sonatype.security.authorization.NoSuchRoleException;

import edu.wisc.nexus.auth.gidm.om.v1_0_0.ManagedRepositories;
import edu.wisc.nexus.auth.gidm.om.v1_0_0.ManagedRepository;

public interface GroupIdManager {

    void removeManagedGroupId(String groupId);

    void addManagedGroupId(String groupId) throws ConfigurationException, IOException,
            NoSuchAuthorizationManagerException, NoSuchRoleException;

    Set<String> getManagedGroupIds();

    void removeManagedRepository(String repositoryId);

    void addManagedRepository(String repositoryId) throws NoSuchRepositoryException;

    ManagedRepositories getManagedRepositories();
    
    ManagedRepository getAsManagedRepository(String repositoryId) throws NoSuchRepositoryException;

}