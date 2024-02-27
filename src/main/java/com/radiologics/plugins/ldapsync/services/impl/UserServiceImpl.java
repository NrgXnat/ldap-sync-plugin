package com.radiologics.plugins.ldapsync.services.impl;

import com.radiologics.plugins.ldapsync.dtos.PermissionDiff;
import com.radiologics.plugins.ldapsync.exceptions.LdapXnatException;
import com.radiologics.plugins.ldapsync.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.nrg.xdat.exceptions.UsernameAuthMappingNotFoundException;
import org.nrg.xdat.security.UserGroupI;
import org.nrg.xdat.security.UserGroupServiceI;
import org.nrg.xdat.security.helpers.Groups;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xdat.services.XdatUserAuthService;
import org.nrg.xft.security.UserI;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static org.nrg.xdat.services.XdatUserAuthService.LDAP;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    final XdatUserAuthService userAuthService;
    final UserGroupServiceI userGroupService;
    final JdbcTemplate jdbcTemplate;
    final XdatUserAuthService xdatUserAuthService;

    public UserServiceImpl(final XdatUserAuthService userAuthService, final UserGroupServiceI userGroupService, final JdbcTemplate jdbcTemplate, final XdatUserAuthService xdatUserAuthService) {
        this.userAuthService = userAuthService;
        this.userGroupService = userGroupService;
        this.jdbcTemplate = jdbcTemplate;
        this.xdatUserAuthService = xdatUserAuthService;
    }

    private String constructGroupId(String projectId, String groupName) {
        return projectId + "_" + groupName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PermissionDiff diff(Map<String, List<String>> existingUsernamesByGroupIdMap, List<String> newUsernames) {
        Map<String, List<String>> usernamesMapByGroupIdToRemove = new HashMap<>();
        List<String> usernamesToAdd = new ArrayList<>();
        List<String> usernamesToKeep = new ArrayList<>();
        Set<String> existingUsernamesSet = new HashSet<>();

        for (String groupId : existingUsernamesByGroupIdMap.keySet()) {
            log.info("Checking the User Group {}", groupId);
            List<String> existingUsernames = existingUsernamesByGroupIdMap.get(groupId);
            List<String> usernamesToRemove = new ArrayList<>();

            log.info("{} existing users found in the User Group {}", existingUsernames.size(), groupId);
            for (String username : existingUsernames) {
                log.info("Found {}", username);
                existingUsernamesSet.add(username);
                if (newUsernames.contains(username)) {
                    continue;
                }

                if (!usernamesMapByGroupIdToRemove.containsKey(groupId)) {
                    log.info("Group {} added to usernamesMapByGroupIdToRemove", groupId);
                    usernamesMapByGroupIdToRemove.put(groupId, usernamesToRemove);
                }

                log.info("{} will be removed from {}", username, groupId);
                usernamesToRemove.add(username);
            }
        }

        for (String username : newUsernames) {
            if (existingUsernamesSet.contains(username)) {
                usernamesToKeep.add(username);
            } else {
                log.info("{} will be added to collaborator", username);
                usernamesToAdd.add(username);
            }
        }

        return new PermissionDiff(usernamesMapByGroupIdToRemove, usernamesToAdd, usernamesToKeep);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeUserFromGroup(String groupId, String username, UserI adminUser) throws Exception {
        Groups.removeUserFromGroup(Users.getUser(username), adminUser, groupId, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addUserToGroup(String projectId, String groupName, String username, UserI adminUser) throws Exception {
        String groupId = constructGroupId(projectId, groupName);
        addUserToGroup(groupId, username, adminUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addUserToGroup(String groupId, String username, UserI adminUser) throws Exception {
        Groups.addUserToGroup(groupId, Users.getUser(username), adminUser, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserGroupI> getUserGroupsByProjectId(String projectId) throws LdapXnatException {
        try {
            return Groups.getGroupsByTag(projectId);
        } catch (Exception e) {
            throw new LdapXnatException("Failed to retrieve user groups for the project " + projectId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, List<String>> getUsernamesByUserGroups(List<UserGroupI> userGroups) {
        Map<String, List<String>> usernamesMap = new HashMap<>();

        for (UserGroupI userGroup : userGroups) {
            String groupId = userGroup.getId();
            List<String> usernames = null;

            try {
                usernames = getUsernamesByGroupId(groupId);
            } catch (UserNotFoundException e) {
            }

            usernamesMap.put(groupId, usernames);
        }

        return usernamesMap;
    }

    private List<String> getUsernamesByGroupId(String groupId) throws UserNotFoundException {
//        String query = String.format(" select login from xdat_user_groupid g left outer join xdat_user u on g.groups_groupid_xdat_user_xdat_user_id = u.xdat_user_id where u.enabled = 1 and u.verified = 1 and groupid = '%s'", groupId);
//        return jdbcTemplate.query(query, new CustomerRowMapper());
        List<String> usernames = userGroupService.getUserIdsForGroup(groupId);

        return usernames.stream()
                .filter(username -> {
                    try {
                        Map<String, UserGroupI> userGroups = Groups.getGroupsForUser(username);
                        return userGroups.containsKey(groupId);
                    } catch (UserNotFoundException e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> retrieveUsernamesFromLogin(List<String> loginIds, String authProviderId) {
        Set<UserI> usersSet = new HashSet<>();

        for (String loginId : loginIds) {
            UserI user = null;
            try {
                user = xdatUserAuthService.getUserDetailsByNameAndAuth(loginId, LDAP, authProviderId);
            } catch (UsernameAuthMappingNotFoundException e) {
                log.info("cannot retrieve username using {}", loginId);
            }

            if (user != null) {
                usersSet.add(user);
            }
        }

        return usersSet.stream().map(user -> user.getUsername()).collect(Collectors.toList());
    }
}
