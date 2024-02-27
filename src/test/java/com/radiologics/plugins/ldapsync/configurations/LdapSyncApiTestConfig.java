package com.radiologics.plugins.ldapsync.configurations;

import com.radiologics.plugins.ldapsync.preferences.LdapSyncSitePreferences;
import com.radiologics.plugins.ldapsync.rest.LdapSyncApi;
import com.radiologics.plugins.ldapsync.services.*;
import org.mockito.Mockito;
import org.nrg.config.services.ConfigService;
import org.nrg.prefs.services.NrgPreferenceService;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.authentication.TestingAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Map;

@Configuration
@EnableWebMvc
@EnableWebSecurity
@Import({RestApiTestConfig.class})
public class LdapSyncApiTestConfig extends WebSecurityConfigurerAdapter {
    @Bean
    public LdapSyncApi ldapSyncApi(final UserManagementServiceI userManagementService,
                                   final RoleHolder roleHolder,
                                   final LdapSynchronizationService ldapSynchronizationService,
                                   final LdapOperationsService ldapService,
                                   final LdapGroupProjectEntityService ldapGroupProjectEntityService) {
        return new LdapSyncApi(userManagementService, roleHolder, ldapSynchronizationService, ldapService, ldapGroupProjectEntityService);
    }

    @Bean
    public LdapSynchronizationService ldapSynchronizationService() {
        return Mockito.mock(LdapSynchronizationService.class);
    }

    @Bean
    public UserService userService() {
        return Mockito.mock(UserService.class);
    }

    @Bean
    public LdapOperationsService ldapService() {
        return Mockito.mock(LdapOperationsService.class);
    }

    @Bean
    public LdapGroupEntityService ldapGroupEntityService() {
        return Mockito.mock(LdapGroupEntityService.class);
    }

    @Bean
    public Map<String, LdapTemplate> ldapTemplatesMap() {
        return Mockito.mock(Map.class);
    }

    @Bean
    public LdapSyncSitePreferences ldapSyncSitePreferences() {
        return Mockito.mock(LdapSyncSitePreferences.class);
    }

    @Bean
    public NrgPreferenceService nrgPreferenceService() {
        return Mockito.mock(NrgPreferenceService.class);
    }

    @Bean
    public ConfigService configService() {
        return Mockito.mock(ConfigService.class);
    }

    @Bean
    public LdapGroupProjectEntityService ldapGroupProjectEntityService() {
        return Mockito.mock(LdapGroupProjectEntityService.class);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(new TestingAuthenticationProvider());
    }
}
