package com.radiologics.plugins.ldapsync.rest;

import com.radiologics.plugins.ldapsync.dtos.TransferLdapGroup;
import com.radiologics.plugins.ldapsync.dtos.LdapGroupProjectManagement;
import com.radiologics.plugins.ldapsync.dtos.TransferLdapGroupProject;
import com.radiologics.plugins.ldapsync.entities.LdapGroup;
import com.radiologics.plugins.ldapsync.entities.LdapGroupProject;
import com.radiologics.plugins.ldapsync.exceptions.*;
import com.radiologics.plugins.ldapsync.services.LdapGroupEntityService;
import com.radiologics.plugins.ldapsync.services.LdapGroupProjectEntityService;
import com.radiologics.plugins.ldapsync.services.LdapProviderService;
import com.radiologics.plugins.ldapsync.utils.LdapGroupConverter;
import com.radiologics.plugins.ldapsync.utils.LdapGroupProjectConverter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.Project;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xnatx.plugins.auth.ldap.provider.XnatLdapAuthenticationProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.nrg.xdat.security.helpers.AccessLevel.*;

@Slf4j
@Api("Group Management API for the LDAP synchronization")
@XapiRestController
@RequestMapping(value = "/ldap-sync/prefs/groups")
public class LdapSyncGroupsApi extends AbstractXapiRestController {
    private final LdapGroupEntityService ldapGroupEntityService;
    private final LdapGroupProjectEntityService ldapGroupProjectEntityService;
    private final LdapProviderService ldapProviderService;

    public LdapSyncGroupsApi(UserManagementServiceI userManagementService, RoleHolder roleHolder, LdapGroupEntityService ldapGroupEntityService, LdapGroupProjectEntityService ldapGroupProjectEntityService, LdapProviderService ldapProviderService) {
        super(userManagementService, roleHolder);
        this.ldapGroupEntityService = ldapGroupEntityService;
        this.ldapGroupProjectEntityService = ldapGroupProjectEntityService;
        this.ldapProviderService = ldapProviderService;
    }

    @ApiOperation(value = "Retrieves paged the LDAP Group Info", notes = "Returns the LDAP Group JSON.", response = List.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the preferences JSON."),
            @ApiResponse(code = 403, message = "Insufficient privileges to retrieve the preferences."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public List<TransferLdapGroup> getAllLdapGroups() {
        return ldapGroupEntityService
                .getAll()
                .stream()
                .sorted(new Comparator<LdapGroup>() {
                    @Override
                    public int compare(LdapGroup l1, LdapGroup l2) {
                        return l1.getLabel().compareTo(l2.getLabel());
                    }
                })
                .map(ldapGroupEntity -> LdapGroupConverter.toDto(ldapGroupEntity))
                .collect(Collectors.toList());
    }

    @ApiOperation(value = "Retrieves the specific project's LDAP Group DN", notes = "Returns the LDAP Group DN's JSON.", response = TransferLdapGroup.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the preferences JSON."),
            @ApiResponse(code = 403, message = "Insufficient privileges to retrieve the preferences."),
            @ApiResponse(code = 409, message = "Already Exists. Please use the PUT method if you want to update."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "/{label}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public TransferLdapGroup getLdapGroup(@PathVariable String label) throws NotFoundException {
        Optional<LdapGroup> optionalLdapGroup = ldapGroupEntityService.findByLabel(label);
        if (!optionalLdapGroup.isPresent()) {
            throw new NotFoundException("LDAP Group Not Found");
        }
        LdapGroup ldapGroup = optionalLdapGroup.get();
        return LdapGroupConverter.toDto(ldapGroup);
    }

    @ApiOperation(value = "Creates a User Group's LDAP Group DN for the LDAP synchronization", notes = "Returns the creat User Group JSON.", response = TransferLdapGroup.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the User Group JSON."),
            @ApiResponse(code = 403, message = "Insufficient privileges to retrieve the preferences."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Admin)
    public TransferLdapGroup createLdapProject(@RequestBody final TransferLdapGroup dto) throws AlreadyExistsException, WorkflowException, LdapInvalidArgumentException {
        dto.setLabel(dto.getLabel());
        LdapGroup created = ldapGroupEntityService.create(dto);
        return LdapGroupConverter.toDto(created);
    }

    @ApiOperation(value = "Updates LDAP Group for the LDAP synchronization", notes = "Returns the updated User Group JSON.", response = TransferLdapGroup.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the LDAP Group JSON."),
            @ApiResponse(code = 403, message = "Insufficient privileges to retrieve the preferences."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "/{originalLabel}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT, restrictTo = Admin)
    public TransferLdapGroup updateLdapGroup(@PathVariable final String originalLabel, @RequestBody final TransferLdapGroup dto) throws NotFoundException, WorkflowException, LdapInvalidArgumentException {
        LdapGroup updated = ldapGroupEntityService.update(originalLabel, dto);
        return LdapGroupConverter.toDto(updated);
    }

    @ApiOperation(value = "Enable/Disable LDAP Group for the LDAP synchronization", notes = "Returns the updated User Group JSON.", response = TransferLdapGroup.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the LDAP Group JSON."),
            @ApiResponse(code = 403, message = "Insufficient privileges to retrieve the preferences."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "/{label}/{ldapGroupEnabled}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT, restrictTo = Admin)
    public TransferLdapGroup enableLdapGroup(@PathVariable final String label, @PathVariable final boolean ldapGroupEnabled) throws NotFoundException, EmptyProjectsException, WorkflowException {
        LdapGroup updated = ldapGroupEntityService.enable(label, ldapGroupEnabled);
        return LdapGroupConverter.toDto(updated);
    }

    @ApiOperation(value = "Delete LDAP Group for the LDAP synchronization", notes = "Returns the updated User Group JSON.")
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the LDAP Group JSON."),
            @ApiResponse(code = 403, message = "Insufficient privileges to retrieve the preferences."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "/{label}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.DELETE, restrictTo = Admin)
    public void deleteLdapGroup(@PathVariable final String label) throws NotFoundException, WorkflowException {
        ldapGroupEntityService.delete(label);
    }

    @ApiOperation(value = "Create LDAP Group Project for the LDAP synchronization", notes = "Returns the updated User Group JSON.")
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the LDAP Group JSON."),
            @ApiResponse(code = 403, message = "Insufficient privileges to retrieve the preferences."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "/{label}/projects/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST, restrictTo = Admin)
    public void createLdapGroupProject(@PathVariable final String label, @PathVariable @Project String projectId) throws AlreadyExistsException, NotFoundException {
        ldapGroupProjectEntityService.create(label, projectId);
    }

    @ApiOperation(value = "Delete LDAP Group Project for the LDAP synchronization", notes = "Returns the updated User Group JSON.")
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the LDAP Group JSON."),
            @ApiResponse(code = 403, message = "Insufficient privileges to retrieve the preferences."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "/projects/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.DELETE, restrictTo = Admin)
    public void deleteLdapGroupProject(@PathVariable @Project String projectId) throws NotFoundException, WorkflowException {
        ldapGroupProjectEntityService.delete(projectId);
    }

    @ApiOperation(value = "Manage LDAP Group Projects for the LDAP synchronization", notes = "Returns the updated User Group JSON.")
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the LDAP Group JSON."),
            @ApiResponse(code = 403, message = "Insufficient privileges to retrieve the preferences."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "/{label}/projects", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT, restrictTo = Admin)
    public TransferLdapGroup manageLdapGroupProjects(@PathVariable String label, @RequestBody final LdapGroupProjectManagement ldapGroupProjectManagement) throws NotFoundException, WorkflowException {
        LdapGroup ldapGroup = ldapGroupEntityService.manageProjects(label, ldapGroupProjectManagement.getProjectIds(), getSessionUser());
        return LdapGroupConverter.toDto(ldapGroup);
    }

    @ApiOperation(value = "Retrieves LDAP Auth Provider IDs", notes = "Returns the LDAP Auth Provider IDs JSON.", response = Set.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the LDAP AUTH Method IDs JSON."),
            @ApiResponse(code = 403, message = "Insufficient privileges to retrieve the preferences."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(value = "/ldap-providers", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public Set<String> getLdapAuthProviderIds() {
        Map<String, XnatLdapAuthenticationProvider> ldapProviderMap = ldapProviderService.getLdapProviders();
        return ldapProviderMap.keySet();
    }
}
