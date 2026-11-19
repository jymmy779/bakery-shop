(() => {
  'use strict';
  const root = document.querySelector('[data-order-cancellation]');
  if (!root) return;
  const panel = root.querySelector('[data-cancellation-panel]');
  const form = panel.querySelector('[data-cancellation-form]');
  const reason = form.querySelector('[name="reason"]');
  const count = form.querySelector('[data-cancellation-count]');
  const error = form.querySelector('#cancel-reason-error');
  const submit = form.querySelector('[data-submit-cancellation]');
  const open = root.querySelector('[data-open-cancellation]');
  const dismiss = form.querySelector('[data-dismiss-cancellation]');
  // Update text nodes in place to avoid re-triggering the shared icon observer.
  const setText = (element, text) => {
    if (element.firstChild?.nodeType === Node.TEXT_NODE) element.firstChild.nodeValue = text;
    else element.appendChild(document.createTextNode(text));
  };
  const updateCount = () => setText(count, `${reason.value.length} / 500`);
  updateCount();
  reason.addEventListener('input', () => {
    reason.setCustomValidity('');
    reason.setAttribute('aria-invalid', 'false');
    setText(error, ' ');
    updateCount();
  });
  let submitting = false;
  form.addEventListener('submit', event => {
    if (submitting) { event.preventDefault(); return; }
    if (!reason.value.trim()) {
      event.preventDefault();
      reason.setAttribute('aria-invalid', 'true');
      reason.setCustomValidity('Vui lòng nhập lý do hủy đơn.');
      setText(error, 'Vui lòng nhập lý do hủy đơn.');
      reason.reportValidity();
      reason.focus();
      return;
    }
    submitting = true;
    submit.disabled = true;
    setText(submit, 'Đang hủy đơn…');
  });
  window.addEventListener('pageshow', event => {
    if (event.persisted) { submitting = false; submit.disabled = false; setText(submit, 'Xác nhận hủy đơn'); }
  });
  // Keep the ordinary HTML form usable when dialogs or JavaScript are unavailable.
  const dialog = document.createElement('dialog');
  if (typeof dialog.showModal !== 'function') return;
  dialog.className = 'orders-cancel__dialog';
  dialog.setAttribute('aria-labelledby', 'cancel-order-title');
  dialog.append(panel);
  root.append(dialog);
  open.hidden = false;
  dismiss.hidden = false;
  open.addEventListener('click', () => { dialog.showModal(); reason.focus(); });
  dismiss.addEventListener('click', () => dialog.close());
  dialog.addEventListener('click', event => {
    const box = dialog.getBoundingClientRect();
    if (event.target === dialog && (event.clientX < box.left || event.clientX > box.right
        || event.clientY < box.top || event.clientY > box.bottom)) dialog.close();
  });
  dialog.addEventListener('close', () => open.focus());
  if (form.dataset.cancellationInvalid === 'true') { dialog.showModal(); reason.focus(); }
})();
