package com.radiologics.plugins.ldapsync.preferences;

import com.google.common.collect.ImmutableMap;
import com.radiologics.plugins.ldapsync.exceptions.LdapInvalidArgumentException;
import com.radiologics.plugins.ldapsync.services.LdapGroupEntityService;
import com.radiologics.plugins.ldapsync.services.LdapProviderService;
import lombok.extern.slf4j.Slf4j;
import org.nrg.xdat.security.user.XnatUserProvider;
import org.nrg.xnat.event.listeners.methods.AbstractXnatPreferenceHandlerMethod;
import org.nrg.xnatx.plugins.auth.ldap.provider.XnatLdapAuthenticationProvider;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class AuthProvidersHandlerMethod extends AbstractXnatPreferenceHandlerMethod  {
    private static final String ENABLED_PROVIDERS = "enabledProviders";

    final LdapProviderService ldapProviderService;
    final Map<String, LdapTemplate> ldapTemplates;
    final LdapGroupEntityService ldapGroupEntityService;

    public AuthProvidersHandlerMethod(final XnatUserProvider primaryAdminUserProvider, LdapProviderService ldapProviderService, Map<String, LdapTemplate> ldapTemplates, LdapGroupEntityService ldapGroupEntityService) {
        super(primaryAdminUserProvider, ENABLED_PROVIDERS);
        this.ldapProviderService = ldapProviderService;
        this.ldapTemplates = ldapTemplates;
        this.ldapGroupEntityService = ldapGroupEntityService;
    }

    @Override
    protected void handlePreferenceImpl(final String preference, final String value) {
        log.info("New enabled providers are {}.", value);

        Map<String, XnatLdapAuthenticationProvider> ldapProvidersMap = ImmutableMap.copyOf(ldapProviderService.getLdapProviders());

        for (String providerId : ldapProvidersMap.keySet()) {
            if (!ldapTemplates.containsKey(providerId)) {
                log.info("Adding the LDAP provider {}", providerId);
                try {
                    ldapTemplates.put(providerId, ldapProviderService.constructLdapTemplate(ldapProvidersMap.get(providerId)));
                } catch (LdapInvalidArgumentException e) {
                    log.error("Failed to construct ldap template for " + providerId, e);
                }
            }
        }

        for (String providerId : ldapTemplates.keySet()) {
            if (!ldapProvidersMap.containsKey(providerId)) {
                log.info("Deleting the LDAP provider {}", providerId);
                ldapTemplates.remove(providerId);
            }
        }

        Set<String> enabledProviderIds = ldapProvidersMap.keySet();
        if (log.isDebugEnabled()) {
            log.info("The updated LDAP providers are [{}]", String.join(",", enabledProviderIds));
        }

        ldapGroupEntityService.disableGroupsWithNonEnabledProviders(enabledProviderIds);
    }
}
