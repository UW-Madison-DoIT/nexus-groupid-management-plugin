/*
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

//Define the configuration panel
Sonatype.repoServer.GroupManagementConfigPanel = function( config ) {
    var config = config || {};
    var defaultConfig = {
      title: 'GroupId Management'
    };
    Ext.apply(this, config, defaultConfig);

    this.sp = Sonatype.lib.Permissions;

    this.actions = {
      refresh : new Ext.Action({
            text : 'Refresh',
            iconCls : 'st-icon-refresh',
            scope : this,
            handler : this.reloadAll
          }),
      deleteAction : new Ext.Action({
            text : 'Delete',
            scope : this,
            handler : this.deleteHandler
          })
    };

    // A record to hold the name and id of a repository
    this.repoTargetRecordConstructor = Ext.data.Record.create([{
          name : 'resourceURI'
        }, {
          name : 'id'
        }, {
          name : 'contentClass'
        }, {
          name : 'name',
          sortType : Ext.data.SortTypes.asUCString
        }]);

    // A record to hold the contentClasses
    this.contentClassRecordConstructor = Ext.data.Record.create([{
          name : 'contentClass'
        }, {
          name : 'name',
          sortType : Ext.data.SortTypes.asUCString
        }]);
    
    // Reader and datastore that queries the server for the list of repo targets
    this.repoTargetsReader = new Ext.data.JsonReader({
          root : 'data',
          id : 'resourceURI'
        },
        this.repoTargetRecordConstructor);
    
    this.repoTargetsDataStore = new Ext.data.Store({
          url : Sonatype.config.repos.urls.repoTargets,
          reader : this.repoTargetsReader,
          sortInfo : {
            field : 'name',
            direction : 'ASC'
          },
          autoLoad : true
        });
    
    this.repoDataStore = new Ext.data.JsonStore({
      root : 'data',
      id : 'id',
      url : Sonatype.config.repos.urls.allRepositories,
      sortInfo : {
        field : 'name',
        direction : 'ASC'
      },
      fields : [{
            name : 'id'
          }, {
            name : 'name',
            sortType : Ext.data.SortTypes.asUCString
          }],
      autoLoad : true
    });

    // Reader and datastore that queries the server for the list of content
    // classes
    this.contentClassesReader = new Ext.data.JsonReader({
          root : 'data',
          id : 'contentClass'
        }, this.contentClassRecordConstructor);
    this.contentClassesDataStore = new Ext.data.Store({
          url : Sonatype.config.repos.urls.repoContentClasses,
          reader : this.contentClassesReader,
          sortInfo : {
            field : 'name',
            direction : 'ASC'
          },
          autoLoad : false
        });

    this.COMBO_WIDTH = 300;

    // Build the form
    this.formConfig = {
      region : 'center',
      width : '100%',
      height : '100%',
      autoScroll : true,
      border : false,
      frame : true,
      collapsible : false,
      collapsed : false,
      labelWidth : 150,
      layoutConfig : {
        labelSeparator : ''
      },

      items : [{
            xtype : 'textfield',
            fieldLabel : 'GroupId',
            itemCls : 'required-field',
            helpText : 'The GroupId to create permissions for',
            name : 'groupId',
            allowBlank : false,
            width : this.COMBO_WIDTH//,
            //validator : this.checkForDuplicates.createDelegate(this) TODO add check for dupe handler
          }, {
            xtype : 'twinpanelchooser',
            titleLeft : 'Permissioned Repositories',
            titleRight : 'Available Repositories',
            name : 'repositories',
            valueField : 'id',
            store : this.repoDataStore,
            required : true
          }],
      buttons : [{
            id : 'savebutton',
            text : 'Save',
            disabled : true
          }, {
            id : 'cancelbutton',
            text : 'Cancel'
          }]
    };

    this.groupIdsGridPanel = new Ext.grid.GridPanel({
          title : 'GroupIds',
          id : 'st-groupIds-grid',

          region : 'north',
          layout : 'fit',
          collapsible : true,
          split : true,
          height : 200,
          minHeight : 150,
          maxHeight : 400,
          frame : false,
          autoScroll : true,
          tbar : [{
                id : 'repoTarget-refresh-btn',
                text : 'Refresh',
                icon : Sonatype.config.resourcePath + '/images/icons/arrow_refresh.png',
                cls : 'x-btn-text-icon',
                scope : this,
                handler : this.reloadAll
              }, {
                id : 'repoTarget-add-btn',
                text : 'Add',
                icon : Sonatype.config.resourcePath + '/images/icons/add.png',
                cls : 'x-btn-text-icon',
                scope : this,
                handler : this.addResourceHandler,
                disabled : !this.sp.checkPermission('nexus:targets', this.sp.CREATE) //TODO also needs priv & role create
              }, {
                id : 'repoTarget-delete-btn',
                text : 'Delete',
                icon : Sonatype.config.resourcePath + '/images/icons/delete.png',
                cls : 'x-btn-text-icon',
                scope : this,
                handler : this.deleteHandler,
                disabled : !this.sp.checkPermission('nexus:targets', this.sp.DELETE) //TODO also needs priv & role delete
              }],

          // grid view options
          ds : this.repoTargetsDataStore,
          sortInfo : {
            field : 'name',
            direction : "ASC"
          },
          loadMask : true,
          deferredRender : false,
          columns : [{
                header : 'Name',
                dataIndex : 'name',
                width : 200
              }],
          autoExpandColumn : 'repo-target-expandable-col',
          disableSelection : false,
          viewConfig : {
            emptyText : 'Click "Add" to create a new GroupId Permission.'
          }
        });
    this.groupIdsGridPanel.getSelectionModel().on('rowselect', this.rowSelect, this);
    this.groupIdsGridPanel.on('rowcontextmenu', this.contextClick, this);

    Sonatype.repoServer.GroupManagementConfigPanel.superclass.constructor.call(this, {
        layout : 'border',
        autoScroll : false,
        width : '100%',
        height : '100%',
        items : [this.groupIdsGridPanel, {
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
                    html : '<div class="little-padding">Select a GroupId to edit it, or click "Add" to create a new one.</div>'
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

