var invalidNumberChars = ['-', '+', 'e', '.'];
$('input[name=ldapSynchronizationRepeat]').on('keydown', function (e) {
  if (invalidNumberChars.includes(e.key)) {
    e.preventDefault();
  }
});
