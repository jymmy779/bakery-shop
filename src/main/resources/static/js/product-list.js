(() => {
  const grid = document.getElementById('product-grid');
  if (!grid) return;
  const count = document.getElementById('result-count');
  const resultContext = document.getElementById('result-context');
  const categories = document.getElementById('category-filters');
  const price = document.getElementById('price-filter');
  const sort = document.getElementById('sort-filter');
  const search = document.getElementById('search-input');
  const pagination = document.getElementById('catalog-pagination');
  const initial = new URLSearchParams(location.search);
  const sorts = {
    latest: 'newest',
    priceAsc: 'price_asc',
    priceDesc: 'price_desc',
    nameAsc: 'name'
  };
  const labels = {
    NEW: 'Mới',
    BEST_SELLER: 'Bán chạy',
    LIMITED: 'Giới hạn',
    SEASONAL: 'Theo mùa'
  };
  const escapeHtml = value => String(value == null ? '' : value).replace(/[&<>"']/g,
    char => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;'
    } [char]));
  const money = value => new Intl.NumberFormat('vi-VN').format(value) + 'đ';
  let categoryId = initial.get('categoryId') || '';
  let page = Math.max(0, Number.parseInt(initial.get('page'), 10) || 0);
  let pending;
  search.value = initial.get('keyword') || '';
  if (resultContext && search.value.trim()) {
    resultContext.textContent = `Kết quả cho “${search.value.trim()}” ·`;
  }
  sort.value = Object.keys(sorts).find(key => sorts[key] === initial.get('sort')) || 'latest';
  price.value = ['all', 'under100', '100to300', 'above300'].includes(initial.get('price')) ?
    initial.get('price') : 'all';

  function card(product) {
    const name = escapeHtml(product.name);
    const image = product.primaryImagePath;
    const safeImage = image && (/^\/(?!\/)/.test(image) || /^https?:\/\//i.test(image)) ?
      image : '/assets/product-placeholder.svg';
    return `<article class="card"><div class="image-wrap"><a href="/products/${product.id}">
      <img src="${escapeHtml(safeImage)}" alt="${name}" loading="lazy"
        onerror="this.onerror=null;this.src='/assets/product-placeholder.svg'"></a>
      ${labels[product.badge] ? `<span class="badge ${product.badge}">${labels[product.badge]}</span>` : ''}
      </div><div class="card-body"><span class="category">${escapeHtml(product.categoryName)}</span>
      <h2><a href="/products/${product.id}">${name}</a></h2>
      <p class="description">${escapeHtml(product.description)}</p>
      <div class="card-bottom"><span class="price">${money(product.price)}</span>
      <a class="add" href="/products/${product.id}" aria-label="Xem ${name}"><span class="material-symbols-outlined">arrow_forward</span></a>
      </div></div></article>`;
  }

  function message(text) {
    grid.innerHTML = `<div class="empty" style="display:block" role="status">${escapeHtml(text)}</div>`;
  }

  async function loadProducts() {
    if (pending) pending.abort();
    const controller = new AbortController();
    pending = controller;
    const params = new URLSearchParams({
      page,
      size: 12,
      sort: sorts[sort.value] || 'newest'
    });
    if (search.value.trim()) params.set('keyword', search.value.trim());
    if (categoryId) params.set('categoryId', categoryId);
    if (price.value === 'under100') params.set('maxPrice', '99999.99');
    if (price.value === '100to300') {
      params.set('minPrice', '100000');
      params.set('maxPrice', '300000');
    }
    if (price.value === 'above300') params.set('minPrice', '300000.01');
    const url = new URLSearchParams(params);
    url.delete('size');
    url.delete('minPrice');
    url.delete('maxPrice');
    if (price.value !== 'all') url.set('price', price.value);
    const query = url.toString();
    history.replaceState(null, '', location.pathname + (query ? '?' + query : ''));
    grid.setAttribute('aria-busy', 'true');
    message('Đang tải sản phẩm...');
    count.textContent = '…';
    pagination.innerHTML = '';
    try {
      const response = await fetch('/api/products?' + params, {
        headers: {
          Accept: 'application/json'
        },
        signal: controller.signal
      });
      if (!response.ok) throw new Error('Không thể tải sản phẩm');
      const data = await response.json();
      if (controller.signal.aborted) return;
      if (page > 0 && page >= data.totalPages) {
        page = Math.max(0, data.totalPages - 1);
        return loadProducts();
      }
      count.textContent = data.totalElements;
      if (resultContext) {
        resultContext.textContent = search.value.trim() ?
          `Kết quả cho “${search.value.trim()}” ·` :
          'Đang hiển thị';
      }
      grid.innerHTML = data.products.map(card).join('');
      if (pagination.dataset.scrollAfterLoad === 'true') {
        pagination.dataset.scrollAfterLoad = 'false';
        document.querySelector('.categories')?.scrollIntoView({
          behavior: 'smooth',
          block: 'start'
        });
      }
      if (!data.products.length) message('Không tìm thấy sản phẩm phù hợp.');
      const button = (label, target, disabled) => `<button type="button" class="page" data-page="${target}" ${disabled ? 'disabled' : ''}>${label}</button>`;
      if (data.totalPages > 1) pagination.innerHTML =
        button('←', page - 1, page === 0) + `<span aria-live="polite">Trang ${page + 1} / ${data.totalPages}</span>` +
        button('→', page + 1, page + 1 >= data.totalPages);
    } catch (error) {
      if (error.name === 'AbortError') return;
      count.textContent = '0';
      message('Không thể tải sản phẩm. Vui lòng tải lại trang để thử lại.');
    } finally {
      if (pending === controller) grid.setAttribute('aria-busy', 'false');
    }
  }

  async function loadCategories() {
    try {
      const response = await fetch('/api/categories', {
        headers: {
          Accept: 'application/json'
        }
      });
      if (!response.ok) throw new Error('Không thể tải danh mục');
      const data = await response.json();
      categories.insertAdjacentHTML('beforeend', data.map(item =>
        `<button type="button" class="chip" data-category="${item.id}">${escapeHtml(item.name)}</button>`).join(''));
      categories.querySelectorAll('[data-category]').forEach(button =>
        button.classList.toggle('active', button.dataset.category === categoryId));
    } catch (error) {
      categories.insertAdjacentHTML('beforeend', '<span role="status">Không thể tải danh mục. Vui lòng tải lại trang.</span>');
    }
  }
  const reset = () => {
    page = 0;
    loadProducts();
  };
  price.addEventListener('change', reset);
  sort.addEventListener('change', reset);
  const headerSearch = document.getElementById('header-search');
  if (headerSearch) headerSearch.addEventListener('submit', event => {
    event.preventDefault();
    search.value = search.value.trim();
    pagination.dataset.scrollAfterLoad = 'true';
    reset();
  });
  categories.addEventListener('click', event => {
    const button = event.target.closest('[data-category]');
    if (!button) return;
    categoryId = button.dataset.category;
    categories.querySelectorAll('[data-category]').forEach(item => item.classList.toggle('active', item === button));
    reset();
  });
  pagination.addEventListener('click', event => {
    const button = event.target.closest('[data-page]');
    if (!button || button.disabled) return;
    page = Number(button.dataset.page);
    pagination.dataset.scrollAfterLoad = 'true';
    loadProducts();
  });
  loadCategories();
  loadProducts();
})();
