package com.radiologics.plugins.ldapsync.entities;

import lombok.*;
import org.hibernate.envers.Audited;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"projectId"})})
@Audited
public class LdapGroupProject extends  AbstractHibernateEntity {
    private LdapGroup ldapGroup;

    private String projectId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ldapGroupEntityId")
    public LdapGroup getLdapGroup() {
        return ldapGroup;
    }

    public void setLdapGroup(LdapGroup ldapGroup) {
        this.ldapGroup = ldapGroup;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
