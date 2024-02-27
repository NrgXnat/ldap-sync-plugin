console.log('ldap-group-management.js');

var XNAT = getObject(XNAT || {});
XNAT.app = getObject(XNAT.app || {});

(function (factory) {
  if (typeof define === 'function' && define.amd) {
    define(factory);
  } else if (typeof exports === 'object') {
    module.exports = factory();
  } else {
    return factory();
  }
})(function () {
  if (!checkLibraryLoaded()) {
    return false;
  }

  var constants = {
    MODAL_WINDOW_NAME: 'loadData',
    LDAP_GROUPS_DIV: '#ldapDiv',
    LDAP_GROUPS_TABLE: '#ldapGroupTable',
    WARNING_MESSAGE_DIV: '#ldapWarningMsg',
    ADD_LDAP_GROUP_LINK: '#addNewLdapGroup',
    ADD_LDAP_GROUP_HOLDER: '#addNewLdapGroupHolder',
    MANAGE_PROJECTS_DIV: '#manageProjectsDiv',
    MANAGE_PROJECTS_TABLE: '#manageProjectsTable',
    TEST_PROJECTS_TABLE: '#testProjectsTable',
    TEST_BUTTON: '#ldapTestButton',
    TEST_BUTTON_LABEL_DEFAULT: 'Test',
    TEST_BUTTON_LABEL_RETRY: 'Retry',
    OPERATION_EDIT: 'EDIT',
    OPERATION_CLONE: 'CLONE',
    OPERATION_DELETE: 'DELETE',
    OPERATION_REFRESH: 'REFRESH',
    OPERATION_CREATE: 'CREATE',
    OPERATION_EDIT_PROJECTS: 'EDIT_PROJECTS',
    MAX_NUM_PROJECT_IDS_DISPLAYED: 5,
    TEST_PROJECT_SELECT: '#testProjectSelect',
    GROUP_DN_DESC_STATIC:
      'If you do not know this value precisely, contact your LDAP administrator.',
    GROUP_DN_DESC_DYNAMIC:
      'For dynamic groups, enter the ${PROJECT} string to use an XNAT ' +
      XNAT.app.displayNames.singular.project +
      ' ID as the group name.',
    STATIC_TYPE: 'Static',
    DYNAMIC_TYPE: 'Dynamic',
    USER_SEARCH_TYPE: 'User',
    GROUP_SEARCH_TYPE: 'Group',
    DEFAULT_USERNAME_ATTR_NAME: 'uid',
    DEFAULT_OBJECT_CLASS_NAME: 'person',
    DEFAULT_USER_MEMBERSHIP_ATTR_NAME: 'memberOf',
    DEFAULT_GROUP_MEMBER_ATTR_NAME: 'member',
    GROUP_DN_PARAMS: {
      PROJECT: 'PROJECT',
    },
  };

  var currentOperation, ldapGroupLabels, manageProjectsTable;

  XNAT.app.ldap = getObject(XNAT.app.ldap || {});

  // flag for checking if LDAP Auth Provider IDs have been retrieved
  XNAT.app.ldap.ldapAuthProvidersLoaded = false;
  XNAT.app.ldap.ldapAuthProviderIds = [];

  // flag for checking if projects have been retrieved
  XNAT.app.ldap.projectIdsLoaded = false;
  XNAT.app.ldap.projectMap = {};
  XNAT.app.ldap.projectIds = [];

  XNAT.app.ldap.ldapGroupLabels = ldapGroupLabels = [];

  XNAT.app.ldap.projectIdMap = {};
  XNAT.app.ldap.ldapGroupForManageProjects = null;
  XNAT.app.ldap.testedDNs = [];

  // Operation Constructors

  function Operation(elem, ldapGroup, type) {
    this.elem = elem;
    this.ldapGroup = ldapGroup;
    this.type = type;
  }

  // Group DN Resolution

  function constructParamStruct(param) {
    return '${' + param + '}';
  }

  function normalizeGroupDN(raw) {
    var normalized = raw;
    Object.keys(constants.GROUP_DN_PARAMS).forEach(function (fieldName) {
      normalized = normalized.replace(
        new RegExp('\\$\\{' + fieldName + '\\}', 'ig'),
        '${' + fieldName + '}'
      );
    });
    return normalized;
  }

  function resolveGroupDN(groupDN, params) {
    var resolved = normalizeGroupDN(groupDN);
    Object.keys(params).forEach(function (fieldName) {
      if (constants.GROUP_DN_PARAMS[fieldName]) {
        resolved = resolveGroupDNParam(resolved, fieldName, params[fieldName]);
      }
    });
    return resolved;
  }

  function resolveGroupDNParam(groupDN, fieldName, value) {
    return groupDN.replace(constructParamStruct(fieldName), value);
  }

  // Event Handlers

  function onEditLdapGroup(e) {
    e.preventDefault();
    var ldapGroup = adjustLdapGroup($(this).closest('tr').data());
    currentOperation = new Operation(this, ldapGroup, constants.OPERATION_EDIT);
    editLdapGroupDialog(ldapGroup);
  }

  function onExpandProjectIds(e) {
    var ldapGroup = adjustLdapGroup($(e.target).closest('tr').data());
    openProjectIdsDialog(ldapGroup);
  }

  function onTestLdap(e) {
    var $form = $(e.target).closest('form');
    $form
      .find('input[name=label]')
      .val($form.find('input[name=label]').val().trim());

    var invalidFields = getInvalidFields($form);

    if (invalidFields.length) {
      XNAT.ui.dialog.message({
        title: false,
        content:
          '<h4>Form Validation Errors Found</h4><p>Please fix errors found in the following fields: <b>' +
          invalidFields.join(', ') +
          '</b></p>',
      });
      return;
    }

    var isDynamic = $form.find('input[name=type]').checked();
    if (isDynamic) {
      var testProjectId = $form.find('select[name=testProjectId]').val();
      if (!testProjectId) {
        xmodal.alert(
          '<strong>Error:</strong> Please select a ' +
            XNAT.app.displayNames.singular.project +
            ' for test.'
        );
        return;
      }
    }

    var authProviderId = $form.find('select[name=authProviderId]').val();
    if (!authProviderId) {
      xmodal.alert('<strong>Error:</strong> Please select the Auth provider.');
      return;
    }

    var formObj = objectify($form);
    var testedDN = formObj.groupDN;
    var resolvedGroupDN = isDynamic
      ? resolveGroupDN(testedDN, { PROJECT: testProjectId })
      : testedDN;
    testLdap(formObj, resolvedGroupDN, $form);
  }

  // Actions
  function serializeForCompleteTest(formObj) {
    return (
      formObj.authProviderId +
      formObj.groupDN +
      formObj.usernameAttrName +
      formObj.ldapSearchType +
      formObj.objectClassName +
      formObj.userMembershipAttrName +
      formObj.groupMemberAttrName
    );
  }

  function testLdap(formObj, resolvedGroupDN, $form) {
    function onTestSuccess(ldapRetrievalRespObj) {
      console.log(ldapRetrievalRespObj);
      if (!ldapRetrievalRespObj.responseType) {
        displayTestResults(
          'Unexpected data returned: ' + JSON.stringify(ldapRetrievalRespObj),
          constants.TEST_BUTTON_LABEL_RETRY
        );
        return;
      }

      var responseType = ldapRetrievalRespObj.responseType;
      var message = ldapRetrievalRespObj.message;
      var ldapUsers = ldapRetrievalRespObj.ldapUsers;
      var searchFilter = ldapRetrievalRespObj.filter;

      var res = responseType.toLowerCase();
      if (!['error', 'warning', 'success'].contains(res)) {
        res = 'error';
      }
      XNAT.ui.banner.top(3000, responseType + ': ' + message, res);

      if (responseType === 'Warning' || responseType === 'Success') {
        var numUsers =
          ldapUsers && Array.isArray(ldapUsers) && ldapUsers.length
            ? ldapUsers.length
            : 0;

        var results = [];
        results.push('- Successfully connected to the LDAP server');
        if (numUsers > 0) {
          results.push(
            '- ' +
              numUsers +
              ' user' +
              (numUsers > 1 ? 's' : '') +
              ' retrieved from the specified DN'
          );
          var arrWithUsername = ldapUsers.filter(function (item) {
            return item.username;
          });
          if (arrWithUsername.length === 0) {
            results.push('- (ERROR) no users have required attribute.');
          } else if (arrWithUsername.length !== numUsers) {
            var numDiff = numUsers - arrWithUsername.length;
            results.push(
              '- (WARNING) ' +
                numDiff +
                ' user' +
                (numDiff > 1 ? 's do' : ' does') +
                ' not have required attribute.'
            );
          }
        } else {
          results.push(
            '- (ERROR) 0 user retrieved. Please check if the DN really does not have users and you wrote the right DN string.'
          );
        }

        var serializedLdapGroup = serializeForCompleteTest(formObj);
        if (isTestedDN(serializedLdapGroup)) {
          XNAT.app.ldap.testedDNs = XNAT.app.ldap.testedDNs.filter(function (
            item
          ) {
            item !== serializedLdapGroup;
          });
        }
        XNAT.app.ldap.testedDNs.push(serializedLdapGroup);

        displayTestResults(results, constants.TEST_BUTTON_LABEL_RETRY, searchFilter);
      } else {
        displayTestResults([message], constants.TEST_BUTTON_LABEL_DEFAULT, searchFilter);
      }

      closeModalPanel(constants.MODAL_WINDOW_NAME);
      xmodal.close();
    }

    function onTestFail(jqXHR) {
      var results = [jqXHR.responseText];
      displayTestResults(results, constants.TEST_BUTTON_LABEL_RETRY);
      XNAT.ui.banner.top(
        3000,
        'Test failed... please see the test result.',
        'error'
      );
    }

    function displayTestResults(results, testButtonText, searchFilter = '') {
      $form.find('textarea[name=testGroupDN]').val(resolvedGroupDN);
      $form.find('input[name=testAuthProviderId]').val(authProviderId);
      $form.find('textarea[name=searchFilter]').val(searchFilter);
      $form
        .find('textarea[name=testResultDetail]')
        .val(Array.isArray(results) ? results.join('\n') : results);
      $form.find(constants.TEST_BUTTON).text(testButtonText);
      $form.find('.test-result-div').show();
      closeModalPanel(constants.MODAL_WINDOW_NAME);
    }

    var authProviderId = formObj.authProviderId;
    XNAT.xhr.postJSON({
      url: XNAT.url.csrfUrl('/xapi/ldap-sync/ldap-retrieve'),
      data: JSON.stringify(
        Object.assign({}, formObj, { groupDN: resolvedGroupDN })
      ),
      success: function (data) {
        onTestSuccess(data);
      },
      error: onTestFail,
    });

    openModalPanel(constants.MODAL_WINDOW_NAME, 'Testing...');
  }

  function loadLdapGroups() {
    XNAT.xhr.ajax({
      type: 'GET',
      url: XNAT.url.csrfUrl('/xapi/ldap-sync/prefs/groups/all'),
      dataType: 'json',
      success: showLdapGroups,
      error: handleLdapGroupsSearchFailure,
    });

    openModalPanel(constants.MODAL_WINDOW_NAME, 'Loading data...');
  }

  function synchronizeLdapGroup() {
    XNAT.xhr.ajax({
      type: 'POST',
      url: XNAT.url.csrfUrl(
        '/xapi/ldap-sync/synchronize/' + currentOperation.ldapGroup.label
      ),
      success: reload,
      error: function (jqXHR) {
        closeModalPanel(constants.MODAL_WINDOW_NAME);
        alert(
          'Could not delete LDAP Group: ' +
            jqXHR.status +
            ': ' +
            jqXHR.responseText
        );
      },
    });
    openModalPanel(constants.MODAL_WINDOW_NAME, 'Synchronizing...');
  }

  function deleteLdapGroup() {
    XNAT.xhr.ajax({
      type: 'DELETE',
      url: XNAT.url.csrfUrl(
        '/xapi/ldap-sync/prefs/groups/' + currentOperation.ldapGroup.label
      ),
      success: loadLdapGroups,
      error: function (jqXHR) {
        closeModalPanel(constants.MODAL_WINDOW_NAME);
        alert(
          'Could not delete LDAP Group: ' +
            jqXHR.status +
            ': ' +
            jqXHR.responseText
        );
      },
    });

    openModalPanel(constants.MODAL_WINDOW_NAME, 'Loading data...');
  }

  function objectify($form) {
    var jsonObj = JSON.parse(JSON.stringify($form));
    jsonObj.label = String(jsonObj.label).trim();
    jsonObj.type =
      jsonObj.type || jsonObj.type === constants.DYNAMIC_TYPE
        ? constants.DYNAMIC_TYPE
        : constants.STATIC_TYPE;
    jsonObj.groupDN = normalizeGroupDN(jsonObj.groupDN);
    jsonObj.ldapSearchType =
      jsonObj.ldapSearchType ||
      jsonObj.ldapSearchType === constants.GROUP_SEARCH_TYPE
        ? constants.GROUP_SEARCH_TYPE
        : constants.USER_SEARCH_TYPE;
    return jsonObj;
  }

  function stringifyForm($form) {
    return JSON.stringify(objectify($form));
  }

  function editLdapGroup($form) {
    XNAT.app.ldap.ldapGroupForManageProjects = null;
    XNAT.xhr.putJSON({
      url: XNAT.url.csrfUrl(
        '/xapi/ldap-sync/prefs/groups/' + currentOperation.ldapGroup.label
      ),
      data: stringifyForm($form),
      success: function () {
        xmodal.close();
        XNAT.ui.dialog.closeAll();
        loadLdapGroups();
        XNAT.ui.banner.top(3000, 'Saved changes to LDAP Group', 'success');
      },
      error: function (jqXHR) {
        closeModalPanel(constants.MODAL_WINDOW_NAME);
        alert(
          'Could not modify LDAP Group: ' +
            jqXHR.status +
            ': ' +
            jqXHR.responseText
        );
      },
    });

    openModalPanel(constants.MODAL_WINDOW_NAME, 'Loading data...');
  }

  function addLdapGroup($form) {
    XNAT.xhr.postJSON({
      url: XNAT.url.csrfUrl('/xapi/ldap-sync/prefs/groups'),
      data: stringifyForm($form),
      success: function (ldapGroup) {
        XNAT.app.ldap.ldapGroupForManageProjects = ldapGroup;
        xmodal.close();
        XNAT.ui.dialog.closeAll();
        loadLdapGroups();
        XNAT.ui.banner.top(3000, 'Created new LDAP Group', 'success');
      },
      error: function (jqXHR) {
        closeModalPanel(constants.MODAL_WINDOW_NAME);
        alert(
          'Could not create new LDAP Group: ' +
            jqXHR.status +
            ': ' +
            jqXHR.responseText
        );
      },
    });

    openModalPanel(constants.MODAL_WINDOW_NAME, 'Loading data...');
  }

  // Utilities

  function capitalizeFirstLetter(string) {
    return string.charAt(0).toUpperCase() + string.slice(1).toLowerCase();
  }

  function checkLibraryLoaded() {
    if (!$ || !$.fn) {
      console.error(
        'Failed to process ldap-group-management.js: JQuery library is not avaliable.'
      );
      return false;
    }
    return true;
  }

  function isTestedDN(serializedLdapGroup) {
    if (!serializedLdapGroup) {
      return false;
    }
    return XNAT.app.ldap.testedDNs.contains(serializedLdapGroup);
  }

  function adjustLdapGroup(raw) {
    raw.label = String(raw.label);
    if (!raw.groupDN && raw.groupDn) {
      raw.groupDN = raw.groupDn;
      delete raw.groupDn;
    }
    if (!Array.isArray(raw.projectIds)) {
      raw.projectIds = raw.projectIds.split(',').filter(function (i) {
        return i.trim();
      });
    }
    return raw;
  }

  function getCheckedProjectIds($form, label) {
    var checkedProjectIds = [];
    XNAT.app.ldap.projectIds.forEach(function (projectId) {
      if ($form.find('input[id=prj_' + projectId + ']').is(':checked')) {
        if (
          !XNAT.app.ldap.projectIdMap[projectId] ||
          XNAT.app.ldap.projectIdMap[projectId] === label
        ) {
          checkedProjectIds.push(projectId);
        }
      }
    });
    return checkedProjectIds;
  }

  // UIs

  function openProjectIdsDialog(ldapGroup) {
    var projectIdsTable = XNAT.table({
      className: 'xnat-table',
      style: { width: '100%' },
      id: constants.MANAGE_PROJECTS_TABLE.substring(1),
    });

    projectIdsTable
      .tr()
      .th({
        addClass: 'center',
        html: '<b>' + XNAT.app.displayNames.singular.project + ' Name</b>',
      })
      .th({
        addClass: 'center',
        html: '<b>' + XNAT.app.displayNames.singular.project + ' ID</b>',
      });

    ldapGroup.projectIds.forEach(function (projectId) {
      projectIdsTable
        .tr()
        .td([spawn('b', XNAT.app.ldap.projectMap[projectId].projectName)])
        .td([spawn('b', projectId)]);
    });

    XNAT.dialog.open({
      title:
        XNAT.app.displayNames.plural.project +
        ' for LDAP Group "' +
        ldapGroup.label +
        '"',
      width: 400,
      content: projectIdsTable.get(),
      buttons: [
        {
          label: 'Close',
          isDefault: true,
          close: true,
        },
      ],
    });
  }

  function setTestAreaVisible($form, visible) {
    if (visible) {
      $form.find('.test-form-div').show();
      $form
        .find('[data-name=groupDN] .description')
        .text(constants.GROUP_DN_DESC_DYNAMIC);
    } else {
      $form.find('.test-form-div').hide();
      $form
        .find('[data-name=groupDN] .description')
        .text(constants.GROUP_DN_DESC_STATIC);
    }
  }

  function setSearchAreaVisible($form, isSearchByGroup) {
    if (isSearchByGroup) {
      $form.find('.member-form-div').hide();
      $form.find('.group-form-div').show();
    } else {
      $form.find('.member-form-div').show();
      $form.find('.group-form-div').hide();
    }
  }

  function getInvalidFields($form) {
    var invalidFields = [];

    $form.find('.validate').each(function () {
      if (!XNAT.validate($(this)).check()) {
        $(this).addClass('invalid');
        invalidFields.push($(this).prop('name'));
      }
    });

    var isSearchByGroup = $form.find('input[name=ldapSearchType]').checked();
    if (isSearchByGroup) {
      invalidFields = invalidFields.filter(function (field) {
        return !['userMembershipAttrName'].includes(field);
      });
    } else {
      invalidFields = invalidFields.filter(function (field) {
        return !['groupMemberAttrName', 'objectClassName'].includes(field);
      });
    }

    return invalidFields;
  }

  function editLdapGroupDialog(ldapGroup) {
    ldapGroup = ldapGroup || {};
    var doWhat = capitalizeFirstLetter(currentOperation.type);
    var originalLdapGroupLabel = ldapGroup.label;

    XNAT.dialog.open({
      title: doWhat + ' LDAP Auth Group ',
      width: 800,
      className: doWhat.toLowerCase() + 'Modal',
      content: spawn('form.panel'),
      afterShow: function (obj) {
        var $form = obj.$modal.find('form');
        if (ldapGroup) {
          setTestAreaVisible($form, ldapGroup.type === constants.DYNAMIC_TYPE);
        }
        setSearchAreaVisible(
          $form,
          ldapGroup && ldapGroup.ldapSearchType === constants.GROUP_SEARCH_TYPE
        );
        $form.find('.test-result-div').hide();
        $form.find('textarea[name=testGroupDN').prop('disabled', true);
        $form.find('textarea[name=searchFilter').prop('disabled', true);
        $form.find('input[name=testAuthProviderId').prop('disabled', true);
        $form.find('textarea[name=testResultDetail').prop('disabled', true);
      },
      beforeShow: function (obj) {
        var $form = obj.$modal.find('form');
        $form.append(
          spawn('!', [
            // XNAT.ui.panel.subhead({ label: 'Basic Form' }).get(),
            XNAT.ui.panel.input.hidden({
              name: 'ldapGroupId',
            }),
            XNAT.ui.panel.input.hidden({
              name: 'ldapGroupEnabled',
              value: false,
            }),
            XNAT.ui.panel.input.text({
              name: 'label',
              label: 'Label',
              addClass: 'validate',
              validation: 'required',
              description:
                'This is strictly for XNAT administrative reference.',
            }),
            XNAT.ui.panel.input.switchbox({
              name: 'type',
              label: 'Group Type',
              onText: constants.DYNAMIC_TYPE,
              offText: constants.STATIC_TYPE,
              addClass: 'toggle-query',
              on: [
                [
                  'change',
                  function (e) {
                    var $form = $(e.target).closest('form');
                    var type = $(e.target).checked();
                    setTestAreaVisible($form, type);
                  },
                ],
              ],
            }),
            XNAT.ui.panel
              .textarea({
                name: 'groupDN',
                label: 'LDAP Group DN',
                code: 'text',
                rows: 3,
                addClass: 'validate',
                validation: 'required',
                description: '',
              })
              .get(),
            XNAT.ui.panel.select.single({
              name: 'authProviderId',
              options: XNAT.app.ldap.ldapAuthProviderIds,
              label: 'LDAP Auth Provider',
              addClass: 'validate',
              validation: 'required not-empty',
            }),
            XNAT.ui.panel.input.text({
              name: 'usernameAttrName',
              label: 'Username Attribute Name',
              addClass: 'validate',
              validation: 'required',
              description:
                'Name of the attribute that stores username ("uid", by default)',
            }),
            XNAT.ui.panel.input.text({
              name: 'objectClassName',
              label: 'Object Class Name',
              addClass: 'validate',
              validation: 'required',
              description:
                'The default Object Class name for Active Directory and OpenLDAP is "person"',
            }),
            XNAT.ui.panel.input.switchbox({
              name: 'ldapSearchType',
              label: 'LDAP Search Type',
              onText: constants.GROUP_SEARCH_TYPE,
              offText: constants.USER_SEARCH_TYPE,
              addClass: 'toggle-query',
              on: [
                [
                  'change',
                  function (e) {
                    var $form = $(e.target).closest('form');
                    var isSearchByGroup = $(e.target).checked();
                    setSearchAreaVisible($form, isSearchByGroup);
                  },
                ],
              ],
            }),
            spawn('div', { className: 'member-form-div' }, [
              XNAT.ui.panel.subhead({ label: 'User Search Type' }).get(),
              XNAT.ui.panel.input.text({
                name: 'userMembershipAttrName',
                label: 'Membership Attribute Name',
                addClass: 'validate',
                validation: 'required',
                description: '"memberOf" or "isMemberOf" are commonly used',
              }),
            ]),
            spawn('div', { className: 'group-form-div' }, [
              XNAT.ui.panel.subhead({ label: 'Group Search Type' }).get(),
              XNAT.ui.panel.input.text({
                name: 'groupMemberAttrName',
                label: 'Member Attribute Name',
                addClass: 'validate',
                validation: 'required',
                description: '"member" or "uniqueMember" are commonly used',
              }),
            ]),
            spawn('div.test-form-div', [
              spawn('hr'),
              XNAT.ui.panel.select.single({
                id: constants.TEST_PROJECT_SELECT.substring(1),
                name: 'testProjectId',
                options: XNAT.app.ldap.projectIds,
                label: 'Test ' + XNAT.app.displayNames.singular.project,
                validation: 'required not-empty',
                description:
                  'This selection is used for testing purposes only and is not saved in this configuration',
              }),
            ]),
            spawn('div', { className: 'test-btn-div' }, [
              spawn(
                'a.btn.primary|href=#!',
                { id: constants.TEST_BUTTON.substring(1) },
                constants.TEST_BUTTON_LABEL_DEFAULT
              ),
            ]),
            spawn('div', { className: 'test-result-div' }, [
              XNAT.ui.panel.subhead({ label: 'Test Result' }).get(),
              XNAT.ui.panel
                .textarea({
                  name: 'testGroupDN',
                  label: 'Group DN',
                  rows: 3,
                })
                .get(),
              XNAT.ui.panel
                .input({ name: 'testAuthProviderId', label: 'Auth Provider' })
                .get(),
              XNAT.ui.panel
                  .textarea({
                    name: 'searchFilter',
                    label: 'Filter',
                    rows: 3,
                  })
                  .get(),
              XNAT.ui.panel
                .textarea({
                  name: 'testResultDetail',
                  label: 'Result Detail',
                  rows: 4,
                })
                .get(),
            ]),
          ])
        );

        $(document).off('click', constants.TEST_BUTTON);
        $(document).on('click', constants.TEST_BUTTON, onTestLdap);

        XNAT.app.ldap.testedDNs = [];
        if (
          currentOperation.type === constants.OPERATION_EDIT ||
          currentOperation.type === constants.OPERATION_CLONE
        ) {
          $form.setValues(ldapGroup);
          $form
            .find('input[name=type]')
            .prop('checked', ldapGroup.type === constants.DYNAMIC_TYPE);
          $form
            .find('input[name=ldapSearchType]')
            .prop(
              'checked',
              ldapGroup.ldapSearchType === constants.GROUP_SEARCH_TYPE
            );
          if (currentOperation.type === constants.OPERATION_EDIT) {
            XNAT.app.ldap.testedDNs.push(serializeForCompleteTest(ldapGroup));
          }
        } else {
          $form
            .find('select')
            .find('option')
            .first()
            .prop('selected', 'selected');

          $form.find('input[name=type]').prop('checked', false);
          $form.find('input[name=ldapSearchType]').prop('checked', false);
          $form
            .find('input[name=usernameAttrName]')
            .val(constants.DEFAULT_USERNAME_ATTR_NAME);
          $form
            .find('input[name=objectClassName]')
            .val(constants.DEFAULT_OBJECT_CLASS_NAME);
          $form
            .find('input[name=userMembershipAttrName]')
            .val(constants.DEFAULT_USER_MEMBERSHIP_ATTR_NAME);
          $form
            .find('input[name=groupMemberAttrName]')
            .val(constants.DEFAULT_GROUP_MEMBER_ATTR_NAME);
        }
      },
      buttons: [
        {
          label: 'Save',
          isDefault: true,
          close: false,
          action: function (obj) {
            var $form = obj.$modal.find('form');
            $form
              .find('input[name=label]')
              .val($form.find('input[name=label]').val().trim());
            var invalidFields = getInvalidFields($form);

            if (invalidFields.length) {
              XNAT.ui.dialog.message({
                title: false,
                content:
                  '<h4>Form Validation Errors Found</h4><p>Please fix errors found in the following fields: <b>' +
                  invalidFields.join(', ') +
                  '</b></p>',
              });
              return false;
            }

            // Validate group DN
            if (!isTestedDN(serializeForCompleteTest(objectify($form)))) {
              xmodal.alert(
                '<strong>Error:</strong> A connection test with the LDAP server associated with the specified Auth provider is required to save.'
              );
              return false;
            }

            // Validate label
            var submittedLabel = $form.find('input[name=label]').val().trim();
            if (!submittedLabel.match(/^[a-z0-9\-_\s]+$/i)) {
              xmodal.alert(
                '<strong>Error:</strong> Alphanumeric, " ", "_", and "-" characters are allowed for the label.'
              );
              return false;
            }
            if (
              submittedLabel !== originalLdapGroupLabel &&
              ldapGroupLabels.contains(submittedLabel)
            ) {
              xmodal.alert('<strong>Error:</strong> Label already exists!');
              $form.find('input[name=label]').addClass('invalid');
              return false;
            }

            currentOperation.type === constants.OPERATION_EDIT
              ? editLdapGroup($form)
              : addLdapGroup($form);
          },
        },
        {
          label: 'Cancel',
          close: true,
        },
      ],
    });
  }

  function initializeProjectsCheckBoxes($form, ldapGroup) {
    XNAT.app.ldap.projectIds.forEach(function (projectId) {
      var chkbox = $form.find('input[id=prj_' + projectId + ']');
      var ldapGroupTxt = $form.find('div[id=prj_label_' + projectId + ']');
      var chkboxDisabled = false;
      var usedLdapGroupLabel = '';
      if (XNAT.app.ldap.projectIdMap[projectId]) {
        usedLdapGroupLabel = XNAT.app.ldap.projectIdMap[projectId];
        if (XNAT.app.ldap.projectIdMap[projectId] !== ldapGroup.label) {
          chkboxDisabled = true;
        }
      }

      chkbox.prop(
        'checked',
        XNAT.app.ldap.projectIdMap[projectId] === ldapGroup.label
      );
      chkbox.prop('disabled', chkboxDisabled);
      ldapGroupTxt.text(usedLdapGroupLabel);
    });
  }

  function initializeShowAllButton() {
    $(constants.MANAGE_PROJECTS_TABLE + ' .filter td:last-child').append(
      spawn('div', { style: { 'text-align': 'center' } }, [
        spawn(
          'a|id=showAllLink|href=#!',
          { style: { 'border-bottom': '1px dotted #000' } },
          ['Show All']
        ),
      ])
    );

    $(constants.MANAGE_PROJECTS_TABLE + ' #showAllLink').off('click');
    $(constants.MANAGE_PROJECTS_TABLE + ' #showAllLink').on(
      'click',
      clearFilterProjects
    );
  }

  function filterProjects(projectId, ldapGroupLabel) {
    var projectFilterInput = $(
      constants.MANAGE_PROJECTS_TABLE + ' #filter-by-projectId .filter-data'
    );
    projectFilterInput.val(projectId);
    projectFilterInput.keyup();

    var currentGroupFilterInput = $(
      constants.MANAGE_PROJECTS_TABLE + ' #filter-by-currentGroup .filter-data'
    );
    currentGroupFilterInput.val(ldapGroupLabel);
    currentGroupFilterInput.keyup();
  }

  function clearFilterProjects() {
    filterProjects('', '');
  }

  function editManageProjectsDialog(ldapGroup) {
    ldapGroup = ldapGroup || {};

    XNAT.dialog.open({
      title:
        'Manage ' +
        XNAT.app.displayNames.singular.project +
        ' Authorization for Group "' +
        ldapGroup.label +
        '"',
      width: 600,
      className: 'manageProjectsModal',
      content: spawn('form.panel'),
      beforeShow: function (obj) {
        var $form = obj.$modal.find('form');

        manageProjectsTable = XNAT.table.dataTable([], {
          id: constants.MANAGE_PROJECTS_TABLE.substring(1),
          data: XNAT.app.ldap.projectIds,
          sortable: 'projectName,projectId,currentGroup',
          filter: 'projectName,projectId,currentGroup',
          table: {
            classes: 'highlight hidden',
          },
          items: {
            projectName: {
              label: XNAT.app.displayNames.singular.project + ' Name',
              apply: function () {
                return XNAT.app.ldap.projectMap[this.toString()].projectName;
              },
            },
            projectId: {
              label: XNAT.app.displayNames.singular.project + ' ID',
              apply: function () {
                return this.toString();
              },
            },
            currentGroup: {
              label: 'Current Group',
              apply: function () {
                var projectId = this.toString();
                return spawn('div|id=prj_label_' + projectId, {
                  disabled: true,
                  className: 'project-label',
                });
              },
            },
            status: {
              label: 'Status',
              td: { className: 'center' },
              apply: function () {
                var projectId = this.toString();
                return spawn('input|type=checkbox|id=prj_' + projectId, {
                  className: 'center',
                });
              },
            },
          },
          messages: {
            noData: '' + "You don't have any alias tokens at this time.",
            error:
              'An error occurred retrieving your alias tokens from the system.',
          },
        });

        $form.append(
          spawn('!', [
            XNAT.ui.panel.input.hidden({
              name: 'label',
              value: ldapGroup.label,
            }),
            manageProjectsTable.get(),
          ])
        );

        initializeProjectsCheckBoxes($form, ldapGroup);
        initializeShowAllButton();

        filterProjects(' ', ' '); // called to prevent error
        if (!ldapGroup.projectIds || !ldapGroup.projectIds.length) {
          clearFilterProjects();
        } else {
          filterProjects('', ldapGroup.label);
        }
      },
      buttons: [
        {
          label: 'Save',
          isDefault: true,
          close: false,
          action: function (obj) {
            var $form = obj.$modal.find('form');
            var label = $form.find('input[name=label]').val();
            var checkedProjectIds = getCheckedProjectIds($form, label);
            manageProjects(label, checkedProjectIds);
          },
        },
        {
          label: 'Cancel',
          close: true,
        },
      ],
    });
  }

  function manageProjects(label, projectIds) {
    XNAT.xhr.putJSON({
      url: XNAT.url.csrfUrl(
        '/xapi/ldap-sync/prefs/groups/' + label + '/projects'
      ),
      data: JSON.stringify({ label: label, projectIds: projectIds }),
      success: function () {
        XNAT.ui.dialog.closeAll();
        loadLdapGroups();
        XNAT.ui.banner.top(3000, 'Saved changes to LDAP Group', 'success');
      },
      error: function (jqXHR) {
        closeModalPanel(constants.MODAL_WINDOW_NAME);
        alert(
          'Could not modify LDAP Group: ' +
            jqXHR.status +
            ': ' +
            jqXHR.responseText
        );
      },
    });

    openModalPanel(constants.MODAL_WINDOW_NAME, 'Loading data...');
  }

  function bindAddButtonHandler() {
    var addButtonHandler = function (optionalLdapGroup) {
      if (!XNAT.app.ldap.ldapAuthProvidersLoaded) {
        XNAT.ui.banner.top(
          2000,
          'LDAP Auth Providers have not been loaded yet. Please wait.',
          'warning'
        );
        return;
      }
      if (!XNAT.app.ldap.ldapAuthProviderIds.length) {
        XNAT.ui.banner.top(
          2000,
          'There is no LDAP Auth Provider configured. Please configure it first.',
          'warning'
        );
        return;
      }
      currentOperation = new Operation(
        this,
        optionalLdapGroup,
        optionalLdapGroup
          ? constants.OPERATION_CLONE
          : constants.OPERATION_CREATE
      );
      editLdapGroupDialog(optionalLdapGroup);
    };
    $(document).off('click', constants.ADD_LDAP_GROUP_LINK);
    $(document).on('click', constants.ADD_LDAP_GROUP_LINK, function () {
      addButtonHandler(undefined);
    });
    $(constants.LDAP_GROUPS_TABLE).on('click', '.cloneGroup', function (e) {
      var ldapGroup = adjustLdapGroup($(this).parents('tr').data());
      addButtonHandler(Object.assign({}, ldapGroup, { label: '' }));
    });
  }

  function bindEditButtonHandler() {
    var editButtonHandler = function () {
      var ldapGroup = adjustLdapGroup($(this).parents('tr').data());
      currentOperation = new Operation(
        this,
        ldapGroup,
        constants.OPERATION_EDIT
      );

      editLdapGroupDialog(ldapGroup);
    };
    $(constants.LDAP_GROUPS_TABLE).off('click', '.editRow');
    $(constants.LDAP_GROUPS_TABLE).on('click', '.editRow', editButtonHandler);
  }

  function bindEditProjectsButtonHandler() {
    var editButtonHandler = function () {
      var ldapGroup = adjustLdapGroup($(this).parents('tr').data());
      currentOperation = new Operation(
        this,
        ldapGroup,
        constants.OPERATION_EDIT_PROJECTS
      );

      editManageProjectsDialog(ldapGroup);
    };
    $(constants.LDAP_GROUPS_TABLE).on(
      'click',
      '.editProjects',
      editButtonHandler
    );
  }

  function bindRefreshButtonHandler() {
    var refreshButtonHandler = function () {
      var ldapGroup = $(this).parents('tr').data();
      currentOperation = new Operation(
        this,
        ldapGroup,
        constants.OPERATION_REFRESH
      );

      if (!ldapGroup.projectIds || ldapGroup.projectIds.length === 0) {
        XNAT.ui.banner.top(
          2000,
          'There is no ' +
            XNAT.app.displayNames.singular.project +
            ' to synchronize.',
          'warning'
        );
        return;
      }
      xmodal.open({
        width: 400,
        height: 150,
        className: 'refreshModal',
        title: 'Confirm LDAP Group Synchronization',
        content:
          'Are you sure you want to synchronize this LDAP Group "' +
          ldapGroup.label +
          '"?',
        okAction: submitCurrentOperation,
      });
    };
    $(constants.LDAP_GROUPS_TABLE).on(
      'click',
      '.refresh',
      refreshButtonHandler
    );
  }

  function bindDeleteButtonHandler() {
    var deleteButtonHandler = function () {
      var ldapGroup = $(this).parents('tr').data();
      currentOperation = new Operation(
        this,
        ldapGroup,
        constants.OPERATION_DELETE
      );

      xmodal.open({
        width: 400,
        height: 150,
        className: 'deleteModal',
        title: 'Confirm LDAP Group Deletion',
        content:
          'Are you sure you want to delete this LDAP Group "' +
          ldapGroup.label +
          '"?',
        okAction: submitCurrentOperation,
      });
    };
    $(constants.LDAP_GROUPS_TABLE).on(
      'click',
      '.deleteRow',
      deleteButtonHandler
    );
  }

  function bindExpandButtonHandler() {
    $(document).off('click', 'button.expand');
    $(document).on('click', 'button.expand', onExpandProjectIds);
  }

  function showLdapGroups(data) {
    var ldapGroupTableData = data;

    // Construct projectIdMap
    XNAT.app.ldap.projectIdMap = {};
    for (var i = 0; i < ldapGroupTableData.length; i++) {
      var ldapGroup = ldapGroupTableData[i];
      ldapGroup.projectIds.sort();
      for (var j = 0; j < ldapGroup.projectIds.length; j++) {
        var projectId = ldapGroup.projectIds[j];
        XNAT.app.ldap.projectIdMap[projectId] = ldapGroup.label;
      }
    }

    // Intialize LDAP Group table container
    $(constants.LDAP_GROUPS_DIV).empty();
    XNAT.app.ldap.ldapGroupLabels = ldapGroupLabels = [];
    for (var i = 0; i < ldapGroupTableData.length; i++) {
      ldapGroupLabels.push(ldapGroupTableData[i].label);
    }

    $(constants.LDAP_GROUPS_DIV).append(
      spawn('div.info', { style: 'margin-bottom: 20px' }, [
        spawn('p', {}, [
          'This panel enables a connected, centralized LDAP user management system to control user authorization permissions within XNAT ' +
            XNAT.app.displayNames.plural.project +
            '. XNAT synchronizes with each connected LDAP server to keep XNAT access up to date.',
        ]),
        spawn('p', {}, [
          'Once a ' +
            XNAT.app.displayNames.singular.project +
            ' begins using this authorization method, existing user roles in a ' +
            XNAT.app.displayNames.singular.project +
            ' will be preserved for all users who are authorized in the LDAP DN. New users authorized by the LDAP DN will be given ‘collaborator’ privileges by default, but can be assigned to other groups by ' +
            XNAT.app.displayNames.singular.project +
            ' owners.',
        ]),
        spawn('p', {}, [
          'If a ' +
            XNAT.app.displayNames.singular.project +
            ' stops using LDAP authorization, user permissions and roles in that ' +
            XNAT.app.displayNames.singular.project +
            ' will be preserved but will no longer be synced with the LDAP server.',
        ]),
      ])
    );

    $(constants.LDAP_GROUPS_DIV).append(
      spawn('div', { id: constants.ADD_LDAP_GROUP_HOLDER.substring(1) }, [
        spawn(
          'a.btn.primary|href=#!',
          { id: constants.ADD_LDAP_GROUP_LINK.substring(1) },
          'Define New Auth Group'
        ),
      ])
    );

    var numEnabledProjects = 0;
    for (const item of ldapGroupTableData) {
      if (item.ldapGroupEnabled) {
        numEnabledProjects += item.projectIds.length;
      }
    }
    $(constants.LDAP_GROUPS_DIV).append(
      spawn('div', { className: 'ldap-group-table-header' }, [
        'LDAP Syncing Enabled for ' +
          numEnabledProjects +
          ' of ' +
          XNAT.app.ldap.projectIds.length +
          ' ' +
          XNAT.app.displayNames.plural.project,
      ])
    );

    var WIDTH = {
      label: '120px',
      groupDN: '160px',
      authProviderId: '50px',
      projectIds: '110px',
      ldapGroupEnabled: '70px',
    };

    var ldapGroupTable = XNAT.table.dataTable([], {
      id: constants.LDAP_GROUPS_TABLE.substring(1),
      data: ldapGroupTableData,
      sortable: 'label,groupDN,authProviderId,projectIds',
      filter: 'label,groupDN,authProviderId,projectIds',
      table: {
        classes: 'highlight hidden',
        on: [['click', 'a.ldap-group-label', onEditLdapGroup]],
      },
      trs: function (tr, data) {
        var dataAttrs = Object.assign({}, data, {
          projectIds: data.projectIds.join(','),
        });
        addDataAttrs(tr, dataAttrs);
      },
      items: {
        label: {
          label: 'LDAP Group',
          th: { style: { width: WIDTH.label } },
          apply: function (label) {
            return spawn(
              'a.ldap-group-label.link',
              {
                href: '#!',
                title: label + ': refresh',
              },
              [displayLongLabel(label, WIDTH.label)]
            );
          },
        },
        groupDN: {
          label: 'Group DN',
          th: { style: { width: WIDTH.groupDN } },
          apply: function (groupDN) {
            return displayLongLabel(groupDN, WIDTH.groupDN);
          },
        },
        // type: {
        //   label: 'Type',
        //   th: { style: { width: WIDTH.type } },
        //   apply: function (type) {
        //     return displayLongLabel(type, WIDTH.type);
        //   },
        // },
        authProviderId: {
          label: 'Auth',
          th: { style: { width: WIDTH.authProviderId } },
          apply: function (authProviderId) {
            return displayLongLabel(authProviderId, WIDTH.authProviderId);
          },
        },
        projectIds: {
          label: XNAT.app.displayNames.plural.project,
          th: { style: { width: WIDTH.projectIds } },
          apply: function () {
            return displayProjectIds(this.label, this.projectIds);
          },
        },
        ldapGroupEnabled: {
          label: 'Sync<br/>Enabled',
          th: { style: { width: WIDTH.ldapGroupEnabled } },
          apply: function () {
            return displayEnabledSwitch(
              this.label,
              this.ldapGroupEnabled,
              this.projectIds
            );
          },
        },
        actions: {
          label: 'Actions',
          className: 'center',
          apply: function () {
            return actionButtons();
          },
        },
      },
      messages: {
        noData: '' + "You don't have any LDAP Group at this time.",
        error: 'An error occurred retrieving LDAP Groups from the system.',
      },
    });

    $(constants.LDAP_GROUPS_DIV).append(ldapGroupTable.get());

    function actionButtons() {
      return spawn(
        'div',
        {
          className: 'action-div',
        },
        [
          spawn('div', [
            spawn(
              'button',
              { className: 'btn action-btn editRow', title: 'Edit' },
              [spawn('i', { className: 'fa fa-pencil' })]
            ),
            spawn(
              'button',
              {
                className: 'btn action-btn editProjects',
                title: 'Manage ' + XNAT.app.displayNames.singular.project,
              },
              [spawn('i', { className: 'fa fa-gear' })]
            ),
            spawn(
              'button',
              {
                className: 'btn action-btn refresh',
                title: 'Synchronize Manually',
              },
              [spawn('i', { className: 'fa fa-refresh' })]
            ),
            spawn(
              'button',
              { className: 'btn action-btn cloneGroup', title: 'Clone' },
              [spawn('i', { className: 'fa fa-clone' })]
            ),
            spawn(
              'button',
              {
                className: 'btn action-btn deleteRow',
                title: 'Delete',
              },
              [spawn('i', { className: 'fa fa-trash' })]
            ),
          ]),
        ]
      );
    }

    function displayLongLabel(text, width) {
      return spawn(
        'span.truncate',
        { style: { width: width }, title: text },
        text
      );
    }

    function displayProjectIds(label, projectIds) {
      var spawned;
      if (!projectIds || !projectIds.length) {
        spawned = spawn('p', ['(None)']);
      } else {
        var displayedNumProjectIds = Math.min(
          constants.MAX_NUM_PROJECT_IDS_DISPLAYED,
          projectIds.length
        );
        var childs = [];
        for (var cnt = 0; cnt < projectIds.length; cnt++) {
          if (cnt < constants.MAX_NUM_PROJECT_IDS_DISPLAYED) {
            childs.push(
              spawn('div.project-id', [
                displayLongLabel(projectIds[cnt], WIDTH.projectIds),
              ])
            );
          } else {
            childs.push(spawn('div.project-id.hidden', [projectIds[cnt]]));
          }
        }
        if (projectIds.length > displayedNumProjectIds) {
          childs.push(
            spawn(
              'button.btn.btn-sm.expand',
              {
                title:
                  'Show All ' + XNAT.app.displayNames.singular.project + ' IDs',
                data: { label: label },
              },
              ['Expand']
            )
          );
        }
        spawned = spawn('div.projects-div', childs);
      }
      return spawned;
    }

    function displayEnabledSwitch(label, ldapGroupEnabled, projectIds) {
      //   if (!projectId || projectIds.length === 0) {
      //     return spawn('div', { className: 'center' }, ['']);
      //   }

      return XNAT.ui.panel.input
        .switchbox({
          name: 'ldapGroupEnabled',
          value: true,
          data: { 'ldap-group-id': label },
          checked: ldapGroupEnabled,
          on: [
            [
              'change',
              function (e) {
                var ldapGroupEnabled = $(e.target).checked();
                var ldapGroup = adjustLdapGroup(
                  $(e.target).closest('tr').data()
                );
                var text = ldapGroupEnabled ? 'enable' : 'disable';

                xmodal.open({
                  width: 400,
                  height: 150,
                  className: 'statusChangeModal',
                  title: 'Confirm to ' + text + ' for the LDAP Group',
                  content:
                    'Are you sure you want to ' +
                    text +
                    ' this LDAP Group "' +
                    ldapGroup.label +
                    '"?',
                  okAction: function () {
                    enableDisableLdapGroup(
                      ldapGroup.label,
                      ldapGroupEnabled,
                      $(e.target)
                    );
                  },
                  cancelAction: function () {
                    $(e.target).prop('checked', !ldapGroupEnabled);
                  },
                  closeBtn: false,
                });
              },
            ],
          ],
        })
        .get();
    }

    function enableDisableLdapGroup(label, ldapGroupEnabled, $target) {
      XNAT.xhr.putJSON({
        url: XNAT.url.csrfUrl(
          '/xapi/ldap-sync/prefs/groups/' + label + '/' + ldapGroupEnabled
        ),
        success: function () {
          loadLdapGroups();
          XNAT.ui.banner.top(
            3000,
            'LDAP Group "' +
              label +
              '" ' +
              (ldapGroupEnabled ? 'Enabled' : 'Disabled'),
            'success'
          );
        },
        error: function (jqXHR) {
          closeModalPanel(constants.MODAL_WINDOW_NAME);
          $target.prop('checked', !$target.checked());
          XNAT.ui.banner.top(
            3000,
            jqXHR.status + ': ' + jqXHR.responseText,
            'error'
          );
        },
      });

      openModalPanel(constants.MODAL_WINDOW_NAME, 'Loading data...');
    }

    bindAddButtonHandler();
    bindEditButtonHandler();
    bindEditProjectsButtonHandler();
    bindRefreshButtonHandler();
    bindDeleteButtonHandler();
    bindExpandButtonHandler();

    closeModalPanel(constants.MODAL_WINDOW_NAME);

    if (XNAT.app.ldap.ldapGroupForManageProjects) {
      editManageProjectsDialog(XNAT.app.ldap.ldapGroupForManageProjects);
      XNAT.app.ldap.ldapGroupForManageProjects = null;
    }
  }

  function handleLdapGroupsSearchFailure(jqXHR) {
    $(constants.LDAP_GROUPS_DIV).text(
      'Error ' + jqXHR.status + ': ' + jqXHR.responseText
    );
    closeModalPanel(constants.MODAL_WINDOW_NAME);
  }

  function populateLdapAuthProviderIds() {
    XNAT.xhr.getJSON({
      url: XNAT.url.restUrl('/xapi/ldap-sync/prefs/groups/ldap-providers'),
      fail: function (e) {
        console.log('Could not retrieve LDAP Auth Provider list', e);
      },
      success: function (data) {
        if (!Array.isArray(data)) {
          console.log('Retrieved LDAP Auth Provider List is not valid', data);
          $(constants.WARNING_MESSAGE_DIV).show();
          return;
        }
        XNAT.app.ldap.ldapAuthProviderIds = data;
        XNAT.app.ldap.ldapAuthProvidersLoaded = true;

        if (data.length > 0) {
          $(constants.WARNING_MESSAGE_DIV).hide();
        } else {
          $(constants.WARNING_MESSAGE_DIV).show();
        }
      },
    });
  }

  function populateProjectsInfo() {
    XNAT.xhr.getJSON({
      url: XNAT.url.restUrl('/REST/projects?allDataOverride=true'),
      fail: function (e) {
        console.log(
          'Could not retrieve ' +
            XNAT.app.displayNames.singular.project +
            ' list',
          e
        );
      },
      success: function (data) {
        if (!data || !data.ResultSet || !data.ResultSet.Result) {
          console.log(
            'Invalid ' +
              XNAT.app.displayNames.singular.project +
              ' list is returned',
            data
          );
          return;
        }
        XNAT.app.ldap.projectMap = {};
        data.ResultSet.Result.forEach((item) => {
          var projectId = item.ID;
          var projectName = escapeHtml(item.name);
          XNAT.app.ldap.projectMap[projectId] = { projectId, projectName };
        });
        XNAT.app.ldap.projectIds = Object.keys(XNAT.app.ldap.projectMap).sort();
        loadLdapGroups();
      },
    });
  }

  function submitCurrentOperation() {
    if (currentOperation.type === constants.OPERATION_DELETE) {
      xmodal.close();
      deleteLdapGroup();
    } else if (currentOperation.type === constants.OPERATION_REFRESH) {
      xmodal.close();
      synchronizeLdapGroup();
    } else {
      xmodal.alert(
        '<strong>Error:</strong> Unsupported operation type: ' +
          currentOperation.type
      );
    }
  }

  function reload() {
    populateProjectsInfo();
    populateLdapAuthProviderIds();
  }

  $(document).on('blur', '.validate', function () {
    $(this).removeClass('invalid');
  });

  $(document).ready(function () {
    reload();
  });
});

//# sourceURL=browsertools://scripts/xnat/plugin/ldap-sync/lpda-group-management.js
