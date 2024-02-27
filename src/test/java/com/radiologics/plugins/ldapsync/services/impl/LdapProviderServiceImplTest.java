package com.radiologics.plugins.ldapsync.services.impl;


import com.radiologics.plugins.ldapsync.services.LdapProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nrg.xnat.security.XnatProviderManager;
import org.nrg.xnat.security.provider.XnatAuthenticationProvider;
import org.nrg.xnatx.plugins.auth.ldap.provider.XnatLdapAuthenticationProvider;
import org.springframework.ldap.core.LdapTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LdapProviderServiceImplTest {

    @Mock XnatProviderManager xnatProviderManager;
    @Mock XnatLdapAuthenticationProvider xnatLdapAuthenticationProvider;
    @Mock XnatAuthenticationProvider xnatAuthenticationProvider;

    LdapProviderService ldapProviderService;

    @BeforeEach
    public void setUp() {
        ldapProviderService = new LdapProviderServiceImpl(xnatProviderManager);
    }

    @Test
    public void testGetLdapProvidersMap() {
        Map<String, XnatAuthenticationProvider> allProviderMap = new HashMap<>();
        allProviderMap.put("localdb", xnatAuthenticationProvider);
        allProviderMap.put("ldap", xnatLdapAuthenticationProvider);

        when(xnatProviderManager.getVisibleEnabledProviders()).thenReturn(allProviderMap);

        Map<String, XnatLdapAuthenticationProvider> providerMap = ldapProviderService.getLdapProviders();
        assertThat(providerMap.size()).isEqualTo(1);
        assertThat(providerMap.get("ldap")).isEqualTo(xnatLdapAuthenticationProvider);
    }

    @Test
    public void testGetEmptyLdapProvidersMaps() {
        Map<String, XnatAuthenticationProvider> allProviderMap = new HashMap<>();
        allProviderMap.put("localdb", xnatAuthenticationProvider);

        when(xnatProviderManager.getVisibleEnabledProviders()).thenReturn(allProviderMap);

        Map<String, XnatLdapAuthenticationProvider> providerMap = ldapProviderService.getLdapProviders();
        assertThat(providerMap.size()).isEqualTo(0);
    }

    @Test
    public void testGetMultipleLdapProvidersMaps() {
        Map<String, XnatAuthenticationProvider> allProviderMap = new HashMap<>();
        allProviderMap.put("ldap1", xnatLdapAuthenticationProvider);
        allProviderMap.put("ldap2", Mockito.mock(XnatLdapAuthenticationProvider.class));

        when(xnatProviderManager.getVisibleEnabledProviders()).thenReturn(allProviderMap);

        Map<String, XnatLdapAuthenticationProvider> providerMap = ldapProviderService.getLdapProviders();
        assertThat(providerMap.size()).isEqualTo(2);
    }
}
