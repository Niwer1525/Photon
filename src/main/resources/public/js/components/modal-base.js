/**
 * Shared modal template & helper
 */
export function wrapModal(id, contentHtml) {
    return `
        <div class="modal-backdrop" id="${id}">
            <div class="modal">
                <button class="modal-close icon-btn" type="button" aria-label="Close modal">
                    <i class="fa-solid fa-xmark"></i>
                </button>
                ${contentHtml}
            </div>
        </div>
    `;
}

export function bindModalClose(element, modalId) {
    const backdrop = element.querySelector(`#${modalId}`);
    const closeBtn = element.querySelector('.modal-close');

    // Click outside modal dialog to dismiss
    backdrop?.addEventListener('click', (e) => {
        if (e.target === backdrop) backdrop.classList.remove('open');
    });

    // Close button dismiss
    closeBtn?.addEventListener('click', () => {
        backdrop?.classList.remove('open');
    });
}