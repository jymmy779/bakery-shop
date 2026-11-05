window.Shop = (() => {
  const escape = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const money = value => new Intl.NumberFormat('vi-VN', {style:'currency', currency:'VND'}).format(value || 0);
  const image = path => path && (/^\/(?!\/)/.test(path) || /^https?:\/\//.test(path)) ? path : '/assets/product-placeholder.svg';
  async function request(url, method = 'GET', body) {
    const headers = {Accept:'application/json'};
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content;
    if (method !== 'GET' && token && header) headers[header] = token;
    const response = await fetch(url, {method, headers, credentials:'same-origin', body:body === undefined ? undefined : JSON.stringify(body)});
    const data = await response.json().catch(() => ({}));
    if (!response.ok || response.redirected) throw new Error(data.message || (response.status === 403 ? 'Phiên làm việc đã hết hạn. Vui lòng tải lại trang.' : 'Không thể thực hiện thao tác. Vui lòng thử lại.'));
    return data;
  }
  function notify(message) {
    let status = document.getElementById('shop-status');
    if (!status) {
      status = document.createElement('p');
      status.id = 'shop-status';
      status.setAttribute('role', 'status');
      status.style.cssText = 'position:fixed;bottom:24px;left:50%;transform:translateX(-50%);z-index:1000;background:#8b263e;color:white;padding:14px 24px;border-radius:12px;max-width:90vw';
      document.body.append(status);
    }
    status.textContent = message;
    status.hidden = false;
    clearTimeout(notify.timer);
    notify.timer = setTimeout(() => status.hidden = true, 5000);
  }
  async function add(productId, quantity = 1) {
    const cart = await request('/cart/items', 'POST', {productId:Number(productId), quantity:Number(quantity)});
    notify('Đã thêm bánh vào giỏ hàng');
    return cart;
  }
  document.addEventListener('click', async event => {
    const button = event.target.closest('[data-add-product]');
    if (!button || button.disabled) return;
    button.disabled = true;
    try { await add(button.dataset.addProduct); } catch (error) { notify(error.message); }
    finally { button.disabled = false; }
  });
  return {escape, money, image, request, notify, add};
})();
