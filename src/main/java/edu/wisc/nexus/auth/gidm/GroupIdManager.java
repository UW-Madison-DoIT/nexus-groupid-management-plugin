package edu.wisc.nexus.auth.gidm;

import java.io.IOException;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.security.authorization.NoSuchAuthorizationManagerException;
import org.sonatype.security.authorization.NoSuchPrivilegeException;
import org.sonatype.security.authorization.NoSuchRoleException;

import edu.wisc.nexus.auth.gidm.om.v1_0_0.ManagedGroupIds;
import edu.wisc.nexus.auth.gidm.om.v1_0_0.ManagedRepositories;
import edu.wisc.nexus.auth.gidm.om.v1_0_0.ManagedRepository;

public interface GroupIdManager {

    void removeManagedGroupId(String groupId) throws NoSuchAuthorizationManagerException, NoSuchPrivilegeException,
            NoSuchRoleException, IOException;

    void addManagedGroupId(String groupId) throws ConfigurationException, IOException,
            NoSuchAuthorizationManagerException, NoSuchRoleException;

    ManagedGroupIds getManagedGroupIds();

    void removeManagedRepository(String repositoryId) throws NoSuchRepositoryException, NoSuchPrivilegeException, NoSuchAuthorizationManagerException, IOException;

    void addManagedRepository(String repositoryId) throws NoSuchRepositoryException, ConfigurationException, IOException, NoSuchAuthorizationManagerException, NoSuchRoleException;

    ManagedRepositories getManagedRepositories();

    ManagedRepository getAsManagedRepository(String repositoryId) throws NoSuchRepositoryException;

}