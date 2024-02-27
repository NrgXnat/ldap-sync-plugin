package com.radiologics.plugins.ldapsync.services.impl;

import com.radiologics.plugins.ldapsync.entities.LdapGroup;
import com.radiologics.plugins.ldapsync.entities.LdapGroupProject;
import com.radiologics.plugins.ldapsync.exceptions.AlreadyExistsException;
import com.radiologics.plugins.ldapsync.exceptions.EmptyProjectsException;
import com.radiologics.plugins.ldapsync.exceptions.NotFoundException;
import com.radiologics.plugins.ldapsync.exceptions.WorkflowException;
import com.radiologics.plugins.ldapsync.repositories.LdapGroupProjectRepository;
import com.radiologics.plugins.ldapsync.services.LdapGroupEntityService;
import com.radiologics.plugins.ldapsync.services.LdapGroupProjectEntityService;
import com.radiologics.plugins.ldapsync.utils.WorkflowUtil;
import lombok.extern.slf4j.Slf4j;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xdat.security.helpers.Users;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@Slf4j
public class HibernateLdapGroupProjectEntityService
        extends AbstractHibernateEntityService<LdapGroupProject, LdapGroupProjectRepository>
        implements LdapGroupProjectEntityService {

    final LdapGroupProjectRepository ldapGroupProjectRepository;
    final LdapGroupEntityService ldapGroupEntityService;

    public HibernateLdapGroupProjectEntityService(LdapGroupProjectRepository ldapGroupProjectRepository, LdapGroupEntityService ldapGroupEntityService) {
        this.ldapGroupProjectRepository = ldapGroupProjectRepository;
        this.ldapGroupEntityService = ldapGroupEntityService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<LdapGroupProject> findByProjectId(String projectId) {
        return Optional.ofNullable(ldapGroupProjectRepository.findByUniqueProperty("projectId", projectId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LdapGroupProject delete(String projectId) throws NotFoundException, WorkflowException {
        log.info("Deleting LDAP Project '{}'", projectId);
        Optional<LdapGroupProject> optionalExistingProject = this.findByProjectId(projectId);

        if (!optionalExistingProject.isPresent()) {
            throw new NotFoundException(String.format("Ldap Group Project for project %s doesn't exist", projectId));
        }
        LdapGroupProject existingProject = optionalExistingProject.get();
        LdapGroup ldapGroup = existingProject.getLdapGroup();
        List<LdapGroupProject> ldapGroupProjects = ldapGroup.getLdapGroupProjects();
        List<LdapGroupProject> deletedProjects = new ArrayList<>();
        for (LdapGroupProject ldapGroupProject : ldapGroupProjects) {
            if (ldapGroupProject.getProjectId().equals(projectId)) {
                deletedProjects.add(ldapGroupProject);
            }
        }
        for (LdapGroupProject ldapGroupProject : deletedProjects) {
            ldapGroupProjects.remove(ldapGroupProject);
        }
        ldapGroupEntityService.update(ldapGroup);

        try {
            WorkflowUtil.createProjectWorkflow(projectId, optionalExistingProject.get().getLdapGroup(), Users.getAdminUser(), false);
        } catch (Exception e) {
            throw new WorkflowException("Failed to create workflow for the project " + projectId, e);
        }

        if (ldapGroupProjects.isEmpty() && ldapGroup.isLdapGroupEnabled()) {
            try {
                log.info("Disabling the LDAP Group '{}' because there is no project associated");
                ldapGroupEntityService.enable(ldapGroup.getLabel(), false);
            } catch (EmptyProjectsException e) {
                log.error("Failed to disable Ldap Group " + ldapGroup.getLabel(), e);
            }
        }

        return existingProject;
    }

//    @Override
//    public LdapGroup manageProjects(LdapGroupProjectManagement ldapGroupProjectManagement) throws NotFoundException {
//        String label = ldapGroupProjectManagement.getLabel();
//        List<String> newProjectIds = ldapGroupProjectManagement.getProjectIds();
//
//        Optional<LdapGroup> optionalLdapGroup = ldapGroupEntityService.findByLabel(label);
//        if (!optionalLdapGroup.isPresent()) {
//            String msg = "LDAP Group not found: " + label;
//            log.error(msg);
//            throw new NotFoundException(msg);
//        }
//        LdapGroup ldapGroup = optionalLdapGroup.get();
//
//        List<LdapGroupProject> ldapGroupProjects = ldapGroup.getLdapGroupProjects();
//        ldapGroup.setLdapGroupProjects(null);
//
//        List<String> existingProjectIds = ldapGroupProjects
//                .stream()
//                .map(ldapGroupProject -> ldapGroupProject.getProjectId())
//                .collect(Collectors.toList());
//
//        List<String> addedProjectIds = new ArrayList<>();
//        List<String> deletedProjectIds = new ArrayList<>();
//        for (String newProjectId : newProjectIds) {
//            if (!existingProjectIds.contains(newProjectId)) {
//                addedProjectIds.add(newProjectId);
//            }
//        }
//        for (String existingProjectId : existingProjectIds) {
//            if (!newProjectIds.contains(existingProjectId)) {
//                deletedProjectIds.add(existingProjectId);
//            }
//        }
//
//        for (String addedProjectId : addedProjectIds) {
//            this.create(LdapGroupProject.builder().ldapGroup(ldapGroup).projectId(addedProjectId));
//        }
//        for (String deletedProjectId : deletedProjectIds) {
//            this.delete(deletedProjectId);
//        }
//
//        return ldapGroupEntityService.findByLabel(label).get();
//    }
}
