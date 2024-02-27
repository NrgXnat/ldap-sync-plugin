package com.radiologics.plugins.ldapsync.configurations;

import com.radiologics.plugins.ldapsync.repositories.LdapGroupRepository;
import org.nrg.framework.test.OrmTestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({OrmTestConfiguration.class})
public class RepositoryTestConfig {
    @Bean
    public LdapGroupRepository ldapGroupRepository() {
        return new LdapGroupRepository();
    }
}
