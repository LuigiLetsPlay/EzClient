(function setInitialTheme() {
    function getUserPreference() {
        if (window.localStorage.getItem("theme")) {
            return window.localStorage.getItem("theme");
        }
        return window.matchMedia("(prefers-color-scheme: dark)").matches
            ? "dark"
            : "light";
    }

    const theme = getUserPreference();
    document.documentElement.dataset.theme = theme;
})();
