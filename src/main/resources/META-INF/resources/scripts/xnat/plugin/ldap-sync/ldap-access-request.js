console.log('ldap-access-request.js');

(function (factory) {
  if (typeof define === 'function' && define.amd) {
    define(factory);
  } else if (typeof exports === 'object') {
    module.exports = factory();
  } else {
    return factory();
  }
})(function () {
  var constants = {
    PROJECT_LIST_DIV_ID: 'min_projects_list',
  };

  function retrieveLdapManagedProjects(resolve, reject) {
    XNAT.xhr.get({
      url: XNAT.url.rootUrl('/xapi/ldap-sync/ldap-projects'),
      dataType: 'json',
      success: resolve,
      error: function (xhr) {
        if (reject) {
          reject(xhr);
        } else {
          XNAT.ui.banner.top(
            3000,
            "Failed to check if this project's access is managed by LDAP",
            'error'
          );
        }
      },
    });
  }

  function hideActiveRequestsOfLdapProjects(ldapProjectIds) {
    ldapProjectIds.forEach(function (projectId) {
      var $projectDivs = $('#' + constants.PROJECT_LIST_DIV_ID).find(
        'div[title=' + projectId + ']'
      );
      if ($projectDivs.length === 0) {
        return;
      }
      var $desc = $projectDivs.find("div:contains('Request access')");
      if ($desc.length) {
        $desc.html(
          'This is a <b>public</b> or <b>protected</b> ' +
            XNAT.app.displayNames.singular.project.toLowerCase() +
            '.'
        );
      }
    });
  }

  $(document).ready(function () {
    // Retrieve LDAP managed projects
    retrieveLdapManagedProjects(function (ldapProjectIds) {
      // First of all, hide active requests of LDAP managed Projects
      hideActiveRequestsOfLdapProjects(ldapProjectIds);

      // Start observing just in case the projects list changes.
      var observer = new MutationObserver(function () {
        hideActiveRequestsOfLdapProjects(ldapProjectIds);
      });
      var config = {
        childList: true,
      };
      observer.observe(
        document.getElementById(constants.PROJECT_LIST_DIV_ID),
        config
      );
    });
  });
});
