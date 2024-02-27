package com.radiologics.plugins.ldapsync.tasks.schedules;

import com.radiologics.plugins.ldapsync.services.LdapSynchronizationService;
import lombok.extern.slf4j.Slf4j;
import org.nrg.framework.task.XnatTask;
import org.nrg.framework.task.services.XnatTaskService;
import org.nrg.xft.schema.XFTManager;
import org.nrg.xnat.services.XnatAppInfo;
import org.nrg.xnat.task.AbstractXnatTask;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@XnatTask(taskId = "ldapSynchronization", description = "LDAP Project Permission Synchronizer", defaultExecutionResolver = "SingleNodeExecutionResolver", executionResolverConfigurable = true)
public class SynchronizationTask extends AbstractXnatTask implements Runnable {
    private final LdapSynchronizationService ldapSynchronizationService;
    private boolean haveLoggedXftInitFailure = false;

    public SynchronizationTask(final XnatTaskService taskService, final XnatAppInfo appInfo, final JdbcTemplate jdbcTemplate, final LdapSynchronizationService ldapSynchronizationService) {
        super(taskService, true, appInfo, jdbcTemplate);

        this.ldapSynchronizationService = ldapSynchronizationService;
    }

    @Override
    protected void runTask() {
        if (!XFTManager.isInitialized()) {
            if (!haveLoggedXftInitFailure) {
                log.info("XFT is not initialized, skipping synchronization task");
                haveLoggedXftInitFailure = true;
            }
            return;
        }

        log.info("Beginning the LDAP Synchronization Task");
        ldapSynchronizationService.synchronize();
    }
}
