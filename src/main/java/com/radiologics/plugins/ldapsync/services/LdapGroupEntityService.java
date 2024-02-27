package com.radiologics.plugins.ldapsync.services;

import com.radiologics.plugins.ldapsync.dtos.TransferLdapGroup;
import com.radiologics.plugins.ldapsync.entities.LdapGroup;
import com.radiologics.plugins.ldapsync.exceptions.*;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xft.security.UserI;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface LdapGroupEntityService extends BaseHibernateService<LdapGroup> {
    /**
     * Finds all the active {@link LdapGroup LDAP Groups}
     * @return The list of active {@link LdapGroup LDAP Groups}
     */
    List<LdapGroup> findActiveGroups();

    /**
     * Finds the {@link LdapGroup LDAP Group} for the specified label.
     * @param label The label of {@link LdapGroup LDAP Group} which is unique
     * @return the {@link LdapGroup LDAP Group} wrapped in an {@link Optional}
     */
    Optional<LdapGroup> findByLabel(String label);

    /**
     * Creates and stores the new {@link LdapGroup LDAP Group}
     * @param transferLdapGroup {@link TransferLdapGroup LDAP Group's Data transfer object}
     * @return the newly created {@link LdapGroup LDAP Group}
     * @throws AlreadyExistsException thrown when the {@link LdapGroup LDAP Group} that has the specified label already exists
     */
    LdapGroup create(TransferLdapGroup transferLdapGroup) throws AlreadyExistsException, WorkflowException, LdapInvalidArgumentException;

    /**
     * Updates the {@link LdapGroup LDAP Group}
     * @param originalLabel the label of the {@link LdapGroup LDAP Group} used to retrieve the existing one
     * @param transferLdapGroup {@link TransferLdapGroup LDAP Group's Data transfer object}
     * @return the updated {@link LdapGroup LDAP Group}
     * @throws NotFoundException thrown when the {@link LdapGroup LDAP Group} that has the original label is not found
     */
    LdapGroup update(String originalLabel, TransferLdapGroup transferLdapGroup) throws NotFoundException, WorkflowException, LdapInvalidArgumentException;

    /**
     * Enable or disable {@link LdapGroup LDAP Group} permanently
     * @param label the label of the {@link LdapGroup LDAP Group} used to retrieve the existing one
     * @param ldapGroupEnabled true if you want to enable the {@link LdapGroup LDAP Group}, false otherwise
     * @return the updated {@link LdapGroup LDAP Group}
     * @throws NotFoundException thrown when the {@link LdapGroup LDAP Group} that has the label is not found
     */
    LdapGroup enable(String label, boolean ldapGroupEnabled) throws NotFoundException, EmptyProjectsException, WorkflowException;

    /**
     * Deletes the {@link LdapGroup LDAP Group}
     * @param label the label of the {@link LdapGroup LDAP Group} used to retrieve the existing one
     * @return the deleted {@link LdapGroup LDAP Group}
     * @throws NotFoundException thrown when the {@link LdapGroup LDAP Group} that has the label is not found
     */
    LdapGroup delete(String label) throws NotFoundException, WorkflowException;

    /**
     * Updates the projects associated with the specified {@link LdapGroup LDAP Group}
     * @param label the label of the {@link LdapGroup LDAP Group} used to retrieve the existing one
     * @param newProjectIds the {@link List list} of projects to update
     * @param currentUser the current {@link UserI user}
     * @return the updated {@link LdapGroup LDAP Group}
     * @throws NotFoundException thrown when the {@link LdapGroup LDAP Group} that has the label is not found
     * @throws WorkflowException
     */
    LdapGroup manageProjects(String label, List<String> newProjectIds, UserI currentUser) throws NotFoundException, WorkflowException;

    /**
     * Disables LDAP Groups with non-enabled providers
     * @param enabledProviderIds
     */
    void disableGroupsWithNonEnabledProviders(Set<String> enabledProviderIds);
}
