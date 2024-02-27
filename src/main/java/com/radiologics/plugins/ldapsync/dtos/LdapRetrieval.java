package com.radiologics.plugins.ldapsync.dtos;

import com.radiologics.plugins.ldapsync.entities.LdapSearchType;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Builder
public class LdapRetrieval {
    private String groupDN;

    private String authProviderId;

    private String usernameAttrName;

    private LdapSearchType ldapSearchType;

    private String objectClassName;

    private String userMembershipAttrName;

    private String groupMemberAttrName;

    public String getSearchFilter() {
        switch (ldapSearchType) {
            case User:
                return String.format("(&%s(%s=%s))", getUserSearchFilter(), userMembershipAttrName, groupDN);
            case Group:
                return String.format("(%s=*)", groupMemberAttrName);
        }
        throw new RuntimeException("Invalid LdapSearchType");
    }

    public String getUserSearchFilter() {
        return String.format("(objectClass=%s)", objectClassName);
    }

    public String getSearchFilterInfo() {
        switch (ldapSearchType) {
            case User:
                return getSearchFilter();
            case Group:
                return String.format("%s in DN %s with %s", getSearchFilter(), groupDN, getUserSearchFilter());
        }
        throw new RuntimeException("Invalid LdapSearchType");
    }
}
