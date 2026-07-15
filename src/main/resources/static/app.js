const state = {
  token: localStorage.getItem("shopease_token"),
  user: JSON.parse(localStorage.getItem("shopease_user") || "null"),
  categories: [],
  products: [],
  cart: null,
  orders: [],
  adminOrders: [],
};

const orderStatuses = ["PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"];
const $ = (id) => document.getElementById(id);

function money(value) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(Number(value || 0));
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function pageContent(page) {
  if (!page) return [];
  if (Array.isArray(page)) return page;
  return page.content || [];
}

function showToast(message, type = "success") {
  const toast = $("toast");
  toast.textContent = message;
  toast.className = `toast ${type}`;
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => {
    toast.className = "toast hidden";
  }, 3600);
}

function getErrorMessage(error) {
  return error?.message || "Something went wrong";
}

async function api(path, options = {}) {
  const headers = {};
  const hasJsonBody = options.body && !(options.body instanceof FormData);

  if (hasJsonBody) {
    headers["Content-Type"] = "application/json";
  }

  if (state.token) {
    headers.Authorization = `Bearer ${state.token}`;
  }

  const response = await fetch(path, {
    ...options,
    headers: {
      ...headers,
      ...(options.headers || {}),
    },
  });

  const text = await response.text();
  let payload = null;

  if (text) {
    try {
      payload = JSON.parse(text);
    } catch {
      payload = text;
    }
  }

  if (!response.ok) {
    if (response.status === 401 && state.token && !path.includes("/api/auth/")) {
      clearSession(false);
    }
    throw new Error(payload?.message || `${response.status} ${response.statusText}`);
  }

  return payload?.data ?? payload;
}

function updateMetrics() {
  $("productCount").textContent = state.products.length;
  $("categoryCount").textContent = state.categories.length;
  $("cartCount").textContent = state.cart?.items?.reduce((sum, item) => sum + item.quantity, 0) || 0;
  $("cartBadge").textContent = $("cartCount").textContent;
  $("orderCount").textContent = state.orders.length;
}

function saveSession(authResponse) {
  state.token = authResponse.token;
  state.user = authResponse.user;
  localStorage.setItem("shopease_token", state.token);
  localStorage.setItem("shopease_user", JSON.stringify(state.user));
  updateSessionUi();
}

function clearSession(showMessage = true) {
  state.token = null;
  state.user = null;
  state.cart = null;
  state.orders = [];
  state.adminOrders = [];
  localStorage.removeItem("shopease_token");
  localStorage.removeItem("shopease_user");
  updateSessionUi();
  renderCart(null);
  renderOrders([]);
  renderAdminOrders([]);
  updateMetrics();

  if (showMessage) {
    showToast("Logged out");
  }
}

function updateSessionUi() {
  const badge = $("sessionBadge");
  const currentUser = $("currentUser");
  const logoutButton = $("logoutButton");
  const adminPanel = $("adminPanel");
  const adminNavLink = $("adminNavLink");

  if (!state.user) {
    badge.textContent = "Guest";
    badge.className = "pill neutral";
    currentUser.classList.add("hidden");
    logoutButton.classList.add("hidden");
    adminPanel.classList.add("hidden");
    adminNavLink.classList.add("hidden");
    renderProducts();
    return;
  }

  const role = state.user.role;
  badge.textContent = role;
  badge.className = `pill ${role === "ADMIN" ? "admin" : "user"}`;
  currentUser.innerHTML = `
    <strong>${escapeHtml(state.user.name)}</strong>
    <span>${escapeHtml(state.user.email)}</span>
    <span>Role: ${escapeHtml(role)}</span>
  `;
  currentUser.classList.remove("hidden");
  logoutButton.classList.remove("hidden");
  adminPanel.classList.toggle("hidden", role !== "ADMIN");
  adminNavLink.classList.toggle("hidden", role !== "ADMIN");
  renderProducts();
}

async function checkHealth() {
  const dot = $("apiStatusDot");

  try {
    const health = await fetch("/actuator/health").then((res) => res.json());
    const online = health.status === "UP";
    $("apiStatus").textContent = online ? "Online" : health.status;
    $("apiStatusHint").textContent = online ? "Store services are ready" : "Store service returned a non-ready status";
    dot.className = `status-dot ${online ? "online" : "offline"}`;
  } catch {
    $("apiStatus").textContent = "Offline";
    $("apiStatusHint").textContent = "Start the application from IntelliJ";
    dot.className = "status-dot offline";
  }
}

async function loadCategories() {
  state.categories = await api("/api/categories");
  renderCategoryOptions();
  updateMetrics();
}

function renderCategoryOptions() {
  const categoryOptions = state.categories
    .map((category) => `<option value="${category.id}">${escapeHtml(category.name)}</option>`)
    .join("");

  $("searchCategory").innerHTML = `<option value="">All categories</option>${categoryOptions}`;

  if (categoryOptions) {
    $("productCategory").disabled = false;
    $("productCategory").innerHTML = categoryOptions;
  } else {
    $("productCategory").disabled = true;
    $("productCategory").innerHTML = `<option value="">Create category first</option>`;
  }
}

async function loadProducts() {
  const loading = $("catalogLoading");
  const params = new URLSearchParams();
  const name = $("searchName").value.trim();
  const brand = $("searchBrand").value.trim();
  const categoryId = $("searchCategory").value;
  const sort = $("sortProducts").value;

  if (name) params.set("name", name);
  if (brand) params.set("brand", brand);
  if (categoryId) params.set("categoryId", categoryId);
  params.set("page", "0");
  params.set("size", "60");
  params.set("sort", sort);

  loading.classList.remove("hidden");

  try {
    const page = await api(`/api/products?${params.toString()}`);
    state.products = pageContent(page);
    renderProducts();
    updateMetrics();
  } finally {
    loading.classList.add("hidden");
  }
}

function renderProducts() {
  const grid = $("productGrid");
  const empty = $("emptyProducts");

  if (!grid || !empty) return;

  empty.classList.toggle("hidden", state.products.length > 0);

  grid.innerHTML = state.products
    .map((product) => {
      const firstLetter = escapeHtml(product.name?.charAt(0) || "P");
      const image = product.imageUrl
        ? `<img src="${escapeHtml(product.imageUrl)}" alt="${escapeHtml(product.name)}" onerror="this.remove()">`
        : "";
      const lowStock = Number(product.stock) <= 3;
      const disabled = product.stock < 1 ? "disabled" : "";
      const adminDelete = state.user?.role === "ADMIN"
        ? `<button class="button delete-product" data-product-id="${product.id}" type="button">Delete</button>`
        : "";

      return `
        <article class="product-card">
          <div class="product-image">
            <span class="stock-badge ${lowStock ? "low" : ""}">${product.stock} in stock</span>
            <span>${firstLetter}</span>
            ${image}
          </div>
          <div class="product-body">
            <div class="product-meta">
              <span>${escapeHtml(product.categoryName || "Category")}</span>
              <span>&bull;</span>
              <span>${escapeHtml(product.brand || "No brand")}</span>
            </div>
            <h3>${escapeHtml(product.name)}</h3>
            <p>${escapeHtml(product.description || "No description available.")}</p>
            <div class="price-row">
              <span class="price">${money(product.price)}</span>
            </div>
            <div class="product-actions">
              <button class="button primary add-to-cart" data-product-id="${product.id}" ${disabled}>
                ${product.stock < 1 ? "Out of stock" : "Add to cart"}
              </button>
              ${adminDelete}
            </div>
          </div>
        </article>
      `;
    })
    .join("");

  document.querySelectorAll(".add-to-cart").forEach((button) => {
    button.addEventListener("click", () => addToCart(Number(button.dataset.productId)));
  });

  document.querySelectorAll(".delete-product").forEach((button) => {
    button.addEventListener("click", () => deleteProduct(Number(button.dataset.productId)));
  });
}

async function addToCart(productId) {
  if (!state.token) {
    showToast("Login or register before adding products to cart", "error");
    document.location.hash = "#account";
    return;
  }

  try {
    state.cart = await api("/api/cart/add", {
      method: "POST",
      body: JSON.stringify({ productId, quantity: 1 }),
    });
    renderCart(state.cart);
    updateMetrics();
    openCartDrawer();
    showToast("Product added to cart");
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  }
}

async function deleteProduct(productId) {
  if (state.user?.role !== "ADMIN") {
    showToast("Only admin can delete products", "error");
    return;
  }

  const confirmed = window.confirm("Delete this product from the store?");
  if (!confirmed) return;

  try {
    await api(`/api/admin/products/${productId}`, { method: "DELETE" });
    showToast("Product deleted");
    await loadProducts();
    await loadCart();
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  }
}

async function loadCart() {
  if (!state.token) {
    renderCart(null);
    updateMetrics();
    return;
  }

  try {
    state.cart = await api("/api/cart");
    renderCart(state.cart);
    updateMetrics();
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  }
}

function renderCart(cart) {
  const container = $("cartItems");
  const total = $("cartTotal");
  const items = cart?.items || [];

  total.textContent = money(cart?.total || 0);

  if (!state.token) {
    container.innerHTML = `<div class="empty-state"><strong>Login required</strong><span>Login or register to start shopping.</span></div>`;
    return;
  }

  if (!items.length) {
    container.innerHTML = `<div class="empty-state"><strong>Your cart is empty</strong><span>Add products from the catalog.</span></div>`;
    return;
  }

  container.innerHTML = items
    .map(
      (item) => `
        <article class="cart-line">
          <div>
            <h3>${escapeHtml(item.productName)}</h3>
            <div class="cart-meta">
              <span>${money(item.unitPrice)} each</span>
              <span>&bull;</span>
              <span>Subtotal ${money(item.subtotal)}</span>
            </div>
          </div>
          <div class="cart-actions">
            <input id="qty-${item.id}" type="number" min="1" value="${item.quantity}" aria-label="Quantity">
            <button class="button ghost update-cart-item" data-item-id="${item.id}" type="button">Update</button>
            <button class="button danger remove-cart-item" data-item-id="${item.id}" type="button">Remove</button>
          </div>
        </article>
      `
    )
    .join("");

  document.querySelectorAll(".update-cart-item").forEach((button) => {
    button.addEventListener("click", () => updateCartItem(Number(button.dataset.itemId)));
  });

  document.querySelectorAll(".remove-cart-item").forEach((button) => {
    button.addEventListener("click", () => removeCartItem(Number(button.dataset.itemId)));
  });
}

async function updateCartItem(itemId) {
  try {
    const quantity = Number($(`qty-${itemId}`).value);
    state.cart = await api(`/api/cart/items/${itemId}`, {
      method: "PUT",
      body: JSON.stringify({ quantity }),
    });
    renderCart(state.cart);
    updateMetrics();
    showToast("Cart updated");
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  }
}

async function removeCartItem(itemId) {
  try {
    state.cart = await api(`/api/cart/items/${itemId}`, { method: "DELETE" });
    renderCart(state.cart);
    updateMetrics();
    showToast("Item removed from cart");
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  }
}

async function placeOrder(event) {
  event.preventDefault();

  if (!state.token) {
    showToast("Login before placing an order", "error");
    return;
  }

  try {
    const order = await api("/api/orders/place", {
      method: "POST",
      body: JSON.stringify({
        shippingAddress: $("shippingAddress").value,
        paymentMethod: $("paymentMethod").value,
      }),
    });

    showToast(`Order #${order.id} placed successfully`);
    closeCartDrawer();
    await Promise.all([loadCart(), loadOrders(), loadProducts()]);
    if (state.user?.role === "ADMIN") {
      await loadAdminOrders();
    }
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  }
}

async function loadOrders() {
  if (!state.token) {
    state.orders = [];
    renderOrders([]);
    updateMetrics();
    return;
  }

  try {
    const page = await api("/api/orders/user?page=0&size=20&sort=createdAt,desc");
    state.orders = pageContent(page);
    renderOrders(state.orders);
    updateMetrics();
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  }
}

function renderOrders(orders) {
  const container = $("ordersList");

  if (!state.token) {
    container.innerHTML = `<div class="empty-state"><strong>No session</strong><span>Login to view order history.</span></div>`;
    return;
  }

  if (!orders.length) {
    container.innerHTML = `<div class="empty-state"><strong>No orders yet</strong><span>Place an order from your cart.</span></div>`;
    return;
  }

  container.innerHTML = orders.map((order) => renderOrderCard(order)).join("");
}

function renderOrderCard(order, admin = false) {
  const items = (order.items || [])
    .map((item) => `<li>${escapeHtml(item.productName)} x ${item.quantity} - ${money(item.subtotal)}</li>`)
    .join("");
  const payment = order.payment
    ? `${order.payment.method} / ${order.payment.status}`
    : "No payment";
  const createdAt = order.createdAt ? new Date(order.createdAt).toLocaleString() : "";

  return `
    <article class="order-card">
      <div class="order-header">
        <h3>Order #${order.id}</h3>
        <span class="order-status">${escapeHtml(order.status)}</span>
      </div>
      <ul class="order-items">${items}</ul>
      <div class="order-footer">
        <div class="order-meta">
          <span>${money(order.totalAmount)}</span>
          <span>&bull;</span>
          <span>${escapeHtml(payment)}</span>
          <span>&bull;</span>
          <span>${escapeHtml(createdAt)}</span>
        </div>
        ${admin ? renderAdminStatusControls(order) : ""}
      </div>
      <small>${escapeHtml(order.shippingAddress)}</small>
    </article>
  `;
}

function renderAdminStatusControls(order) {
  const options = orderStatuses
    .map((status) => `<option value="${status}" ${status === order.status ? "selected" : ""}>${status}</option>`)
    .join("");

  return `
    <div class="admin-order-actions">
      <select id="status-${order.id}" aria-label="Order status">${options}</select>
      <button class="button primary update-order-status" data-order-id="${order.id}" type="button">Update</button>
    </div>
  `;
}

async function loadAdminOrders() {
  if (state.user?.role !== "ADMIN") {
    state.adminOrders = [];
    renderAdminOrders([]);
    return;
  }

  try {
    const page = await api("/api/admin/orders?page=0&size=30&sort=createdAt,desc");
    state.adminOrders = pageContent(page);
    renderAdminOrders(state.adminOrders);
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  }
}

function renderAdminOrders(orders) {
  const container = $("adminOrdersList");

  if (state.user?.role !== "ADMIN") {
    container.innerHTML = "";
    return;
  }

  if (!orders.length) {
    container.innerHTML = `<div class="empty-state"><strong>No orders yet</strong><span>Customer orders will appear here.</span></div>`;
    return;
  }

  container.innerHTML = orders.map((order) => renderOrderCard(order, true)).join("");

  document.querySelectorAll(".update-order-status").forEach((button) => {
    button.addEventListener("click", () => updateOrderStatus(Number(button.dataset.orderId)));
  });
}

async function updateOrderStatus(orderId) {
  try {
    const status = $(`status-${orderId}`).value;
    await api(`/api/admin/orders/${orderId}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status }),
    });
    showToast(`Order #${orderId} updated`);
    await Promise.all([loadAdminOrders(), loadOrders(), loadProducts()]);
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  }
}

async function createCategory(event) {
  event.preventDefault();

  try {
    await api("/api/admin/categories", {
      method: "POST",
      body: JSON.stringify({
        name: $("categoryName").value,
        description: $("categoryDescription").value,
      }),
    });
    $("categoryForm").reset();
    await loadCategories();
    showToast("Category created");
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  }
}

async function createProduct(event) {
  event.preventDefault();

  if (!$("productCategory").value) {
    showToast("Create a category before adding products", "error");
    return;
  }

  try {
    await api("/api/admin/products", {
      method: "POST",
      body: JSON.stringify({
        name: $("productName").value,
        description: $("productDescription").value,
        price: Number($("productPrice").value),
        stock: Number($("productStock").value),
        brand: $("productBrand").value,
        categoryId: Number($("productCategory").value),
        imageUrl: $("productImageUrl").value,
      }),
    });
    $("productForm").reset();
    renderCategoryOptions();
    await loadProducts();
    showToast("Product created");
    document.location.hash = "#catalog";
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  }
}

async function login(event) {
  event.preventDefault();

  try {
    const authResponse = await api("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({
        email: $("loginEmail").value,
        password: $("loginPassword").value,
      }),
    });
    saveSession(authResponse);
    showToast(`Welcome ${authResponse.user.name}`);
    await afterAuthRefresh();
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  }
}

async function register(event) {
  event.preventDefault();

  try {
    const authResponse = await api("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({
        name: $("registerName").value,
        email: $("registerEmail").value,
        password: $("registerPassword").value,
      }),
    });
    saveSession(authResponse);
    showToast(`Account created for ${authResponse.user.name}`);
    await afterAuthRefresh();
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  }
}

async function afterAuthRefresh() {
  await Promise.all([loadCart(), loadOrders(), loadAdminOrders()]);
}

function openCartDrawer() {
  $("cartDrawer").classList.add("open");
  $("cartDrawer").setAttribute("aria-hidden", "false");
  document.body.classList.add("drawer-open");
}

function closeCartDrawer() {
  $("cartDrawer").classList.remove("open");
  $("cartDrawer").setAttribute("aria-hidden", "true");
  document.body.classList.remove("drawer-open");
}

function clearFilters() {
  $("searchName").value = "";
  $("searchBrand").value = "";
  $("searchCategory").value = "";
  $("sortProducts").value = "createdAt,desc";
  loadProducts().catch((error) => showToast(getErrorMessage(error), "error"));
}

function fillSampleRegister() {
  const stamp = Date.now();
  $("registerName").value = "ShopEase Customer";
  $("registerEmail").value = `user${stamp}@example.com`;
  $("registerPassword").value = "User@1234";
}

function fillAdminLogin() {
  $("loginEmail").value = "admin@shopease.com";
  $("loginPassword").value = "Admin@123";
  document.location.hash = "#account";
}

function bindEvents() {
  $("loginForm").addEventListener("submit", login);
  $("registerForm").addEventListener("submit", register);
  $("logoutButton").addEventListener("click", () => clearSession(true));
  $("fillSampleRegister").addEventListener("click", fillSampleRegister);
  $("fillAdminLogin").addEventListener("click", fillAdminLogin);

  $("openCartDrawer").addEventListener("click", openCartDrawer);
  $("closeCartDrawer").addEventListener("click", closeCartDrawer);
  $("cartBackdrop").addEventListener("click", closeCartDrawer);

  $("productSearchForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      await loadProducts();
    } catch (error) {
      showToast(getErrorMessage(error), "error");
    }
  });

  $("clearFilters").addEventListener("click", clearFilters);
  $("refreshProducts").addEventListener("click", () => loadProducts().catch((error) => showToast(getErrorMessage(error), "error")));
  $("refreshCart").addEventListener("click", loadCart);
  $("refreshOrders").addEventListener("click", loadOrders);
  $("refreshAdminOrders").addEventListener("click", loadAdminOrders);

  $("orderForm").addEventListener("submit", placeOrder);
  $("categoryForm").addEventListener("submit", createCategory);
  $("productForm").addEventListener("submit", createProduct);
}

async function init() {
  bindEvents();
  updateSessionUi();
  renderCart(null);
  renderOrders([]);
  renderAdminOrders([]);
  updateMetrics();

  await checkHealth();

  try {
    await loadCategories();
    await loadProducts();
    if (state.token) {
      await afterAuthRefresh();
    }
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  }
}

init();
