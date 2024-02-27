package com.radiologics.plugins.ldapsync.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ProjectSyncResult {
    private String projectId;

    @JsonProperty("missing-accounts")
    @Builder.Default
    private List<String> missingAccounts = new ArrayList<>();

    @JsonProperty("added-or-existing")
    @Builder.Default
    private List<String> addedOrExistingAccounts = new ArrayList<>();

    @JsonProperty("removed")
    @Builder.Default
    private List<String> removedAccounts = new ArrayList<>();
}
