package com.radiologics.plugins.ldapsync.services;

import com.radiologics.plugins.ldapsync.entities.LdapGroup;

import javax.mail.MessagingException;

public interface LdapEmailService {
    void sendDisabledEmail(LdapGroup ldapGroup) throws MessagingException;
}
