package edu.wisc.nexus.auth.gidm;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeRepositoryPropertyDescriptor;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeRepositoryTargetPropertyDescriptor;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.target.Target;
import org.sonatype.nexus.proxy.target.TargetRegistry;
import org.sonatype.security.SecuritySystem;
import org.sonatype.security.authorization.AuthorizationManager;
import org.sonatype.security.authorization.NoSuchAuthorizationManagerException;
import org.sonatype.security.authorization.NoSuchPrivilegeException;
import org.sonatype.security.authorization.NoSuchRoleException;
import org.sonatype.security.authorization.Privilege;
import org.sonatype.security.authorization.Role;
import org.sonatype.security.realms.privileges.application.ApplicationPrivilegeMethodPropertyDescriptor;

import com.google.common.collect.ImmutableSet;

import edu.wisc.nexus.auth.gidm.config.GroupManagementPluginConfiguration;
import edu.wisc.nexus.auth.gidm.om.v1_0_0.ManagedGroupId;
import edu.wisc.nexus.auth.gidm.om.v1_0_0.ManagedGroupIds;
import edu.wisc.nexus.auth.gidm.om.v1_0_0.ManagedRepositories;
import edu.wisc.nexus.auth.gidm.om.v1_0_0.ManagedRepository;

@Component( role = GroupIdManager.class )
public class DefaultGroupIdManager extends AbstractLogEnabled implements GroupIdManager {
    private static final String READONLY_ROLE_SUFFIX = "READONLY";
    private static final String DEPLOYER_ROLE_SUFFIX = "DEPLOYER";
    private static final String SECURITY_CONTEXT = "default";
    private static final String GIDM_PREFIX = "GIDM";
    private static final String GIDM_NAME_PREFIX = GIDM_PREFIX + ": ";
    private static final String GIDM_ID_PREFIX = GIDM_PREFIX + "_";
    
    private static final ContentClass M2_CONTENT_CLASS = new Maven2ContentClass();
    
    private static final Pattern GROUPID_DELIM = Pattern.compile("\\.");
    private static final Pattern VALID_GROUPID_PART = Pattern.compile("^([a-zA-Z_]{1}[a-zA-Z0-9_]*(\\.[a-zA-Z_]{1}[a-zA-Z0-9_]*)*)?$");
    
    private static final Set<String> DEPLOYER_METHODS = ImmutableSet.of("create", "read");
    private static final Set<String> READONLY_METHODS = ImmutableSet.of("read");
    private static final Set<String> PRIVILEGE_METHODS = ImmutableSet.<String>builder().addAll(DEPLOYER_METHODS).addAll(READONLY_METHODS).build();
    
    @Requirement( hint = "protected" )
    private RepositoryRegistry repositoryRegistry;

    @Requirement
    private TargetRegistry targetRegistry;
    
    @Requirement
    private SecuritySystem securitySystem;
    
    @Requirement
    private GroupManagementPluginConfiguration groupManagementPluginConfiguration;
    
    @Requirement
    private NexusConfiguration nexusConfiguration;

    
    @Override
    public ManagedRepositories getManagedRepositories() {
        final ManagedRepositories managedRepositories = new ManagedRepositories();
        
        final Set<String> managedRepositoryIds = new HashSet<String>(this.groupManagementPluginConfiguration.getManagedRepositories());
        
        for (final Repository repository : this.repositoryRegistry.getRepositories()) {
            final ManagedRepository managedRepository = createManagedRepository(repository);
            
            if (managedRepositoryIds.remove(repository.getId())) {
                managedRepositories.addManagedRepository(managedRepository);
            }
            else {
                managedRepositories.addUnmanagedRepository(managedRepository);
            }
        }
        
        if (!managedRepositoryIds.isEmpty()) {
            getLogger().warn("The following managed repository IDs no longer exist as repositories in Nexus: " + managedRepositoryIds);
        }
        
        return managedRepositories;
    }
    
    @Override
    public ManagedRepository getAsManagedRepository(String repositoryId) throws NoSuchRepositoryException {
        final Repository repository = this.repositoryRegistry.getRepository(repositoryId);
        return createManagedRepository(repository);
    }

    @Override
    public void addManagedRepository(String repositoryId) throws NoSuchRepositoryException, ConfigurationException, IOException, NoSuchAuthorizationManagerException, NoSuchRoleException {
        final Repository repository;
        try {
            //Call verifies that the repository exists
            repository = this.repositoryRegistry.getRepository(repositoryId);
        }
        catch (NoSuchRepositoryException e) {
            throw new NoSuchRepositoryException("Cannot manage repository that doesn't exist: " + repositoryId, e);
        }
        
        groupManagementPluginConfiguration.addManagedRepository(repositoryId);

        //Go through and add all privs/roles needed for the new repository for every existing managed groupId
        final ManagedRepository managedRepository = this.createManagedRepository(repository);
        final ManagedGroupIds managedGroupIds = getManagedGroupIds();
        for (final ManagedGroupId managedGroupId : managedGroupIds.getManagedGroupIds()) {
            this.addRepositoryForGroupId(managedGroupId.getGroupId(), managedRepository);
        }
        
        this.nexusConfiguration.saveConfiguration();
    }
    
    @Override
    public void removeManagedRepository(String repositoryId) throws NoSuchRepositoryException, NoSuchPrivilegeException, NoSuchAuthorizationManagerException, IOException {
        final ManagedRepository repository = this.getAsManagedRepository(repositoryId);
        
        groupManagementPluginConfiguration.removeManagedRepository(repositoryId);
        
        final ManagedGroupIds managedGroupIds = getManagedGroupIds();
        for (final ManagedGroupId managedGroupId : managedGroupIds.getManagedGroupIds()) {
            this.removeRepositoryForGroupId(managedGroupId.getGroupId(), repository);
        }
        
        this.nexusConfiguration.saveConfiguration();
    }
    
    @Override
    public ManagedGroupIds getManagedGroupIds() {
        final ManagedGroupIds managedGroupIds = new ManagedGroupIds();
        
        for (final Target target : this.targetRegistry.getTargetsForContentClass(M2_CONTENT_CLASS)) {
            final String id = target.getId();
            if (id.startsWith(GIDM_ID_PREFIX)) {
                final String groupId = id.substring(GIDM_ID_PREFIX.length());
                final ManagedGroupId managedGroupId = createManagedGroupId(groupId);
                managedGroupIds.addManagedGroupId(managedGroupId);
            }
        }
        
        return managedGroupIds;
    }
    
    @Override
    public void addManagedGroupId(String groupId) throws ConfigurationException, IOException, NoSuchAuthorizationManagerException, NoSuchRoleException {
        final Logger logger = this.getLogger();
        
        //Validate the groupId and convert it to a repo target pattern
        final String targetPattern = groupIdToTargetPattern(groupId);

        //Get or Create the Target and persist the changes
        final String targetId = GIDM_ID_PREFIX + groupId;
        Target managedTarget = this.targetRegistry.getRepositoryTarget(targetId);
        if (managedTarget == null) {
            //Just using the name as the id ... hope thats ok!
            managedTarget = new Target(targetId, GIDM_NAME_PREFIX + groupId, M2_CONTENT_CLASS, Collections.singleton(targetPattern));
            logger.info("Created new repository target: " + managedTarget.getName());
        }
        else {
            final Set<String> patternTexts = managedTarget.getPatternTexts();
            patternTexts.clear();
            patternTexts.add(targetPattern);
            logger.info("Updated existing repository target: " + managedTarget.getName());
        }
        this.targetRegistry.addRepositoryTarget(managedTarget);

        
        final AuthorizationManager authorizationManager = this.securitySystem.getAuthorizationManager( SECURITY_CONTEXT );
        
        //Get or Create the deployer and readonly Roles, need these here to add the privs to them as they are created in the next step
        final Role deployerRole = getOrCreateRole(authorizationManager, groupId, DEPLOYER_ROLE_SUFFIX);
        final Set<String> deployerPrivs = deployerRole.getPrivileges();
        deployerPrivs.clear();

        final Role readOnlyRole = getOrCreateRole(authorizationManager, groupId, READONLY_ROLE_SUFFIX);
        final Set<String> readOnlyPrivs = readOnlyRole.getPrivileges();
        readOnlyPrivs.clear();

        //Assumes priv name is unique
        final Map<String, Privilege> existingPrivs = new HashMap<String, Privilege>();
        for (final Privilege priv : authorizationManager.listPrivileges()) {
            existingPrivs.put(priv.getName(), priv);
        }
        
        /*
         * Adds create/read privs for each managed repository
         */
        final ManagedRepositories managedRepositoriesObj = this.getManagedRepositories();
        for (final ManagedRepository repository : managedRepositoriesObj.getManagedRepositories()) {
            addRepositoryForGroupId(groupId,
                    repository,
                    managedTarget,
                    authorizationManager,
                    deployerPrivs,
                    readOnlyPrivs,
                    existingPrivs);
        }
        
        //Add the roles
        authorizationManager.updateRole(deployerRole);
        authorizationManager.updateRole(readOnlyRole);
        
        this.nexusConfiguration.saveConfiguration();
    }
    
    @Override
    public void removeManagedGroupId(String groupId) throws NoSuchAuthorizationManagerException, NoSuchPrivilegeException, NoSuchRoleException, IOException {
        final Logger logger = getLogger();
        
        final AuthorizationManager authorizationManager = this.securitySystem.getAuthorizationManager( SECURITY_CONTEXT );
        
        //Assumes priv name is unique
        final Map<String, Privilege> existingPrivs = new HashMap<String, Privilege>();
        for (final Privilege priv : authorizationManager.listPrivileges()) {
            existingPrivs.put(priv.getName(), priv);
        }
        
        /*
         * Deletes privs
         */
        final ManagedRepositories managedRepositoriesObj = this.getManagedRepositories();
        for (final ManagedRepository repository : managedRepositoriesObj.getManagedRepositories()) {
            removeRepositoryForGroupId(groupId, repository, authorizationManager, existingPrivs);
        }
        
        //Delete roles
        final String deployerRoleId = this.createRoleId(groupId, DEPLOYER_ROLE_SUFFIX);
        authorizationManager.deleteRole(deployerRoleId);
        logger.info("Deleted role: " + deployerRoleId);

        final String readOnlyRoleId = this.createRoleId(groupId, READONLY_ROLE_SUFFIX);
        authorizationManager.deleteRole(readOnlyRoleId);
        logger.info("Deleted role: " + readOnlyRoleId);
        
        
        //delete the repository target
        final String targetId = GIDM_ID_PREFIX + groupId;
        this.targetRegistry.removeRepositoryTarget(targetId);
        logger.info("Deleted repository target: " + targetId);
        
        this.nexusConfiguration.saveConfiguration();
    }
    
    /**
     * Remove repository from management
     */
    protected void removeRepositoryForGroupId(String groupId, ManagedRepository repository) throws NoSuchPrivilegeException, NoSuchAuthorizationManagerException {
        final AuthorizationManager authorizationManager = this.securitySystem.getAuthorizationManager( SECURITY_CONTEXT );

        //Assumes priv name is unique
        final Map<String, Privilege> existingPrivs = new HashMap<String, Privilege>();
        for (final Privilege priv : authorizationManager.listPrivileges()) {
            existingPrivs.put(priv.getName(), priv);
        }
        
        this.removeRepositoryForGroupId(groupId, repository, authorizationManager, existingPrivs);
    }

    protected void removeRepositoryForGroupId(String groupId, ManagedRepository repository,
            AuthorizationManager authorizationManager, Map<String, Privilege> existingPrivs)
            throws NoSuchPrivilegeException {
        
        final Logger logger = getLogger();
        
        for (final String method : PRIVILEGE_METHODS) {
            final String name = createPrivilegeName(repository, groupId, method);
            final Privilege priv = existingPrivs.remove(name);
            if (priv != null) {
                authorizationManager.deletePrivilege(priv.getId());
                logger.info("Deleted privilege: " + priv.getName());
            }
        }
    }
    
    /**
     * Setup Privs and Roles for the newly managed repository
     */
    protected void addRepositoryForGroupId(String groupId, ManagedRepository repository) throws InvalidConfigurationException, NoSuchAuthorizationManagerException, NoSuchRoleException {
        final String targetId = GIDM_ID_PREFIX + groupId;
        final Target managedTarget = this.targetRegistry.getRepositoryTarget(targetId);
        if (managedTarget == null) {
            throw new IllegalStateException("Failed to find repository target '" + targetId + "' for managed groupId: " + groupId);
        }
        
        final AuthorizationManager authorizationManager = this.securitySystem.getAuthorizationManager( SECURITY_CONTEXT );
        
        //Get or Create the deployer and readonly Roles, need these here to add the privs to them as they are created in the next step
        final Role deployerRole = getOrCreateRole(authorizationManager, groupId, DEPLOYER_ROLE_SUFFIX);
        final Set<String> deployerPrivs = deployerRole.getPrivileges();

        final Role readOnlyRole = getOrCreateRole(authorizationManager, groupId, READONLY_ROLE_SUFFIX);
        final Set<String> readOnlyPrivs = readOnlyRole.getPrivileges();

        //Assumes priv name is unique
        final Map<String, Privilege> existingPrivs = new HashMap<String, Privilege>();
        for (final Privilege priv : authorizationManager.listPrivileges()) {
            existingPrivs.put(priv.getName(), priv);
        }
        
        addRepositoryForGroupId(groupId,
                repository,
                managedTarget,
                authorizationManager,
                deployerPrivs,
                readOnlyPrivs,
                existingPrivs);

        authorizationManager.updateRole(deployerRole);
        authorizationManager.updateRole(readOnlyRole);
    }

    /**
     * Logic shared when adding new managed GroupIds and Repositories.
     */
    protected void addRepositoryForGroupId(String groupId, ManagedRepository repository, Target managedTarget,
            AuthorizationManager authorizationManager, Set<String> deployerPrivs, Set<String> readOnlyPrivs,
            Map<String, Privilege> existingPrivs) throws InvalidConfigurationException {

        final Logger logger = this.getLogger();
        
        for (final String method : PRIVILEGE_METHODS) {
            final String name = createPrivilegeName(repository, groupId, method);
            
            //Check for existing priv before creating a new one
            Privilege priv = existingPrivs.get(name);
            if (priv == null) {
                priv = new Privilege();
                logger.info("Creating new privilege: " + name);
            }
            else {
                logger.info("Updating existing privilege: " + name);
            }
            
            priv.setName(name);
            priv.setDescription(priv.getName());
            priv.setType(TargetPrivilegeDescriptor.TYPE);
      
            priv.addProperty(ApplicationPrivilegeMethodPropertyDescriptor.ID, method);
            priv.addProperty(TargetPrivilegeRepositoryTargetPropertyDescriptor.ID, managedTarget.getId());
            priv.addProperty(TargetPrivilegeRepositoryPropertyDescriptor.ID, repository.getId());
      
            //Store, capturing updated reference
            priv = authorizationManager.addPrivilege(priv);
            
            //Build up the priv lists
            if (DEPLOYER_METHODS.contains(method)) {
                deployerPrivs.add(priv.getId());
            }
            if (READONLY_METHODS.contains(method)) {
                readOnlyPrivs.add(priv.getId());
            }
        }
    }

    protected ManagedRepository createManagedRepository(final Repository repository) {
        final ManagedRepository managedRepository = new ManagedRepository();
        managedRepository.setId(repository.getId());
        managedRepository.setName(repository.getName());
        return managedRepository;
    }
    
    protected ManagedGroupId createManagedGroupId(final String groupId) {
        final ManagedGroupId managedGroupId = new ManagedGroupId();
        managedGroupId.setGroupId(groupId);
        return managedGroupId;
    }

    protected String createPrivilegeName(final ManagedRepository repository, final String groupId, final String method) {
        return GIDM_NAME_PREFIX + repository.getName() + " - " + groupId + " - (" + method + ")";
    }

    protected Role getOrCreateRole(AuthorizationManager authorizationManager, String groupId, String roleSuffix) throws InvalidConfigurationException {
        final String roleId = createRoleId(groupId, roleSuffix);
        
        Role role = null;
        try {
            role = authorizationManager.getRole(roleId);
            role.setName(GIDM_NAME_PREFIX + groupId + " - " + roleSuffix);
            getLogger().info("Updating existing role: " + role.getName());
        }
        catch (NoSuchRoleException e) {
            //Ignore
        }
        if (role == null) {
            role = new Role(roleId, GIDM_NAME_PREFIX + groupId + " - " + roleSuffix, "", SECURITY_CONTEXT, false, new HashSet<String>(), new HashSet<String>());
            getLogger().info("Creating new roll: " + role.getName());
            authorizationManager.addRole(role);
        }
        
        return role;
    }

    protected String createRoleId(String groupId, String roleSuffix) {
        return GIDM_ID_PREFIX + groupId + "_" + roleSuffix;
    }

    protected String groupIdToTargetPattern(String groupId) {
        final StringBuilder targetPattern = new StringBuilder(groupId.length() + 3);
        for (final String idPart : GROUPID_DELIM.split(groupId)) {
            if (!VALID_GROUPID_PART.matcher(idPart).matches()) {
                throw new IllegalArgumentException("'" + idPart + "' is not valid in a managed groupId: " + groupId);
            }
         
            targetPattern.append("/").append(idPart);
        }
        targetPattern.append("/.*");
        final String pattern = targetPattern.toString();
        
        this.getLogger().info("Converted groupId '" + groupId + "' to target pattern '" + pattern + "'");
        
        return pattern;
    }
}
