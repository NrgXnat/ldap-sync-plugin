package com.radiologics.plugins.ldapsync.dtos;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class LdapUserSearchResult {
    private String cn;

    private String sn;

    private String username;
}
