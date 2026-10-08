/**
 * js/ui/theme.js
 * Controls light/dark/system theme states and persists preferences.
 */

const THEMES = ['system', 'light', 'dark'];

/**
 * Returns the resolved appearance ('dark' or 'light')
 */
export function getEffectiveTheme(preference) {
    if (preference === 'system' || !preference) return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    return preference;
}

export function updateThemeIcon() {
    const btn = document.getElementById('themeToggle');
    if (!btn) return;

    const savedPreference = localStorage.getItem('theme-preference') || 'system';

    // Update icon to reflect current preference state
    switch (savedPreference) {
        case 'system':
            btn.innerHTML = '<i class="fa-solid fa-circle-half-stroke"></i>';
            btn.setAttribute('aria-label', 'Theme: System default');
            btn.title = 'Theme: System';
            break;
        case 'light':
            btn.innerHTML = '<i class="fa-solid fa-sun"></i>';
            btn.setAttribute('aria-label', 'Theme: Light');
            btn.title = 'Theme: Light';
            break;
        case 'dark':
            btn.innerHTML = '<i class="fa-solid fa-moon"></i>';
            btn.setAttribute('aria-label', 'Theme: Dark');
            btn.title = 'Theme: Dark';
            break;
    }
}

/**
 * Applies the effective theme to the DOM.
 */
export function applyTheme(preference) {
    const html = document.documentElement;
    const effective = getEffectiveTheme(preference);

    html.setAttribute('data-theme', preference); // 'system' | 'light' | 'dark'
    html.setAttribute('data-color-scheme', effective); // 'dark' | 'light'
    
    updateThemeIcon();
}

export function initTheme() {
    const savedTheme = localStorage.getItem('theme-preference') || 'system';
    applyTheme(savedTheme);

    // React immediately when the user changes OS appearance
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
        const currentPreference = localStorage.getItem('theme-preference') || 'system';
        if (currentPreference === 'system') applyTheme('system');
    });
}

export function toggleTheme() {
    const current = localStorage.getItem('theme-preference') || 'system';
    const nextIndex = (THEMES.indexOf(current) + 1) % THEMES.length;
    const nextTheme = THEMES[nextIndex];

    localStorage.setItem('theme-preference', nextTheme);
    applyTheme(nextTheme);
}