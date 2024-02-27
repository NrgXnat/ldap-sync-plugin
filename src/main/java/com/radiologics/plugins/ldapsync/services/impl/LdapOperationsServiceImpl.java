package com.radiologics.plugins.ldapsync.services.impl;

import com.radiologics.plugins.ldapsync.dtos.LdapRetrieval;
import com.radiologics.plugins.ldapsync.dtos.LdapUserSearchResult;
import com.radiologics.plugins.ldapsync.entities.LdapSearchType;
import com.radiologics.plugins.ldapsync.exceptions.GroupDNNotFoundException;
import com.radiologics.plugins.ldapsync.exceptions.LdapInvalidArgumentException;
import com.radiologics.plugins.ldapsync.exceptions.LdapNetworkException;
import com.radiologics.plugins.ldapsync.exceptions.LdapXnatException;
import com.radiologics.plugins.ldapsync.services.LdapProviderService;
import com.radiologics.plugins.ldapsync.services.LdapOperationsService;
import com.radiologics.plugins.ldapsync.utils.LdapAttributesConverter;
import lombok.extern.slf4j.Slf4j;
import org.nrg.xnatx.plugins.auth.ldap.provider.XnatLdapAuthenticationProvider;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Service;

import javax.naming.directory.Attribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LdapOperationsServiceImpl implements LdapOperationsService {
    final LdapProviderService ldapProviderService;
    private final Map<String, LdapTemplate> ldapTemplatesMap;

    public LdapOperationsServiceImpl(LdapProviderService ldapProviderService, final Map<String, LdapTemplate> ldapTemplatesMap) {
        this.ldapProviderService = ldapProviderService;
        this.ldapTemplatesMap = ldapTemplatesMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<LdapUserSearchResult> retrieveLdapUsers(LdapRetrieval ldapRetrieval) throws LdapNetworkException, LdapXnatException, GroupDNNotFoundException, LdapInvalidArgumentException {
        LdapSearchType ldapSearchType = ldapRetrieval.getLdapSearchType();

        switch (ldapSearchType) {
            case User:
                return retrieveLdapUsersViaUser(ldapRetrieval);

            case Group:
                return retrieveLdapUsersViaGroup(ldapRetrieval);

            default:
                throw new IllegalArgumentException("LDAP Search Type is required");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<LdapUserSearchResult> retrieveLdapUsersViaGroup(final LdapRetrieval ldapRetrieval) throws LdapNetworkException, GroupDNNotFoundException, LdapInvalidArgumentException {
        List<String> userDNs = retrieveUserDNs(ldapRetrieval);
        return retrieveLdapUsersWithUserDNs(userDNs, ldapRetrieval);
    }

    private List<String> retrieveUserDNs(final LdapRetrieval ldapRetrieval)
            throws LdapNetworkException, GroupDNNotFoundException, LdapInvalidArgumentException {
        Map<String, XnatLdapAuthenticationProvider> ldapProvidersMap = ldapProviderService.getLdapProviders();
        XnatLdapAuthenticationProvider ldapAuthenticationProvider = ldapProvidersMap.get(ldapRetrieval.getAuthProviderId());
        String baseDN = ldapRetrieval.getGroupDN();
        String groupMemberAttrName = ldapRetrieval.getGroupMemberAttrName();
        LdapTemplate ldapTemplate = ldapProviderService.constructLdapTemplate(ldapAuthenticationProvider, baseDN);
        String filter = ldapRetrieval.getSearchFilter();
        List<String> userDNs = new ArrayList<>();
        try {
            log.info("filter: {}, DN: {}", filter, baseDN);
            ldapTemplate.search(
                    "",
                    filter,
                    (AttributesMapper<Void>) attrs -> {
                        Attribute usernameAttr = attrs.get(groupMemberAttrName);
                        if (usernameAttr == null) {
                            log.warn("There is no {} field in the LDAP object of {}", groupMemberAttrName, baseDN);
                            String stringed = LdapAttributesConverter.stringifyAttrs(attrs);
                            log.debug(stringed);
                        } else {
                            int numUsers = usernameAttr.size();
                            for (int i = 0; i < numUsers; i++) {
                                userDNs.add((String)usernameAttr.get(i));
                            }
                        }
                        return null;
                    }
            );
        } catch (NameNotFoundException e) {
            throw new GroupDNNotFoundException(String.format("Group DN '%s' not found", baseDN));
        } catch (Exception e) {
            throw new LdapNetworkException(e);
        }
        return userDNs;
    }

    private List<LdapUserSearchResult> ldapSearchUsersCore(final LdapTemplate ldapTemplate, final String dn, final String filter, final String usernameAttrName) {
        log.info("filter: {}, DN: {}", filter, dn);
        return ldapTemplate.search(
                "",
                filter,
                (AttributesMapper<LdapUserSearchResult>) attrs -> {
                    String cn = null;
                    String sn = null;
                    String username = null;

                    Attribute cnAttr = attrs.get("cn");
                    if (cnAttr != null && cnAttr.get() != null) {
                        cn = attrs.get("cn").get().toString();
                    }

                    Attribute snAttr = attrs.get("sn");
                    if (snAttr != null && snAttr.get() != null) {
                        sn = attrs.get("sn").get().toString();
                    }

                    Attribute usernameAttr = attrs.get(usernameAttrName);
                    if (usernameAttr == null) {
                        log.warn("There is no {} field in the LDAP object of {}", usernameAttrName, dn);
                        String stringed = LdapAttributesConverter.stringifyAttrs(attrs);
                        log.debug(stringed);
                    } else {
                        username = attrs.get(usernameAttrName).get().toString();
                    }
                    return LdapUserSearchResult.builder()
                            .cn(cn)
                            .sn(sn)
                            .username(username)
                            .build();
                }
        ).stream().filter(ldapUserSearchResult -> ldapUserSearchResult != null).collect(Collectors.toList());
    }

    private Optional<LdapUserSearchResult> retrieveLdapUserWithUserDN(final String userDN, final LdapRetrieval ldapRetrieval)
            throws LdapInvalidArgumentException {
        Map<String, XnatLdapAuthenticationProvider> ldapProvidersMap = ldapProviderService.getLdapProviders();
        XnatLdapAuthenticationProvider ldapAuthenticationProvider = ldapProvidersMap.get(ldapRetrieval.getAuthProviderId());
        LdapTemplate ldapTemplate = ldapProviderService.constructLdapTemplate(ldapAuthenticationProvider, userDN);
        String filter = ldapRetrieval.getUserSearchFilter();

        List<LdapUserSearchResult> ldapUserSearchResults = ldapSearchUsersCore(ldapTemplate, userDN, filter,
                ldapRetrieval.getUsernameAttrName());
        if (ldapUserSearchResults == null || ldapUserSearchResults.size() == 0) {
            return Optional.empty();
        }
        return Optional.of(ldapUserSearchResults.get(0));
    }

    private List<LdapUserSearchResult> retrieveLdapUsersWithUserDNs(final List<String> userDNs, final LdapRetrieval ldapRetrieval)
            throws LdapNetworkException {
        List<LdapUserSearchResult> ldapUserSearchResults = new ArrayList<>();
        for(final String userDN : userDNs) {
            try {
                Optional<LdapUserSearchResult> optionalLdapUserSearchResults = retrieveLdapUserWithUserDN(userDN,
                        ldapRetrieval);
                optionalLdapUserSearchResults.ifPresent(ldapUserSearchResults::add);
            } catch (Exception e) {
                String msg = String.format("Failed to search the user DN '%s' for the group DN '%s'", userDN,
                        ldapRetrieval.getGroupDN());
                log.error(msg, e);
                throw new LdapNetworkException(msg, e);
            }
        }
        return ldapUserSearchResults;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<LdapUserSearchResult> retrieveLdapUsersViaUser(final LdapRetrieval ldapRetrieval) throws LdapNetworkException, LdapXnatException, GroupDNNotFoundException {
        LdapTemplate ldapTemplate;
        String authProviderId = ldapRetrieval.getAuthProviderId();
        if (authProviderId == null) {
            if (ldapTemplatesMap.size() == 1) {
                ldapTemplate = ldapTemplatesMap.values().stream().findFirst().get();
            } else {
                throw new LdapXnatException("Cannot find single ldap provider: multiple LDAP providers exist");
            }
        } else {
            ldapTemplate = ldapTemplatesMap.get(authProviderId);
        }

        if (ldapTemplate == null) {
            throw new LdapXnatException("cannot find LDAP auth provider named " + authProviderId);
        }

        String searchFilter = ldapRetrieval.getSearchFilter();
        String groupDN = ldapRetrieval.getGroupDN();
        try {
            return ldapSearchUsersCore(ldapTemplate, groupDN, searchFilter, ldapRetrieval.getUsernameAttrName());
        } catch (NameNotFoundException e) {
            throw new GroupDNNotFoundException(String.format("Group DN '%s' not found", groupDN));
        } catch (Exception e) {
            String msg = "Failed to search from the group DN " + groupDN;
            log.error(msg, e);
            throw new LdapNetworkException(msg, e);
        }
    }
}
