package com.radiologics.plugins.ldapsync.utils;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class LdapAttributesConverterTest {
    @Test
    public void testStringifyAttrs() {
        Attributes attributes = new BasicAttributes();
        attributes.put(new BasicAttribute("name", "name_value"));
        attributes.put(new BasicAttribute("uid", "uid_value"));
        attributes.put(new BasicAttribute("password", "password"));

        String stringed = LdapAttributesConverter.stringifyAttrs(attributes);
        assertThat(stringed).isEqualTo("[name=name_value, uid=uid_value]");
    }

    @Test
    public void testStringifyAttrsThrowsNamingException() throws NamingException {
        Attributes attributes = Mockito.mock(Attributes.class);
        NamingEnumeration enumeration = Mockito.mock(NamingEnumeration.class);
        when(attributes.getAll()).thenReturn(enumeration);
        when(enumeration.hasMore()).thenThrow(NamingException.class);

        assertThat(LdapAttributesConverter.stringifyAttrs(attributes)).isEqualTo("[]");
    }
}
