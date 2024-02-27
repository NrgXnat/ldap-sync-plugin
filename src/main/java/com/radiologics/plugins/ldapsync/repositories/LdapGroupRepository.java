package com.radiologics.plugins.ldapsync.repositories;

import com.radiologics.plugins.ldapsync.entities.LdapGroup;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.springframework.stereotype.Repository;

@Repository
public class LdapGroupRepository extends AbstractHibernateDAO<LdapGroup> {
}
