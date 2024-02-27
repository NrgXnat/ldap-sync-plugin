package com.radiologics.plugins.ldapsync.aspects;

import com.radiologics.plugins.ldapsync.exceptions.NotFoundException;
import com.radiologics.plugins.ldapsync.exceptions.WorkflowException;
import com.radiologics.plugins.ldapsync.services.LdapGroupProjectEntityService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.ItemI;
import org.nrg.xnat.eventservice.aspects.EventServiceItemSaveAspect;
import org.nrg.xnat.eventservice.events.ProjectEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LdapSyncEventServiceAspect {
    private final LdapGroupProjectEntityService ldapGroupProjectEntityService;

    @Autowired
    public LdapSyncEventServiceAspect(LdapGroupProjectEntityService ldapGroupProjectEntityService) {
        this.ldapGroupProjectEntityService = ldapGroupProjectEntityService;
    }

    /**
     * This is a bit of a workaround so that we can hook to project delete without enabling event service;
     * {@link EventServiceItemSaveAspect} only triggers {@link ProjectEvent} when event service is enabled.
     *
     * @param item the item
     */
    @AfterReturning(pointcut = "@annotation(org.nrg.xft.utils.EventServiceTrigger) " +
            "&& args(item, ..)" +
            "&& execution(* org.nrg.xft.utils.SaveItemHelper.delete(..))")
    public void triggerOnItemDelete(final ItemI item) {
        if (!XnatProjectdata.SCHEMA_ELEMENT_NAME.equals(item.getXSIType())) {
            return;
        }

        String projectId = new XnatProjectdata(item).getId();
        try {
            ldapGroupProjectEntityService.delete(projectId);
        } catch (NotFoundException e) {
            log.debug("Project {} not found", projectId);
        } catch (WorkflowException e) {
            log.error("Failed to create workflow for the project " + projectId, e);
        }

    }
}
