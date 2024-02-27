package com.radiologics.plugins.ldapsync.dtos;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Builder
public class TransferLdapGroupProject {
    private TransferLdapGroup ldapGroup;

    private String projectId;
}
