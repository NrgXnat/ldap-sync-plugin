package com.radiologics.plugins.ldapsync.preferences;

import com.radiologics.plugins.ldapsync.dtos.LdapSyncSitePrefs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nrg.framework.configuration.ConfigPaths;
import org.nrg.framework.constants.Scope;
import org.nrg.framework.services.NrgEventServiceI;
import org.nrg.framework.utilities.OrderedProperties;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.nrg.prefs.services.NrgPreferenceService;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LdapSyncSitePreferencesTest {
    @Mock NrgPreferenceService preferenceService;
    @Mock NrgEventServiceI eventService;
    @Mock ConfigPaths configPaths;
    @Mock OrderedProperties initPrefs;

    LdapSyncSitePreferences ldapSyncSitePreferences;

    @BeforeEach
    public void setUpMocks() {
        ldapSyncSitePreferences = new LdapSyncSitePreferences(preferenceService, eventService, configPaths, initPrefs);
    }

    @Test
    public void testGetLdapSynchronizationRepeat() {
        when(preferenceService.getPreferenceValue(anyString(), eq("ldapSynchronizationRepeat"), any(Scope.class), anyString())).thenReturn("60000");

        assertThat(ldapSyncSitePreferences.getLdapSynchronizationRepeat()).isEqualTo(60000L);
    }

    @Test
    public void testGetAllPrefs() {
        Mockito.lenient().when(preferenceService.getPreferenceValue(anyString(), eq("ldapSyncSiteWideEnabled"), any(Scope.class), anyString())).thenReturn("true");
        Mockito.lenient().when(preferenceService.getPreferenceValue(anyString(), eq("ldapSynchronizationRepeat"), any(Scope.class), anyString())).thenReturn("60000");
        Mockito.lenient().when(preferenceService.getPreferenceValue("ldapSync", "ldapSyncAuthProviderId")).thenReturn("ldap");

        assertThat(ldapSyncSitePreferences.getAllPrefs()).isNotNull();
    }

    @Test
    public void testsStLdapSynchronizationRepeat() {
        LdapSyncSitePreferences mockedLdapSyncSitePreferences = Mockito.mock(LdapSyncSitePreferences.class);
        Mockito.doCallRealMethod().when(mockedLdapSyncSitePreferences).setLdapSynchronizationRepeat(anyLong());
        mockedLdapSyncSitePreferences.setLdapSynchronizationRepeat(10000L);
    }

    @Test
    public void testsStLdapSynchronizationRepeatThrowsInvalidPreferenceName() throws InvalidPreferenceName {
        LdapSyncSitePreferences mockedLdapSyncSitePreferences = Mockito.mock(LdapSyncSitePreferences.class);
        doThrow(InvalidPreferenceName.class).when(mockedLdapSyncSitePreferences).setLongValue(anyLong(), anyString());
        Mockito.doCallRealMethod().when(mockedLdapSyncSitePreferences).setLdapSynchronizationRepeat(anyLong());
        mockedLdapSyncSitePreferences.setLdapSynchronizationRepeat(10000L);
    }

    @Test
    public void testUpdatePreference() {
        LdapSyncSitePreferences mockedLdapSyncSitePreferences = Mockito.mock(LdapSyncSitePreferences.class);
        Mockito.doCallRealMethod().when(mockedLdapSyncSitePreferences).updatePreference(any(LdapSyncSitePrefs.class));
        LdapSyncSitePrefs ldapSyncSitePrefs = LdapSyncSitePrefs.builder().build();
        mockedLdapSyncSitePreferences.updatePreference(ldapSyncSitePrefs);
        verify(mockedLdapSyncSitePreferences, times(1)).setLdapSynchronizationRepeat(anyLong());
    }
}
