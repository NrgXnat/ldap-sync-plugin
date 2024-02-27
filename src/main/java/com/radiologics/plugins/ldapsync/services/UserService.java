package com.radiologics.plugins.ldapsync.services;

import com.radiologics.plugins.ldapsync.dtos.PermissionDiff;
import com.radiologics.plugins.ldapsync.exceptions.LdapXnatException;
import org.nrg.xdat.security.UserGroupI;
import org.nrg.xft.security.UserI;

import java.util.List;
import java.util.Map;

public interface UserService {
    /**
     * Finds the difference between the usernames in the existing {@link UserGroupI User groups} and the new usernames
     * @param existingUsernamesByGroupIdMap existing usernames {@link Map map}
     * @param newUsernames new usernames retrieved for the groupDN associated with a {@link com.radiologics.plugins.ldapsync.entities.LdapGroup LDAP Group}
     * @return
     */
    PermissionDiff diff(Map<String, List<String>> existingUsernamesByGroupIdMap, List<String> newUsernames);

    /**
     * Removes a user from the {@link UserGroupI User group} matched with the specified group ID
     * @param groupId the ID of {@link UserGroupI User group}
     * @param username the username to be deleted
     * @param adminUser {@link UserI admin user}
     * @throws Exception
     */
    void removeUserFromGroup(String groupId, String username, UserI adminUser) throws Exception;

    /**
     * Adds a user to the {@link UserGroupI User group} matched with the specified group ID
     * @param projectId the ID of {@link org.nrg.xdat.om.XnatProjectdata XNAT project}
     * @param groupName the Group Name
     * @param username the username to be added
     * @param adminUser {@link UserI admin user}
     * @throws Exception
     */
    void addUserToGroup(String projectId, String groupName, String username, UserI adminUser) throws Exception;

    /**
     * Adds a user to the {@link UserGroupI User group} matched with the specified group ID
     * @param groupId the ID of {@link UserGroupI User group}
     * @param username the username to be added
     * @param adminUser {@link UserI admin user}
     * @throws Exception
     */
    void addUserToGroup(String groupId, String username, UserI adminUser) throws Exception;

    /**
     * Gets all the {@link UserGroupI User groups} associated with the specified  {@link org.nrg.xdat.om.XnatProjectdata XNAT project}
     * @param projectId the ID of {@link org.nrg.xdat.om.XnatProjectdata XNAT project}
     * @return the {@link List list} of the {@link UserGroupI User groups}
     * @throws LdapXnatException
     */
    List<UserGroupI> getUserGroupsByProjectId(String projectId) throws LdapXnatException;

    /**
     * Gets the {@link Map map} of the usernames per {@link UserGroupI User groups}
     * @param userGroups {@link UserGroupI User groups}
     * @return the {@link Map map} of the usernames retrieved
     */
    Map<String, List<String>> getUsernamesByUserGroups(List<UserGroupI> userGroups);

    /**
     * Retrieves usernames associated with the login ID and the LDAP auth method ID. Possible for the matched usernames not to exist
     * @param loginIds Login ID to be used to search
     * @param authProviderId the LDAP auth provider ID
     * @return the usernames retrieved
     */
    List<String> retrieveUsernamesFromLogin(List<String> loginIds, String authProviderId);
}
