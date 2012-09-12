/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package edu.wisc.nexus.auth.gidm.config;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.stream.XMLStreamException;

import org.codehaus.plexus.component.annotations.Component;

import edu.wisc.nexus.auth.gidm.config.v1_0_0.Configuration;
import edu.wisc.nexus.auth.gidm.config.v1_0_0.io.stax.NexusGroupManagementPluginConfigurationStaxReader;
import edu.wisc.nexus.auth.gidm.config.v1_0_0.io.stax.NexusGroupManagementPluginConfigurationStaxWriter;

@Component(role = GroupManagementPluginConfiguration.class, hint = "default")
public class DefaultGroupManagementPluginConfiguration extends AbstractRefreshingFileLoader<Configuration> implements
    GroupManagementPluginConfiguration {

    @org.codehaus.plexus.component.annotations.Configuration(value = "${nexus-work}/conf/gidm-plugin.xml")
    private File configurationFile;

    //Thread safe vars
    private volatile Set<String> managedRepositories = null;
    
    public DefaultGroupManagementPluginConfiguration() {
        super("RUTAuthPluginConfiguration");
    }
    
    @Override
    protected Configuration createConfiguration() {
        return new Configuration();
    }

    @Override
    protected File getConfigurationFile() {
        return this.configurationFile;
    }

    @Override
    protected int getRefreshInterval(Configuration configuration) {
        if (configuration == null) {
            return 60;
        }
        
        return configuration.getRefreshInterval();
    }
    
    @Override
    public Set<String> getManagedRepositories() {
        return Collections.unmodifiableSet(this.managedRepositories);
    }

    public void addManagedRepository(String repository) {
        if (this.managedRepositories == null) {
            //Force load of the config data
            this.refreshConfiguration();
        }
        
        this.managedRepositories.add(repository);
        
        //Trigger a save
        this.refreshConfiguration();
    }
    
    public void removeManagedRepository(String repository) {
        if (this.managedRepositories == null) {
            //Force load of the config data
            this.refreshConfiguration();
        }
        
        this.managedRepositories.remove(repository);
        
        //Trigger a save
        this.refreshConfiguration();
    }

    @Override
    protected void writeConfiguration(Writer w, Configuration configuration) throws IOException {
        try {
            new NexusGroupManagementPluginConfigurationStaxWriter().write(w, configuration);
        }
        catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected Configuration readConfiguration(Reader r) throws IOException {
        try {
            return new NexusGroupManagementPluginConfigurationStaxReader().read(r);
        }
        catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }
    
    @Override
    protected boolean preSave(Configuration configuration) {
        //Save the changes if managed repos has been loaded and is different than what is in the config
        if (this.managedRepositories != null && !this.managedRepositories.equals(configuration.getManagedRepositories())) {
            configuration.setManagedRepositories(this.managedRepositories);
            return true;
        }
        
        return false;
    }

    @Override
    protected void postLoad(Configuration configuration) {
        final Set<String> repos = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        if (configuration != null){
            final Set<String> managedRepos = configuration.getManagedRepositories();
            repos.addAll(managedRepos);
        }
        this.managedRepositories = repos;
    }
}
