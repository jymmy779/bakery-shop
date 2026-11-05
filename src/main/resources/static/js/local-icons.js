(() => {
  const iconMap = {
    account_circle: "user-round", person: "user-round", badge: "contact",
    search: "search", manage_search: "search-code", menu: "menu",
    shopping_bag: "shopping-bag", add_shopping_cart: "shopping-cart",
    arrow_forward: "arrow-right", chevron_right: "chevron-right", close: "x",
    check: "check", done: "check", check_circle: "circle-check", verified: "badge-check",
    verified_user: "shield-check", shield: "shield", schedule: "clock", timer: "timer",
    call: "phone", phone_in_talk: "phone-call", phone_iphone: "smartphone", mail: "mail",
    location_on: "map-pin", pin_drop: "map-pin", add_location_alt: "map-pin-plus",
    near_me: "navigation", location_searching: "locate-fixed", qr_code_2: "qr-code",
    support_agent: "headset", menu_book: "book-open", logout: "log-out",
    receipt_long: "receipt-text", local_shipping: "truck", cake: "cake", photo_camera: "camera",
    star: "star", stars: "sparkles", redeem: "gift", lock: "lock-keyhole",
    lock_reset: "key-round", visibility: "eye", visibility_off: "eye-off",
    error: "triangle-alert", info: "info", bolt: "zap", notifications_active: "bell-ring",
    format_quote: "quote", local_fire_department: "flame", archive: "archive", cookie: "cookie",
    bakery_dining: "croissant", oven_gen: "oven", local_cafe: "coffee", eco: "leaf",
    nature_people: "hand-heart", water_drop: "droplet", storefront: "store", spa: "leaf",
    workspace_premium: "award", award_star: "award", sticky_note_2: "notebook-pen",
    chair: "armchair", moped: "bike", grain: "wheat", favorite: "heart", edit: "pencil",
    arrow_back: "arrow-left", expand_more: "chevron-down", expand_less: "chevron-up",
    remove: "minus", add: "plus", photo: "image", inventory_2: "package", help: "circle-help",
    touch_app: "hand-heart",
  };

  function renderIcon(element) {
    const name = element.textContent.trim();
    if (!name || (element.dataset.iconName === name && element.querySelector("svg"))) return;
    const icon = iconMap[name];
    if (!icon) return;

    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.setAttribute("aria-hidden", "true");
    svg.setAttribute("focusable", "false");
    const use = document.createElementNS("http://www.w3.org/2000/svg", "use");
    use.setAttribute("href", `/assets/lucide-sprite.svg#${icon}`);
    svg.append(use);
    element.replaceChildren(svg);
    element.dataset.iconReady = "true";
    element.dataset.iconName = name;
  }

  function scan(root) {
    if (root.matches?.(".material-symbols-outlined")) renderIcon(root);
    root.querySelectorAll?.(".material-symbols-outlined").forEach(renderIcon);
  }

  scan(document);
  new MutationObserver((mutations) => {
    for (const mutation of mutations) {
      if (mutation.type === "characterData") scan(mutation.target.parentElement);
      mutation.addedNodes.forEach((node) => {
        if (node.nodeType === Node.ELEMENT_NODE) scan(node);
        else if (node.nodeType === Node.TEXT_NODE) scan(node.parentElement);
      });
      if (mutation.type === "childList") scan(mutation.target);
    }
  }).observe(document.documentElement, {subtree: true, childList: true, characterData: true});
})();
