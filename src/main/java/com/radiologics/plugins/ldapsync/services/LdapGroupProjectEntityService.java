package com.radiologics.plugins.ldapsync.services;

import com.radiologics.plugins.ldapsync.entities.LdapGroup;
import com.radiologics.plugins.ldapsync.entities.LdapGroupProject;
import com.radiologics.plugins.ldapsync.exceptions.AlreadyExistsException;
import com.radiologics.plugins.ldapsync.exceptions.NotFoundException;
import com.radiologics.plugins.ldapsync.exceptions.WorkflowException;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

import java.util.Optional;

public interface LdapGroupProjectEntityService extends BaseHibernateService<LdapGroupProject> {
    /**
     * Finds the {@link LdapGroupProject LDAP Project} for the specified project ID.
     * @param projectId The ID of the project for which you want to retrieve the {@link LdapGroupProject LDAP Project}.
     * @return the {@link LdapGroupProject LDAP Project} wrapped in an {@link Optional}
     */
    Optional<LdapGroupProject> findByProjectId(String projectId);

    /**
     * Deletes the {@link LdapGroupProject LDAP Project}
     * @param projectId The ID of the project for which you want to delete the {@link LdapGroupProject LDAP Project}.
     * @return the deleted {@link LdapGroupProject LDAP Project}
     * @throws NotFoundException thrown when the {@link LdapGroup LDAP Group} that has the label is not found
     */
    LdapGroupProject delete(String projectId) throws NotFoundException, WorkflowException;
}
