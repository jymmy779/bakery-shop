(() => {
  const dialog = document.getElementById('app-confirm-dialog');
  const toast = document.getElementById('app-toast');

  if (dialog && !window.AppConfirm) {
    const title = dialog.querySelector('#app-confirm-title');
    const message = dialog.querySelector('#app-confirm-message');
    const cancel = dialog.querySelector('#app-confirm-cancel');
    const accept = dialog.querySelector('#app-confirm-accept');
    let resolveChoice;
    let trigger;

    const finish = value => {
      const resolve = resolveChoice;
      resolveChoice = null;
      if (dialog.open) dialog.close();
      resolve?.(value);
      trigger?.focus({preventScroll: true});
      trigger = null;
    };

    cancel.addEventListener('click', () => finish(false));
    accept.addEventListener('click', () => finish(true));
    dialog.addEventListener('click', event => {
      if (event.target === dialog) finish(false);
    });
    dialog.addEventListener('cancel', event => {
      event.preventDefault();
      finish(false);
    });

    window.AppConfirm = {
      open({heading = 'Xác nhận xóa', text, confirmLabel = 'Xóa'} = {}) {
        if (!text || dialog.open) return Promise.resolve(false);
        title.textContent = heading;
        message.textContent = text;
        accept.textContent = confirmLabel;
        trigger = document.activeElement;
        return new Promise(resolve => {
          resolveChoice = resolve;
          dialog.showModal();
          cancel.focus();
        });
      }
    };
  }

  if (toast && !window.AppToast) {
    const message = toast.querySelector('[data-toast-message]');
    const icon = toast.querySelector('[data-toast-icon]');
    const card = toast.firstElementChild;
    let dismissTimer;

    window.AppToast = {
      show(text, type = 'success') {
        const isError = type === 'error';
        message.textContent = text;
        toast.setAttribute('role', isError ? 'alert' : 'status');
        icon.textContent = isError ? 'error' : 'check_circle';
        icon.classList.toggle('text-secondary-fixed', !isError);
        icon.classList.toggle('text-error-container', isError);
        card.classList.toggle('bg-inverse-surface', !isError);
        card.classList.toggle('text-inverse-on-surface', !isError);
        card.classList.toggle('bg-error', isError);
        card.classList.toggle('text-on-error', isError);
        toast.classList.remove('-translate-y-3', 'opacity-0', 'pointer-events-none');
        toast.classList.add('translate-y-0', 'opacity-100');
        clearTimeout(dismissTimer);
        dismissTimer = setTimeout(() => {
          toast.classList.add('-translate-y-3', 'opacity-0', 'pointer-events-none');
          toast.classList.remove('translate-y-0', 'opacity-100');
        }, 3200);
      }
    };
  }
})();
