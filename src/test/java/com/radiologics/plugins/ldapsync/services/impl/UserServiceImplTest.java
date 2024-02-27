package com.radiologics.plugins.ldapsync.services.impl;

import com.radiologics.plugins.ldapsync.dtos.PermissionDiff;
import com.radiologics.plugins.ldapsync.exceptions.LdapXnatException;
import com.radiologics.plugins.ldapsync.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nrg.xdat.security.UserGroupI;
import org.nrg.xdat.security.UserGroupServiceI;
import org.nrg.xdat.security.helpers.Groups;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xdat.services.XdatUserAuthService;
import org.nrg.xft.security.UserI;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {
    @Mock
    XdatUserAuthService userAuthService;
    @Mock
    UserGroupServiceI userGroupService;
    @Mock
    JdbcTemplate jdbcTemplate;
    @Mock
    XdatUserAuthService xdatUserAuthService;

    @Mock
    UserI mockedAdmin;

    UserService userService;

    @BeforeEach
    public void setUpMock() {
        userService = new UserServiceImpl(userAuthService, userGroupService, jdbcTemplate, xdatUserAuthService);
    }

    @Test
    public void testDiff() {
        Map<String, List<String>> existingUsernamesByGroupIdMap = new HashMap<>();
        List<String> collaboratorUsernames = new ArrayList<>();
        collaboratorUsernames.add("user-1");
        List<String> ownerUsernames = new ArrayList<>();
        collaboratorUsernames.add("user-2");
        existingUsernamesByGroupIdMap.put("collaborators", collaboratorUsernames);
        existingUsernamesByGroupIdMap.put("owners", ownerUsernames);

        List<String> newUsernames = new ArrayList<>();
        newUsernames.add("user-2");
        newUsernames.add("user-3");

        PermissionDiff permissionDiff = userService.diff(existingUsernamesByGroupIdMap, newUsernames);
        Map<String, List<String>> usernamesMapByGroupIdToRemove = permissionDiff.getUsernamesMapByGroupIdToRemove();
        List<String> usernamesToAdd = permissionDiff.getUsernamesToAdd();

        assertThat(usernamesMapByGroupIdToRemove.size()).isEqualTo(1);
        assertThat(usernamesToAdd.size()).isEqualTo(1);
    }

    @Test
    public void testRemoveUserFromGroup() throws Exception {
        try (MockedStatic<Groups> mockedGroups = mockStatic(Groups.class);
            MockedStatic<Users> mockedUsers = mockStatic(Users.class)
        ) {
            mockedUsers.when(() -> Users.getUser(anyString())).thenReturn(Mockito.mock(UserI.class));
            userService.removeUserFromGroup("groupId", "username", mockedAdmin);

            mockedGroups.verify(
                    () -> Groups.removeUserFromGroup(any(UserI.class), any(UserI.class), anyString(), eq(null)),
                    times(1));

            mockedUsers.verify(
                    () -> Users.getUser(anyString()),
                    times(1));
        }
    }

    @Test
    public void testAddUserToGroup() throws Exception {
        try (MockedStatic<Groups> mockedGroups = mockStatic(Groups.class);
             MockedStatic<Users> mockedUsers = mockStatic(Users.class)
        ) {

            mockedUsers.when(() -> Users.getUser(anyString())).thenReturn(Mockito.mock(UserI.class));
            userService.addUserToGroup("projectId", "groupId", "username", mockedAdmin);

            mockedGroups.verify(
                    () -> Groups.addUserToGroup(anyString(), any(UserI.class), any(UserI.class), eq(null)),
                    times(1));

            mockedUsers.verify(
                    () -> Users.getUser(anyString()),
                    times(1));
        }
    }

    @Test
    public void testGetUserGroupsByProjectId() throws LdapXnatException {
        try (MockedStatic<Groups> mockedGroups = mockStatic(Groups.class)) {

            List<UserGroupI> userGroups = Arrays.asList(Mockito.mock(UserGroupI.class));
            mockedGroups.when(() -> Groups.getGroupsByTag(anyString())).thenReturn(userGroups);
            List<UserGroupI> returnedUserGroups = userService.getUserGroupsByProjectId("projectId");

            assertThat(returnedUserGroups).isEqualTo(userGroups);
        }
    }

    @Test
    public void testGetUserGroupsByProjectIdThrowsException() throws LdapXnatException {
        try (MockedStatic<Groups> mockedGroups = mockStatic(Groups.class)) {

            List<UserGroupI> userGroups = Arrays.asList(Mockito.mock(UserGroupI.class));
            mockedGroups.when(() -> Groups.getGroupsByTag(anyString())).thenThrow(Exception.class);

            assertThrows(LdapXnatException.class, () -> {
                userService.getUserGroupsByProjectId("projectId");
            });
        }
    }

    @Test
    public void testGetUsernamesMapByUserGroups() throws UserNotFoundException {
        try (MockedStatic<Groups> mockedGroups = mockStatic(Groups.class)) {
            UserGroupI mockedUserGroup = Mockito.mock(UserGroupI.class);
            when(mockedUserGroup.getId()).thenReturn("groupId");
            List<UserGroupI> userGroups = Arrays.asList(mockedUserGroup);

            when(userGroupService.getUserIdsForGroup(anyString())).thenReturn(Arrays.asList("user-1"));

            Map<String, UserGroupI> userGroupMap = new HashMap<>();
            userGroupMap.put("groupId", Mockito.mock(UserGroupI.class));
            mockedGroups.when(() -> Groups.getGroupsForUser("user-1")).thenReturn(userGroupMap);

            assertThat(userService.getUsernamesByUserGroups(userGroups).size()).isEqualTo(1);
        }
    }

    @Test
    public void testGetUsernamesMapByUserGroupsThrowsUserNotFound() throws UserNotFoundException {
        try (MockedStatic<Groups> mockedGroups = mockStatic(Groups.class)) {
            UserGroupI mockedUserGroup = Mockito.mock(UserGroupI.class);
            when(mockedUserGroup.getId()).thenReturn("groupId");
            List<UserGroupI> userGroups = Arrays.asList(mockedUserGroup);

            when(userGroupService.getUserIdsForGroup(anyString())).thenThrow(UserNotFoundException.class);

            Map<String, UserGroupI> userGroupMap = new HashMap<>();
            userGroupMap.put("groupId", Mockito.mock(UserGroupI.class));
            mockedGroups.when(() -> Groups.getGroupsForUser("user-1")).thenThrow(UserNotFoundException.class);

            Map<String, List<String>> returnedUsernamesMap  = userService.getUsernamesByUserGroups(userGroups);

            assertThat(returnedUsernamesMap.size()).isEqualTo(1);
        }
    }
}
