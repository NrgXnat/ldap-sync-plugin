package com.radiologics.plugins.ldapsync.rest;

import com.radiologics.plugins.ldapsync.dtos.*;
import com.radiologics.plugins.ldapsync.entities.LdapGroupProject;
import com.radiologics.plugins.ldapsync.entities.LdapSearchType;
import com.radiologics.plugins.ldapsync.exceptions.*;
import com.radiologics.plugins.ldapsync.services.LdapGroupProjectEntityService;
import com.radiologics.plugins.ldapsync.services.LdapOperationsService;
import com.radiologics.plugins.ldapsync.services.LdapSynchronizationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.Project;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.security.helpers.Permissions;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xft.security.UserI;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.nrg.xdat.security.helpers.AccessLevel.Admin;
import static org.nrg.xdat.security.helpers.AccessLevel.Delete;

@Slf4j
@Api("LDAP synchronization API")
@XapiRestController
@RequestMapping(value="/ldap-sync")
public class LdapSyncApi extends AbstractXapiRestController {

    private final LdapSynchronizationService ldapSynchronizationService;
    private final LdapOperationsService ldapOperationsService;
    private final LdapGroupProjectEntityService ldapGroupProjectEntityService;

    public LdapSyncApi(final UserManagementServiceI userManagementService,
                       final RoleHolder roleHolder,
                       final LdapSynchronizationService ldapSynchronizationService,
                       final LdapOperationsService ldapOperationsService,
                       final LdapGroupProjectEntityService ldapGroupProjectEntityService) {
        super(userManagementService, roleHolder);
        this.ldapSynchronizationService = ldapSynchronizationService;
        this.ldapOperationsService = ldapOperationsService;
        this.ldapGroupProjectEntityService = ldapGroupProjectEntityService;
    }

    @ApiOperation(value = "Manually synchronize user authorities for the projects associated with a single LDAP Group", notes = "", response = List.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Successfully synchronized."),
            @ApiResponse(code = 403, message = "Insufficient privileges to synchronize."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value ="/synchronize/{ldapGroupLabel}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Admin)
    public LdapGroupSyncResult synchronizeForLdapGroup(@PathVariable String ldapGroupLabel) throws LdapNetworkException, SynchronizationException, NotFoundException, LdapXnatException {
        return ldapSynchronizationService.synchronizeUserAuthorities(ldapGroupLabel);
    }
    
    @ApiOperation(value = "Manually synchronize user authorities for a single project", notes = "", response = ProjectSyncResult.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Successfully synchronized."),
            @ApiResponse(code = 403, message = "Insufficient privileges to synchronize."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value ="/synchronize/{ldapGroupLabel}/projects/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Admin)
    public ProjectSyncResult synchronizeForProject(@PathVariable String ldapGroupLabel, @PathVariable String projectId) throws LdapNetworkException, SynchronizationException, NotFoundException, LdapXnatException {
        return ldapSynchronizationService.synchronizeUserAuthorities(ldapGroupLabel, projectId);
    }
    
    @ApiOperation(value = "Retrieve LDAP UIDs from LDAP server", notes = "", response = List.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Successfully Retrieved."),
            @ApiResponse(code = 403, message = "Insufficient privileges to retrieve."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value ="/ldap-retrieve", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Admin)
    public LdapRetrievalResponse retrieveLdapUids(@RequestBody LdapRetrieval ldapRetrieval) throws LdapXnatException {
        List<LdapUserSearchResult> ldapUserSearchResults;
        LdapRetrievalResponse.LdapRetrievalResponseBuilder builder = LdapRetrievalResponse.builder()
                .filter(ldapRetrieval.getSearchFilterInfo());
        try {
            ldapUserSearchResults = ldapOperationsService.retrieveLdapUsers(ldapRetrieval);
        } catch(LdapInvalidArgumentException e) {
            return builder
                    .responseType(LdapRetrievalResponseType.Error)
                    .message("Group DN is not valid")
                    .build();
        } catch(LdapNetworkException e) {
            return builder
                    .responseType(LdapRetrievalResponseType.Error)
                    .message("Could not connect to auth provider or search was malformed")
                    .build();
        } catch (GroupDNNotFoundException e) {
            return builder
                    .responseType(LdapRetrievalResponseType.Error)
                    .message("Connected to auth provider, but group DN not found")
                    .build();
        }

        if (ldapUserSearchResults.isEmpty()) {
            String message = ldapRetrieval.getLdapSearchType() == LdapSearchType.User ? "connected to auth provider, but no users found" : "connected to auth provider, group DN found, but no users found";
            return builder
                    .responseType(LdapRetrievalResponseType.Error)
                    .message(message)
                    .build();
        }

        long numUsersThatHaveUsername = ldapUserSearchResults.stream().filter(ldapUserSearchResult -> !StringUtils.isBlank(ldapUserSearchResult.getUsername())).count();
        builder.ldapUsers(ldapUserSearchResults);
        if (numUsersThatHaveUsername == 0) {
            return builder
                    .responseType(LdapRetrievalResponseType.Error)
                    .message("connected to auth provider, group DN found, users found, but no users have required attribute")
                    .build();
        }

        if (numUsersThatHaveUsername != ldapUserSearchResults.size()) {
            return builder
                    .responseType(LdapRetrievalResponseType.Warning)
                    .message("connected to auth provider, group DN found, users found, some users are missing required attribute")
                    .build();
        }

        return builder
                .responseType(LdapRetrievalResponseType.Success)
                .message("connected to auth provider, group DN found, users found, all users have required attribute")
                .build();
    }

    @ApiOperation(value = "Check if project is managed by LDAP", response = Boolean.class)
    @ApiResponses({@ApiResponse(code = 200, message = "True if project is managed by LDAP."),
            @ApiResponse(code = 403, message = "Insufficient privileges."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value ="/is-ldap-managed/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.GET, restrictTo = Delete)
    public Boolean isLdapManaged(@PathVariable @Project String projectId)  {
        LdapGroupProject ldapGroupProject = ldapGroupProjectEntityService.findByProjectId(projectId).orElse(null);
        return ldapGroupProject != null && ldapGroupProject.getLdapGroup().isEnabled();
    }


    @ApiOperation(value = "Get LDAP managed projects", notes = "", response = ProjectSyncResult.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Successfully Get LDAP managed projects"),
            @ApiResponse(code = 403, message = "Insufficient privileges to synchronize."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value ="/ldap-projects", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public List<String> getLdapManagedProjects()  {
        UserI user = getSessionUser();
        return ldapGroupProjectEntityService.getAll().stream()
                .map(LdapGroupProject::getProjectId)
                .filter(projectId -> Permissions.canReadProject(user, projectId))
                .collect(Collectors.toList());
    }
}
