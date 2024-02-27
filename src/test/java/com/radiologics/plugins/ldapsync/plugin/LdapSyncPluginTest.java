package com.radiologics.plugins.ldapsync.plugin;

import com.radiologics.plugins.ldapsync.services.LdapProviderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nrg.xnat.security.provider.ProviderAttributes;
import org.nrg.xnatx.plugins.auth.ldap.provider.LdapAttributeHelper;
import org.nrg.xnatx.plugins.auth.ldap.provider.XnatLdapAuthenticationProvider;
import org.springframework.ldap.core.LdapTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LdapSyncPluginTest {
    @Mock
    LdapProviderService ldapProviderService;

    @Mock
    XnatLdapAuthenticationProvider xnatLdapAuthenticationProvider;

    private ProviderAttributes createProviderAttributes(
            String providerId,
            String authMethod,
            String displayName,
            boolean visible,
            boolean autoEnable,
            boolean autoVerified,
            String ldapAddress,
            String ldapSearchBase,
            String ldapUserDN,
            String ldapPassword
    ) {
        Properties properties = new Properties();
        if (ldapAddress != null) {
            properties.setProperty(XnatLdapAuthenticationProvider.LDAP_ADDRESS, ldapAddress);
        }
        if (ldapSearchBase != null) {
            properties.setProperty(XnatLdapAuthenticationProvider.LDAP_SEARCH_BASE, ldapSearchBase);
        }
        if (ldapUserDN != null) {
            properties.setProperty(XnatLdapAuthenticationProvider.LDAP_USERDN, ldapUserDN);
        }
        if (ldapPassword != null) {
            properties.setProperty(XnatLdapAuthenticationProvider.LDAP_PASSWORD, ldapPassword);
        }
        ProviderAttributes attributes = new ProviderAttributes(
                providerId, authMethod, displayName, visible, autoEnable, autoVerified, properties
        );

        return attributes;
    }

    @Test
    public void testLdapTemplateMap() {
        Map<String, XnatLdapAuthenticationProvider> ldapProvidersMap = new HashMap<>();
        ldapProvidersMap.put("test-ldap", xnatLdapAuthenticationProvider);
        when(ldapProviderService.getLdapProviders()).thenReturn(ldapProvidersMap);

        try (MockedStatic<LdapAttributeHelper> mockedLdapAttributeHelper = mockStatic(LdapAttributeHelper.class)) {
            ProviderAttributes attributes = createProviderAttributes("providerId", "authMethod", "displayName", true, true, true,
                    "ldap://localhost", "ou=people,dc=example,dc=org", "cn=admin,dc=example,dc=org", "password");
            mockedLdapAttributeHelper.when(() -> LdapAttributeHelper.getAttributes(xnatLdapAuthenticationProvider)).thenReturn(attributes);

            LdapSyncPlugin ldapSyncPlugin = new LdapSyncPlugin(ldapProviderService);
            Map<String, LdapTemplate> ldapTemplateMap = ldapSyncPlugin.ldapTemplates();

            assertThat(ldapTemplateMap).isNotNull();
            assertThat(ldapTemplateMap.keySet().size()).isEqualTo(1);
        }
    }

    @Test
    public void testInvalidLdapAddressOfLdapTemplateMap() {
        Map<String, XnatLdapAuthenticationProvider> ldapProvidersMap = new HashMap<>();
        ldapProvidersMap.put("test-ldap", xnatLdapAuthenticationProvider);
        when(ldapProviderService.getLdapProviders()).thenReturn(ldapProvidersMap);

        try (MockedStatic<LdapAttributeHelper> mockedLdapAttributeHelper = mockStatic(LdapAttributeHelper.class)) {
            ProviderAttributes attributes = createProviderAttributes("providerId", "authMethod", "displayName", true, true, true,
                    null, "ou=people,dc=example,dc=org", "cn=admin,dc=example,dc=org", "password");
            mockedLdapAttributeHelper.when(() -> LdapAttributeHelper.getAttributes(xnatLdapAuthenticationProvider)).thenReturn(attributes);

            LdapSyncPlugin ldapSyncPlugin = new LdapSyncPlugin(ldapProviderService);
            Map<String, LdapTemplate> ldapTemplateMap = ldapSyncPlugin.ldapTemplates();

            assertThat(ldapTemplateMap).isNotNull();
            assertThat(ldapTemplateMap.keySet().size()).isEqualTo(1);
        }
    }

    @Test
    public void testInvalidLdapSearchBaseOfLdapTemplateMap() {
        Map<String, XnatLdapAuthenticationProvider> ldapProvidersMap = new HashMap<>();
        ldapProvidersMap.put("test-ldap", xnatLdapAuthenticationProvider);
        when(ldapProviderService.getLdapProviders()).thenReturn(ldapProvidersMap);

        try (MockedStatic<LdapAttributeHelper> mockedLdapAttributeHelper = mockStatic(LdapAttributeHelper.class)) {
            ProviderAttributes attributes = createProviderAttributes("providerId", "authMethod", "displayName", true, true, true,
                    "ldap://localhost", null, "cn=admin,dc=example,dc=org", "password");
            mockedLdapAttributeHelper.when(() -> LdapAttributeHelper.getAttributes(xnatLdapAuthenticationProvider)).thenReturn(attributes);

            LdapSyncPlugin ldapSyncPlugin = new LdapSyncPlugin(ldapProviderService);
            Map<String, LdapTemplate> ldapTemplateMap = ldapSyncPlugin.ldapTemplates();

            assertThat(ldapTemplateMap).isNotNull();
            assertThat(ldapTemplateMap.keySet().size()).isEqualTo(1);
        }
    }

    @Test
    public void testInvalidLdapUserDNOfLdapTemplateMap() {
        Map<String, XnatLdapAuthenticationProvider> ldapProvidersMap = new HashMap<>();
        ldapProvidersMap.put("test-ldap", xnatLdapAuthenticationProvider);
        when(ldapProviderService.getLdapProviders()).thenReturn(ldapProvidersMap);

        try (MockedStatic<LdapAttributeHelper> mockedLdapAttributeHelper = mockStatic(LdapAttributeHelper.class)) {
            ProviderAttributes attributes = createProviderAttributes("providerId", "authMethod", "displayName", true, true, true,
                    "ldap://localhost", "ou=people,dc=example,dc=org", null, "password");
            mockedLdapAttributeHelper.when(() -> LdapAttributeHelper.getAttributes(xnatLdapAuthenticationProvider)).thenReturn(attributes);

            LdapSyncPlugin ldapSyncPlugin = new LdapSyncPlugin(ldapProviderService);
            Map<String, LdapTemplate> ldapTemplateMap = ldapSyncPlugin.ldapTemplates();

            assertThat(ldapTemplateMap).isNotNull();
            assertThat(ldapTemplateMap.keySet().size()).isEqualTo(1);
        }
    }

    @Test
    public void testInvalidLdapPasswordOfLdapTemplateMap() {
        Map<String, XnatLdapAuthenticationProvider> ldapProvidersMap = new HashMap<>();
        ldapProvidersMap.put("test-ldap", xnatLdapAuthenticationProvider);
        when(ldapProviderService.getLdapProviders()).thenReturn(ldapProvidersMap);

        try (MockedStatic<LdapAttributeHelper> mockedLdapAttributeHelper = mockStatic(LdapAttributeHelper.class)) {
            ProviderAttributes attributes = createProviderAttributes("providerId", "authMethod", "displayName", true, true, true,
                    "ldap://localhost", "ou=people,dc=example,dc=org", "cn=admin,dc=example,dc=org", null);
            mockedLdapAttributeHelper.when(() -> LdapAttributeHelper.getAttributes(xnatLdapAuthenticationProvider)).thenReturn(attributes);

            LdapSyncPlugin ldapSyncPlugin = new LdapSyncPlugin(ldapProviderService);
            Map<String, LdapTemplate> ldapTemplateMap = ldapSyncPlugin.ldapTemplates();

            assertThat(ldapTemplateMap).isNotNull();
            assertThat(ldapTemplateMap.keySet().size()).isEqualTo(1);
        }
    }
}
