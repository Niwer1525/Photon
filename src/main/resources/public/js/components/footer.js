export class IncFooter extends HTMLElement {
    connectedCallback() {
        this.innerHTML = `
            <div class="footer-links">
                <a href="#" id="footerStoreLink" target="_blank" rel="noopener noreferrer" title="Store">
                    <i class="fa-solid fa-store fa-lg"></i>
                </a>
                <a href="#" id="footerTosLink" target="_blank" rel="noopener noreferrer" title="Terms of Service">
                    <i class="fa-solid fa-scale-balanced fa-lg"></i>
                </a>
                <a href="#" id="footerTosaleLink" target="_blank" rel="noopener noreferrer" title="Terms of Sale">
                    <i class="fa-solid fa-scale-unbalanced fa-lg"></i>
                </a>
                <a href="#" id="footerPrivacyLink" target="_blank" rel="noopener noreferrer" title="Privacy Policy">
                    <i class="fa-solid fa-shield-halved fa-lg"></i>
                </a>
            </div>
            <p class="footer-backend">
                Powered by <a href="https://git.niwer.dev/Photon" target="_blank" rel="noopener noreferrer">Photon</a>, the open-source backend behind Niwer's products. Inspect our code and verify its security anytime.
            </p>
        `;
    }
}

customElements.define('inc-footer', IncFooter);