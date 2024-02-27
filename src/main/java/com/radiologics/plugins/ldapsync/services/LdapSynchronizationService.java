package com.radiologics.plugins.ldapsync.services;

import com.radiologics.plugins.ldapsync.dtos.LdapGroupSyncResult;
import com.radiologics.plugins.ldapsync.dtos.ProjectSyncResult;
import com.radiologics.plugins.ldapsync.entities.LdapGroup;
import com.radiologics.plugins.ldapsync.exceptions.*;
import org.springframework.ldap.core.LdapTemplate;

public interface LdapSynchronizationService {
    /**
     * Retrieves usernames using the information of the {@link LdapGroup LDAP Group} associated with the specified label and synchronize their user authorities to the mapped projects
     * @param label the label of the {@link LdapGroup LDAP Group} used to retrieve the existing one
     * @return {@link LdapGroupSyncResult Result Type}
     * @throws SynchronizationException thrown when error happened while adding or deleting user authorities from a XNAT project
     * @throws LdapNetworkException thrown when exceptions happened while communicating with the LDAP server
     * @throws NotFoundException thrown when the {@link LdapGroup LDAP Group} that has the specified label is not found
     * @throws LdapXnatException thrown when the {@link LdapTemplate LDAP Template} matched with the specified Provider ID is not found or when a user group is not found in a project.
     */
    LdapGroupSyncResult synchronizeUserAuthorities(final String label)
            throws SynchronizationException, LdapNetworkException, NotFoundException, LdapXnatException;

    /**
     * Retrieves usernames from the specified LDAP server using the specified groupDN and synchronize their user authorities to the specified project
     * @param ldapGroupLabel
     * @param projectId
     * @return Synchronization result
     * @throws NotFoundException thrown when the LDAP Group is not found
     * @throws SynchronizationException thrown when error happened while adding or deleting user authorities from a XNAT project
     * @throws LdapNetworkException thrown when exceptions happened while communicating with the LDAP server
     * @throws LdapXnatException thrown when the {@link LdapTemplate LDAP Template} matched with the specified Provider ID is not found or when a user group is not found in a project.
     */
    ProjectSyncResult synchronizeUserAuthorities(String ldapGroupLabel, String projectId)
            throws NotFoundException, LdapNetworkException, LdapXnatException, SynchronizationException;

    /**
     * Retrieves usernames from the specified LDAP server using the specified groupDN and synchronize their user authorities to the mapped projects
     * @param ldapGroup LDAP group
     * @return {@link LdapGroupSyncResult Result Type}
     * @throws SynchronizationException thrown when error happened while adding or deleting user authorities from a XNAT project
     * @throws LdapNetworkException thrown when exceptions happened while communicating with the LDAP server
     * @throws LdapXnatException thrown when the {@link LdapTemplate LDAP Template} matched with the specified Provider ID is not found or when a user group is not found in a project.
     */
    LdapGroupSyncResult synchronizeUserAuthorities(final LdapGroup ldapGroup)
            throws SynchronizationException, LdapNetworkException, LdapXnatException;

    /**
     * Initiates synchronization works for all the active {@link LdapGroup LDAP Groups}
     */
    void synchronize();
}
