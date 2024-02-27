package com.radiologics.plugins.ldapsync.utils;

import com.radiologics.plugins.ldapsync.dtos.TransferLdapGroup;
import com.radiologics.plugins.ldapsync.entities.LdapGroup;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class LdapGroupConverterTest {
    @Test
    public void testToDto() {
        LdapGroup ldapGroup = LdapGroup.builder().groupDN("groupDN").build();

        TransferLdapGroup transferLdapGroup = LdapGroupConverter.toDto(ldapGroup);

        LdapGroup ldapGroup2 = LdapGroup.builder().groupDN(transferLdapGroup.getGroupDN()).build();

        assertThat(ldapGroup2.toString()).isEqualTo(ldapGroup.toString());
    }
}
