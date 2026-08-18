// Vendroid mobile helpers (injected with Vencord)
(function () {
  if (window.VencordMobile) return;

  window.VencordMobile = {
    onBackPress: function () {
      // Return true if handled by page; false lets native go back
      try {
        if (window.history && window.history.length > 1) {
          window.history.back();
          return true;
        }
      } catch (e) {}
      return false;
    },
  };

  // Reduce some mobile-web jank: passive touch listeners hint
  try {
    var style = document.createElement("style");
    style.textContent =
      "html,body{overscroll-behavior:none;-webkit-tap-highlight-color:transparent;}" +
      "*{-webkit-overflow-scrolling:touch;}";
    document.documentElement.appendChild(style);
  } catch (e) {}
})();
