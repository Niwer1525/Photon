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
            <div>
                <a href="#top" class="back-to-top" title="Back to top" aria-label="Back to top">
                    <i class="fa fa-arrow-up" aria-hidden="true"></i>
                    <span class="sr-only">Back to top</span>
                </a>
            </div>
            <hr>
            <p>Made with ❤️ by Niwer</p>
            <p>
                Powered by <a href="https://git.niwer.dev/Photon" target="_blank" rel="noopener noreferrer">Photon</a>, the open-source backend behind Niwer's products. Inspect our code and verify its security anytime.
            </p>
            <a href="https://sponsor.niwer.dev">Toss a coin to your Niwer</a>
        `;
    }
}

customElements.define('inc-footer', IncFooter);