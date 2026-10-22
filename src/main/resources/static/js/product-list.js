(() => {
  const pageSize = 12;
  const form = document.getElementById('catalog-filter-form');
  const grid = document.getElementById('catalog-grid');
  const empty = document.getElementById('catalog-empty');
  const count = document.getElementById('catalog-count');
  const category = document.getElementById('category');
  const pagination = document.getElementById('catalog-pagination');
  const urlParams = new URLSearchParams(location.search);
  let products = [];
  let currentPage = Math.max(Number(urlParams.get('page') || 0), 0);

  form.elements.keyword.value = urlParams.get('keyword') || '';
  form.elements.sort.value = urlParams.get('sort') || 'name';

  const escapeHtml = value => String(value ?? '').replace(/[&<>'"]/g, char => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
  })[char]);
  const money = value => new Intl.NumberFormat('vi-VN').format(value || 0) + ' ₫';
  const imageUrl = path => !path ? '/assets/product-placeholder.svg'
    : (/^(https?:)?\//.test(path) ? path : '/' + path.replace(/^\/+/, ''));

  function createCard(product) {
    const soldOut = product.stockQuantity < 1;
    return `<article class="catalog-card">
      <a class="catalog-media" href="/products/${product.id}">
        <img src="${escapeHtml(imageUrl(product.primaryImagePath))}" alt="${escapeHtml(product.name)}" loading="lazy" onerror="this.onerror=null;this.src='/assets/product-placeholder.svg'">
        <span>${soldOut ? 'Tạm hết' : escapeHtml(product.categoryName)}</span>
      </a>
      <div class="catalog-card-body">
        <small>${escapeHtml(product.categoryName || 'LUNE SIGNATURE')}</small>
        <h2><a href="/products/${product.id}">${escapeHtml(product.name)}</a></h2>
        <p>${escapeHtml(product.description || 'Bánh tươi thủ công, làm mới mỗi ngày.')}</p>
        <div><strong>${money(product.price)}</strong><a href="/products/${product.id}" aria-label="Xem ${escapeHtml(product.name)}">→</a></div>
      </div>
    </article>`;
  }

  function syncUrl() {
    const params = new URLSearchParams();
    const keyword = form.elements.keyword.value.trim();
    if (keyword) params.set('keyword', keyword);
    if (category.value) params.set('categoryId', category.value);
    if (form.elements.sort.value !== 'name') params.set('sort', form.elements.sort.value);
    if (currentPage > 0) params.set('page', currentPage);
    history.replaceState(null, '', location.pathname + (params.size ? '?' + params : ''));
  }

  function filteredProducts() {
    const keyword = form.elements.keyword.value.trim().toLocaleLowerCase('vi');
    const categoryId = category.value;
    const sort = form.elements.sort.value;
    const result = products.filter(product => {
      const text = `${product.name} ${product.description}`.toLocaleLowerCase('vi');
      return (!keyword || text.includes(keyword))
        && (!categoryId || String(product.categoryId) === categoryId);
    });
    result.sort((a, b) => sort === 'price_asc' ? a.price - b.price
      : sort === 'price_desc' ? b.price - a.price
      : a.name.localeCompare(b.name, 'vi'));
    return result;
  }

  function pageButton(label, page, disabled, active = false) {
    return `<button type="button" data-page="${page}" ${disabled ? 'disabled' : ''} class="${active ? 'active' : ''}">${label}</button>`;
  }

  function render() {
    const result = filteredProducts();
    const totalPages = Math.ceil(result.length / pageSize);
    if (currentPage >= totalPages) currentPage = Math.max(totalPages - 1, 0);
    const visible = result.slice(currentPage * pageSize, (currentPage + 1) * pageSize);
    grid.innerHTML = visible.map(createCard).join('');
    grid.hidden = visible.length === 0;
    empty.hidden = visible.length !== 0;
    count.textContent = `${result.length} món bánh`;
    pagination.innerHTML = totalPages > 1
      ? `${pageButton('←', currentPage - 1, currentPage === 0)}${Array.from({length: totalPages}, (_, page) => pageButton(page + 1, page, false, page === currentPage)).join('')}${pageButton('→', currentPage + 1, currentPage + 1 >= totalPages)}`
      : '';
    syncUrl();
  }

  async function loadProducts() {
    try {
      const response = await fetch('/api/products?size=100&sort=name', {
        headers: {Accept: 'application/json'}
      });
      if (!response.ok) throw new Error('Không thể tải danh sách sản phẩm');
      const data = await response.json();
      products = data.products.filter(product => product.status === 'SELLING');
      const categories = [...new Map(products.map(product => [product.categoryId, product.categoryName])).entries()]
        .sort((a, b) => a[1].localeCompare(b[1], 'vi'));
      category.insertAdjacentHTML('beforeend', categories
        .map(([id, name]) => `<option value="${id}">${escapeHtml(name)}</option>`).join(''));
      category.value = urlParams.get('categoryId') || '';
      render();
    } catch (error) {
      grid.innerHTML = '<div class="catalog-loading">Không thể tải sản phẩm. Vui lòng thử lại sau.</div>';
      count.textContent = 'Không thể tải sản phẩm';
    }
  }

  form.addEventListener('submit', event => {
    event.preventDefault();
    currentPage = 0;
    render();
  });
  form.elements.sort.addEventListener('change', () => {
    currentPage = 0;
    render();
  });
  category.addEventListener('change', () => {
    currentPage = 0;
    render();
  });
  pagination.addEventListener('click', event => {
    const button = event.target.closest('[data-page]');
    if (!button || button.disabled) return;
    currentPage = Number(button.dataset.page);
    render();
    window.scrollTo({top: 180, behavior: 'smooth'});
  });

  loadProducts();
})();
