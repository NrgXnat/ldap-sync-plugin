package com.radiologics.plugins.ldapsync.services.impl;

import com.radiologics.plugins.ldapsync.exceptions.LdapInvalidArgumentException;
import com.radiologics.plugins.ldapsync.services.LdapProviderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.xnat.security.XnatProviderManager;
import org.nrg.xnat.security.provider.ProviderAttributes;
import org.nrg.xnat.security.provider.XnatAuthenticationProvider;
import org.nrg.xnatx.plugins.auth.ldap.provider.LdapAttributeHelper;
import org.nrg.xnatx.plugins.auth.ldap.provider.XnatLdapAuthenticationProvider;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.stereotype.Service;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LdapProviderServiceImpl implements LdapProviderService {
    private static final List<String> REQUIRED_ATTRIBUTES = Arrays.asList(XnatLdapAuthenticationProvider.LDAP_ADDRESS,
            XnatLdapAuthenticationProvider.LDAP_SEARCH_BASE,
            XnatLdapAuthenticationProvider.LDAP_USERDN,
            XnatLdapAuthenticationProvider.LDAP_PASSWORD);

    private final XnatProviderManager xnatProviderManager;

    public LdapProviderServiceImpl(XnatProviderManager xnatProviderManager) {
        this.xnatProviderManager = xnatProviderManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, XnatLdapAuthenticationProvider> getLdapProviders() {
        Map<String, XnatLdapAuthenticationProvider> ldapProviders = new HashMap<>();
        Map<String, XnatAuthenticationProvider> providers = xnatProviderManager.getVisibleEnabledProviders();

        for (String providerId : providers.keySet()) {
            XnatAuthenticationProvider provider = providers.get(providerId);

            if (provider instanceof XnatLdapAuthenticationProvider) {
                XnatLdapAuthenticationProvider ldapAuthProvider = (XnatLdapAuthenticationProvider) provider;
                ldapProviders.put(providerId, ldapAuthProvider);
            }
        }
        return ldapProviders;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LdapTemplate constructLdapTemplate(XnatLdapAuthenticationProvider ldapProvider) throws LdapInvalidArgumentException {
        return constructLdapTemplate(ldapProvider, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LdapTemplate constructLdapTemplate(XnatLdapAuthenticationProvider ldapProvider, String rawBase) throws LdapInvalidArgumentException {
        ProviderAttributes attributes = LdapAttributeHelper.getAttributes(ldapProvider);

        validateLdapAttributes(attributes);
        String base = rawBase != null ? rawBase : attributes.getProperty(XnatLdapAuthenticationProvider.LDAP_SEARCH_BASE);
        validateBaseDN(base);

        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(attributes.getProperty(XnatLdapAuthenticationProvider.LDAP_ADDRESS));
        contextSource.setBase(base);
        contextSource.setUserDn(attributes.getProperty(XnatLdapAuthenticationProvider.LDAP_USERDN));
        contextSource.setPassword(attributes.getProperty(XnatLdapAuthenticationProvider.LDAP_PASSWORD));
        contextSource.afterPropertiesSet();

        return new LdapTemplate(contextSource);
    }

    private void validateLdapAttributes(ProviderAttributes attributes) throws LdapInvalidArgumentException {
        final List<String> missing = REQUIRED_ATTRIBUTES.stream()
                .filter(attribute -> StringUtils.isBlank(attributes.getProperty(attribute)))
                .sorted()
                .collect(Collectors.toList());
        if (!missing.isEmpty()) {
            if (missing.size() == 1) {
                throw new LdapInvalidArgumentException("The " + missing.get(0) + " property is required but not configured");
            }
            throw new LdapInvalidArgumentException("The following properties are required but not configured:\n\n * " + String.join("\n * ", missing));
        }
    }

    private void validateBaseDN(String base) throws LdapInvalidArgumentException {
        try {
            new LdapName(base);
        } catch (InvalidNameException e) {
            throw new LdapInvalidArgumentException("Base DN is not valid: " + base);
        }
    }
}
