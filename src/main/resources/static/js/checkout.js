(() => {
  'use strict';

  const form = document.getElementById('checkout-form');
  if (!form) return;
  const reviewButton = document.getElementById('checkout-review');
  const dialog = document.getElementById('checkout-review-dialog');
  const confirmButton = document.getElementById('checkout-confirm');
  let submitting = false;
  const fields = Array.from(form.querySelectorAll('.checkout-field input, .checkout-field textarea'));
  const addressFields = ['receiverName', 'receiverPhone', 'receiverAddress', 'note'];
  let manualAddress = Object.fromEntries(addressFields.map(id => [id, document.getElementById(id).value]));
  let selectedAddress = form.querySelector('[name="savedAddress"]:checked')?.value || 'manual';
  const initialAddress = form.querySelector('[name="savedAddress"]:checked');
  if (initialAddress && selectedAddress !== 'manual'
      && (initialAddress.dataset.name !== manualAddress.receiverName
          || initialAddress.dataset.phone !== manualAddress.receiverPhone
          || initialAddress.dataset.address !== manualAddress.receiverAddress)) {
    const manualRadio = form.querySelector('[name="savedAddress"][value="manual"]');
    if (manualRadio) manualRadio.checked = true;
    selectedAddress = 'manual';
  }
  if (selectedAddress !== 'manual') {
    manualAddress.receiverAddress = '';
    manualAddress.note = '';
  }

  // Keep existing text nodes so the shared icon observer can safely inspect mutations.
  const setText = (element, text) => {
    if (element.textContent === text) return;
    if (element.childNodes.length === 1 && element.firstChild.nodeType === Node.TEXT_NODE) {
      element.firstChild.nodeValue = text;
    } else {
      element.textContent = text;
    }
  };

  const showError = (field, message) => {
    const error = document.getElementById(`${field.id}-error`);
    field.setAttribute('aria-invalid', String(Boolean(message)));
    if (error) {
      setText(error, message);
      error.hidden = !message;
    }
    return !message;
  };

  function formatDate(value) {
    return value.split('-').reverse().join('/');
  }

  const validate = field => {
    const value = field.value.trim();
    let message = '';
    if (field.required && !value) message = 'Vui lòng điền thông tin này.';
    else if (field.id === 'receiverPhone' && !/^(0|\+84)[0-9]{9}$/.test(value)) message = 'Nhập số điện thoại gồm 10 chữ số bắt đầu bằng 0 hoặc dùng +84.';
    else if (field.type === 'email' && value && field.validity.typeMismatch) message = 'Vui lòng nhập địa chỉ email hợp lệ.';
    else if (field.type === 'date' && value && value < field.min) message = `Ngày nhận sớm nhất là ${formatDate(field.min)}.`;
    else if (field.validity.badInput) message = 'Vui lòng nhập giá trị hợp lệ.';
    else if (field.maxLength > 0 && field.value.length > field.maxLength) message = `Vui lòng nhập tối đa ${field.maxLength} ký tự.`;
    else if ((field.id === 'note' || field.name.startsWith('productNotes[')) && /<\/?[a-z][^>]*>|<\s*!|<\s*\?/i.test(value)) message = 'Lời nhắn chỉ nhận văn bản, vui lòng bỏ thẻ HTML.';
    return showError(field, message);
  };

  const updateCount = field => {
    const counter = document.getElementById(field.dataset.noteCounter);
    if (counter) setText(counter, `${field.value.length}/200`);
  };

  fields.forEach(field => {
    updateCount(field);
    field.addEventListener('blur', () => validate(field));
    field.addEventListener('input', () => {
      updateCount(field);
      if (field.getAttribute('aria-invalid') === 'true') validate(field);
    });
  });

  form.querySelectorAll('[name="savedAddress"]').forEach(radio => {
    radio.addEventListener('change', () => {
      if (selectedAddress === 'manual') {
        manualAddress = Object.fromEntries(addressFields.map(id => [id, document.getElementById(id).value]));
      }
      const values = radio.value === 'manual' ? manualAddress : {
        receiverName: radio.dataset.name || '', receiverPhone: radio.dataset.phone || '',
        receiverAddress: radio.dataset.address || '', note: radio.dataset.note || ''
      };
      addressFields.forEach(id => {
        const field = document.getElementById(id);
        field.value = values[id];
        showError(field, '');
        updateCount(field);
      });
      selectedAddress = radio.value;
    });
  });

  function review() {
    if (reviewButton.disabled) return;
    const invalidFields = fields.filter(field => !validate(field));
    if (invalidFields.length) {
      invalidFields[0].closest('details')?.setAttribute('open', '');
      invalidFields[0].focus();
      return;
    }
    const value = id => document.getElementById(id).value.trim();
    const preview = {
      'review-receiver': value('receiverName'), 'review-phone': value('receiverPhone'),
      'review-address': value('receiverAddress'),
      'review-delivery': `${formatDate(value('deliveryDate'))}${value('deliveryTime') ? ` · ${value('deliveryTime')}` : ''}`,
      'review-email': value('receiverEmail') || 'Không nhận qua email',
      'review-note': value('note') || 'Không có lời nhắn'
    };
    Object.entries(preview).forEach(([id, text]) => setText(document.getElementById(id), text));
    dialog.showModal();
  }

  form.querySelectorAll('.checkout-product__image img').forEach(image => {
    image.addEventListener('error', () => { image.src = '/assets/product-placeholder.svg'; }, { once: true });
    if (image.complete && !image.naturalWidth) image.src = '/assets/product-placeholder.svg';
  });

  reviewButton.addEventListener('click', review);
  form.addEventListener('submit', event => {
    if (submitting) { event.preventDefault(); return; }
    if (event.submitter !== confirmButton || !dialog.open) {
      event.preventDefault();
      review();
      return;
    }
    const invalidFields = fields.filter(field => !validate(field));
    if (invalidFields.length) {
      event.preventDefault();
      dialog.close();
      invalidFields[0].closest('details')?.setAttribute('open', '');
      invalidFields[0].focus();
      return;
    }
    submitting = true;
    confirmButton.disabled = true;
    reviewButton.disabled = true;
    setText(confirmButton, 'Đang đặt hàng…');
    form.setAttribute('aria-busy', 'true');
    dialog.querySelectorAll('[data-close-review]').forEach(button => { button.disabled = true; });
    // Continue the native same-origin POST, including Spring's CSRF token.
  });
  dialog.addEventListener('cancel', event => { if (submitting) event.preventDefault(); });
  document.querySelectorAll('[data-close-review]').forEach(button => button.addEventListener('click', () => dialog.close()));
  dialog.addEventListener('close', () => reviewButton.focus());
  window.addEventListener('pageshow', event => {
    if (event.persisted) window.location.reload();
  });
})();
