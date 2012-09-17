/**
 * Copyright 2012, Board of Regents of the University of
 * Wisconsin System. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Board of Regents of the University of Wisconsin
 * System licenses this file to you under the Apache License,
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

import java.util.Set;


/**
 * Interface that manages Remote User Plugin Configuration data.
 */
public interface GroupManagementPluginConfiguration {
    /**
     * @return Read only set of all repositories that are managed by the groupId plugin
     */
    Set<String> getManagedRepositories();
    
    /**
     * @param repository Repository to manage
     */
    void addManagedRepository(String repository);
    
    /**
     * @param repository Repository to remove from management
     */
    void removeManagedRepository(String repository);
}
