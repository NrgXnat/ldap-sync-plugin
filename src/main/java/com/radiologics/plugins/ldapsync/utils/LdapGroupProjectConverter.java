package com.radiologics.plugins.ldapsync.utils;

import com.radiologics.plugins.ldapsync.dtos.TransferLdapGroupProject;
import com.radiologics.plugins.ldapsync.entities.LdapGroupProject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LdapGroupProjectConverter {
    public static TransferLdapGroupProject toDto(LdapGroupProject ldapGroupProject) {
        return new TransferLdapGroupProject(LdapGroupConverter.toDto(ldapGroupProject.getLdapGroup()), ldapGroupProject.getProjectId());
    }
}
