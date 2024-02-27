package com.radiologics.plugins.ldapsync.entities;

import lombok.*;
import org.hibernate.envers.Audited;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@ToString
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"label"})})
@Audited
public class LdapGroup extends AbstractHibernateEntity {
    private String label;

    private String groupDN;

    private LdapGroupType type;

    private String authProviderId;

    @Builder.Default
    private List<LdapGroupProject> ldapGroupProjects = new ArrayList<>();

    @Builder.Default
    private boolean ldapGroupEnabled = false;

    @Builder.Default
    private String usernameAttrName = "uid";

    @Builder.Default
    private LdapSearchType ldapSearchType = LdapSearchType.User;

    /* Begin - Fields for the "User" search type */
    @Builder.Default
    // By default, Active Directory and OpenLDAP use person.
    private String objectClassName = "person";

    @Builder.Default
    private String userMembershipAttrName = "memberOf";
    /* End - Fields for the "User" search type */

    /* Begin - Fields for the "Group" search type */
    @Builder.Default
    // Commonly, member or uniqueMemberOf
    private String groupMemberAttrName = "member";
    /* End - Fields for the "Group" search type */

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getGroupDN() {
        return groupDN;
    }

    public void setGroupDN(String groupDN) {
        this.groupDN = groupDN;
    }

    @Enumerated(EnumType.STRING)
    public LdapGroupType getType() {
        return type;
    }

    public void setType(LdapGroupType type) {
        this.type = type;
    }

    public String getAuthProviderId() {
        return authProviderId;
    }

    public void setAuthProviderId(String authProviderId) {
        this.authProviderId = authProviderId;
    }

    @OneToMany(
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            targetEntity = LdapGroupProject.class,
            mappedBy = "ldapGroup"
    )
    public List<LdapGroupProject> getLdapGroupProjects() {
        return ldapGroupProjects;
    }

    public void setLdapGroupProjects(List<LdapGroupProject> ldapGroupProjects) {
        this.ldapGroupProjects = ldapGroupProjects;
    }

    public boolean isLdapGroupEnabled() {
        return ldapGroupEnabled;
    }

    public void setLdapGroupEnabled(boolean ldapGroupEnabled) {
        this.ldapGroupEnabled = ldapGroupEnabled;
    }

    public String getUsernameAttrName() {
        return usernameAttrName;
    }

    public void setUsernameAttrName(String usernameAttrName) {
        this.usernameAttrName = usernameAttrName;
    }

    @Enumerated(EnumType.STRING)
    public LdapSearchType getLdapSearchType() {
        return ldapSearchType;
    }

    public void setLdapSearchType(LdapSearchType ldapSearchType) {
        this.ldapSearchType = ldapSearchType;
    }

    public String getObjectClassName() {
        return objectClassName;
    }

    public void setObjectClassName(String objectClassName) {
        this.objectClassName = objectClassName;
    }

    public String getUserMembershipAttrName() {
        return userMembershipAttrName;
    }

    public void setUserMembershipAttrName(String userMembershipAttrName) {
        this.userMembershipAttrName = userMembershipAttrName;
    }

    public String getGroupMemberAttrName() {
        return groupMemberAttrName;
    }

    public void setGroupMemberAttrName(String groupMemberAttrName) {
        this.groupMemberAttrName = groupMemberAttrName;
    }
}
