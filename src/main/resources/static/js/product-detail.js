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
  main.src = source.src;
  main.alt = source.alt;
}

async function handleAddToCart() {
  const form = document.getElementById('product-purchase-form');
  const input = document.getElementById('quantity-input');
  const button = document.getElementById('add-to-cart-btn');
  const feedback = document.getElementById('cart-feedback');
  if (button.disabled || !form.reportValidity()) return;
  const token = document.getElementById('cart-csrf-token');
  button.disabled = true;
  feedback.textContent = 'Đang thêm vào giỏ...';
  try {
    const response = await fetch('/cart/items', {
      method: 'POST',
      headers: {'Content-Type': 'application/json', [token.dataset.header]: token.value},
      body: JSON.stringify({productId: Number(form.dataset.productId), quantity: Number(input.value)})
    });
    const result = await response.json();
    if (!response.ok) throw new Error(result.message || 'Không thể thêm sản phẩm vào giỏ');
    feedback.textContent = `Đã thêm ${input.value} ${document.getElementById('product-name').textContent} vào giỏ hàng`;
  } catch (error) {
    feedback.textContent = error.message || 'Không thể thêm sản phẩm vào giỏ. Vui lòng thử lại.';
  } finally {
    button.disabled = false;
  }
}
