/**
 * js/ui/theme.js
 * Controls light/dark/system theme states and persists preferences.
 */

export function updateThemeIcon() {
    const btn = document.getElementById('themeToggle');
    if (!btn) return;

    const html = document.documentElement;
    const currentTheme = html.getAttribute('data-theme');
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const isDark = currentTheme === 'dark' || (currentTheme === 'system' && prefersDark);

    btn.innerHTML = isDark ? '<i class="fa-solid fa-sun"></i>' : '<i class="fa-solid fa-moon"></i>';
}

export function initTheme() {
    const savedTheme = localStorage.getItem('theme-preference');
    if (savedTheme) {
        document.documentElement.setAttribute('data-theme', savedTheme);
    }
    updateThemeIcon();

    // Keep icon synced if the user is using system preferences and changes OS theme
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
        if (document.documentElement.getAttribute('data-theme') === 'system') {
            updateThemeIcon();
        }
    });
}

export function toggleTheme() {
    const html = document.documentElement;
    const current = html.getAttribute('data-theme');
    const next = current === 'dark' ? 'light' : 'dark';

    html.setAttribute('data-theme', next);
    localStorage.setItem('theme-preference', next);
    updateThemeIcon();
}