package com.radiologics.plugins.ldapsync.tasks.initializations;

import com.radiologics.plugins.ldapsync.services.LdapProviderService;
import lombok.extern.slf4j.Slf4j;
import org.nrg.xnat.initialization.tasks.AbstractInitializingTask;
import org.nrg.xnatx.plugins.auth.ldap.provider.XnatLdapAuthenticationProvider;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LdapServerSetUpChecker extends AbstractInitializingTask {
    static final String TASK_NAME = "Check that a LDAP server is defined";

    final LdapProviderService ldapProviderService;

    public LdapServerSetUpChecker(LdapProviderService ldapProviderService) {
        this.ldapProviderService = ldapProviderService;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    protected void callImpl() {
        log.info("Checking LDAP server is properly set up");

        Map<String, XnatLdapAuthenticationProvider> ldapProviders = ldapProviderService.getLdapProviders();

        if (ldapProviders == null || ldapProviders.size() == 0) {
            log.warn("No LDAP provider found");
        } else {;
            log.info("LDAP providers {} found", ldapProviders.keySet().stream().collect(Collectors.joining(", ", "[", "]")));
        }
    }
}
