function incrementQty() {
  const input = document.getElementById('quantity-input');
  input.value = Math.min(Number(input.max), Math.max(1, Number(input.value) || 1) + 1);
}

function decrementQty() {
  const input = document.getElementById('quantity-input');
  input.value = Math.max(1, (Number(input.value) || 1) - 1);
}

function updateMainImage(button) {
  const source = button.querySelector('img');
  const main = document.getElementById('main-product-image');
  if (!source || !main) return;

  const previousSrc = main.getAttribute('src');
  const previousAlt = main.alt;
  main.src = source.src;
  main.alt = source.alt;
  source.src = previousSrc;
  source.alt = previousAlt;

  document.querySelectorAll('.gallery-thumb').forEach(thumbnail => {
    thumbnail.setAttribute('aria-pressed', thumbnail === button ? 'true' : 'false');
  });
}

async function handleAddToCart() {
  const form = document.getElementById('product-purchase-form');
  const input = document.getElementById('quantity-input');
  const button = document.getElementById('add-to-cart-btn');
  if (button.disabled || !form.reportValidity()) return;
  const token = document.getElementById('cart-csrf-token');
  const buttonText = document.getElementById('add-to-cart-text');
  const idleLabel = buttonText.textContent;
  button.disabled = true;
  buttonText.textContent = 'Đang thêm…';
  try {
    const response = await fetch('/cart/items', {
      method: 'POST',
      headers: {'Content-Type': 'application/json', [token.dataset.header]: token.value},
      body: JSON.stringify({productId: Number(form.dataset.productId), quantity: Number(input.value)})
    });
    const result = await response.json();
    if (!response.ok) throw new Error(result.message || 'Không thể thêm sản phẩm vào giỏ');
    window.updateCartBadge?.(result.totalQuantity);
    window.AppToast?.show(`Đã thêm ${input.value} ${document.getElementById('product-name').textContent} vào giỏ hàng`);
  } catch (error) {
    window.AppToast?.show(error.message || 'Không thể thêm sản phẩm vào giỏ. Vui lòng thử lại.', 'error');
  } finally {
    button.disabled = false;
    buttonText.textContent = idleLabel;
  }
}
