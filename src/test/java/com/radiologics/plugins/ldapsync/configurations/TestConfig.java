package com.radiologics.plugins.ldapsync.configurations;

import org.mockito.Mockito;
import org.nrg.framework.test.OrmTestConfiguration;
import org.nrg.prefs.services.NrgPreferenceService;
import org.nrg.xdat.daos.XdatUserAuthDAO;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xdat.services.impl.hibernate.HibernateXdatUserAuthService;
import org.nrg.xnat.security.XnatProviderManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({OrmTestConfiguration.class})
public class TestConfig {

    @Bean
    public SiteConfigPreferences preferences() {
        return Mockito.mock(SiteConfigPreferences.class);
    }

    @Bean
    public HibernateXdatUserAuthService xdatUserAuthService() {
        return Mockito.mock(HibernateXdatUserAuthService.class);
    }

    @Bean
    public NrgPreferenceService nrgPreferenceService() {
        return Mockito.mock(NrgPreferenceService.class);
    }

    @Bean
    public UserManagementServiceI userManagementServiceI() {
        return Mockito.mock(UserManagementServiceI.class);
    }

    @Bean
    public XdatUserAuthDAO xdatUserAuthDAO() {
        return Mockito.mock(XdatUserAuthDAO.class);
    }

    @Bean
    public XnatProviderManager xnatProviderManager() {
        return Mockito.mock(XnatProviderManager.class);
    }
}
