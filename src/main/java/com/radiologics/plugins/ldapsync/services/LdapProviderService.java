package com.radiologics.plugins.ldapsync.services;

import com.radiologics.plugins.ldapsync.exceptions.LdapInvalidArgumentException;
import org.nrg.xnatx.plugins.auth.ldap.provider.XnatLdapAuthenticationProvider;
import org.springframework.ldap.core.LdapTemplate;

import java.util.Map;

public interface LdapProviderService {
    /**
     * Gets the {@link Map map} of the pairs of the LDAP provider ID and its {@link XnatLdapAuthenticationProvider LDAP Provider} defined on XNAT
     * @return the {@link Map map} of the pair of the LDAP provider ID and its {@link XnatLdapAuthenticationProvider LDAP Provider}
     */
    Map<String, XnatLdapAuthenticationProvider> getLdapProviders();

    /**
     * Constructs LdapTemplate object using the given Ldap auth provider.
     * @param ldapProvider
     * @return created LdapTemplate
     * @throws IllegalArgumentException thrown required fields are not provided
     */
    LdapTemplate constructLdapTemplate(XnatLdapAuthenticationProvider ldapProvider) throws IllegalArgumentException, LdapInvalidArgumentException;

    LdapTemplate constructLdapTemplate(XnatLdapAuthenticationProvider ldapProvider, String base) throws IllegalArgumentException, LdapInvalidArgumentException;
}
