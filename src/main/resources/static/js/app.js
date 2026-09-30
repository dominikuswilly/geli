// Shop Warehouse Management System - Client Application

let currentVariants = [];
let searchTimeout = null;

document.addEventListener('DOMContentLoaded', () => {
  setupTabs();
  loadDashboard();
  loadCatalog();
  loadAllVariantsForSelects();
  loadStockLedger();
});

// Currency Formatter
function formatRupiah(amount) {
  if (amount === null || amount === undefined) return 'Rp 0';
  return new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    maximumFractionDigits: 0
  }).format(amount);
}

// Toast Notifications
function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;

  const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️';
  toast.innerHTML = `
    <span style="font-size: 1.25rem;">${icon}</span>
    <div style="flex: 1; word-break: break-word;">${message}</div>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 4500);
}

// Tabs Navigation
function setupTabs() {
  const buttons = document.querySelectorAll('.tab-btn');
  buttons.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetId = btn.getAttribute('data-tab');

      buttons.forEach(b => b.classList.remove('active'));
      document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

      btn.classList.add('active');
      const targetContent = document.getElementById(targetId);
      if (targetContent) targetContent.classList.add('active');

      if (targetId === 'tab-dashboard') loadDashboard();
      if (targetId === 'tab-catalog') loadCatalog();
      if (targetId === 'tab-pos') loadAllVariantsForSelects();
      if (targetId === 'tab-operations') loadAllVariantsForSelects();
      if (targetId === 'tab-ledger') loadStockLedger();
    });
  });
}

// 1. Dashboard
async function loadDashboard() {
  try {
    const res = await fetch('/api/v1/dashboard/summary');
    if (!res.ok) throw new Error('Gagal mengambil data dashboard');
    const data = await res.json();

    document.getElementById('dash-total-items').textContent = data.totalItems;
    document.getElementById('dash-total-variants').textContent = data.totalVariants;
    document.getElementById('dash-total-stock').textContent = data.totalStockQuantity.toLocaleString('id-ID');
    document.getElementById('dash-total-valuation').textContent = formatRupiah(data.totalValuation);

    document.getElementById('dash-in-stock').textContent = data.inStockCount;
    document.getElementById('dash-low-stock').textContent = data.lowStockCount;
    document.getElementById('dash-out-of-stock').textContent = data.outOfStockCount;
  } catch (err) {
    console.error(err);
  }
}

// 2. Catalog (Items)
async function loadCatalog(query = '') {
  try {
    let url = '/api/v1/items?size=50';
    if (query && query.trim() !== '') {
      url += `&query=${encodeURIComponent(query.trim())}`;
    }

    const res = await fetch(url);
    if (!res.ok) throw new Error('Gagal memuat katalog');
    const data = await res.json();

    const tbody = document.getElementById('catalog-table-body');
    tbody.innerHTML = '';

    if (!data.content || data.content.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-muted); padding: 2rem;">Tidak ada item ditemukan.</td></tr>`;
      return;
    }

    data.content.forEach(item => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td><strong style="color: var(--primary); font-family: monospace;">${item.code}</strong></td>
        <td>
          <div style="font-weight: 600;">${item.name}</div>
          <div style="font-size: 0.8rem; color: var(--text-muted);">${item.description || '-'}</div>
        </td>
        <td><span class="badge badge-info">${item.category || 'Umum'}</span></td>
        <td style="font-weight: 600;">${formatRupiah(item.basePrice)}</td>
        <td><span class="badge" style="background: rgba(255,255,255,0.08);">${item.variantCount} Varian</span></td>
        <td>
          <span class="badge ${item.totalStock > 5 ? 'badge-success' : item.totalStock > 0 ? 'badge-warning' : 'badge-danger'}">
            ${item.totalStock} Unit
          </span>
        </td>
        <td>
          <div style="display: flex; gap: 0.5rem;">
            <button class="btn-secondary" style="padding: 0.35rem 0.65rem; font-size: 0.8rem;" onclick="openVariantsModal(${item.id})">
              🔍 Varian & Stok
            </button>
            <button class="btn-danger" style="padding: 0.35rem 0.65rem; font-size: 0.8rem;" onclick="deleteItem(${item.id})">
              Hapus
            </button>
          </div>
        </td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {
    console.error(err);
    showToast('Gagal memuat katalog barang: ' + err.message, 'error');
  }
}

function debounceSearch() {
  clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => {
    const q = document.getElementById('catalog-search').value;
    loadCatalog(q);
  }, 300);
}

// 3. Item & Variant Modals
function openCreateItemModal() {
  document.getElementById('create-item-form').reset();
  document.getElementById('modal-create-item').classList.add('active');
}

async function handleCreateItemSubmit(e) {
  e.preventDefault();
  const payload = {
    code: document.getElementById('item-code').value.trim(),
    name: document.getElementById('item-name').value.trim(),
    category: document.getElementById('item-category').value.trim(),
    basePrice: parseFloat(document.getElementById('item-price').value),
    description: document.getElementById('item-desc').value.trim()
  };

  try {
    const res = await fetch('/api/v1/items', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Gagal menyimpan item');
    }

    showToast(`Produk '${data.name}' berhasil ditambahkan!`, 'success');
    closeModal('modal-create-item');
    loadCatalog();
    loadDashboard();
    loadAllVariantsForSelects();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function deleteItem(id) {
  if (!confirm('Apakah Anda yakin ingin menghapus item ini berserta seluruh variannya?')) return;
  try {
    const res = await fetch(`/api/v1/items/${id}`, { method: 'DELETE' });
    if (!res.ok) throw new Error('Gagal menghapus item');
    showToast('Item berhasil dihapus', 'info');
    loadCatalog();
    loadDashboard();
    loadAllVariantsForSelects();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// Variant Management Modal
let activeItemId = null;

async function openVariantsModal(itemId) {
  activeItemId = itemId;
  closeCreateVariantForm();
  document.getElementById('modal-variants').classList.add('active');

  try {
    const res = await fetch(`/api/v1/items/${itemId}`);
    if (!res.ok) throw new Error('Gagal memuat detail item');
    const item = await res.json();

    document.getElementById('modal-item-title').textContent = `${item.name} (${item.code})`;
    document.getElementById('modal-item-sub').textContent = `Harga Dasar: ${formatRupiah(item.basePrice)} | Kategori: ${item.category || '-'}`;
    document.getElementById('variant-parent-item-id').value = item.id;

    renderVariantsTable(item.variants);
  } catch (err) {
    showToast(err.message, 'error');
  }
}

function renderVariantsTable(variants) {
  const tbody = document.getElementById('variants-table-body');
  tbody.innerHTML = '';

  if (!variants || variants.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 1.5rem;">Belum ada varian terdaftar untuk produk ini. Silakan tambahkan varian.</td></tr>`;
    return;
  }

  variants.forEach(v => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td><strong style="color: var(--accent-cyan); font-family: monospace;">${v.sku}</strong></td>
      <td>
        <div style="font-weight: 600;">${v.variantName}</div>
        <div style="font-size: 0.75rem; color: var(--text-muted);">${v.attributesJson || ''}</div>
      </td>
      <td>
        <strong>${formatRupiah(v.effectivePrice)}</strong>
        ${v.price ? `<span style="font-size: 0.75rem; color: var(--accent-amber);"> (Override)</span>` : `<span style="font-size: 0.75rem; color: var(--text-muted);"> (Base)</span>`}
      </td>
      <td>
        <span class="badge ${v.currentStock > 5 ? 'badge-success' : v.currentStock > 0 ? 'badge-warning' : 'badge-danger'}">
          ${v.currentStock} Unit
        </span>
      </td>
      <td>
        ${v.currentStock > 0 ? '<span class="badge badge-success">Tersedia</span>' : '<span class="badge badge-danger">Habis</span>'}
      </td>
      <td>
        <button class="btn-danger" style="padding: 0.25rem 0.5rem; font-size: 0.75rem;" onclick="deleteVariant(${v.id})">
          Hapus
        </button>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

function openCreateVariantForm() {
  document.getElementById('create-variant-form').reset();
  document.getElementById('variant-form-container').style.display = 'block';
}

function closeCreateVariantForm() {
  document.getElementById('variant-form-container').style.display = 'none';
}

async function handleCreateVariantSubmit(e) {
  e.preventDefault();
  const itemId = document.getElementById('variant-parent-item-id').value;
  const priceVal = document.getElementById('variant-price').value;

  const payload = {
    sku: document.getElementById('variant-sku').value.trim(),
    variantName: document.getElementById('variant-name').value.trim(),
    price: priceVal ? parseFloat(priceVal) : null,
    initialStock: parseInt(document.getElementById('variant-stock').value) || 0,
    attributesJson: document.getElementById('variant-attrs').value.trim()
  };

  try {
    const res = await fetch(`/api/v1/items/${itemId}/variants`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    if (!res.ok) throw new Error(data.message || 'Gagal menambahkan varian');

    showToast(`Varian '${data.variantName}' (${data.sku}) berhasil dibuat!`, 'success');
    closeCreateVariantForm();
    openVariantsModal(itemId);
    loadCatalog();
    loadDashboard();
    loadAllVariantsForSelects();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function deleteVariant(id) {
  if (!confirm('Hapus varian ini?')) return;
  try {
    const res = await fetch(`/api/v1/variants/${id}`, { method: 'DELETE' });
    if (!res.ok) throw new Error('Gagal menghapus varian');
    showToast('Varian berhasil dihapus', 'info');
    if (activeItemId) openVariantsModal(activeItemId);
    loadCatalog();
    loadDashboard();
    loadAllVariantsForSelects();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

function closeModal(modalId) {
  document.getElementById(modalId).classList.remove('active');
}

// 4. POS Simulator & Anti-Overselling Guard
async function loadAllVariantsForSelects() {
  try {
    const res = await fetch('/api/v1/variants');
    if (!res.ok) throw new Error('Gagal memuat varian');
    currentVariants = await res.json();

    const posSelect = document.getElementById('pos-variant-select');
    const restockSelect = document.getElementById('restock-variant-select');
    const adjustSelect = document.getElementById('adjust-variant-select');

    const buildOptions = () => {
      let html = '<option value="">-- Pilih Varian Produk --</option>';
      currentVariants.forEach(v => {
        const stockLabel = v.currentStock > 0 ? `[Stok: ${v.currentStock}]` : `[STOK HABIS (0)]`;
        html += `<option value="${v.id}">${v.itemName || ''} - ${v.variantName} (${v.sku}) ${stockLabel}</option>`;
      });
      return html;
    };

    const optHtml = buildOptions();
    if (posSelect) posSelect.innerHTML = optHtml;
    if (restockSelect) restockSelect.innerHTML = optHtml;
    if (adjustSelect) adjustSelect.innerHTML = optHtml;

    onPosVariantChange();
  } catch (err) {
    console.error(err);
  }
}

function onPosVariantChange() {
  const select = document.getElementById('pos-variant-select');
  const variantId = parseInt(select.value);
  const variant = currentVariants.find(v => v.id === variantId);

  if (!variant) {
    document.getElementById('preview-item-name').textContent = '-';
    document.getElementById('preview-variant-sku').textContent = '-';
    document.getElementById('preview-unit-price').textContent = 'Rp 0';
    document.getElementById('preview-available-stock').textContent = '-';
    document.getElementById('preview-stock-status').innerHTML = '-';
    document.getElementById('preview-total-price').textContent = 'Rp 0';
    return;
  }

  document.getElementById('preview-item-name').textContent = `${variant.itemName || 'Item'} - ${variant.variantName}`;
  document.getElementById('preview-variant-sku').textContent = variant.sku;
  document.getElementById('preview-unit-price').textContent = formatRupiah(variant.effectivePrice);

  const stockBadge = variant.currentStock > 5 
    ? `<span class="badge badge-success">${variant.currentStock} Unit</span>`
    : variant.currentStock > 0 
    ? `<span class="badge badge-warning">${variant.currentStock} Unit (Hampir Habis)</span>`
    : `<span class="badge badge-danger">KOSONG (0 Unit)</span>`;

  document.getElementById('preview-available-stock').innerHTML = stockBadge;

  const statusBadge = variant.currentStock > 0 
    ? `<span class="badge badge-success">Siap Dijual</span>`
    : `<span class="badge badge-danger">Out of Stock (Penjualan Dicegah)</span>`;

  document.getElementById('preview-stock-status').innerHTML = statusBadge;

  updatePosPreview();
}

function updatePosPreview() {
  const select = document.getElementById('pos-variant-select');
  const variantId = parseInt(select.value);
  const variant = currentVariants.find(v => v.id === variantId);
  const qty = parseInt(document.getElementById('pos-quantity').value) || 0;

  if (variant && qty > 0) {
    const total = variant.effectivePrice * qty;
    document.getElementById('preview-total-price').textContent = formatRupiah(total);
  } else {
    document.getElementById('preview-total-price').textContent = 'Rp 0';
  }
}

async function processSaleDeduction() {
  const select = document.getElementById('pos-variant-select');
  const variantId = parseInt(select.value);
  const qty = parseInt(document.getElementById('pos-quantity').value);
  const ref = document.getElementById('pos-reference').value.trim() || `POS-${Date.now().toString().slice(-6)}`;
  const notes = document.getElementById('pos-notes').value.trim() || 'Penjualan kasir online/POS';

  if (!variantId) {
    showToast('Silakan pilih varian produk terlebih dahulu!', 'error');
    return;
  }

  if (!qty || qty <= 0) {
    showToast('Kuantitas pembelian harus lebih besar dari 0!', 'error');
    return;
  }

  const payload = {
    variantId: variantId,
    quantity: qty,
    referenceNumber: ref,
    notes: notes
  };

  try {
    const res = await fetch('/api/v1/stocks/deduct', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await res.json();

    if (res.status === 409) {
      // ANTI-OVERSELLING GUARD TRIGGERED!
      showToast(`🛡️ DITOLAK ANTI-OVERSELLING: ${data.message}`, 'error');
      return;
    }

    if (!res.ok) {
      throw new Error(data.message || 'Transaksi gagal');
    }

    showToast(`✅ Transaksi Berhasil! Sisa stok untuk ${data.sku}: ${data.currentStock} unit.`, 'success');
    loadDashboard();
    loadCatalog();
    await loadAllVariantsForSelects();
    select.value = variantId;
    onPosVariantChange();
    loadStockLedger();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// 5. Operations (Restock & Opname)
async function handleRestockSubmit(e) {
  e.preventDefault();
  const variantId = parseInt(document.getElementById('restock-variant-select').value);
  const qty = parseInt(document.getElementById('restock-qty').value);
  const ref = document.getElementById('restock-ref').value.trim() || `PO-${Date.now().toString().slice(-6)}`;
  const notes = document.getElementById('restock-notes').value.trim();

  if (!variantId) {
    showToast('Pilih varian untuk restock!', 'error');
    return;
  }

  const payload = { variantId, quantity: qty, referenceNumber: ref, notes };

  try {
    const res = await fetch('/api/v1/stocks/in', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.message || 'Gagal menambahkan stok');

    showToast(`Stok ${data.sku} berhasil ditambahkan! Saldo saat ini: ${data.currentStock} unit.`, 'success');
    document.getElementById('restock-form').reset();
    loadDashboard();
    loadCatalog();
    loadAllVariantsForSelects();
    loadStockLedger();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function handleAdjustSubmit(e) {
  e.preventDefault();
  const variantId = parseInt(document.getElementById('adjust-variant-select').value);
  const actualQty = parseInt(document.getElementById('adjust-actual-qty').value);
  const reason = document.getElementById('adjust-reason').value;
  const notes = document.getElementById('adjust-notes').value.trim();

  if (!variantId) {
    showToast('Pilih varian untuk penyesuaian!', 'error');
    return;
  }

  const payload = { variantId, actualQuantity: actualQty, reason, notes };

  try {
    const res = await fetch('/api/v1/stocks/adjust', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.message || 'Gagal menyimpan penyesuaian');

    showToast(`Penyesuaian stok berhasil! Saldo baru: ${data.currentStock} unit.`, 'success');
    document.getElementById('adjust-form').reset();
    loadDashboard();
    loadCatalog();
    loadAllVariantsForSelects();
    loadStockLedger();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// 6. Stock Ledger (Audit Trail)
async function loadStockLedger() {
  try {
    const res = await fetch('/api/v1/stocks/history?size=50');
    if (!res.ok) throw new Error('Gagal memuat mutasi stok');
    const data = await res.json();

    const tbody = document.getElementById('ledger-table-body');
    tbody.innerHTML = '';

    if (!data.content || data.content.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 2rem;">Belum ada riwayat transaksi mutasi stok.</td></tr>`;
      return;
    }

    data.content.forEach(tx => {
      const tr = document.createElement('tr');
      const badgeClass = tx.trxType === 'SALE_DEDUCT' ? 'badge-danger' :
                         tx.trxType === 'RESTOCK' ? 'badge-success' :
                         tx.trxType === 'INITIAL' ? 'badge-info' : 'badge-warning';

      const qtyDisplay = tx.quantityChange > 0 
        ? `<strong style="color: #34d399;">+${tx.quantityChange}</strong>` 
        : `<strong style="color: #fb7185;">${tx.quantityChange}</strong>`;

      const formattedTime = new Date(tx.createdAt).toLocaleString('id-ID', {
        dateStyle: 'short',
        timeStyle: 'medium'
      });

      tr.innerHTML = `
        <td style="font-size: 0.8rem; color: var(--text-muted); font-family: monospace;">${formattedTime}</td>
        <td>
          <div style="font-weight: 600;">${tx.itemName || '-'}</div>
          <div style="font-size: 0.8rem; color: var(--text-muted);">${tx.variantName || '-'}</div>
        </td>
        <td><strong style="color: var(--accent-cyan); font-family: monospace;">${tx.sku || '-'}</strong></td>
        <td><span class="badge ${badgeClass}">${tx.trxType}</span></td>
        <td>${qtyDisplay}</td>
        <td><strong style="font-size: 1rem;">${tx.balanceAfter}</strong></td>
        <td style="font-family: monospace; font-size: 0.85rem;">${tx.referenceNo || '-'}</td>
        <td style="font-size: 0.85rem; color: var(--text-muted);">${tx.notes || '-'}</td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {
    console.error(err);
  }
}
