## GroupID Authorization Management
This plugin adds a simply workflow UI around permissioning read/write access to specific GroupIDs within Nexus. There are no custom Realms here but just some code and UI to make doing all of the underlying Nexus Target, Priviledge and Role creation more sane.

## Plugin Installation

## Plugin Configuration
The plugin stores its configuration in `$NEXUS_BASE/sonatype-work/nexus/conf/gidm-plugin.xml`. The file will be created automatically if it does not exist.

## Example Usage
For example if I want to manage the "edu.wisc.my.portal" GroupId:
* I would add it via the plugin's GroupId management UI
* Find the user(s) I want to give access to the GroupId and add either the *GIDM: edu.wisc.my.portal - DEPLOYER* or *GIDM: edu.wisc.my.portal - READONLY* Roles

All repositories marked as "managed" by the plugin will be affected by these permissions.
