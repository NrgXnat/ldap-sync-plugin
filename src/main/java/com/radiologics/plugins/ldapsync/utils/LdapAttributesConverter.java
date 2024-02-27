package com.radiologics.plugins.ldapsync.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LdapAttributesConverter {
    public static String stringifyAttrs(Attributes attrs) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        try {
            NamingEnumeration<? extends Attribute> enumeration = attrs.getAll();

            int cnt = 0;
            while (enumeration.hasMore()) {
                Attribute attr = enumeration.next();
                String name = attr.getID();
                if (name.toLowerCase().contains("password")) {
                    continue;
                }
                if (cnt > 0) {
                    sb.append(", ");
                }
                sb.append(name);
                sb.append("=");
                sb.append(attr.get());
                cnt++;
            }
        } catch (NamingException e) {
            log.warn("Failed to stringifyAttrs", e);
        }
        sb.append("]");
        return sb.toString();
    }
}
