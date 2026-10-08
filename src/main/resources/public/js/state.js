/**
 * Core Application State
 */
export const state = {
    token: '',
    userToken: localStorage.getItem('photon-user-token') || '',
    account: JSON.parse(localStorage.getItem('photon-account')) || null,
    pending2FATicket: '',
    purchaseToken: new URLSearchParams(window.location.search).get('token') || '',
    activePage: 'overview',
    config: null,
    licenseProducts: [],
    entitlements: [],
    configSchema: [
        { key: 'bot_activity', label: 'Bot Activity', type: 'text' },
        { key: 'discord_bot_token', label: 'Discord Bot Token', type: 'password' },
        { key: 'discord_bot_id', label: 'Discord Bot ID', type: 'text' },
        { key: 'official_discord_server_id', label: 'Official Discord Server ID', type: 'text' },
        { key: 'network_console_channel_id', label: 'Console Channel ID', type: 'text' },
        { key: 'server_creator_role_id', label: 'Server Creator Role ID', type: 'text' },
        { key: 'webserver_port', label: 'Webserver Port', type: 'number' },
        { key: 'stripe_api_key', label: 'Stripe API Key', type: 'password' },
        { key: 'stripe_webhook_secret', label: 'Stripe Webhook Secret', type: 'password' },
        { key: 'api_version', label: 'API Version', type: 'text' },
        { key: 'mod_version', label: 'Mod Version', type: 'text' },
        { key: 'launcher_version', label: 'Launcher Version', type: 'text' },
        { key: 'store_url', label: 'Store URL', type: 'url' },
        { key: 'terms_of_service_url', label: 'Terms of Service', type: 'url' },
        { key: 'terms_of_sale_url', label: 'Terms of Sale', type: 'url' },
        { key: 'privacy_policy_url', label: 'Privacy Policy', type: 'url' }
    ]
};