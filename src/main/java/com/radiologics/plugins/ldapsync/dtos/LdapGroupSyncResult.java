package com.radiologics.plugins.ldapsync.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Setter
@Getter
@Builder
public class LdapGroupSyncResult {
    private SynchronizationResultType resultType;

    @Builder.Default
    private List<ProjectSyncResult> projectSyncResults = new ArrayList<>();
}
