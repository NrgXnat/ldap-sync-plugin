package org.nrg.xnatx.plugins.auth.ldap.provider;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.nrg.xnat.security.provider.ProviderAttributes;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LdapAttributeHelper {
    public static ProviderAttributes getAttributes(XnatLdapAuthenticationProvider ldapProvider) {
        return ldapProvider.getAttributes();
    }
}
