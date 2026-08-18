(function () {
  if (window.VencordMobile) return;
  window.VencordMobile = {
    onBackPress: function () {
      try {
        if (window.history && window.history.length > 1) {
          window.history.back();
          return true;
        }
      } catch (e) {}
      return false;
    },
  };
})();
