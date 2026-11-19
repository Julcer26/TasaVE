/* ==========================================================================
   Tasa VE Landing Page Interactive Logic & Feature Simulator
   ========================================================================== */

(() => {
  const RATES = {
    USD_BCV: { label: "USD BCV", symbol: "$", rate: 916.6477, tomorrow: 921.4820, decimals: 4 },
    EUR_BCV: { label: "EUR BCV", symbol: "€", rate: 985.3240, tomorrow: 990.1150, decimals: 4 },
    USDT: { label: "USDT Binance", symbol: "₮", rate: 1024.50, tomorrow: 1024.50, decimals: 2 }
  };

  // Theme Toggle
  const root = document.documentElement;
  const themeBtn = document.getElementById("theme-toggle");
  const savedTheme = localStorage.getItem("tasave_web_theme");
  if (savedTheme === "light" || savedTheme === "dark") {
    root.setAttribute("data-theme", savedTheme);
  }
  if (themeBtn) {
    themeBtn.addEventListener("click", () => {
      const next = root.getAttribute("data-theme") === "light" ? "dark" : "light";
      root.setAttribute("data-theme", next);
      localStorage.setItem("tasave_web_theme", next);
    });
  }

  // Mobile Menu
  const menuBtn = document.getElementById("mobile-menu-btn");
  const menuPanel = document.getElementById("mobile-menu-panel");
  if (menuBtn && menuPanel) {
    menuBtn.addEventListener("click", () => {
      const open = menuPanel.classList.toggle("is-open");
      menuBtn.setAttribute("aria-expanded", String(open));
    });
    menuPanel.querySelectorAll("a").forEach((link) => {
      link.addEventListener("click", () => {
        menuPanel.classList.remove("is-open");
        menuBtn.setAttribute("aria-expanded", "false");
      });
    });
  }

  // Toast helper
  const toastEl = document.getElementById("web-toast");
  let toastTimer = null;
  function showToast(msg) {
    if (!toastEl) return;
    toastEl.textContent = msg;
    toastEl.classList.add("is-visible");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toastEl.classList.remove("is-visible"), 2400);
  }

  // Formatters
  const fmtMoney = (n, d = 2) =>
    Number(n || 0).toLocaleString("en-US", { minimumFractionDigits: d, maximumFractionDigits: d });

  // ---------------- Interactive Simulator State ----------------
  const TAB_ORDER = ["converter", "scanner", "cart", "trends", "checkout"];
  const state = {
    activeTab: "converter",
    autoplay: !window.matchMedia("(prefers-reduced-motion: reduce)").matches,
    progress: 0,
    // Converter
    rateKey: "USD_BCV",
    tomorrowBcv: false,
    amount: 25,
    vesToForeign: false,
    // OCR
    ocrIndex: 0,
    ocrSamples: [
      { item: "Queso Blanco 1kg", usd: 7.80 },
      { item: "Combo Desayuno", usd: 14.50 },
      { item: "Aceite de Oliva 500ml", usd: 11.90 },
      { item: "Paquete Café Molido", usd: 6.25 }
    ],
    // Cart
    cartItems: [
      { id: "cafe", name: "Café Molido 500g", price: 6.25, qty: 1 },
      { id: "harina", name: "Harina P.A.N. 1kg", price: 1.35, qty: 2 },
      { id: "aceite", name: "Aceite Vegetal 1L", price: 3.40, qty: 1 }
    ],
    useIva: true,
    useIgtf: false,
    // Trends
    trendRange: "1M",
    // Checkout
    checkoutTotalUsd: 20.00,
    paidCashUsd: 10.00,
    paidPagoMovilBs: 0
  };

  const tabBtns = Array.from(document.querySelectorAll(".demo-tab-btn"));
  const autoplayBtn = document.getElementById("autoplay-toggle");
  const autoplayLabel = document.getElementById("autoplay-label");
  const stageEl = document.getElementById("phone-screen-stage");

  function setAutoplay(enabled) {
    state.autoplay = enabled;
    state.progress = 0;
    tabBtns.forEach((btn) => {
      const bar = btn.querySelector(".demo-progress");
      if (bar) bar.style.width = "0%";
    });
    if (autoplayBtn) {
      autoplayBtn.textContent = enabled ? "Pausar tour" : "Reproducir tour";
      autoplayBtn.setAttribute("aria-pressed", String(enabled));
    }
    if (autoplayLabel) {
      autoplayLabel.textContent = enabled
        ? "Recorrido automático activo (cambia cada 4.5 s)"
        : "Modo manual — interactúa libremente con cada pantalla";
    }
  }

  if (autoplayBtn) {
    autoplayBtn.addEventListener("click", () => setAutoplay(!state.autoplay));
  }

  tabBtns.forEach((btn) => {
    btn.addEventListener("click", () => {
      const tab = btn.getAttribute("data-tab");
      if (tab) {
        setAutoplay(false);
        selectTab(tab);
      }
    });
  });

  function selectTab(tab) {
    state.activeTab = tab;
    tabBtns.forEach((btn) => {
      const active = btn.getAttribute("data-tab") === tab;
      btn.classList.toggle("is-active", active);
      btn.setAttribute("aria-selected", String(active));
      const bar = btn.querySelector(".demo-progress");
      if (bar && !active) bar.style.width = "0%";
    });
    renderStage();
  }

  function renderStage() {
    if (!stageEl) return;
    if (state.activeTab === "converter") renderConverter();
    else if (state.activeTab === "scanner") renderScanner();
    else if (state.activeTab === "cart") renderCart();
    else if (state.activeTab === "trends") renderTrends();
    else if (state.activeTab === "checkout") renderCheckout();
  }

  function renderConverter() {
    const info = RATES[state.rateKey];
    const activeRate = state.tomorrowBcv && state.rateKey !== "USDT" ? info.tomorrow : info.rate;
    const rawVal = Number(state.amount) || 0;
    const vesVal = state.vesToForeign ? rawVal : rawVal * activeRate;
    const foreignVal = state.vesToForeign ? (activeRate > 0 ? rawVal / activeRate : 0) : rawVal;

    stageEl.innerHTML = `
      <div class="sim-topbar">
        <span class="sim-brand">Tasa VE</span>
        <span class="sim-badge ${state.tomorrowBcv ? "amber" : ""}">
          ${state.tomorrowBcv ? "BCV Mañana" : "En Vivo • 4 Dec."}
        </span>
      </div>

      <div class="sim-pill-row" role="group" aria-label="Seleccionar moneda">
        ${Object.keys(RATES).map((k) => `
          <button type="button" class="sim-chip ${state.rateKey === k ? "is-selected" : ""}" data-rate="${k}">
            ${RATES[k].label}
          </button>
        `).join("")}
        <button type="button" class="sim-chip is-amber ${state.tomorrowBcv ? "is-selected" : ""}" id="sim-toggle-tomorrow">
          ${state.tomorrowBcv ? "Ver Hoy" : "BCV Mañana"}
        </button>
      </div>

      <div class="sim-card">
        <span class="sim-label">${state.vesToForeign ? "Bolívares (VES)" : info.label}</span>
        <div class="sim-input-row">
          <input id="sim-conv-input" class="sim-input" type="number" min="0" step="any" value="${rawVal}" aria-label="Monto a convertir">
          <button type="button" class="sim-chip is-selected" id="sim-swap-dir" title="Invertir moneda">Bs. ↔ ${info.symbol}</button>
        </div>
        <div class="sim-sub-row">
          <span>Tasa: Bs. ${fmtMoney(activeRate, info.decimals)}</span>
          <span>${state.rateKey === "USDT" ? "P2P Tiempo Real" : (state.tomorrowBcv ? "Tasa Mañana" : "BCV Oficial")}</span>
        </div>
      </div>

      <div class="sim-card">
        <span class="sim-label">${state.vesToForeign ? "Equivalente en " + info.label : "Resultado en Bolívares"}</span>
        <div class="sim-big-result" id="sim-conv-result">
          ${state.vesToForeign ? `${info.symbol} ${fmtMoney(foreignVal, 2)}` : `Bs. ${fmtMoney(vesVal, 2)}`}
        </div>
        <div class="sim-sub-row" id="sim-conv-equiv">
          <span>USD BCV: $${fmtMoney(RATES.USD_BCV.rate > 0 ? vesVal / RATES.USD_BCV.rate : 0, 2)}</span>
          <span>EUR BCV: €${fmtMoney(RATES.EUR_BCV.rate > 0 ? vesVal / RATES.EUR_BCV.rate : 0, 2)}</span>
        </div>
      </div>
    `;

    function updateConverterOutputs() {
      const val = Number(state.amount) || 0;
      const vVal = state.vesToForeign ? val : val * activeRate;
      const fVal = state.vesToForeign ? (activeRate > 0 ? val / activeRate : 0) : val;
      const resEl = stageEl.querySelector("#sim-conv-result");
      const eqEl = stageEl.querySelector("#sim-conv-equiv");
      if (resEl) {
        resEl.textContent = state.vesToForeign ? `${info.symbol} ${fmtMoney(fVal, 2)}` : `Bs. ${fmtMoney(vVal, 2)}`;
      }
      if (eqEl) {
        eqEl.innerHTML = `<span>USD BCV: $${fmtMoney(RATES.USD_BCV.rate > 0 ? vVal / RATES.USD_BCV.rate : 0, 2)}</span><span>EUR BCV: €${fmtMoney(RATES.EUR_BCV.rate > 0 ? vVal / RATES.EUR_BCV.rate : 0, 2)}</span>`;
      }
    }

    stageEl.querySelectorAll("[data-rate]").forEach((el) => {
      el.addEventListener("click", () => {
        setAutoplay(false);
        state.rateKey = el.getAttribute("data-rate");
        renderConverter();
      });
    });
    stageEl.querySelector("#sim-toggle-tomorrow").addEventListener("click", () => {
      setAutoplay(false);
      state.tomorrowBcv = !state.tomorrowBcv;
      renderConverter();
    });
    stageEl.querySelector("#sim-swap-dir").addEventListener("click", () => {
      setAutoplay(false);
      state.vesToForeign = !state.vesToForeign;
      renderConverter();
    });
    stageEl.querySelector("#sim-conv-input").addEventListener("input", (e) => {
      setAutoplay(false);
      state.amount = parseFloat(e.target.value) || 0;
      updateConverterOutputs();
    });
  }

  function renderScanner() {
    const sample = state.ocrSamples[state.ocrIndex % state.ocrSamples.length];
    const rate = RATES.USD_BCV.rate;
    const bs = sample.usd * rate;

    stageEl.innerHTML = `
      <div class="sim-topbar">
        <span class="sim-brand">Escáner OCR Local</span>
        <span class="sim-badge">3 gratis / día +5</span>
      </div>

      <div class="ocr-viewfinder">
        <div class="ocr-laser" aria-hidden="true"></div>
        <span class="sim-label">Etiqueta detectada en cámara</span>
        <div class="ocr-detected">$${fmtMoney(sample.usd, 2)}</div>
        <div style="color:#94A3B8;font-size:0.86rem;margin-top:4px;">${sample.item}</div>
      </div>

      <div class="sim-card">
        <span class="sim-label">Conversión instantánea (BCV ${fmtMoney(rate, 4)})</span>
        <div class="sim-big-result">Bs. ${fmtMoney(bs, 2)}</div>
        <div class="sim-sub-row">
          <span>Procesamiento 100% en dispositivo</span>
          <span>Sin subir fotos</span>
        </div>
      </div>

      <div class="sim-pill-row">
        <button type="button" class="btn btn-primary btn-sm" id="sim-next-scan" style="flex:1;">Escanear otro precio</button>
        <button type="button" class="sim-chip is-selected" id="sim-add-scan-cart">+ Al carrito</button>
      </div>
    `;

    stageEl.querySelector("#sim-next-scan").addEventListener("click", () => {
      setAutoplay(false);
      state.ocrIndex = (state.ocrIndex + 1) % state.ocrSamples.length;
      renderScanner();
    });
    stageEl.querySelector("#sim-add-scan-cart").addEventListener("click", () => {
      setAutoplay(false);
      state.cartItems.push({
        id: "scan_" + Date.now(),
        name: sample.item,
        price: sample.usd,
        qty: 1
      });
      showToast(`"${sample.item}" agregado al carrito`);
      selectTab("cart");
    });
  }

  function renderCart() {
    const rate = RATES.USD_BCV.rate;
    const subtotal = state.cartItems.reduce((acc, it) => acc + it.price * it.qty, 0);
    const iva = state.useIva ? subtotal * 0.16 : 0;
    const igtf = state.useIgtf ? (subtotal + iva) * 0.03 : 0;
    const totalUsd = subtotal + iva + igtf;
    const totalBs = totalUsd * rate;

    stageEl.innerHTML = `
      <div class="sim-topbar">
        <span class="sim-brand">Mi Carrito</span>
        <span class="sim-badge">${state.cartItems.reduce((a, b) => a + b.qty, 0)} uds.</span>
      </div>

      <div class="sim-card" style="max-height:200px;overflow-y:auto;">
        ${state.cartItems.map((it, idx) => `
          <div class="cart-item-row">
            <div>
              <div style="font-weight:700;font-size:0.88rem;">${it.name}</div>
              <div style="font-size:0.78rem;color:#94A3B8;">$${fmtMoney(it.price, 2)} c/u</div>
            </div>
            <div class="qty-controls">
              <button type="button" class="qty-btn" data-dec="${idx}" aria-label="Restar ${it.name}">-</button>
              <strong style="min-width:18px;text-align:center;">${it.qty}</strong>
              <button type="button" class="qty-btn" data-inc="${idx}" aria-label="Sumar ${it.name}">+</button>
            </div>
          </div>
        `).join("")}
      </div>

      <div class="sim-pill-row">
        <button type="button" class="sim-chip ${state.useIva ? "is-selected" : ""}" id="sim-iva">IVA 16%: ${state.useIva ? "Sí" : "No"}</button>
        <button type="button" class="sim-chip is-amber ${state.useIgtf ? "is-selected" : ""}" id="sim-igtf">IGTF 3%: ${state.useIgtf ? "Sí" : "No"}</button>
      </div>

      <div class="sim-card">
        <div class="sim-sub-row" style="padding-top:0;">
          <span>Subtotal</span>
          <strong>$${fmtMoney(subtotal, 2)}</strong>
        </div>
        ${state.useIva ? `<div class="sim-sub-row"><span>IVA (16%)</span><span>+$${fmtMoney(iva, 2)}</span></div>` : ""}
        ${state.useIgtf ? `<div class="sim-sub-row" style="color:#F59E0B;"><span>IGTF (3%)</span><span>+$${fmtMoney(igtf, 2)}</span></div>` : ""}
        <div class="sim-big-result" style="margin-top:8px;">$${fmtMoney(totalUsd, 2)} / Bs. ${fmtMoney(totalBs, 2)}</div>
      </div>
    `;

    stageEl.querySelectorAll("[data-inc]").forEach((b) => {
      b.addEventListener("click", () => {
        setAutoplay(false);
        const i = Number(b.getAttribute("data-inc"));
        state.cartItems[i].qty += 1;
        renderCart();
      });
    });
    stageEl.querySelectorAll("[data-dec]").forEach((b) => {
      b.addEventListener("click", () => {
        setAutoplay(false);
        const i = Number(b.getAttribute("data-dec"));
        if (state.cartItems[i].qty > 1) state.cartItems[i].qty -= 1;
        else if (state.cartItems.length > 1) state.cartItems.splice(i, 1);
        renderCart();
      });
    });
    stageEl.querySelector("#sim-iva").addEventListener("click", () => {
      setAutoplay(false);
      state.useIva = !state.useIva;
      renderCart();
    });
    stageEl.querySelector("#sim-igtf").addEventListener("click", () => {
      setAutoplay(false);
      state.useIgtf = !state.useIgtf;
      renderCart();
    });
  }

  function renderTrends() {
    const spreadPct = ((RATES.USDT.rate - RATES.USD_BCV.rate) / RATES.USD_BCV.rate) * 100;
    const curves = {
      "1S": {
        bcv: "M 10,108 L 70,102 L 130,95 L 190,89 L 250,82 L 310,76 L 360,70",
        usdt: "M 10,78 L 70,72 L 130,68 L 190,56 L 250,48 L 310,44 L 360,34",
        deltaBcv: "+1.84%",
        deltaUsdt: "+2.45%"
      },
      "1M": {
        bcv: "M 10,126 L 70,115 L 130,102 L 190,94 L 250,84 L 310,78 L 360,68",
        usdt: "M 10,96 L 70,84 L 130,74 L 190,64 L 250,52 L 310,42 L 360,30",
        deltaBcv: "+6.40%",
        deltaUsdt: "+7.80%"
      },
      "3M": {
        bcv: "M 10,142 L 70,128 L 130,118 L 190,102 L 250,88 L 310,76 L 360,64",
        usdt: "M 10,116 L 70,104 L 130,90 L 190,74 L 250,58 L 310,42 L 360,26",
        deltaBcv: "+15.20%",
        deltaUsdt: "+18.60%"
      }
    }[state.trendRange];

    stageEl.innerHTML = `
      <div class="sim-topbar">
        <span class="sim-brand">Brecha y Tendencia</span>
        <span class="sim-badge amber">Brecha ${fmtMoney(spreadPct, 1)}%</span>
      </div>

      <div class="sim-card">
        <span class="sim-label">Diferencial BCV vs Paralelo (USDT)</span>
        <div style="display:flex;justify-content:space-between;align-items:baseline;">
          <strong style="font-size:1.35rem;color:#34D399;">Bs. ${fmtMoney(RATES.USD_BCV.rate, 4)}</strong>
          <strong style="font-size:1.25rem;color:#F59E0B;">Bs. ${fmtMoney(RATES.USDT.rate, 2)}</strong>
        </div>
        <div class="sim-sub-row">
          <span>USD BCV (${curves.deltaBcv})</span>
          <span>USDT (${curves.deltaUsdt})</span>
        </div>
      </div>

      <div class="sim-card">
        <div class="sim-pill-row" style="margin-bottom:10px;">
          ${["1S", "1M", "3M"].map((r) => `
            <button type="button" class="sim-chip ${state.trendRange === r ? "is-selected" : ""}" data-range="${r}">${r}</button>
          `).join("")}
        </div>
        <svg viewBox="0 0 370 155" width="100%" height="155" role="img" aria-label="Gráfico comparativo BCV y USDT">
          <line x1="10" y1="40" x2="360" y2="40" stroke="rgba(255,255,255,0.06)" />
          <line x1="10" y1="85" x2="360" y2="85" stroke="rgba(255,255,255,0.06)" />
          <line x1="10" y1="130" x2="360" y2="130" stroke="rgba(255,255,255,0.06)" />
          <path d="${curves.usdt}" fill="none" stroke="#F59E0B" stroke-width="3" stroke-linecap="round" />
          <path d="${curves.bcv}" fill="none" stroke="#10B981" stroke-width="3" stroke-linecap="round" />
        </svg>
      </div>
    `;

    stageEl.querySelectorAll("[data-range]").forEach((btn) => {
      btn.addEventListener("click", () => {
        setAutoplay(false);
        state.trendRange = btn.getAttribute("data-range");
        renderTrends();
      });
    });
  }

  function renderCheckout() {
    const rate = RATES.USD_BCV.rate;
    const totalUsd = Number(state.checkoutTotalUsd) || 0;
    const totalBs = totalUsd * rate;
    const paidUsd = (Number(state.paidCashUsd) || 0) + (Number(state.paidPagoMovilBs) || 0) / rate;
    const diffUsd = totalUsd - paidUsd;
    const isChange = diffUsd < -0.005;
    const absUsd = Math.abs(diffUsd);
    const absBs = absUsd * rate;

    const statusTitle = isChange ? "VUELTO A ENTREGAR" : (absUsd <= 0.005 ? "CUENTA SALDADA" : "FALTA POR PAGAR");
    const whatsappText = `*Tasa VE • Resumen de Cobro*
Total: $${fmtMoney(totalUsd, 2)} • Bs. ${fmtMoney(totalBs, 2)}
Tasa (USD BCV): Bs. ${fmtMoney(rate, 4)}
${isChange ? "Vuelto" : "Por pagar"}: $${fmtMoney(absUsd, 2)} / Bs. ${fmtMoney(absBs, 2)}

*Pago Móvil (Comercio)*
Banco: 0134 - Banesco
CI/RIF: V-18730063
Teléfono: 0424-1533502

Descarga Tasa VE: https://tasave.net`;

    stageEl.innerHTML = `
      <div class="sim-topbar">
        <span class="sim-brand">Cobros y Pago Mixto</span>
        <span class="sim-badge">24 Bancos VE</span>
      </div>

      <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
        <div class="sim-card" style="padding:10px;">
          <span class="sim-label">Total cuenta ($)</span>
          <input id="sim-ck-total" class="sim-input" style="font-size:1rem;padding:7px 10px;" type="number" min="0" step="any" value="${totalUsd}">
        </div>
        <div class="sim-card" style="padding:10px;">
          <span class="sim-label">Abono efectivo ($)</span>
          <input id="sim-ck-cash" class="sim-input" style="font-size:1rem;padding:7px 10px;" type="number" min="0" step="any" value="${state.paidCashUsd}">
        </div>
      </div>

      <div class="png-mini-card">
        <div class="png-logo-lockup">
          <span class="png-badge-tile" aria-hidden="true">
            <img src="assets/logo_tasave_transparent_512.png" alt="" width="22" height="22" style="width:22px;height:22px;display:block;">
          </span>
          <span>Tasa VE</span>
        </div>
        <span class="sim-label" id="sim-ck-status">${statusTitle}</span>
        <div class="sim-big-result" id="sim-ck-amount" style="font-size:1.3rem;">$${fmtMoney(absUsd, 2)} / Bs. ${fmtMoney(absBs, 2)}</div>
        <div style="margin-top:10px;padding-top:10px;border-top:1px solid rgba(255,255,255,0.08);font-size:0.82rem;color:#CBD5E1;">
          <strong style="color:#34D399;display:block;margin-bottom:2px;">DATOS PAGO MÓVIL • Comercio</strong>
          Banco: 0134 - Banesco &bull; V-18730063 &bull; 0424-1533502
        </div>
      </div>

      <button type="button" class="btn btn-primary btn-sm" id="sim-copy-wa" style="width:100%;">
        Copiar mensaje WhatsApp con link tasave.net
      </button>
    `;

    function updateCheckoutLive() {
      const tUsd = Number(state.checkoutTotalUsd) || 0;
      const pUsd = (Number(state.paidCashUsd) || 0) + (Number(state.paidPagoMovilBs) || 0) / rate;
      const dUsd = tUsd - pUsd;
      const chg = dUsd < -0.005;
      const aUsd = Math.abs(dUsd);
      const aBs = aUsd * rate;
      const sEl = stageEl.querySelector("#sim-ck-status");
      const aEl = stageEl.querySelector("#sim-ck-amount");
      if (sEl) sEl.textContent = chg ? "VUELTO A ENTREGAR" : (aUsd <= 0.005 ? "CUENTA SALDADA" : "FALTA POR PAGAR");
      if (aEl) aEl.textContent = `$${fmtMoney(aUsd, 2)} / Bs. ${fmtMoney(aBs, 2)}`;
    }

    stageEl.querySelector("#sim-ck-total").addEventListener("input", (e) => {
      setAutoplay(false);
      state.checkoutTotalUsd = parseFloat(e.target.value) || 0;
      updateCheckoutLive();
    });
    stageEl.querySelector("#sim-ck-cash").addEventListener("input", (e) => {
      setAutoplay(false);
      state.paidCashUsd = parseFloat(e.target.value) || 0;
      updateCheckoutLive();
    });
    stageEl.querySelector("#sim-copy-wa").addEventListener("click", async () => {
      setAutoplay(false);
      const tUsd = Number(state.checkoutTotalUsd) || 0;
      const tBs = tUsd * rate;
      const pUsd = (Number(state.paidCashUsd) || 0) + (Number(state.paidPagoMovilBs) || 0) / rate;
      const dUsd = tUsd - pUsd;
      const chg = dUsd < -0.005;
      const aUsd = Math.abs(dUsd);
      const aBs = aUsd * rate;
      const liveText = `*Tasa VE • Resumen de Cobro*\nTotal: $${fmtMoney(tUsd, 2)} • Bs. ${fmtMoney(tBs, 2)}\nTasa (USD BCV): Bs. ${fmtMoney(rate, 4)}\n${chg ? "Vuelto" : "Por pagar"}: $${fmtMoney(aUsd, 2)} / Bs. ${fmtMoney(aBs, 2)}\n\n*Pago Móvil (Comercio)*\nBanco: 0134 - Banesco\nCI/RIF: V-18730063\nTeléfono: 0424-1533502\n\nDescarga Tasa VE: https://tasave.net`;
      try {
        await navigator.clipboard.writeText(liveText);
        showToast("Resumen de cobro copiado listo para WhatsApp");
      } catch {
        showToast("Toca y mantén para copiar el resumen");
      }
    });
  }

  // Initial render & gentle autoplay timer
  setAutoplay(state.autoplay);
  selectTab("converter");

  setInterval(() => {
    if (!state.autoplay) return;
    state.progress += 2;
    const activeBtn = tabBtns.find((b) => b.getAttribute("data-tab") === state.activeTab);
    if (activeBtn) {
      const bar = activeBtn.querySelector(".demo-progress");
      if (bar) bar.style.width = `${Math.min(state.progress, 100)}%`;
    }
    if (state.progress >= 100) {
      state.progress = 0;
      const idx = TAB_ORDER.indexOf(state.activeTab);
      const nextTab = TAB_ORDER[(idx + 1) % TAB_ORDER.length];
      selectTab(nextTab);
    }
  }, 90);
})();
