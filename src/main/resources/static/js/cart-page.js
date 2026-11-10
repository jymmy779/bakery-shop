(() => {
  const container = document.getElementById('cart-items');
  const {escape:e, money, image, request, notify} = Shop;
  let cart;
  let busy = false;
  function render(data) {
    cart = data;
    container.innerHTML = data.items.length ? data.items.map(item => `<article class="bg-surface-container-lowest rounded-xl p-space-md shadow-sm flex flex-wrap items-center gap-space-md" data-item="${item.productId}"><a href="/products/${item.productId}"><img class="w-24 h-24 rounded-lg object-cover" src="${e(image(item.imagePath))}" alt="${e(item.productName)}" onerror="this.onerror=null;this.src='/assets/product-placeholder.svg'"></a><div class="flex-1"><a href="/products/${item.productId}" class="font-semibold">${e(item.productName)}</a><p>${money(item.unitPrice)}</p><label>Số lượng <input class="w-20 rounded-lg p-2 bg-surface-container" aria-label="Số lượng ${e(item.productName)}" type="number" min="1" step="1" value="${item.quantity}" data-quantity="${item.productId}"></label></div><strong>${money(item.lineTotal)}</strong><button type="button" data-remove="${item.productId}" aria-label="Xóa ${e(item.productName)}" class="p-2 text-error">Xóa</button></article>`).join('') : '<p class="p-space-lg">Giỏ hàng đang trống. <a href="/products" class="underline">Chọn bánh ngay</a></p>';
    document.getElementById('cart-page-count').textContent = `${data.totalQuantity} món bánh`;
    document.getElementById('cart-subtotal-label').textContent = `Tạm tính (${data.totalQuantity} bánh)`;
    document.getElementById('cart-subtotal').textContent = money(data.totalAmount);
    document.getElementById('cart-total').textContent = money(data.totalAmount);
    document.getElementById('checkout-button').disabled = data.items.length === 0;
    document.getElementById('clear-cart').disabled = data.items.length === 0;
  }
  async function change(operation) {
    if (busy) return;
    busy = true;
    document.querySelectorAll('#cart-items input, #cart-items button, #clear-cart, #checkout-button').forEach(el => el.disabled = true);
    try { render(await operation()); }
    catch (error) { notify(error.message); if (cart) render(cart); }
    finally { busy = false; container.querySelectorAll('input, button').forEach(el => el.disabled = false); if (cart) {document.getElementById('clear-cart').disabled = !cart.items.length; document.getElementById('checkout-button').disabled = !cart.items.length;} }
  }
  container.addEventListener('change', event => {
    const input = event.target.closest('[data-quantity]');
    if (!input) return;
    const quantity = Number(input.value);
    if (!Number.isInteger(quantity) || quantity < 1) { notify('Số lượng phải là số nguyên từ 1 trở lên'); render(cart); return; }
    change(() => request('/cart/items/' + input.dataset.quantity, 'PATCH', {quantity}));
  });
  container.addEventListener('click', event => {const button = event.target.closest('[data-remove]'); if (button) change(() => request('/cart/items/' + button.dataset.remove, 'DELETE'));});
  document.getElementById('clear-cart').addEventListener('click', () => {
    if (!cart?.items.length || !confirm('Xóa tất cả bánh trong giỏ?')) return;
    change(async () => {
      try { for (const item of [...cart.items]) await request('/cart/items/' + item.productId, 'DELETE'); }
      catch (error) { notify(error.message); }
      return request('/cart/items');
    });
  });
  document.getElementById('checkout-button').addEventListener('click', () => {if (!busy && cart?.items.length) location.href = '/checkout';});
  request('/cart/items').then(render).catch(error => {container.textContent = error.message; notify(error.message);});
})();
