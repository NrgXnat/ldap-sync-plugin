XNAT = getObject(XNAT || {});
XNAT.plugin = getObject(XNAT.plugin || {});
XNAT.plugin.ldapSync = getObject(XNAT.plugin.ldapSync || {});

(function(factory){
    if (typeof define === 'function' && define.amd) {
        define(factory);
    }
    else if (typeof exports === 'object') {
        module.exports = factory();
    }
    else {
        return factory();
    }
}(function() {
    const init_banner = function() {
        const projectID = XNAT.data.page.projectID;

        const disable_user_mgmt_buttons = function() {
            $('#invite_user_button').prop('disabled', true);
            $('#popup_all_users_button').prop('disabled', true);
            const observer = new MutationObserver(function(mutations) {
                mutations.forEach(function(mutation) {
                    if (mutation.addedNodes && mutation.addedNodes.length > 0) {
                        // element added to DOM
                        mutation.addedNodes.forEach(function(el) {
                            const $el = $(el).find('.remove-user');
                            if ($el.length > 0) {
                                $el.prop('disabled', true);
                                $el.find('.x').addClass('disabled');
                            }
                        });
                    }
                });
            });
            observer.observe(
                document.getElementById('user_mgmt_div'),
                {
                    childList: true,
                    subtree: true
                }
            );
        };

        const show_warning_message = function() {
            XNAT.plugin.ldapSync.ldapWarningContainer.show();
            XNAT.plugin.ldapSync.ldapWarningContainer.find("span.loading").hide();
            XNAT.plugin.ldapSync.ldapWarningContainer.find("span.message").show();
        };

        XNAT.xhr.get({
            url: XNAT.url.rootUrl('/xapi/ldap-sync/is-ldap-managed/' + projectID),
            success: function(enabled) {
                if (enabled) {
                    disable_user_mgmt_buttons();
                    show_warning_message();
                } else {
                    XNAT.plugin.ldapSync.ldapWarningContainer.hide();
                }
            },
            error: function(xhr) {
                XNAT.plugin.ldapSync.ldapWarningContainer.hide();
                if (xhr.status !== 403) {
                    XNAT.ui.banner.top(
                        3000,
                        "Failed to check if this project's access is managed by LDAP",
                        'error'
                    );
                }
            }
        });
    }

    $(document).ready(function() {
        XNAT.plugin.ldapSync.ldapWarningContainer = $('#ldap-warning-container');

        init_banner();

        XNAT.plugin.ldapSync.ldapWarningContainer.find("span.close").click(function() {
            XNAT.plugin.ldapSync.ldapWarningContainer.hide();
        });
    });
}));