package com.radiologics.plugins.ldapsync.dtos;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
@Builder
public class LdapSyncSitePrefs {
    private long ldapSynchronizationRepeat;
}
