package com.radiologics.plugins.ldapsync.services.impl;

import com.radiologics.plugins.ldapsync.dtos.*;
import com.radiologics.plugins.ldapsync.entities.LdapGroup;
import com.radiologics.plugins.ldapsync.entities.LdapGroupProject;
import com.radiologics.plugins.ldapsync.entities.LdapGroupType;
import com.radiologics.plugins.ldapsync.entities.LdapSearchType;
import com.radiologics.plugins.ldapsync.exceptions.*;
import com.radiologics.plugins.ldapsync.preferences.LdapSyncSitePreferences;
import com.radiologics.plugins.ldapsync.services.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.nrg.xdat.security.UserGroupI;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xft.security.UserI;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LdapSynchronizationServiceImpl implements LdapSynchronizationService {
    public static final String COLLABORATOR_GROUP_NAME = "collaborator";
    public static final String PROJECT_PARAM = "PROJECT";

    private final UserService userService;
    private final LdapOperationsService ldapOperationsService;
    private final LdapGroupEntityService ldapGroupEntityService;
    private final LdapSyncSitePreferences ldapSyncSitePreferences;
    private final Map<String, LdapTemplate> ldapTemplates;
    private final ScheduledExecutorService executorService;

    public LdapSynchronizationServiceImpl(final UserService userService,
                                          final LdapOperationsService ldapOperationsService,
                                          final LdapGroupEntityService ldapGroupEntityService,
                                          final LdapSyncSitePreferences ldapSyncSitePreferences,
                                          final Map<String, LdapTemplate> ldapTemplates,
                                          final ScheduledExecutorService ldapSyncScheduledExecutorService) {
        this.userService = userService;
        this.ldapOperationsService = ldapOperationsService;
        this.ldapGroupEntityService = ldapGroupEntityService;
        this.ldapSyncSitePreferences = ldapSyncSitePreferences;
        this.ldapTemplates = ldapTemplates;
        this.executorService = ldapSyncScheduledExecutorService;
    }

    private boolean isLdapProviderAvailable(String authProviderId) {
        return ldapTemplates.containsKey(authProviderId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LdapGroupSyncResult synchronizeUserAuthorities(final String label) throws NotFoundException, LdapNetworkException, LdapXnatException, SynchronizationException {
        Optional<LdapGroup> optionalLdapGroup = ldapGroupEntityService.findByLabel(label);
        if (!optionalLdapGroup.isPresent()) {
            throw new NotFoundException("LDAP Group " + label + " not found");
        }
        LdapGroup ldapGroup = optionalLdapGroup.get();

        return synchronizeUserAuthorities(ldapGroup);
    }

    private String resolveGroupDN(final String rawGroupDN, String projectId) {
        return rawGroupDN.replaceAll(String.format("(?i)\\$\\{%s\\}", PROJECT_PARAM), projectId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProjectSyncResult synchronizeUserAuthorities(String ldapGroupLabel, String projectId) throws NotFoundException, LdapNetworkException, LdapXnatException, SynchronizationException {
        Optional<LdapGroup> optionalLdapGroup = ldapGroupEntityService.findByLabel(ldapGroupLabel);
        if (!optionalLdapGroup.isPresent()) {
            throw new NotFoundException("LDAP Group Not Found");
        }
        LdapGroup ldapGroup = optionalLdapGroup.get();
        if (ldapGroup.getLdapGroupProjects().stream()
                .noneMatch(ldapGroupProject -> ldapGroupProject.getProjectId().equals(projectId))) {
            throw new NotFoundException(String.format("Project '%s' is not found in the LDAP Group '%s'",
                    projectId, ldapGroupLabel));
        }

        return synchronizeUserAuthorities(ldapGroup, projectId, Users.getAdminUser());
    }

    private ProjectSyncResult synchronizeUserAuthorities(final LdapGroup ldapGroup,
                                                         final String projectId,
                                                         final UserI adminUser)
            throws LdapNetworkException, LdapXnatException, SynchronizationException {
        String groupDN = ldapGroup.getGroupDN();
        LdapGroupType type = ldapGroup.getType();
        String authProviderId = ldapGroup.getAuthProviderId();
        String usernameAttrName = ldapGroup.getUsernameAttrName();
        LdapSearchType ldapSearchType = ldapGroup.getLdapSearchType();
        String objectClassName = ldapGroup.getObjectClassName();
        String userMembershipAttrName = ldapGroup.getUserMembershipAttrName();
        String groupMemberAttrName = ldapGroup.getGroupMemberAttrName();

        String resolvedGroupDN = groupDN;
        if (type == LdapGroupType.Dynamic) {
            resolvedGroupDN = resolveGroupDN(groupDN, projectId);
        }

        // Retrieving users from LDAP
        List<LdapUserSearchResult> ldapUserSearchResults;
        try {
            ldapUserSearchResults = ldapOperationsService.retrieveLdapUsers(
                    LdapRetrieval.builder()
                            .groupDN(resolvedGroupDN)
                            .authProviderId(authProviderId)
                            .usernameAttrName(usernameAttrName)
                            .ldapSearchType(ldapSearchType)
                            .objectClassName(objectClassName)
                            .userMembershipAttrName(userMembershipAttrName)
                            .groupMemberAttrName(groupMemberAttrName)
                            .build()
            );
            log.info("{} LDAP Users found from LDAP groupDN {}", ldapUserSearchResults.size(), resolvedGroupDN);
        } catch (GroupDNNotFoundException e) {
            log.warn("Group DN {} not found.. All the XNAT's user authorities for the group '{}' will be removed.", resolvedGroupDN, ldapGroup.getLabel());
            ldapUserSearchResults = new ArrayList<>();
        } catch (LdapInvalidArgumentException e) {
            log.warn("Failed to retrieve LDAP users for Group DN " + resolvedGroupDN + ".. All the XNAT's user authorities for the group '" + ldapGroup.getLabel() + "' will be removed.", e);
            ldapUserSearchResults = new ArrayList<>();
        }
        List<String> ldapUsernames = ldapUserSearchResults.stream().map(LdapUserSearchResult::getUsername)
                .filter(Objects::nonNull).collect(Collectors.toList());

        // Retrieve usernames using LDAP UIDs
        List<String> newUsernames = userService.retrieveUsernamesFromLogin(ldapUsernames, authProviderId);
        log.info("{} xdat_usernames out of {} LDAP UIDs found", newUsernames.size(), ldapUsernames.size());
        List<String> missedUsernames = ListUtils.subtract(ldapUsernames, newUsernames);

        List<UserGroupI> userGroups = userService.getUserGroupsByProjectId(projectId);
        if (userGroups == null || userGroups.size() == 0) {
            log.warn("Groups for " + projectId + " not found");
            return ProjectSyncResult.builder().projectId(projectId).missingAccounts(ldapUsernames).build();
        }
        log.info("{} User Groups in the XNAT Project {} found", userGroups.size(), projectId);

        // Retrieving existing users
        Map<String, List<String>> existingUsernamesByGroupIdMap = userService.getUsernamesByUserGroups(userGroups);

        PermissionDiff permissionDiff = userService.diff(existingUsernamesByGroupIdMap, newUsernames);
        if (isDiffEmpty(permissionDiff)) {
            log.info("No users to update for the Group {}", projectId);
            return ProjectSyncResult.builder()
                    .projectId(projectId)
                    .missingAccounts(missedUsernames)
                    .addedOrExistingAccounts(newUsernames).build();
        }

        List<String> removedUsernames = removeUsernames(projectId, permissionDiff.getUsernamesMapByGroupIdToRemove(), adminUser);
        List<String> addedUsernames = addUsernames(projectId, permissionDiff.getUsernamesToAdd(), adminUser);

        return ProjectSyncResult.builder()
                .projectId(projectId)
                .missingAccounts(missedUsernames)
                .addedOrExistingAccounts(ListUtils.union(addedUsernames, permissionDiff.getUsernamesToKeep()))
                .removedAccounts(removedUsernames)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LdapGroupSyncResult synchronizeUserAuthorities(final LdapGroup ldapGroup) throws SynchronizationException, LdapNetworkException, LdapXnatException {
        UserI adminUser = Users.getAdminUser();

        String label = ldapGroup.getLabel();
        String groupDN = ldapGroup.getGroupDN();
        String authProviderId = ldapGroup.getAuthProviderId();
        List<String> projectIds = ldapGroup.getLdapGroupProjects().stream()
                .map(LdapGroupProject::getProjectId)
                .collect(Collectors.toList());

        log.info("Checking the LDAP Group '{}'", label);

        if (StringUtils.isEmpty(groupDN)) {
            return LdapGroupSyncResult.builder().resultType(SynchronizationResultType.EndedWithNoUpdate).build();
        }
        if (StringUtils.isEmpty(authProviderId)) {
            return LdapGroupSyncResult.builder().resultType(SynchronizationResultType.EndedWithNoUpdate).build();
        }
        if (!isLdapProviderAvailable(authProviderId)) {
            log.warn("Skipped the LDAP Group '{}' because its auth provider '{}' is not available", label, authProviderId);
        }
        if (!ldapGroup.isLdapGroupEnabled()) {
            log.info("Skipping. The synchronization for Group {} is not enabled.", label);
            return LdapGroupSyncResult.builder().resultType(SynchronizationResultType.SyncNotEnabled).build();
        }

        List<ProjectSyncResult> projectSyncResults = new ArrayList<>();
        for (String projectId : projectIds) {
           projectSyncResults.add(synchronizeUserAuthorities(ldapGroup, projectId, adminUser));
        }

        return LdapGroupSyncResult.builder()
                .resultType(SynchronizationResultType.EndedWithUpdate)
                .projectSyncResults(projectSyncResults)
                .build();
    }

    private boolean isDiffEmpty(PermissionDiff permissionDiff) {
        Map<String, List<String>> usernamesMapByGroupIdToRemove = permissionDiff.getUsernamesMapByGroupIdToRemove();
        List<String> usernamesToAdd = permissionDiff.getUsernamesToAdd();
        return usernamesMapByGroupIdToRemove.keySet().size() == 0 && usernamesToAdd.size() == 0;
    }

    private List<String> removeUsernames(String projectId, Map<String, List<String>> usernamesMapByGroupIdToRemove, UserI adminUser) throws SynchronizationException {
        List<String> removedUsernames = new ArrayList<>();
        int removedCount = 0;
        for (String groupId : usernamesMapByGroupIdToRemove.keySet()) {
            List<String> usernamesToRemove = usernamesMapByGroupIdToRemove.get(groupId);

            for (String username : usernamesToRemove) {
                try {
                    userService.removeUserFromGroup(groupId, username, adminUser);
                    log.info("Removed {} from {}", username, groupId);

                    removedUsernames.add(username);
                    removedCount++;
                } catch (UserNotFoundException e) {
                    log.warn("Username {} not found", username);
                } catch (Exception e) {
                    String msg = "Failed to remove a group (" + projectId + ") from user (" + username + ")";
                    log.error(msg, e);
                    throw new SynchronizationException(msg, e);
                }
            }
        }

        if (removedCount > 0) {
            log.info("{} permissions removed for {}", removedCount, projectId);
        } else {
            log.info("Nothing has been removed");
        }

        return removedUsernames;
    }

    private List<String> addUsernames(String projectId, List<String> usernamesToAdd, UserI adminUser) throws SynchronizationException {
        Set<String> addedUsernames = new TreeSet<>();

        int addedCount = 0;
        for (String username : usernamesToAdd) {
            try {
                userService.addUserToGroup(projectId, COLLABORATOR_GROUP_NAME, username, adminUser);
                log.info("Added User {} to {}", username, projectId);

                addedUsernames.add(username);
                addedCount++;
            } catch (UserNotFoundException e) {
                log.info("Username {} not found", username);
            } catch (Exception e) {
                String msg = "Failed to add user " + username + " to " + projectId;
                log.error(msg, e);
                throw new SynchronizationException(msg, e);
            }
        }

        if (addedCount > 0) {
            log.info("{} permissions added for {}", addedCount, projectId);
        } else {
            log.info("Nothing has been added for {}", projectId);
        }

        return new ArrayList<>(addedUsernames);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void synchronize() {
        log.debug("Initiating LDAP synchronization");

        Collection<LdapGroup> ldapGroups = ldapGroupEntityService.findActiveGroups();
        if (ldapGroups.isEmpty()) {
            log.debug("No LDAP Groups to synchronize");
            return;
        }

        Set<LdapGroup> unavailableGroups = ldapGroups.stream()
                .filter(ldapGroup -> !isLdapProviderAvailable(ldapGroup.getAuthProviderId()))
                .collect(Collectors.toSet());
        if (!unavailableGroups.isEmpty()) {
            String unavailableGroupsStr = unavailableGroups.stream()
                    .map(ldapGroup -> String.format("%s (%s)", ldapGroup.getLabel(), ldapGroup.getAuthProviderId()))
                    .collect(Collectors.joining(", "));
            log.warn("The following {} LDAP Groups cannot be synced because their auth providers are not available: {}",
                    unavailableGroups.size(), unavailableGroupsStr);
        }

        List<LdapGroup> ldapGroupsToSync = ldapGroups.stream()
                .filter(ldapGroup -> !unavailableGroups.contains(ldapGroup) &&
                        ldapGroup.isLdapGroupEnabled() && !ldapGroup.getLdapGroupProjects().isEmpty())
                .collect(Collectors.toList());
        if (ldapGroupsToSync.isEmpty()) {
            return;
        }

        log.debug("Found {} enabled LDAP Group(s)", ldapGroups.size());
        long syncFrequency = TimeUnit.MINUTES.toMillis(ldapSyncSitePreferences.getLdapSynchronizationRepeat());
        long rate = (long) Math.floor((double) syncFrequency / ldapGroupsToSync.size());
        for (int i = 0; i < ldapGroupsToSync.size(); i++) {
            LdapGroup ldapGroup = ldapGroupsToSync.get(i);
            executorService.schedule(() -> {
                log.debug("Synchronizing LDAP Group {}", ldapGroup.getLabel());
                try {
                    synchronizeUserAuthorities(ldapGroup);
                } catch (SynchronizationException | LdapNetworkException | LdapXnatException e) {
                    log.error("Error while syncing {} ({})", ldapGroup.getLabel(), ldapGroup.getGroupDN(), e);
                }
            }, rate * i, TimeUnit.MILLISECONDS);
        }
        log.debug("Finished submitting synchronization jobs");
    }
}
