(() => {
  const grid = document.getElementById('product-grid');
  if (!grid) return;
  const {escape:e, money, image, request} = Shop;
  const badges = {NEW:'Mới', BEST_SELLER:'Bán chạy', LIMITED:'Giới hạn', SEASONAL:'Theo mùa'};
  const categories = document.getElementById('category-filters');
  const price = document.getElementById('price-filter');
  const sort = document.getElementById('sort-filter');
  const pages = document.querySelector('.pagination');
  const search = document.getElementById('catalog-search');
  const params = new URLSearchParams(location.search);
  let categoryId = params.get('categoryId') || '';
  let page = Math.max(0, Number(params.get('page')) || 0);
  let version = 0;
  search.elements.keyword.value = params.get('keyword') || '';
  sort.value = params.get('sort') || 'name';
  if (params.has('minPrice')) price.elements.minPrice.value = params.get('minPrice');
  if (params.has('maxPrice')) price.elements.maxPrice.value = params.get('maxPrice');
  async function load() {
    const current = ++version;
    const query = new URLSearchParams({sort:sort.value, page, size:12});
    if (categoryId) query.set('categoryId', categoryId);
    const keyword = search.elements.keyword.value.trim();
    if (keyword) query.set('keyword', keyword);
    for (const key of ['minPrice','maxPrice']) if (price.elements[key].value !== '') query.set(key, price.elements[key].value);
    grid.setAttribute('aria-busy', 'true');
    try {
      const data = await request('/api/products?' + query);
      if (current !== version) return;
      grid.innerHTML = data.products.length ? data.products.map(p => `<article class="card bg-surface-container-lowest rounded-xl overflow-hidden shadow-sm"><a class="image-wrap block" style="display:block" href="/products/${p.id}"><img style="width:100%;aspect-ratio:4/3;object-fit:cover" src="${e(image(p.primaryImagePath || p.imagePaths?.[0]))}" alt="${e(p.name)}" onerror="this.onerror=null;this.src='/assets/product-placeholder.svg'">${badges[p.badge] ? `<span class="badge ${e(p.badge)}">${badges[p.badge]}</span>` : ''}</a><div class="card-body p-4"><span class="category">${e(p.categoryName)}</span><h2><a href="/products/${p.id}">${e(p.name)}</a></h2><p class="description">${e(p.description)}</p><div class="card-bottom flex justify-between items-center"><strong class="price">${money(p.price)}</strong><button class="add rounded-full p-2 bg-primary-container text-on-primary" data-add-product="${p.id}" ${p.stockQuantity < 1 ? 'disabled' : ''} aria-label="Thêm ${e(p.name)} vào giỏ">${p.stockQuantity < 1 ? 'Hết hàng' : '<span class="material-symbols-outlined">add_shopping_cart</span>'}</button></div></div></article>`).join('') : '<p>Không tìm thấy bánh phù hợp.</p>';
      document.getElementById('result-count').textContent = data.totalElements;
      pages.innerHTML = data.totalPages > 1 ? `<button class="page" data-page="${page-1}" ${page === 0 ? 'disabled' : ''}>←</button><span>Trang ${page+1} / ${data.totalPages}</span><button class="page" data-page="${page+1}" ${page+1 >= data.totalPages ? 'disabled' : ''}>→</button>` : '';
      history.replaceState(null, '', location.pathname + '?' + query);
    } catch (error) {
      if (current !== version) return;
      grid.textContent = error.message;
      pages.innerHTML = '';
      document.getElementById('result-count').textContent = '0';
    } finally { if (current === version) grid.setAttribute('aria-busy', 'false'); }
  }
  request('/api/categories').then(data => {
    categories.innerHTML = [{id:'',name:'Tất cả'}, ...data].map(c => `<button type="button" class="chip ${String(c.id) === categoryId ? 'active' : ''}" aria-pressed="${String(c.id) === categoryId}" data-category-id="${c.id}">${e(c.name)}</button>`).join('');
  }).catch(error => Shop.notify(error.message));
  categories.addEventListener('click', event => {
    const button = event.target.closest('[data-category-id]');
    if (!button) return;
    categoryId = button.dataset.categoryId;
    categories.querySelectorAll('button').forEach(b => { b.classList.toggle('active', b === button); b.setAttribute('aria-pressed', b === button); });
    page = 0; load();
  });
  for (const form of [search, price]) form.addEventListener('submit', event => {event.preventDefault(); page = 0; load();});
  sort.addEventListener('change', () => {page = 0; load();});
  pages.addEventListener('click', event => { const button = event.target.closest('[data-page]'); if (button && !button.disabled) {page = Number(button.dataset.page); load();} });
  load();
})();
