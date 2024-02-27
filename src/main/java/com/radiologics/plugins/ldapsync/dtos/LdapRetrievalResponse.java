package com.radiologics.plugins.ldapsync.dtos;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Builder
public class LdapRetrievalResponse {
    @Builder.Default
    private LdapRetrievalResponseType responseType = LdapRetrievalResponseType.Success;

    private String message;

    private String filter;

    private List<LdapUserSearchResult> ldapUsers;
}
