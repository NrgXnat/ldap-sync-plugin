package com.radiologics.plugins.ldapsync.utils;

import com.radiologics.plugins.ldapsync.entities.LdapGroup;
import com.radiologics.plugins.ldapsync.services.impl.LdapGroupActionType;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.utils.WorkflowUtils;

public class WorkflowUtil {
    private final static String LDAP_GROUP_TABLE_NAME = "xhbm_ldap_group";

    public static void createGroupWorkflow(final long ldapGroupId, final String label, final UserI currentUser, final LdapGroupActionType actionType) throws Exception {
        String action;
        switch (actionType) {
            case Create:
                action = "Created LDAP Auth Group";
                break;

            case Modify:
                action = "Modified LDAP Auth Group";
                break;

            case Delete:
                action = "Deleted LDAP Auth Group";
                break;

            case Enable:
                action = "Enabled LDAP Auth Group";
                break;

            case Disable:
                action = "Disabled LDAP Auth Group";
                break;

            default:
                throw new IllegalArgumentException("Action Type is not valid: " + actionType);
        }

        final PersistentWorkflowI workflow = PersistentWorkflowUtils.buildOpenWorkflow(
                currentUser,
                LDAP_GROUP_TABLE_NAME,
                Long.toString(ldapGroupId),
                "",
                EventUtils.newEventInstance(
                        EventUtils.CATEGORY.DATA,
                        EventUtils.getType("", EventUtils.TYPE.PROCESS),
                        action,
                        "",
                        label));
        workflow.setStepDescription(PersistentWorkflowUtils.COMPLETE);
        workflow.setComments("LDAP Group Label is " + label);
        WorkflowUtils.complete(workflow, workflow.buildEvent());
    }

    public static void createProjectWorkflow(String projectId, LdapGroup ldapGroup, UserI currentUser, boolean created) throws Exception {
        String action = created ? "Added project to LDAP Group" : "Deleted project from LDAP Group";
        final PersistentWorkflowI workflow = PersistentWorkflowUtils.buildOpenWorkflow(
                currentUser,
                "xnat:projectData",
                projectId,
                projectId,
                EventUtils.newEventInstance(
                        EventUtils.CATEGORY.DATA,
                        EventUtils.getType("", EventUtils.TYPE.PROCESS),
                        action,
                        "",
                        ldapGroup.getLabel()));
        workflow.setStepDescription(PersistentWorkflowUtils.COMPLETE);
        workflow.setComments("LDAP Group ID is " + ldapGroup.getId());
        WorkflowUtils.complete(workflow, workflow.buildEvent());
    }
}
