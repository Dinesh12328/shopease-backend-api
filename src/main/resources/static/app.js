const state = {
  token: localStorage.getItem("shopease_token"),
  user: JSON.parse(localStorage.getItem("shopease_user") || "null"),
  categories: [],
  products: [],
  cart: null,
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
    throw new Error(payload?.message || `${response.status} ${response.statusText}`);
  }

  return payload?.data ?? payload;
}

function pageContent(page) {
  if (!page) return [];
  if (Array.isArray(page)) return page;
  return page.content || [];
}

function saveSession(authResponse) {
  state.token = authResponse.token;
  state.user = authResponse.user;
  localStorage.setItem("shopease_token", state.token);
  localStorage.setItem("shopease_user", JSON.stringify(state.user));
  updateSessionUi();
}

function clearSession() {
  state.token = null;
  state.user = null;
  state.cart = null;
  localStorage.removeItem("shopease_token");
  localStorage.removeItem("shopease_user");
  updateSessionUi();
  renderCart(null);
  renderOrders([]);
  renderAdminOrders([]);
}

function updateSessionUi() {
  const badge = $("sessionBadge");
  const currentUser = $("currentUser");
  const logoutButton = $("logoutButton");
  const adminPanel = $("adminPanel");

  if (!state.user) {
    badge.textContent = "Guest";
    badge.className = "badge muted";
    currentUser.classList.add("hidden");
    logoutButton.classList.add("hidden");
    adminPanel.classList.add("hidden");
    return;
  }

  const role = state.user.role;
  badge.textContent = role;
  badge.className = `badge ${role === "ADMIN" ? "admin" : "user"}`;
  currentUser.innerHTML = `
    <strong>${escapeHtml(state.user.name)}</strong><br>
    ${escapeHtml(state.user.email)}<br>
    Role: ${escapeHtml(role)}
  `;
  currentUser.classList.remove("hidden");
  logoutButton.classList.remove("hidden");
  adminPanel.classList.toggle("hidden", role !== "ADMIN");
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

async function checkHealth() {
  try {
    const health = await fetch("/actuator/health").then((res) => res.json());
    $("apiStatus").textContent = health.status === "UP" ? "Online" : health.status;
    $("apiStatusHint").textContent = "Spring Boot API is reachable";
  } catch {
    $("apiStatus").textContent = "Offline";
    $("apiStatusHint").textContent = "Start the backend from IntelliJ";
  }
}

async function loadCategories() {
  state.categories = await api("/api/categories");
  renderCategoryOptions();
}

function renderCategoryOptions() {
  const categoryOptions = state.categories
    .map((category) => `<option value="${category.id}">${escapeHtml(category.name)}</option>`)
    .join("");

  $("searchCategory").innerHTML = `<option value="">All categories</option>${categoryOptions}`;
  $("productCategory").innerHTML = categoryOptions || `<option value="">Create a category first</option>`;
}

async function loadProducts() {
  const params = new URLSearchParams();
  const name = $("searchName").value.trim();
  const brand = $("searchBrand").value.trim();
  const categoryId = $("searchCategory").value;
  const sort = $("sortProducts").value;

  if (name) params.set("name", name);
  if (brand) params.set("brand", brand);
  if (categoryId) params.set("categoryId", categoryId);
  params.set("page", "0");
  params.set("size", "50");
  params.set("sort", sort);

  const page = await api(`/api/products?${params.toString()}`);
  state.products = pageContent(page);
  renderProducts();
}

function renderProducts() {
  const grid = $("productGrid");
  const empty = $("emptyProducts");

  empty.classList.toggle("hidden", state.products.length > 0);

  grid.innerHTML = state.products
    .map((product) => {
      const image = product.imageUrl
        ? `<img src="${escapeHtml(product.imageUrl)}" alt="${escapeHtml(product.name)}" onerror="this.remove()">`
        : escapeHtml(product.name?.charAt(0) || "P");
      const disabled = product.stock < 1 ? "disabled" : "";

      return `
        <article class="product-card">
          <div class="product-image">${image}</div>
          <div class="product-body">
            <div class="product-meta">
              <span>${escapeHtml(product.categoryName || "Category")}</span>
              <span>•</span>
              <span>${escapeHtml(product.brand || "No brand")}</span>
            </div>
            <h3>${escapeHtml(product.name)}</h3>
            <p>${escapeHtml(product.description || "No description available.")}</p>
            <div class="price-row">
              <span class="price">${money(product.price)}</span>
              <span class="stock">${product.stock} in stock</span>
            </div>
            <button class="button primary add-to-cart" data-product-id="${product.id}" ${disabled}>
              ${product.stock < 1 ? "Out of Stock" : "Add to Cart"}
            </button>
          </div>
        </article>
      `;
    })
    .join("");

  document.querySelectorAll(".add-to-cart").forEach((button) => {
    button.addEventListener("click", () => addToCart(button.dataset.productId));
  });
}

async function addToCart(productId) {
  if (!state.token) {
    showToast("Please login or register before adding to cart", "error");
    document.location.hash = "#auth";
    return;
  }

  try {
    state.cart = await api("/api/cart/add", {
      method: "POST",
      body: JSON.stringify({ productId: Number(productId), quantity: 1 }),
    });
    renderCart(state.cart);
    showToast("Product added to cart");
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  }
}

async function loadCart() {
  if (!state.token) {
    renderCart(null);
    return;
  }

  try {
    state.cart = await api("/api/cart");
    renderCart(state.cart);
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
    container.innerHTML = `<div class="empty">Login or register to use the cart.</div>`;
    return;
  }

  if (items.length === 0) {
    container.innerHTML = `<div class="empty">Your cart is empty.</div>`;
    return;
  }

  container.innerHTML = items
    .map(
      (item) => `
        <div class="cart-line">
          <div>
            <h3>${escapeHtml(item.productName)}</h3>
            <div class="product-meta">
              <span>${money(item.unitPrice)} each</span>
              <span>•</span>
              <span>Subtotal ${money(item.subtotal)}</span>
            </div>
          </div>
          <div class="cart-actions">
            <input id="qty-${item.id}" type="number" min="1" value="${item.quantity}">
            <button class="button ghost update-cart-item" data-item-id="${item.id}" type="button">Update</button>
            <button class="button danger remove-cart-item" data-item-id="${item.id}" type="button">Remove</button>
          </div>
        </div>
      `
    )
    .join("");

  document.querySelectorAll(".update-cart-item").forEach((button) => {
    button.addEventListener("click", () => updateCartItem(button.dataset.itemId));
  });

  document.querySelectorAll(".remove-cart-item").forEach((button) => {
    button.addEventListener("click", () => removeCartItem(button.dataset.itemId));
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
    showToast("Cart updated");
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  }
}

async function removeCartItem(itemId) {
  try {
    state.cart = await api(`/api/cart/items/${itemId}`, { method: "DELETE" });
    renderCart(state.cart);
    showToast("Item removed from cart");
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  }
}

async function placeOrder(event) {
  event.preventDefault();

  if (!state.token) {
    showToast("Please login before placing an order", "error");
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
    await loadCart();
    await loadOrders();
    await loadProducts();
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  }
}

async function loadOrders() {
  if (!state.token) {
    renderOrders([]);
    return;
  }

  try {
    const page = await api("/api/orders/user?page=0&size=20&sort=createdAt,desc");
    renderOrders(pageContent(page));
  } catch (error) {
    showToast(getErrorMessage(error), "error");
  }
}

function renderOrders(orders) {
  const container = $("ordersList");

  if (!state.token) {
    container.innerHTML = `<div class="empty">Login to see order history.</div>`;
    return;
  }

  if (!orders.length) {
    container.innerHTML = `<div class="empty">No orders yet.</div>`;
    return;
  }

  container.innerHTML = orders.map(renderOrderCard).join("");
}

function renderOrderCard(order, admin = false) {
  const items = (order.items || [])
    .map((item) => `<li>${escapeHtml(item.productName)} × ${item.quantity} — ${money(item.subtotal)}</li>`)
    .join("");
  const payment = order.payment
    ? `${order.payment.method} / ${order.payment.status}`
    : "No payment";
  const createdAt = order.createdAt ? new Date(order.createdAt).toLocaleString() : "";

  return `
    <article class="order-card">
      <div class="order-header">
        <h3>Order #${order.id}</h3>
        <span class="badge muted">${escapeHtml(order.status)}</span>
      </div>
      <ul class="order-items">${items}</ul>
      <div class="order-footer">
        <div>
          <strong>${money(order.totalAmount)}</strong><br>
          <small>${escapeHtml(payment)} · ${escapeHtml(createdAt)}</small><br>
          <small>${escapeHtml(order.shippingAddress)}</small>
        </div>
        ${admin ? renderAdminStatusControls(order) : ""}
      </div>
    </article>
  `;
}

function renderAdminStatusControls(order) {
  const options = orderStatuses
    .map((status) => `<option value="${status}" ${status === order.status ? "selected" : ""}>${status}</option>`)
    .join("");

  return `
    <div class="admin-order-actions">
      <select id="status-${order.id}">${options}</select>
      <button class="button primary update-order-status" data-order-id="${order.id}" type="button">Update</button>
    </div>
  `;
}

async function loadAdminOrders() {
  if (state.user?.role !== "ADMIN") {
    renderAdminOrders([]);
    return;
  }

  try {
    const page = await api("/api/admin/orders?page=0&size=30&sort=createdAt,desc");
    renderAdminOrders(pageContent(page));
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
    container.innerHTML = `<div class="empty">No orders yet.</div>`;
    return;
  }

  container.innerHTML = orders.map((order) => renderOrderCard(order, true)).join("");

  document.querySelectorAll(".update-order-status").forEach((button) => {
    button.addEventListener("click", () => updateOrderStatus(button.dataset.orderId));
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
    await loadAdminOrders();
    await loadOrders();
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
    await loadProducts();
    showToast("Product created");
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
  await loadCart();
  await loadOrders();
  await loadAdminOrders();
}

function bindEvents() {
  $("loginForm").addEventListener("submit", login);
  $("registerForm").addEventListener("submit", register);
  $("logoutButton").addEventListener("click", () => {
    clearSession();
    showToast("Logged out");
  });

  $("productSearchForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      await loadProducts();
    } catch (error) {
      showToast(getErrorMessage(error), "error");
    }
  });

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
