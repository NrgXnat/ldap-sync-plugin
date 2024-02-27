# LDAP Synchronization Plugin

The LDAP Synchronization Plugin allows users (enterprise users) the ability to manage the XNAT's project-level authorization through existing, centralized LDAP-based user management system. The plugin periodically synchronizes with thsese systems to keep XNAT access up-to-date. Theoretically, the plugin should support all of the LDAP servers like Microsoft Active Directory that are compatible with the latest versions of LDAP protocol.

## Dependencies

1. XNAT version 1.8.x
1. [XNAT Authentication Plugin](https://bitbucket.org/xnatx/ldap-auth-plugin)

## Build

To build the plugin:

1. If you haven't already, clone this repository and cd into the newly cloned folder.
1. Build the plugin: `./gradlew clean xnatPluginJar` (on Windows, you can use the batch file: `gradlew.bat clean xnatPluginJar`). This should build the plugin in the file `build/libs/filesystems_plugin-${VERSION}-xpl.jar`
1. Copy the plugin jar to your plugins folder: `cp build/libs/filesystems_plugin-${VERSION}-xpl.jar ${xnat.home}/plugins/`

## Configure

- Navigate to `Administer>Plugin Settings>Preferences` to set your preferences for the plugin
- Navigate to `Project Page>Project Settings on Actions menu>Settings`
