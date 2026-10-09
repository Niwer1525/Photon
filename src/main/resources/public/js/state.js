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
    licenseProducts: [],
    entitlements: [],
};