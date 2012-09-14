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
import org.sonatype.security.authorization.NoSuchRoleException;
import org.sonatype.security.authorization.Privilege;
import org.sonatype.security.authorization.Role;
import org.sonatype.security.realms.privileges.application.ApplicationPrivilegeMethodPropertyDescriptor;

import edu.wisc.nexus.auth.gidm.config.GroupManagementPluginConfiguration;
import edu.wisc.nexus.auth.gidm.om.v1_0_0.ManagedRepositories;
import edu.wisc.nexus.auth.gidm.om.v1_0_0.ManagedRepository;

@Component( role = GroupIdManager.class )
public class DefaultGroupIdManager extends AbstractLogEnabled implements GroupIdManager {
    private static final String SECURITY_CONTEXT = "default";
    private static final String GIDM_PREFIX = "GIDM";
    private static final String GIDM_NAME_PREFIX = GIDM_PREFIX + ": ";
    private static final String GIDM_ID_PREFIX = GIDM_PREFIX + "_";
    private static final ContentClass M2_CONTENT_CLASS = new Maven2ContentClass();
    private static final Pattern GROUPID_DELIM = Pattern.compile("\\.");
    private static final Pattern VALID_GROUPID_PART = Pattern.compile("^([a-zA-Z_]{1}[a-zA-Z0-9_]*(\\.[a-zA-Z_]{1}[a-zA-Z0-9_]*)*)?$");
    
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

    protected ManagedRepository createManagedRepository(final Repository repository) {
        final ManagedRepository managedRepository = new ManagedRepository();
        managedRepository.setId(repository.getId());
        managedRepository.setName(repository.getName());
        return managedRepository;
    }

    @Override
    public void addManagedRepository(String repositoryId) throws NoSuchRepositoryException {
        try {
            //Call verifies that the repository exists
            this.repositoryRegistry.getRepository(repositoryId);
        }
        catch (NoSuchRepositoryException e) {
            throw new NoSuchRepositoryException("Cannot manage repository that doesn't exist: " + repositoryId, e);
        }
        
        groupManagementPluginConfiguration.addManagedRepository(repositoryId);
    }
    
    @Override
    public void removeManagedRepository(String repositoryId) {
        groupManagementPluginConfiguration.removeManagedRepository(repositoryId);
    }
    
    @Override
    public Set<String> getManagedGroupIds() {
        final Set<String> managedGroupIds = new HashSet<String>();
        
        for (final Target target : this.targetRegistry.getTargetsForContentClass(M2_CONTENT_CLASS)) {
            final String id = target.getId();
            if (id.startsWith(GIDM_ID_PREFIX)) {
                managedGroupIds.add(id.substring(GIDM_ID_PREFIX.length()));
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
        Target managedTarget = findRepositoryTarget(targetId);
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
        final Role deployerRole = getOrCreateRole(authorizationManager, groupId, "DEPLOYER");
        final Set<String> deployerPrivs = deployerRole.getPrivileges();
        deployerPrivs.clear();

        final Role readOnlyRole = getOrCreateRole(authorizationManager, groupId, "READONLY");
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
        final ManagedRepositories managedRepositories = this.getManagedRepositories();
        for (final ManagedRepository repository : managedRepositories.getManagedRepositories()) {
            //TODO do these method names come from somewhere I can source?
            //note only bother setting up create and read privs
            for (final String method : new String[] { "create", "read" }) {
                final String name = GIDM_NAME_PREFIX + repository.getName() + " - (" + method + ")";
                
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
                deployerPrivs.add(priv.getId());
                if ("read".equals(method)) {
                    readOnlyPrivs.add(priv.getId());
                }
            }
        }
        
        //Add the roles
        authorizationManager.addRole(deployerRole);
        authorizationManager.addRole(readOnlyRole);
        
        this.nexusConfiguration.saveConfiguration();
    }

    protected Role getOrCreateRole(AuthorizationManager authorizationManager, String groupId, String roleSuffix) {
        final String roleId = GIDM_ID_PREFIX + groupId + "_" + roleSuffix;
        
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
        }
        
        return role;
    }

    protected Target findRepositoryTarget(final String targetName) {
        Target managedTarget = null;
        for (final Target target : this.targetRegistry.getTargetsForContentClass(M2_CONTENT_CLASS)) {
            if (targetName.equals(target.getName())) {
                managedTarget = target;
                break;
            }
        }
        return managedTarget;
    }

    protected String groupIdToTargetPattern(String groupId) {
        final StringBuilder targetPattern = new StringBuilder(groupId.length() + 3);
        for (final String idPart : GROUPID_DELIM.split(groupId)) {
            if (!VALID_GROUPID_PART.matcher(idPart).matches()) {
                throw new IllegalArgumentException("'" + idPart + "' is not valid in a managed groupId: " + groupId);
            }
         
            targetPattern.append("/").append(idPart);
        }
        targetPattern.append("/*");
        final String pattern = targetPattern.toString();
        
        this.getLogger().info("Converted groupId '" + groupId + "' to target pattern '" + pattern + "'");
        
        return pattern;
    }
    
    @Override
    public void removeManagedGroupId(String groupId) {
        
    }
}
