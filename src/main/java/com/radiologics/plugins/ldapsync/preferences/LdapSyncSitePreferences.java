package com.radiologics.plugins.ldapsync.preferences;

import com.radiologics.plugins.ldapsync.dtos.LdapSyncSitePrefs;
import lombok.extern.slf4j.Slf4j;
import org.nrg.framework.configuration.ConfigPaths;
import org.nrg.framework.services.NrgEventServiceI;
import org.nrg.framework.utilities.OrderedProperties;
import org.nrg.prefs.annotations.NrgPreference;
import org.nrg.prefs.annotations.NrgPreferenceBean;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.nrg.prefs.services.NrgPreferenceService;
import org.nrg.xdat.preferences.EventTriggeringAbstractPreferenceBean;

@Slf4j
@NrgPreferenceBean(toolId = com.radiologics.plugins.ldapsync.preferences.LdapSyncSitePreferences.LDAP_SYNC_TOOL_ID,
        toolName = "LDAP Synchronization Preferences",
        description = "Manages the configurations and settings for the LDAP synchronization work.",
        strict = false)
public class LdapSyncSitePreferences extends EventTriggeringAbstractPreferenceBean {
    public static final String LDAP_SYNC_TOOL_ID = "ldapSync";

    private static final String REPEAT = "ldapSynchronizationRepeat";

    public LdapSyncSitePreferences(final NrgPreferenceService preferenceService, final NrgEventServiceI eventService, final ConfigPaths configPaths, final OrderedProperties initPrefs) {
        super(preferenceService, eventService, configPaths, initPrefs);
    }

    @NrgPreference(defaultValue = "1")
    public long getLdapSynchronizationRepeat() {
        return getLongValue("ldapSynchronizationRepeat");
    }

    public void setLdapSynchronizationRepeat(final long ldapSynchronizationRepeat) {
        try {
            setLongValue(ldapSynchronizationRepeat, "ldapSynchronizationRepeat");
        } catch (InvalidPreferenceName e) {
            log.error("Invalid preference name 'ldapSynchronizationRepeat': something is very wrong here.", e);
        }
    }

    public void updatePreference(final LdapSyncSitePrefs ldapSyncSitePrefs) {
        setLdapSynchronizationRepeat(ldapSyncSitePrefs.getLdapSynchronizationRepeat());
    }

    public LdapSyncSitePrefs getAllPrefs() {
        return new LdapSyncSitePrefs(getLdapSynchronizationRepeat());
    }
}
