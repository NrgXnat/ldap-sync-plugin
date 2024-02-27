package com.radiologics.plugins.ldapsync.dtos;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionDiffTest {
    @Test
    public void testPermissionDiffDto() {
        List<String> usernamesToRemove = Arrays.asList("test-1", "test-2");
        Map<String, List<String>> usernamesMapByGroupIdToRemove = new HashMap<>();
        usernamesMapByGroupIdToRemove.put("project1_collaborator", usernamesToRemove);
        List<String> usernamesToAdd = Arrays.asList("test-3");
        List<String> usernamesToKeep = Arrays.asList("test-4");

        PermissionDiff permissionDiff = new PermissionDiff(usernamesMapByGroupIdToRemove, usernamesToAdd, usernamesToKeep);

        assertThat(permissionDiff.getUsernamesMapByGroupIdToRemove()).isEqualTo(usernamesMapByGroupIdToRemove);
        assertThat(permissionDiff.getUsernamesToAdd()).isEqualTo(usernamesToAdd);

        permissionDiff.setUsernamesMapByGroupIdToRemove(usernamesMapByGroupIdToRemove);
        permissionDiff.setUsernamesToAdd(usernamesToAdd);

        assertThat(permissionDiff.getUsernamesMapByGroupIdToRemove()).isEqualTo(usernamesMapByGroupIdToRemove);
        assertThat(permissionDiff.getUsernamesToAdd()).isEqualTo(usernamesToAdd);
    }
}
