/*
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
//Define the configuration panel
Sonatype.repoServer.GroupManagementConfigPanel = function( config ) {
    var config = config || {};
    var defaultConfig = {
      title: 'GroupId Management'
    };
    Ext.apply(this, config, defaultConfig);

    Sonatype.repoServer.GroupManagementConfigPanel.superclass.constructor.call(this, {
        layout : 'border',
        autoScroll : false,
        width : '100%',
        height : '100%',
        items : [ {
              xtype : 'panel',
              id : 'groupId-config-forms',
              title : 'GroupId Management',
              layout : 'card',
              region : 'center',
              activeItem : 0,
              deferredRender : false,
              autoScroll : false,
              frame : false,
              items : [{
                    xtype : 'panel',
                    layout : 'fit',
                    html : '<div class="little-padding"><a href="' + Sonatype.config.resourcePath  + '/static/gidm/html/configManagedRepos.html">Configure Managed Repositories</a></div>' +
                           '<div class="little-padding"><a href="' + Sonatype.config.resourcePath  + '/static/gidm/html/configManagedGroupIds.html">Configure Managed GroupIds</a></div>'
                  }]
            }]
      });
    
    this.formCards = this.findById('groupId-config-forms');
};



Ext.extend(Sonatype.repoServer.GroupManagementConfigPanel, Ext.Panel, {

});





// Add link to config panel
Sonatype.Events.addListener( 'nexusNavigationInit', function( nexusPanel ) {
  nexusPanel.add( {
    enabled: Sonatype.lib.Permissions.checkPermission( 'nexus:gidmconf', Sonatype.lib.Permissions.READ ),
    sectionId: 'st-nexus-security',
    title: 'GroupId Management',
    tabId: 'groupid-management',
    tabCode: Sonatype.repoServer.GroupManagementConfigPanel
  } );
} );

