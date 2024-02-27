package com.radiologics.plugins.ldapsync.rest;

import com.radiologics.plugins.ldapsync.dtos.LdapSyncSitePrefs;
import com.radiologics.plugins.ldapsync.preferences.LdapSyncSitePreferences;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.nrg.xdat.security.helpers.AccessLevel.Admin;

@Slf4j
@Api("Site-Wide Preferences API for the LDAP synchronization")
@XapiRestController
@RequestMapping(value="/ldap-sync/prefs/site")
public class LdapSyncSitePreferencesApi extends AbstractXapiRestController {
    private final LdapSyncSitePreferences prefs;

    public LdapSyncSitePreferencesApi(UserManagementServiceI userManagementService, RoleHolder roleHolder, LdapSyncSitePreferences prefs) {
        super(userManagementService, roleHolder);
        this.prefs = prefs;
    }

    @ApiOperation(value = "Retrieves the preferences for the LDAP synchronization", notes = "Returns the preference JSON.", response = LdapSyncSitePrefs.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the preferences JSON."),
            @ApiResponse(code = 403, message = "Insufficient privileges to retrieve the preferences."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = Admin)
    public LdapSyncSitePrefs getPrefs() {
        return prefs.getAllPrefs();
    }

    @ApiOperation(value = "Sets the preferences for the LDAP synchronization", notes = "Returns the updated preference JSON.", response = LdapSyncSitePrefs.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Returns the preferences JSON."),
            @ApiResponse(code = 403, message = "Insufficient privileges to retrieve the preferences."),
            @ApiResponse(code = 500, message = "An unexpected or unknown error occurred.")})
    @XapiRequestMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT, restrictTo = Admin)
    public LdapSyncSitePrefs updatePref(@RequestBody final LdapSyncSitePrefs dto) {
        prefs.updatePreference(dto);
        return prefs.getAllPrefs();
    }
}
