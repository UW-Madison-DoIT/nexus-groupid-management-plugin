# Manage Nexus Authorization by GroupID

Ever wanted to let users read or write to your Nexus repositories but just under a specific GroupId? All the tools are there to do it but the steps are painful and require a lot of clicking.

Enter the nexus-groupid-management-plugin. You specify the repositories you want managed and then add managed GroupIDs. The plugin sets up Repository Targets, Roles, and Privileges for the repositories and groups. It is then as easy as specifying which roles each user has.

For example if I want to manage the "edu.wisc.my.portal" GroupId:
* I would add it via the plugin's GroupId management UI
* Find the user(s) I want to give access to the GroupId and add either the *GIDM: edu.wisc.my.portal - DEPLOYER* or *GIDM: edu.wisc.my.portal - READONLY* Roles

All repositories marked as "managed" by the plugin will be affected by these permissions.