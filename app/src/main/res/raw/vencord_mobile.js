!(() => {
    const { findLazy, Common, onceReady } = Vencord.Webpack;
    const ModalEscapeHandler = findLazy(m => m.binds?.length === 1 && m.binds[0] === "esc");

    let isSidebarOpen = false;
    onceReady.then(() => {
        Common.FluxDispatcher.subscribe("MOBILE_WEB_SIDEBAR_OPEN", () => {
            isSidebarOpen = true;
        });
        Common.FluxDispatcher.subscribe("MOBILE_WEB_SIDEBAR_CLOSE", () => {
            isSidebarOpen = false;
        });
    });

    window.VencordMobile = {
        onBackPress() {
            if (ModalEscapeHandler.action() === false) return true;

            const quickCssWin = window.__VENCORD_MONACO_WIN__?.deref();
            if (quickCssWin && !quickCssWin.closed) {
                quickCssWin.close();
                delete window.__VENCORD_MONACO_WIN__;
                return true;
            }

            if (!isSidebarOpen) {
                Common.FluxDispatcher.dispatch({ type: "MOBILE_WEB_SIDEBAR_OPEN" });
                return true;
            }

            return false;
        }
    };

    document.addEventListener("DOMContentLoaded", () => document.documentElement.appendChild(
        Object.assign(document.createElement("link"), {
            rel: "stylesheet",
            type: "text/css",
            href: "https://github.com/Vendicated/Vencord/releases/download/devbuild/browser.css"
        })
    ), { once: true });
})();
