(() => {
  const container = document.getElementById('cart-items');
  if (!container) return;
  const feedback = document.getElementById('cart-feedback');
  let pending = false;
  const money = new Intl.NumberFormat('vi-VN', {maximumFractionDigits: 0});
  const show = (id, visible) => {
    document.getElementById(id).style.display = visible ? '' : 'none';
  };

  function render(cart) {
    const items = new Map(cart.items.map(item => [String(item.productId), item]));
    container.querySelectorAll('[data-product-id]').forEach(row => {
      const item = items.get(row.dataset.productId);
      if (!item) {
        row.remove();
        return;
      }
      const input = row.querySelector('.cart-quantity');
      input.value = item.quantity;
      input.dataset.currentQuantity = item.quantity;
      input.max = item.availableStock;
      input.disabled = item.availableStock < 1;
      row.querySelector('[data-action="decrease"]').disabled = item.quantity <= 1 || item.availableStock < 1;
      row.querySelector('[data-action="increase"]').disabled = !item.available || item.quantity >= item.availableStock;
      row.querySelector('[data-action="remove"]').disabled = false;
      row.querySelector('h2 a').textContent = item.productName;
      row.querySelector('[data-unit-price]').textContent = money.format(item.unitPrice);
      row.querySelector('[data-stock]').textContent = item.availableStock;
      row.querySelector('[data-availability]').textContent = item.availabilityMessage || '';
      row.querySelector('[data-line-total]').textContent = money.format(item.lineTotal) + ' ₫';
    });
    document.getElementById('cart-total-quantity').textContent = cart.totalQuantity;
    document.getElementById('cart-total-amount').textContent = money.format(cart.totalAmount) + ' ₫';
    document.getElementById('cart-preparation-days').textContent = cart.maximumPreparationDays;
    const badge = document.getElementById('cart-count');
    if (badge) badge.textContent = cart.totalQuantity;
    show('cart-content', cart.items.length > 0);
    show('cart-empty', cart.items.length === 0);
    show('cart-checkout', cart.checkoutAllowed);
    const checkout = document.getElementById('cart-checkout');
    if (cart.checkoutAllowed) checkout.setAttribute('href', '/checkout');
    else checkout.removeAttribute('href');
    show('cart-checkout-disabled', !cart.checkoutAllowed);
    show('cart-checkout-warning', !cart.checkoutAllowed);
  }

  async function change(row, method, quantity) {
    if (pending) return;
    const input = row.querySelector('.cart-quantity');
    if (method !== 'DELETE' && (!Number.isInteger(quantity) || quantity < 1
        || quantity > Number(input.max))) {
      feedback.textContent = `Số lượng phải từ 1 đến ${input.max}.`;
      input.value = input.dataset.currentQuantity;
      return;
    }
    pending = true;
    const controls = [...container.querySelectorAll('button, input')];
    const disabled = controls.map(control => control.disabled);
    controls.forEach(control => { control.disabled = true; });
    const checkout = document.getElementById('cart-checkout');
    const checkoutVisible = checkout.style.display !== 'none';
    show('cart-checkout', false);
    show('cart-checkout-disabled', true);
    feedback.textContent = '';
    try {
      const token = document.querySelector('meta[name="_csrf"]').content;
      const header = document.querySelector('meta[name="_csrf_header"]').content;
      const response = await fetch('/cart/items/' + row.dataset.productId, {
        method,
        headers: {'Content-Type': 'application/json', [header]: token},
        ...(method === 'DELETE' ? {} : {body: JSON.stringify({quantity})})
      });
      const result = await response.json();
      if (!response.ok) throw new Error(result.message || 'Không thể cập nhật giỏ hàng');
      render(result);
    } catch (error) {
      feedback.textContent = error.message || 'Không thể cập nhật giỏ hàng. Vui lòng thử lại.';
      input.value = input.dataset.currentQuantity;
      controls.forEach((control, index) => { control.disabled = disabled[index]; });
      show('cart-checkout', checkoutVisible);
      show('cart-checkout-disabled', !checkoutVisible);
    } finally {
      pending = false;
    }
  }

  container.addEventListener('click', event => {
    const button = event.target.closest('[data-action]');
    if (!button || button.disabled) return;
    const row = button.closest('[data-product-id]');
    const input = row.querySelector('.cart-quantity');
    const action = button.dataset.action;
    const quantity = action === 'increase' ? Number(input.value) + 1
      : Math.min(Number(input.max), Number(input.value) - 1);
    change(row, action === 'remove' ? 'DELETE' : 'PATCH', quantity);
  });
  container.addEventListener('change', event => {
    if (!event.target.matches('.cart-quantity')) return;
    change(event.target.closest('[data-product-id]'), 'PATCH', Number(event.target.value));
  });
})();
