package com.radiologics.plugins.ldapsync.tasks.schedules;

import com.radiologics.plugins.ldapsync.preferences.LdapSyncSitePreferences;
import com.radiologics.plugins.ldapsync.services.LdapSynchronizationService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.nrg.framework.task.services.XnatTaskService;
import org.nrg.xdat.security.user.XnatUserProvider;
import org.nrg.xnat.event.listeners.methods.AbstractScheduledXnatPreferenceHandlerMethod;
import org.nrg.xnat.services.XnatAppInfo;
import org.nrg.xnat.task.AbstractXnatRunnable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Slf4j
@Component
@Getter(PROTECTED)
@Setter(PRIVATE)
public class SynchronizationHandlerMethod extends AbstractScheduledXnatPreferenceHandlerMethod {
    private static final String REPEAT   = "ldapSynchronizationRepeat";
    private static final long MIN_REPEAT_IN_MS = 60 * 1000;

    private final XnatTaskService taskService;
    private final JmsTemplate jmsTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final XnatAppInfo appInfo;
    private final LdapSynchronizationService ldapSynchronizationService;

    private long ldapSynchronizationRepeat;

    public SynchronizationHandlerMethod(final LdapSyncSitePreferences preferences, final ThreadPoolTaskScheduler scheduler, final XnatTaskService taskService, final JmsTemplate jmsTemplate, final XnatUserProvider primaryAdminUserProvider, final XnatAppInfo appInfo, final JdbcTemplate jdbcTemplate, final LdapSynchronizationService ldapSynchronizationService) {
        super(scheduler, primaryAdminUserProvider, REPEAT);

        this.taskService = taskService;
        this.jmsTemplate = jmsTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.appInfo = appInfo;
        this.ldapSynchronizationService = ldapSynchronizationService;

        setLdapSynchronizationRepeat(preferences.getLdapSynchronizationRepeat());
    }

    @Override
    protected AbstractXnatRunnable getTask() {
        return new SynchronizationTask(getTaskService(), getAppInfo(), getJdbcTemplate(), getLdapSynchronizationService());
    }

    @Override
    protected Trigger getTrigger() {
        long period = Math.max(ldapSynchronizationRepeat * 60 * 1000, MIN_REPEAT_IN_MS);
        return new PeriodicTrigger(period);
    }

    @Override
    protected void handlePreferenceImpl(String preference, String value) {
        log.info("Found preference {} that this handler can handle, setting value to {}", preference, value);
        switch (preference) {
            case REPEAT:
                setLdapSynchronizationRepeat(Long.parseLong(value));
                break;
        }
    }
}
