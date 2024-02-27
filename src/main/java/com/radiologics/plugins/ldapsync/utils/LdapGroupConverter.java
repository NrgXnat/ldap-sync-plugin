package com.radiologics.plugins.ldapsync.utils;

import com.radiologics.plugins.ldapsync.dtos.TransferLdapGroup;
import com.radiologics.plugins.ldapsync.entities.LdapGroup;
import com.radiologics.plugins.ldapsync.entities.LdapGroupProject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LdapGroupConverter {
    public static TransferLdapGroup toDto(LdapGroup ldapGroup) {
        return TransferLdapGroup.builder()
                .label(ldapGroup.getLabel())
                .groupDN(ldapGroup.getGroupDN())
                .type(ldapGroup.getType())
                .authProviderId(ldapGroup.getAuthProviderId())
                .projectIds( ldapGroup.getLdapGroupProjects().stream()
                        .map(LdapGroupProject::getProjectId).collect(Collectors.toList()))
                .ldapGroupEnabled(ldapGroup.isLdapGroupEnabled())
                .usernameAttrName(ldapGroup.getUsernameAttrName())
                .ldapSearchType(ldapGroup.getLdapSearchType())
                .objectClassName(ldapGroup.getObjectClassName())
                .userMembershipAttrName(ldapGroup.getUserMembershipAttrName())
                .groupMemberAttrName(ldapGroup.getGroupMemberAttrName())
                .build();
    }
}
