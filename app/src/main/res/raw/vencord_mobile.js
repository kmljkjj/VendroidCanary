// Vendroid Canary — mobile helpers (runs after Vencord)
(function () {
  if (window.__VendroidMobileInit) return;
  window.__VendroidMobileInit = 1;

  window.VencordMobile = window.VencordMobile || {
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

  try {
    var style = document.createElement("style");
    style.id = "vendroid-canary-css";
    style.textContent = [
      "html,body{overscroll-behavior:none;-webkit-tap-highlight-color:transparent;}",
      "/* Reduce some paint cost on low-end devices */",
      "*{scrollbar-width:thin;}",
    ].join("");
    (document.head || document.documentElement).appendChild(style);
  } catch (e) {}

  // Discord sometimes crashes settings when window.outerWidth is 0 in WebView
  try {
    if (!window.outerWidth || window.outerWidth < 100) {
      Object.defineProperty(window, "outerWidth", {
        get: function () {
          return window.innerWidth || 1280;
        },
      });
      Object.defineProperty(window, "outerHeight", {
        get: function () {
          return window.innerHeight || 800;
        },
      });
    }
  } catch (e) {}
})();
