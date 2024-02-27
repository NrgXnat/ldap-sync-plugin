package com.radiologics.plugins.ldapsync.services.impl;

import com.radiologics.plugins.ldapsync.dtos.TransferLdapGroup;
import com.radiologics.plugins.ldapsync.entities.LdapGroup;
import com.radiologics.plugins.ldapsync.entities.LdapGroupProject;
import com.radiologics.plugins.ldapsync.entities.LdapSearchType;
import com.radiologics.plugins.ldapsync.exceptions.*;
import com.radiologics.plugins.ldapsync.repositories.LdapGroupRepository;
import com.radiologics.plugins.ldapsync.services.LdapEmailService;
import com.radiologics.plugins.ldapsync.services.LdapGroupEntityService;
import com.radiologics.plugins.ldapsync.services.LdapProviderService;
import com.radiologics.plugins.ldapsync.utils.WorkflowUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.nrg.framework.orm.DatabaseHelper;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xdat.om.base.auto.AutoXnatProjectdata;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.plugins.auth.ldap.provider.XnatLdapAuthenticationProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.mail.MessagingException;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class HibernateLdapGroupEntityService extends AbstractHibernateEntityService<LdapGroup, LdapGroupRepository> implements LdapGroupEntityService {
    private final LdapGroupRepository ldapGroupRepository;
    private final LdapProviderService ldapProviderService;
    private final LdapEmailService ldapEmailService;
    private final DatabaseHelper databaseHelper;

    public HibernateLdapGroupEntityService(final LdapGroupRepository ldapGroupRepository,
                                           final LdapProviderService ldapProviderService,
                                           final LdapEmailService ldapEmailService,
                                           final JdbcTemplate template,
                                           final TransactionTemplate transactionTemplate) {
        this.ldapGroupRepository = ldapGroupRepository;
        this.ldapProviderService = ldapProviderService;
        this.ldapEmailService = ldapEmailService;
        this.databaseHelper = new DatabaseHelper(template, transactionTemplate);
    }

    private void validate(TransferLdapGroup transferLdapGroup) throws LdapInvalidArgumentException {
        String label = transferLdapGroup.getLabel();
        if (StringUtils.isBlank(label)) {
            throw new LdapInvalidArgumentException("Label is required");
        }
        if (!label.matches("^[a-zA-Z0-9\\-_\\s]*$")) {
            throw new LdapInvalidArgumentException("Alphanumeric and - characters are allowed for label");
        }
        validateAuthProviderId(transferLdapGroup.getAuthProviderId());

        String usernameAttrName = transferLdapGroup.getUsernameAttrName();
        if (StringUtils.isBlank(usernameAttrName)) {
            throw new LdapInvalidArgumentException("Username Attribute Name is required");
        }

        LdapSearchType ldapSearchType = transferLdapGroup.getLdapSearchType();
        if (ldapSearchType == null) {
            throw new LdapInvalidArgumentException("Ldap Search Type is required");
        }
        switch (ldapSearchType) {
            case User: {
                String objectClassName = transferLdapGroup.getObjectClassName();
                if (StringUtils.isBlank(objectClassName)) {
                    throw new LdapInvalidArgumentException("Object Class Name is required");
                }
                String userMembershipAttrName = transferLdapGroup.getUserMembershipAttrName();
                if (StringUtils.isBlank(userMembershipAttrName)) {
                    throw new LdapInvalidArgumentException("Membership Attribute Name is required");
                }
                break;
            }

            case Group: {
                String groupMemberAttrName = transferLdapGroup.getGroupMemberAttrName();
                if (StringUtils.isBlank(groupMemberAttrName)) {
                    throw new LdapInvalidArgumentException("Member Attribute Name is required");
                }
                break;
            }
        }
    }

    private void validateAuthProviderId(String authProviderId) {
        if (StringUtils.isEmpty(authProviderId)) {
            throw new IllegalArgumentException("Auth Provider ID is required");
        }

        Map<String, XnatLdapAuthenticationProvider> ldapProvidersMap = ldapProviderService.getLdapProviders();
        if (!ldapProvidersMap.containsKey(authProviderId)) {
            throw new IllegalArgumentException("Invalid Auth Provider Id");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<LdapGroup> findActiveGroups() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("enabled", true);
        properties.put("ldapGroupEnabled", true);
        List<LdapGroup> ldapGroups = ldapGroupRepository.findByProperties(properties);
        if (ldapGroups == null) {
            ldapGroups = new ArrayList<>();
        }
        return ldapGroups;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<LdapGroup> findByLabel(String label) {
        return Optional.ofNullable(ldapGroupRepository.findByUniqueProperty("label", label));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LdapGroup create(TransferLdapGroup transferLdapGroup) throws AlreadyExistsException, WorkflowException, LdapInvalidArgumentException {
        String label = transferLdapGroup.getLabel();
        String groupDN = transferLdapGroup.getGroupDN();
        String authProviderId = transferLdapGroup.getAuthProviderId();
        String usernameAttrName = transferLdapGroup.getUsernameAttrName();
        LdapSearchType ldapSearchType = transferLdapGroup.getLdapSearchType();
        String objectClassName = transferLdapGroup.getObjectClassName();
        String userMembershipAttrName = transferLdapGroup.getUserMembershipAttrName();
        String groupMemberAttrName = transferLdapGroup.getGroupMemberAttrName();

        log.info("Creating the LDAP Group (label: {}, groupDN: {})", label, groupDN);

        validate(transferLdapGroup);

        Optional<LdapGroup> possibleExistingGroup = this.findByLabel(label);
        if (possibleExistingGroup.isPresent()) {
            throw new AlreadyExistsException(String.format("%s already exists", label));
        }

        LdapGroup created = this.create(
                LdapGroup.builder()
                        .label(label)
                        .groupDN(groupDN)
                        .type(transferLdapGroup.getType())
                        .authProviderId(authProviderId)
                        .usernameAttrName(usernameAttrName)
                        .ldapSearchType(ldapSearchType)
                        .objectClassName(objectClassName)
                        .userMembershipAttrName(userMembershipAttrName)
                        .groupMemberAttrName(groupMemberAttrName)
                        .ldapGroupProjects(new ArrayList<>())
                        .build());

        try {
            WorkflowUtil.createGroupWorkflow(created.getId(), label, Users.getAdminUser(), LdapGroupActionType.Create);
        } catch (Exception e) {
            throw new WorkflowException("Failed to create workflow for the group " + label, e);
        }

        return created;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LdapGroup update(String originalLabel, TransferLdapGroup transferLdapGroup) throws NotFoundException, WorkflowException, LdapInvalidArgumentException {
        String label = transferLdapGroup.getLabel();
        String groupDN = transferLdapGroup.getGroupDN();
        String authProviderId = transferLdapGroup.getAuthProviderId();
        String usernameAttrName = transferLdapGroup.getUsernameAttrName();
        LdapSearchType ldapSearchType = transferLdapGroup.getLdapSearchType();
        String objectClassName = transferLdapGroup.getObjectClassName();
        String userMembershipAttrName = transferLdapGroup.getUserMembershipAttrName();
        String groupMemberAttrName = transferLdapGroup.getGroupMemberAttrName();

        log.info("Updating the LDAP Group (label: {})", label);

        validate(transferLdapGroup);

        Optional<LdapGroup> optionalExistingGroup = this.findByLabel(originalLabel);
        if (!optionalExistingGroup.isPresent()) {
            throw new NotFoundException(String.format("%s doesn't exist", originalLabel));
        }
        LdapGroup existingGroup = optionalExistingGroup.get();

        existingGroup.setLabel(label);
        existingGroup.setGroupDN(groupDN);
        existingGroup.setType(transferLdapGroup.getType());
        existingGroup.setAuthProviderId(authProviderId);
        existingGroup.setLdapGroupEnabled(transferLdapGroup.isLdapGroupEnabled());
        existingGroup.setUsernameAttrName(transferLdapGroup.getUsernameAttrName());
        existingGroup.setLdapSearchType(transferLdapGroup.getLdapSearchType());
        existingGroup.setObjectClassName(transferLdapGroup.getObjectClassName());
        existingGroup.setUserMembershipAttrName(transferLdapGroup.getUserMembershipAttrName());
        existingGroup.setGroupMemberAttrName(transferLdapGroup.getGroupMemberAttrName());

        update(existingGroup);

        try {
            WorkflowUtil.createGroupWorkflow(existingGroup.getId(), label, Users.getAdminUser(), LdapGroupActionType.Modify);
        } catch (Exception e) {
            throw new WorkflowException("Failed to create workflow for the group " + label, e);
        }

        return existingGroup;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LdapGroup enable(String label, boolean ldapGroupEnabled) throws NotFoundException, EmptyProjectsException, WorkflowException {
        Optional<LdapGroup> optionalExistingGroup = this.findByLabel(label);

        if (!optionalExistingGroup.isPresent()) {
            String msg = "LDAP Group not found: " + label;
            log.error(msg);
            throw new NotFoundException(msg);
        }
        LdapGroup existingGroup = optionalExistingGroup.get();

        List<LdapGroupProject> projects = existingGroup.getLdapGroupProjects();
        if (ldapGroupEnabled && projects.isEmpty()) {
            throw new EmptyProjectsException("Cannot enable the group because there is no project associated");
        }

        existingGroup.setLdapGroupEnabled(ldapGroupEnabled);

        update(existingGroup);

        if (ldapGroupEnabled) {
            removePendingPARs(projects.stream().map(LdapGroupProject::getProjectId).collect(Collectors.toList()));
        }

        try {
            WorkflowUtil.createGroupWorkflow(existingGroup.getId(), label, Users.getAdminUser(), existingGroup.isLdapGroupEnabled() ? LdapGroupActionType.Enable : LdapGroupActionType.Disable);
        } catch (Exception e) {
            throw new WorkflowException("Failed to create workflow for the group " + label, e);
        }

        return existingGroup;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LdapGroup delete(String label) throws NotFoundException, WorkflowException {
        Optional<LdapGroup> optionalExistingGroup = this.findByLabel(label);

        if (!optionalExistingGroup.isPresent()) {
            String msg = "LDAP Group not found: " + label;
            log.error(msg);
            throw new NotFoundException(msg);
        }
        LdapGroup existingGroup = optionalExistingGroup.get();

        this.delete(existingGroup);

        try {
            WorkflowUtil.createGroupWorkflow(existingGroup.getId(), label, Users.getAdminUser(), LdapGroupActionType.Delete);
        } catch (Exception e) {
            throw new WorkflowException("Failed to create workflow for the group " + label, e);
        }

        return existingGroup;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public LdapGroup manageProjects(String label, List<String> newProjectIds, UserI currentUser) throws NotFoundException, WorkflowException {
        Optional<LdapGroup> optionalLdapGroup = this.findByLabel(label);
        if (!optionalLdapGroup.isPresent()) {
            String msg = "LDAP Group not found: " + label;
            log.error(msg);
            throw new NotFoundException(msg);
        }
        LdapGroup ldapGroup = optionalLdapGroup.get();

        List<LdapGroupProject> ldapGroupProjects = ldapGroup.getLdapGroupProjects();
        List<String> existingProjectIds = ldapGroupProjects.stream().map(ldapGroupProject -> ldapGroupProject.getProjectId()).collect(Collectors.toList());

        List<LdapGroupProject> tempLdapGroupProjects = new ArrayList<>();
        for (LdapGroupProject ldapGroupProject : ldapGroupProjects) {
            if (newProjectIds.contains(ldapGroupProject.getProjectId())) {
                tempLdapGroupProjects.add(ldapGroupProject);
            }
        }

        List<String> addedProjectIds = new ArrayList<>();
        for (String newProjectId : newProjectIds) {
            if (!existingProjectIds.contains(newProjectId)) {
                tempLdapGroupProjects.add(LdapGroupProject.builder().ldapGroup(ldapGroup).projectId(newProjectId).build());
                addedProjectIds.add(newProjectId);
            }
        }

        ldapGroupProjects.clear();
        ldapGroupProjects.addAll(tempLdapGroupProjects);

        this.update(ldapGroup);

        if (ldapGroup.isLdapGroupEnabled()) {
            // Remove pending PARs for projects that have been added to LDAP authorization management
            removePendingPARs(addedProjectIds);
        }

        // Creates workflows for changed projects
        for (String projectId : addedProjectIds) {
            try {
                WorkflowUtil.createProjectWorkflow(projectId, ldapGroup, currentUser,true);
            } catch (Exception e) {
                throw new WorkflowException("Failed to create workflow for the project " + projectId, e);
            }
        }
        List<String> deletedProjectIds = ListUtils.subtract(existingProjectIds, newProjectIds);
        for (String projectId : deletedProjectIds) {
            try {
                WorkflowUtil.createProjectWorkflow(projectId, ldapGroup, currentUser, false);
            } catch (Exception e) {
                throw new WorkflowException("Failed to create workflow for the project " + projectId, e);
            }
        }

        if (tempLdapGroupProjects.isEmpty() && ldapGroup.isLdapGroupEnabled()) {
            try {
                log.debug("Disabling the LDAP Group '{}' because there is no project associated", label);
                enable(ldapGroup.getLabel(), false);
            } catch (EmptyProjectsException e) {
                log.error("Failed to disable Ldap Group {}", label, e);
            }
        }

        return ldapGroup;
    }

    private void removePendingPARs(List<String> projectIds) {
        if (projectIds.isEmpty()) {
            return;
        }

        // Confirm project ids are valid and make SQL IN clause list
        String projectList = projectIds.stream()
                .map(pid -> AutoXnatProjectdata.getXnatProjectdatasById(pid, null, false))
                .filter(Objects::nonNull)
                .map(project -> String.format("'%s'", project.getId()))
                .collect(Collectors.joining(","));
        String query = String.format("DELETE FROM xs_par_table WHERE approved IS NULL AND proj_id IN (%s)", projectList);
        try {
            databaseHelper.executeScript(query);
        } catch (Exception e) {
            log.error("Unable to remove pending PARs for projects {}", projectList, e);
        }
    }

    @Override
    public void disableGroupsWithNonEnabledProviders(Set<String> enabledProviderIds) {
        List<LdapGroup> activeGroups = this.findActiveGroups()
                .stream()
                .filter(ldapGroup -> !enabledProviderIds.contains(ldapGroup.getAuthProviderId()))
                .collect(Collectors.toList());

        for (LdapGroup ldapGroup : activeGroups) {
            try {
                enable(ldapGroup.getLabel(), false);
                ldapEmailService.sendDisabledEmail(ldapGroup);
            } catch (NotFoundException e) {
                log.warn("LDAP Group '{}' not found", ldapGroup.getLabel());
            } catch (MessagingException e) {
                log.error("Failed to send disabled email for the LDAP Group '{}'", ldapGroup.getLabel());
            } catch (EmptyProjectsException e) {
                log.warn("Cannot disable the group", e);
            } catch (WorkflowException e) {
                log.error("Error happened while disabling LDAP Group " + ldapGroup.getLabel(), e);
            }
        }
    }
}
