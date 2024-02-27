package com.radiologics.plugins.ldapsync.services.impl;

import com.radiologics.plugins.ldapsync.entities.LdapGroup;
import com.radiologics.plugins.ldapsync.services.LdapEmailService;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LdapEmailServiceImpl implements LdapEmailService {
    private static final String EMAIL_SUBJECT_FORMAT = "XNAT LDAP Group '%s' Disabled";

    @Override
    public void sendDisabledEmail(LdapGroup ldapGroup) throws MessagingException {
        String ldapGroupLabel = ldapGroup.getLabel();
        String authProviderId = ldapGroup.getAuthProviderId();
        String projects = String.join(", ", ldapGroup.getLdapGroupProjects().stream().map(ldapGroupProject -> ldapGroupProject.getProjectId()).collect(Collectors.toList()));
        String email = XDAT.getSiteConfigPreferences().getAdminEmail();
        log.info("Sending email about the disabling of the LDAP Group '{}' to {}", ldapGroupLabel, email);

        String subject = String.format(EMAIL_SUBJECT_FORMAT, ldapGroupLabel);
        Map<String, String> params = new HashMap<>();
        Context context = new VelocityContext(params);
        context.put("label", ldapGroupLabel);
        context.put("time", (new Date()).toString());
        context.put("projects", projects);
        context.put("reason", String.format("LDAP Auth Provider ID '%s' is not available", authProviderId));

        String htmlBody = emailBody(context, true);
        String textBody = emailBody(context, false);
        XDAT.getMailService().sendHtmlMessage(email, new String[]{email}, null, null, subject, htmlBody, textBody);
    }

    private String emailBody(final Context context, final boolean html) {
//        context.put("summary", parameters.get("summary"));
//        context.put("time", (new Date()).toString());
//        context.put("user", user);
//        context.put("postgres_version", PoolDBUtils.ReturnStatisticQuery("SELECT version();", "version", user.getDBName(), user.getLogin()));
//        context.put("siteLogoPath", XDAT.getSiteLogoPath());

        if (html) {
            context.put("html", "html");
            return AdminUtils.populateVmTemplate(context, "/screens/email/DisabledLdapGroupHtml.vm");
        } else {
            return AdminUtils.populateVmTemplate(context, "/screens/email/DisabledLdapGroup.vm");
        }
    }
}
