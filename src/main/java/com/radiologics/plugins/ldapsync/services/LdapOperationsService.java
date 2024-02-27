package com.radiologics.plugins.ldapsync.services;

import com.radiologics.plugins.ldapsync.dtos.LdapRetrieval;
import com.radiologics.plugins.ldapsync.dtos.LdapUserSearchResult;
import com.radiologics.plugins.ldapsync.exceptions.GroupDNNotFoundException;
import com.radiologics.plugins.ldapsync.exceptions.LdapInvalidArgumentException;
import com.radiologics.plugins.ldapsync.exceptions.LdapNetworkException;
import com.radiologics.plugins.ldapsync.exceptions.LdapXnatException;
import org.springframework.ldap.core.LdapTemplate;

import java.util.List;

public interface LdapOperationsService {
    List<LdapUserSearchResult> retrieveLdapUsers(LdapRetrieval ldapRetrieval)
            throws LdapNetworkException, LdapXnatException, GroupDNNotFoundException, LdapInvalidArgumentException;

    List<LdapUserSearchResult> retrieveLdapUsersViaGroup(final LdapRetrieval ldapRetrieval)
            throws LdapXnatException, LdapNetworkException, GroupDNNotFoundException, LdapInvalidArgumentException;

    /**
     * Retrieves the {@link List list} of {@link LdapUserSearchResult search results} in the specified groupDN from the provided LDAP provider
     * @param ldapRetrieval LDAP retrieval
     * @return the {@link List list} of {@link LdapUserSearchResult search results} retrieved
     * @throws LdapNetworkException thrown when exceptions happened while communicating with the LDAP server associated with the specified LDAP provider ID
     * @throws LdapXnatException thrown when the {@link LdapTemplate LDAP Template} matched with the provided Provider ID is not found
     */
    List<LdapUserSearchResult> retrieveLdapUsersViaUser(final LdapRetrieval ldapRetrieval)
            throws LdapNetworkException, LdapXnatException, GroupDNNotFoundException;
}
