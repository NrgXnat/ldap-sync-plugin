package com.radiologics.plugins.ldapsync.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Setter
@Getter
public class PermissionDiff {
    private Map<String, List<String>> usernamesMapByGroupIdToRemove;

    private List<String> usernamesToAdd;

    private List<String> usernamesToKeep;
}
