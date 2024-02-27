package com.radiologics.plugins.ldapsync.dtos;

import com.radiologics.plugins.ldapsync.entities.LdapGroupType;
import com.radiologics.plugins.ldapsync.entities.LdapSearchType;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Builder
public class TransferLdapGroup {
    private String label;

    private String groupDN;
    
    private LdapGroupType type;

    private String authProviderId;

    private List<String> projectIds;

    private boolean ldapGroupEnabled;

    private String usernameAttrName;

    private LdapSearchType ldapSearchType;

    private String objectClassName;

    private String userMembershipAttrName;

    private String groupMemberAttrName;
}
