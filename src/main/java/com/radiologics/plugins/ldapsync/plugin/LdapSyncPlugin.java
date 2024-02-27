package com.radiologics.plugins.ldapsync.plugin;

import com.radiologics.plugins.ldapsync.services.LdapProviderService;
import lombok.extern.slf4j.Slf4j;
import org.nrg.framework.annotations.XnatPlugin;
import org.nrg.xnatx.plugins.auth.ldap.provider.XnatLdapAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Lazy;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@XnatPlugin(value = "ldapSyncPlugin",
        name = "LDAP Synchronization Plugin",
        logConfigurationFile = "com/radiologics/plugins/ldapsync/plugin/ldap-sync-logback.xml",
        entityPackages = "com.radiologics.plugins.ldapsync.entities"
)
@ComponentScan(value = {"com.radiologics.plugins.ldapsync"})
@EnableScheduling
public class LdapSyncPlugin {
    final LdapProviderService ldapProviderService;

    public LdapSyncPlugin(LdapProviderService ldapProviderService) {
        this.ldapProviderService = ldapProviderService;
    }

    @Bean
    @Lazy
    public Map<String, LdapTemplate> ldapTemplates() {
        Map<String, XnatLdapAuthenticationProvider> ldapProvidersMap = ldapProviderService.getLdapProviders();
        Map<String, LdapTemplate> ldapTemplatesMap = new HashMap<>();

        for (String providerId : ldapProvidersMap.keySet()) {
            XnatLdapAuthenticationProvider ldapProvider = ldapProvidersMap.get(providerId);

            try {
                ldapTemplatesMap.put(providerId, ldapProviderService.constructLdapTemplate(ldapProvider));
            } catch (Exception e) {
                log.warn("provider " + providerId + " is skipped", e);
            }
        }

        return ldapTemplatesMap;
    }

    @Bean(name = "ldapSyncScheduledExecutorService")
    public ScheduledExecutorService ldapSyncScheduledExecutorService() {
        ThreadPoolExecutorFactoryBean tBean = new ThreadPoolExecutorFactoryBean();
        tBean.setThreadNamePrefix("ldap-sync-");
        return Executors.newScheduledThreadPool(5, tBean);
    }
}
