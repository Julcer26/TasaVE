/* ==========================================================================
   Tasa VE — Live Rates Engine, Hero Instant Converter & 5-Module Simulator
   ========================================================================== */

(() => {
  const CACHE_KEY = "tasave_live_rates_v2";
  const RATES = {
    USD_BCV: { label: "USD BCV", symbol: "$", rate: 916.65, tomorrow: 921.48, decimals: 4 },
    EUR_BCV: { label: "EUR BCV", symbol: "€", rate: 985.32, tomorrow: 990.12, decimals: 4 },
    USDT: { label: "USDT", symbol: "₮", rate: 1024.50, tomorrow: 1024.50, decimals: 2 },
    COP: { label: "COP", symbol: "COP", rate: 916.65 / 4320.0, tomorrow: 921.48 / 4320.0, decimals: 4, trm: 4320.0 }
  };

  // ---------------- Theme Toggle ----------------
  const root = document.documentElement;
  const colorSchemeMeta = document.querySelector('meta[name="color-scheme"]');
  const themeBtn = document.getElementById("theme-toggle");

  function applyTheme(theme) {
    if (theme === "light" || theme === "dark") {
      root.setAttribute("data-theme", theme);
      if (colorSchemeMeta) colorSchemeMeta.content = theme;
    } else {
      root.setAttribute("data-theme", "dark");
      if (colorSchemeMeta) colorSchemeMeta.content = "light dark";
    }
  }
  try { applyTheme(localStorage.getItem("tasave_web_theme")); } catch (_) { applyTheme(null); }

  if (themeBtn) {
    themeBtn.addEventListener("click", () => {
      const next = root.getAttribute("data-theme") === "light" ? "dark" : "light";
      applyTheme(next);
      try { localStorage.setItem("tasave_web_theme", next); } catch (_) {}
    });
  }
  window.addEventListener("storage", (e) => {
    if (e.key === "tasave_web_theme") applyTheme(e.newValue);
  });

  // ---------------- Mobile Menu & Sticky Bar ----------------
  const menuBtn = document.getElementById("mobile-menu-btn");
  const menuPanel = document.getElementById("mobile-menu-panel");
  if (menuBtn && menuPanel) {
    menuBtn.addEventListener("click", () => {
      const open = menuPanel.classList.toggle("is-open");
      menuBtn.setAttribute("aria-expanded", String(open));
    });
    menuPanel.querySelectorAll("a").forEach((a) => {
      a.addEventListener("click", () => {
        menuPanel.classList.remove("is-open");
        menuBtn.setAttribute("aria-expanded", "false");
      });
    });
  }

  const stickyBar = document.getElementById("mobile-sticky-cta");
  if (stickyBar) {
    window.addEventListener("scroll", () => {
      const show = window.scrollY > 520;
      stickyBar.classList.toggle("is-visible", show);
      stickyBar.setAttribute("aria-hidden", String(!show));
    }, { passive: true });
  }

  // ---------------- Toast & Formatters ----------------
  const toastEl = document.getElementById("web-toast");
  let toastTimer = null;
  function showToast(msg) {
    if (!toastEl) return;
    toastEl.textContent = msg;
    toastEl.classList.add("is-visible");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toastEl.classList.remove("is-visible"), 2400);
  }

  const fmtMoney = (n, d = 2) =>
    Number(n || 0).toLocaleString("es-VE", { minimumFractionDigits: d, maximumFractionDigits: d });

  // ---------------- Live Rates Fetch (with localStorage cache) ----------------
  function updateMarketRibbon(sourceLabel) {
    const usdEl = document.getElementById("live-usd-bcv");
    const eurEl = document.getElementById("live-eur-bcv");
    const usdtEl = document.getElementById("live-usdt");
    const spreadEl = document.getElementById("live-spread");
    const copEl = document.getElementById("live-cop");
    const statusEl = document.getElementById("live-rate-status");

    if (usdEl) usdEl.textContent = `Bs. ${fmtMoney(RATES.USD_BCV.rate, 4)}`;
    if (eurEl) eurEl.textContent = `Bs. ${fmtMoney(RATES.EUR_BCV.rate, 4)}`;
    if (usdtEl) usdtEl.textContent = `Bs. ${fmtMoney(RATES.USDT.rate, 2)}`;
    if (spreadEl && RATES.USD_BCV.rate > 0) {
      const pct = ((RATES.USDT.rate - RATES.USD_BCV.rate) / RATES.USD_BCV.rate) * 100;
      spreadEl.textContent = `${pct >= 0 ? "+" : ""}${fmtMoney(pct, 1)}%`;
    }
    if (copEl) copEl.textContent = `${fmtMoney(RATES.COP.trm || 4320, 0)} / USD`;
    if (statusEl && sourceLabel) statusEl.textContent = sourceLabel;
  }

  function hydrateCachedRates() {
    try {
      const raw = localStorage.getItem(CACHE_KEY);
      if (!raw) return;
      const saved = JSON.parse(raw);
      if (saved.usd > 0) { RATES.USD_BCV.rate = saved.usd; RATES.USD_BCV.tomorrow = saved.usdNext || saved.usd; }
      if (saved.eur > 0) { RATES.EUR_BCV.rate = saved.eur; RATES.EUR_BCV.tomorrow = saved.eurNext || saved.eur; }
      if (saved.usdt > 0) { RATES.USDT.rate = saved.usdt; RATES.USDT.tomorrow = saved.usdt; }
      if (saved.trm > 0) { RATES.COP.trm = saved.trm; }
      RATES.COP.rate = RATES.USD_BCV.rate / (RATES.COP.trm || 4320);
      RATES.COP.tomorrow = RATES.USD_BCV.tomorrow / (RATES.COP.trm || 4320);
    } catch (_) {}
  }

  async function fetchLiveRates() {
    hydrateCachedRates();
    updateMarketRibbon("Tasas de referencia activas");
    renderHeroConverter();

    try {
      const [dolaresRes, eurRes, trmRes] = await Promise.allSettled([
        fetch("https://ve.dolarapi.com/v1/dolares", { cache: "no-store" }).then((r) => r.ok ? r.json() : null),
        fetch("https://open.er-api.com/v6/latest/EUR", { cache: "no-store" }).then((r) => r.ok ? r.json() : null),
        fetch("https://co.dolarapi.com/v1/trm", { cache: "no-store" }).then((r) => r.ok ? r.json() : null)
      ]);

      let updated = false;
      if (dolaresRes.status === "fulfilled" && Array.isArray(dolaresRes.value)) {
        const oficial = dolaresRes.value.find((d) => d.fuente === "oficial");
        const paralelo = dolaresRes.value.find((d) => d.fuente === "paralelo");
        if (oficial && Number(oficial.promedio) > 0) {
          RATES.USD_BCV.rate = Number(oficial.promedio);
          RATES.USD_BCV.tomorrow = Number(oficial.promedio);
          updated = true;
        }
        if (paralelo && Number(paralelo.promedio) > 0) {
          RATES.USDT.rate = Number(paralelo.promedio);
          RATES.USDT.tomorrow = Number(paralelo.promedio);
          updated = true;
        }
      }
      if (eurRes.status === "fulfilled" && eurRes.value && eurRes.value.rates && Number(eurRes.value.rates.USD) > 0) {
        const eurToUsd = Number(eurRes.value.rates.USD);
        RATES.EUR_BCV.rate = RATES.USD_BCV.rate * eurToUsd;
        RATES.EUR_BCV.tomorrow = RATES.EUR_BCV.rate;
      }
      if (trmRes.status === "fulfilled" && trmRes.value && Number(trmRes.value.valor) > 0) {
        RATES.COP.trm = Number(trmRes.value.valor);
      }
      RATES.COP.rate = RATES.USD_BCV.rate / (RATES.COP.trm || 4320);
      RATES.COP.tomorrow = RATES.COP.rate;

      if (updated) {
        try {
          localStorage.setItem(CACHE_KEY, JSON.stringify({
            usd: RATES.USD_BCV.rate,
            eur: RATES.EUR_BCV.rate,
            usdt: RATES.USDT.rate,
            trm: RATES.COP.trm,
            ts: Date.now()
          }));
        } catch (_) {}
        const nowStr = new Date().toLocaleTimeString("es-VE", { hour: "2-digit", minute: "2-digit" });
        updateMarketRibbon(`En vivo • ${nowStr}`);
        renderHeroConverter();
        renderStage();
      }
    } catch (_) {
      updateMarketRibbon("Referencia local activa");
    }
  }

  // ---------------- Hero Instant Converter ----------------
  const heroState = { rateKey: "USD_BCV", vesToForeign: false, amount: 25 };
  const heroInput = document.getElementById("hero-amount-input");
  const heroOutput = document.getElementById("hero-result-output");
  const heroInputCap = document.getElementById("hero-input-caption");
  const heroOutputCap = document.getElementById("hero-output-caption");
  const heroRateCap = document.getElementById("hero-rate-caption");
  const heroDirLabel = document.getElementById("hero-dir-label");
  const heroRateBtns = Array.from(document.querySelectorAll("[data-hero-rate]"));

  function renderHeroConverter() {
    const info = RATES[heroState.rateKey] || RATES.USD_BCV;
    const raw = Number(heroState.amount) || 0;
    const rate = info.rate;
    const result = heroState.vesToForeign ? (rate > 0 ? raw / rate : 0) : raw * rate;
    const shortCode = heroState.rateKey === "USD_BCV" ? "USD" : (heroState.rateKey === "EUR_BCV" ? "EUR" : info.label);

    if (heroDirLabel) heroDirLabel.textContent = heroState.vesToForeign ? `Bs. → ${shortCode}` : `${shortCode} → Bs.`;
    if (heroInputCap) heroInputCap.textContent = heroState.vesToForeign ? "Monto en Bolívares (Bs.)" : `Monto en ${info.label}`;
    if (heroOutputCap) heroOutputCap.textContent = heroState.vesToForeign ? `Equivalente en ${info.label}` : "Resultado exacto en Bolívares";
    if (heroOutput) {
      heroOutput.textContent = heroState.vesToForeign
        ? `${info.symbol === "COP" ? "" : info.symbol + " "}${fmtMoney(result, info.symbol === "COP" ? 0 : 2)}${info.symbol === "COP" ? " COP" : ""}`
        : `Bs. ${fmtMoney(result, 2)}`;
    }
    if (heroRateCap) {
      heroRateCap.textContent = heroState.rateKey === "COP"
        ? `1.000 COP = Bs. ${fmtMoney(rate * 1000, 2)} • TRM ${fmtMoney(info.trm || 4320, 0)}`
        : `Tasa ${info.label}: Bs. ${fmtMoney(rate, info.decimals)}`;
    }
  }

  heroRateBtns.forEach((btn) => {
    btn.addEventListener("click", () => {
      heroState.rateKey = btn.getAttribute("data-hero-rate");
      heroRateBtns.forEach((b) => b.classList.toggle("is-active", b === btn));
      renderHeroConverter();
    });
  });

  const heroSwapBtn = document.getElementById("hero-swap-btn");
  if (heroSwapBtn) {
    heroSwapBtn.addEventListener("click", () => {
      heroState.vesToForeign = !heroState.vesToForeign;
      renderHeroConverter();
    });
  }
  if (heroInput) {
    heroInput.addEventListener("input", (e) => {
      heroState.amount = parseFloat(e.target.value) || 0;
      renderHeroConverter();
    });
  }
  const heroCopyBtn = document.getElementById("hero-copy-btn");
  if (heroCopyBtn && heroOutput) {
    heroCopyBtn.addEventListener("click", async () => {
      try {
        await navigator.clipboard.writeText(heroOutput.textContent.trim());
        showToast(`Copiado: ${heroOutput.textContent.trim()}`);
      } catch {
        showToast("Selecciona el monto para copiarlo");
      }
    });
  }

  // ---------------- Hero Device Screen Switcher ----------------
  const heroDeviceImg = document.getElementById("hero-device-img");
  const dockBtns = Array.from(document.querySelectorAll("[data-hero-screen]"));
  dockBtns.forEach((btn) => {
    btn.addEventListener("click", () => {
      dockBtns.forEach((b) => b.classList.toggle("is-active", b === btn));
      if (heroDeviceImg) {
        heroDeviceImg.src = btn.getAttribute("data-hero-screen");
        heroDeviceImg.alt = btn.getAttribute("data-hero-alt") || "";
      }
    });
  });

  // ---------------- Bento Real-Screen Switcher ----------------
  document.querySelectorAll("[data-spotlight-group]").forEach((groupEl) => {
    const groupId = groupEl.getAttribute("data-spotlight-group");
    const targetImg = document.getElementById(`${groupId}-img`);
    if (!targetImg) return;
    const pills = Array.from(groupEl.querySelectorAll(".mini-pill"));
    pills.forEach((pill) => {
      pill.addEventListener("click", () => {
        pills.forEach((p) => p.classList.toggle("is-active", p === pill));
        const nextSrc = pill.getAttribute("data-src");
        const nextAlt = pill.getAttribute("data-alt");
        if (nextSrc) targetImg.src = nextSrc;
        if (nextAlt) targetImg.alt = nextAlt;
      });
    });
  });

  // ---------------- Pricing Monthly / Yearly Toggle ----------------
  const billingBtns = Array.from(document.querySelectorAll("[data-billing]"));
  const ultraPriceVal = document.getElementById("ultra-price-val");
  const ultraPricePeriod = document.getElementById("ultra-price-period");
  billingBtns.forEach((btn) => {
    btn.addEventListener("click", () => {
      const mode = btn.getAttribute("data-billing");
      billingBtns.forEach((b) => b.classList.toggle("is-active", b === btn));
      if (ultraPriceVal && ultraPricePeriod) {
        if (mode === "monthly") {
          ultraPriceVal.textContent = "$0.99";
          ultraPricePeriod.textContent = "/ mes • cancela cuando quieras";
        } else {
          ultraPriceVal.textContent = "$7.99";
          ultraPricePeriod.textContent = "/ año (equivale a $0.67/mes)";
        }
      }
    });
  });

  // ---------------- Interactive 5-Module Simulator ----------------
  const TAB_ORDER = ["converter", "scanner", "cart", "utiles", "checkout"];
  const state = {
    activeTab: "converter",
    autoplay: !window.matchMedia("(prefers-reduced-motion: reduce)").matches,
    progress: 0,
    rateKey: "USD_BCV",
    tomorrowBcv: false,
    amount: 25,
    vesToForeign: false,
    ocrIndex: 0,
    ocrSamples: [
      { item: "Queso Blanco 1kg", usd: 7.80 },
      { item: "Combo Desayuno", usd: 14.50 },
      { item: "Aceite de Oliva 500ml", usd: 11.90 },
      { item: "Café Molido 500g", usd: 6.25 }
    ],
    cartItems: [
      { id: "cafe", name: "Café Molido 500g", price: 6.25, qty: 1 },
      { id: "harina", name: "Harina P.A.N. 1kg", price: 1.35, qty: 2 },
      { id: "aceite", name: "Aceite Vegetal 1L", price: 3.40, qty: 1 }
    ],
    useIva: true,
    useIgtf: false,
    utilMode: "compare",
    checkoutTotalUsd: 25.00,
    paidCashUsd: 10.00,
    hideSensitive: false
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
      autoplayBtn.textContent = enabled ? "Pausar" : "Reproducir";
      autoplayBtn.setAttribute("aria-pressed", String(enabled));
    }
    if (autoplayLabel) {
      autoplayLabel.textContent = enabled ? "Modo automático activo" : "Modo manual — interactúa libremente";
    }
  }

  if (autoplayBtn) autoplayBtn.addEventListener("click", () => setAutoplay(!state.autoplay));

  tabBtns.forEach((btn) => {
    btn.addEventListener("click", () => {
      const tab = btn.getAttribute("data-tab");
      if (tab) { setAutoplay(false); selectTab(tab); }
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
    else if (state.activeTab === "utiles") renderUtiles();
    else if (state.activeTab === "checkout") renderCheckout();
  }

  function renderConverter() {
    const info = RATES[state.rateKey] || RATES.USD_BCV;
    const activeRate = state.tomorrowBcv && state.rateKey !== "USDT" ? info.tomorrow : info.rate;
    const rawVal = Number(state.amount) || 0;
    const vesVal = state.vesToForeign ? rawVal : rawVal * activeRate;
    const foreignVal = state.vesToForeign ? (activeRate > 0 ? rawVal / activeRate : 0) : rawVal;

    stageEl.innerHTML = `
      <div class="sim-topbar">
        <span class="sim-brand">Tasa VE</span>
        <span class="sim-badge ${state.tomorrowBcv ? "amber" : ""}">${state.tomorrowBcv ? "BCV Mañana" : "SUDEBAN • BCV"}</span>
      </div>
      <div class="sim-pill-row" role="group" aria-label="Seleccionar moneda">
        ${Object.keys(RATES).map((k) => `
          <button type="button" class="sim-chip ${state.rateKey === k ? "is-selected" : ""}" data-rate="${k}">${RATES[k].label}</button>
        `).join("")}
        <button type="button" class="sim-chip is-amber ${state.tomorrowBcv ? "is-selected" : ""}" id="sim-toggle-tomorrow">${state.tomorrowBcv ? "Ver Hoy" : "BCV Mañana"}</button>
      </div>
      <div class="sim-card">
        <span class="sim-label">${state.vesToForeign ? "Bolívares (VES)" : info.label}</span>
        <div class="sim-input-row">
          <input id="sim-conv-input" class="sim-input" type="number" min="0" step="any" value="${rawVal}" aria-label="Monto a convertir">
          <button type="button" class="sim-chip is-selected" id="sim-swap-dir">Bs. ↔ ${info.symbol}</button>
        </div>
        <div class="sim-sub-row">
          <span class="mono">${state.rateKey === "COP" ? `1.000 COP = Bs. ${fmtMoney(activeRate * 1000, 2)}` : `Tasa: Bs. ${fmtMoney(activeRate, info.decimals)}`}</span>
          <span>${state.rateKey === "USDT" ? "P2P Ref." : (state.rateKey === "COP" ? "Frontera TRM" : "BCV Oficial")}</span>
        </div>
      </div>
      <div class="sim-card">
        <span class="sim-label">${state.vesToForeign ? "Equivalente en " + info.label : "Resultado en Bolívares"}</span>
        <div class="sim-big-result" id="sim-conv-result">${state.vesToForeign ? `${info.symbol} ${fmtMoney(foreignVal, 2)}` : `Bs. ${fmtMoney(vesVal, 2)}`}</div>
        <div class="sim-sub-row mono" id="sim-conv-equiv">
          <span>USD: $${fmtMoney(RATES.USD_BCV.rate > 0 ? vesVal / RATES.USD_BCV.rate : 0, 2)}</span>
          <span>EUR: €${fmtMoney(RATES.EUR_BCV.rate > 0 ? vesVal / RATES.EUR_BCV.rate : 0, 2)}</span>
        </div>
      </div>
    `;

    stageEl.querySelectorAll("[data-rate]").forEach((el) => {
      el.addEventListener("click", () => { setAutoplay(false); state.rateKey = el.getAttribute("data-rate"); renderConverter(); });
    });
    stageEl.querySelector("#sim-toggle-tomorrow").addEventListener("click", () => { setAutoplay(false); state.tomorrowBcv = !state.tomorrowBcv; renderConverter(); });
    stageEl.querySelector("#sim-swap-dir").addEventListener("click", () => { setAutoplay(false); state.vesToForeign = !state.vesToForeign; renderConverter(); });
    stageEl.querySelector("#sim-conv-input").addEventListener("input", (e) => {
      setAutoplay(false);
      state.amount = parseFloat(e.target.value) || 0;
      const val = Number(state.amount) || 0;
      const vVal = state.vesToForeign ? val : val * activeRate;
      const fVal = state.vesToForeign ? (activeRate > 0 ? val / activeRate : 0) : val;
      const resEl = stageEl.querySelector("#sim-conv-result");
      const eqEl = stageEl.querySelector("#sim-conv-equiv");
      if (resEl) resEl.textContent = state.vesToForeign ? `${info.symbol} ${fmtMoney(fVal, 2)}` : `Bs. ${fmtMoney(vVal, 2)}`;
      if (eqEl) eqEl.innerHTML = `<span>USD: $${fmtMoney(RATES.USD_BCV.rate > 0 ? vVal / RATES.USD_BCV.rate : 0, 2)}</span><span>EUR: €${fmtMoney(RATES.EUR_BCV.rate > 0 ? vVal / RATES.EUR_BCV.rate : 0, 2)}</span>`;
    });
  }

  function renderScanner() {
    const sample = state.ocrSamples[state.ocrIndex % state.ocrSamples.length];
    const rate = RATES.USD_BCV.rate;
    const bs = sample.usd * rate;
    stageEl.innerHTML = `
      <div class="sim-topbar">
        <span class="sim-brand">Escáner OCR 123</span>
        <span class="sim-badge">100% en dispositivo</span>
      </div>
      <div class="ocr-viewfinder">
        <div class="ocr-laser" aria-hidden="true"></div>
        <span class="sim-label">Precio detectado por cámara</span>
        <div class="ocr-detected">$${fmtMoney(sample.usd, 2)}</div>
        <div style="color:#98A4B8;font-size:0.84rem;margin-top:4px;">${sample.item}</div>
      </div>
      <div class="sim-card">
        <span class="sim-label">Conversión instantánea (BCV ${fmtMoney(rate, 4)})</span>
        <div class="sim-big-result">Bs. ${fmtMoney(bs, 2)}</div>
        <div class="sim-sub-row"><span>Funciona sin internet</span><span>Sin subir fotos</span></div>
      </div>
      <div class="sim-pill-row">
        <button type="button" class="btn btn-primary btn-sm" id="sim-next-scan" style="flex:1;">Escanear otro precio</button>
        <button type="button" class="sim-chip is-selected" id="sim-add-scan-cart">+ Al carrito</button>
      </div>
    `;
    stageEl.querySelector("#sim-next-scan").addEventListener("click", () => { setAutoplay(false); state.ocrIndex = (state.ocrIndex + 1) % state.ocrSamples.length; renderScanner(); });
    stageEl.querySelector("#sim-add-scan-cart").addEventListener("click", () => {
      setAutoplay(false);
      state.cartItems.push({ id: "scan_" + Date.now(), name: sample.item, price: sample.usd, qty: 1 });
      showToast(`"${sample.item}" agregado al carrito`);
      selectTab("cart");
    });
  }

  function renderCart() {
    const rate = RATES.USD_BCV.rate;
    const subtotal = state.cartItems.reduce((a, it) => a + it.price * it.qty, 0);
    const iva = state.useIva ? subtotal * 0.16 : 0;
    const igtf = state.useIgtf ? (subtotal + iva) * 0.03 : 0;
    const totalUsd = subtotal + iva + igtf;
    const totalBs = totalUsd * rate;

    stageEl.innerHTML = `
      <div class="sim-topbar">
        <span class="sim-brand">Mi Carrito</span>
        <span class="sim-badge">${state.cartItems.reduce((a, b) => a + b.qty, 0)} uds.</span>
      </div>
      <div class="sim-card" style="max-height:190px;overflow-y:auto;">
        ${state.cartItems.map((it, idx) => `
          <div class="cart-item-row">
            <div>
              <div style="font-weight:600;font-size:0.86rem;">${it.name}</div>
              <div class="mono" style="font-size:0.76rem;color:#98A4B8;">$${fmtMoney(it.price, 2)} c/u</div>
            </div>
            <div class="qty-controls">
              <button type="button" class="qty-btn" data-dec="${idx}" aria-label="Restar ${it.name}">−</button>
              <strong class="mono" style="min-width:18px;text-align:center;">${it.qty}</strong>
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
        <div class="sim-sub-row" style="padding-top:0;"><span>Subtotal</span><strong class="mono">$${fmtMoney(subtotal, 2)}</strong></div>
        ${state.useIva ? `<div class="sim-sub-row"><span>IVA (16%)</span><span class="mono">+$${fmtMoney(iva, 2)}</span></div>` : ""}
        ${state.useIgtf ? `<div class="sim-sub-row" style="color:#E5A93C;"><span>IGTF (3%)</span><span class="mono">+$${fmtMoney(igtf, 2)}</span></div>` : ""}
        <div class="sim-big-result" style="margin-top:8px;font-size:1.3rem;">$${fmtMoney(totalUsd, 2)} / Bs. ${fmtMoney(totalBs, 2)}</div>
      </div>
    `;
    stageEl.querySelectorAll("[data-inc]").forEach((b) => b.addEventListener("click", () => { setAutoplay(false); state.cartItems[Number(b.getAttribute("data-inc"))].qty += 1; renderCart(); }));
    stageEl.querySelectorAll("[data-dec]").forEach((b) => b.addEventListener("click", () => {
      setAutoplay(false);
      const i = Number(b.getAttribute("data-dec"));
      if (state.cartItems[i].qty > 1) state.cartItems[i].qty -= 1;
      else if (state.cartItems.length > 1) state.cartItems.splice(i, 1);
      renderCart();
    }));
    stageEl.querySelector("#sim-iva").addEventListener("click", () => { setAutoplay(false); state.useIva = !state.useIva; renderCart(); });
    stageEl.querySelector("#sim-igtf").addEventListener("click", () => { setAutoplay(false); state.useIgtf = !state.useIgtf; renderCart(); });
  }

  function renderUtiles() {
    const bcv = RATES.USD_BCV.rate;
    const usdt = RATES.USDT.rate;
    let bodyHtml = "";
    if (state.utilMode === "compare") {
      const price = 100;
      const bsNeeded = price * bcv;
      const usdtNeeded = usdt > 0 ? bsNeeded / usdt : price;
      const savings = price - usdtNeeded;
      bodyHtml = `
        <div class="sim-card">
          <span class="sim-label">Cuenta de ejemplo: $100,00</span>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:6px;">
            <div style="padding:10px;border-radius:10px;background:rgba(16,185,129,0.14);border:1px solid rgba(16,185,129,0.4);">
              <div style="font-size:0.72rem;color:#34D399;font-weight:700;">Cambiando USDT</div>
              <strong class="mono" style="font-size:1rem;">${fmtMoney(usdtNeeded, 2)} USDT</strong>
              <div class="mono" style="font-size:0.7rem;color:#98A4B8;">Bs. ${fmtMoney(bsNeeded, 2)}</div>
            </div>
            <div style="padding:10px;border-radius:10px;background:rgba(255,255,255,0.04);border:1px solid rgba(255,255,255,0.08);">
              <div style="font-size:0.72rem;color:#98A4B8;font-weight:700;">Efectivo $ / Zelle</div>
              <strong class="mono" style="font-size:1rem;">$100,00</strong>
              <div style="font-size:0.7rem;color:#98A4B8;">Pago directo</div>
            </div>
          </div>
          <div class="sim-sub-row" style="margin-top:8px;color:#34D399;font-weight:700;">
            <span>${savings > 0 ? `Conviene USDT: ahorras $${fmtMoney(savings, 2)}` : "Conviene pagar directo en $"}</span>
          </div>
        </div>
      `;
    } else {
      const total = 80;
      const perPerson = total / 4;
      bodyHtml = `
        <div class="sim-card">
          <span class="sim-label">Cuenta $80,00 entre 4 personas (BCV)</span>
          <div class="sim-big-result" style="font-size:1.25rem;margin-top:4px;">$${fmtMoney(perPerson, 2)} • Bs. ${fmtMoney(perPerson * bcv, 2)} c/u</div>
          <div class="sim-sub-row" style="margin-top:8px;">
            <span class="mono">Total: Bs. ${fmtMoney(total * bcv, 2)}</span>
            <span>USD, EUR, Bs., USDT y COP</span>
          </div>
        </div>
      `;
    }

    stageEl.innerHTML = `
      <div class="sim-topbar">
        <span class="sim-brand">Útiles del Día a Día</span>
        <span class="sim-badge">Gratis</span>
      </div>
      <div class="sim-pill-row">
        <button type="button" class="sim-chip ${state.utilMode === "compare" ? "is-selected" : ""}" data-util="compare">¿Con qué pago?</button>
        <button type="button" class="sim-chip ${state.utilMode === "split" ? "is-selected" : ""}" data-util="split">Dividir cuenta</button>
      </div>
      ${bodyHtml}
    `;
    stageEl.querySelectorAll("[data-util]").forEach((b) => b.addEventListener("click", () => { setAutoplay(false); state.utilMode = b.getAttribute("data-util"); renderUtiles(); }));
  }

  function renderCheckout() {
    const rate = RATES.USD_BCV.rate;
    const totalUsd = Number(state.checkoutTotalUsd) || 0;
    const paidUsd = Number(state.paidCashUsd) || 0;
    const diffUsd = totalUsd - paidUsd;
    const isChange = diffUsd < -0.005;
    const absUsd = Math.abs(diffUsd);
    const absBs = absUsd * rate;
    const statusTitle = isChange ? "VUELTO A ENTREGAR" : (absUsd <= 0.005 ? "CUENTA SALDADA" : "FALTA POR PAGAR");
    const maskedData = state.hideSensitive
      ? "0134 - Banesco • ••••063 • ••••502"
      : "0134 - Banesco • V-24123456 • 0414-1234567";

    stageEl.innerHTML = `
      <div class="sim-topbar">
        <span class="sim-brand">Cobros y Abonos</span>
        <button type="button" class="sim-chip ${state.hideSensitive ? "is-amber is-selected" : ""}" id="sim-toggle-eye">
          ${state.hideSensitive ? "Mostrar datos" : "Ocultar datos"}
        </button>
      </div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
        <div class="sim-card" style="padding:10px;">
          <span class="sim-label">Total cuenta ($)</span>
          <input id="sim-ck-total" class="sim-input" style="font-size:1rem;padding:7px 10px;" type="number" min="0" step="any" value="${totalUsd}">
        </div>
        <div class="sim-card" style="padding:10px;">
          <span class="sim-label">Abono ($)</span>
          <input id="sim-ck-cash" class="sim-input" style="font-size:1rem;padding:7px 10px;" type="number" min="0" step="any" value="${paidUsd}">
        </div>
      </div>
      <div class="png-mini-card">
        <div class="png-logo-lockup">
          <span>Tasa VE • Tarjeta PNG</span>
          <span class="sim-badge">24 Bancos VE</span>
        </div>
        <span class="sim-label" id="sim-ck-status">${statusTitle}</span>
        <div class="sim-big-result" id="sim-ck-amount" style="font-size:1.25rem;">$${fmtMoney(absUsd, 2)} / Bs. ${fmtMoney(absBs, 2)}</div>
        <div style="margin-top:9px;padding-top:9px;border-top:1px solid rgba(255,255,255,0.08);font-size:0.8rem;color:#CBD5E1;">
          <strong style="color:#34D399;display:block;margin-bottom:2px;">PAGO MÓVIL • Comercio</strong>
          <span class="mono">${maskedData}</span>
        </div>
      </div>
      <button type="button" class="btn btn-primary btn-sm" id="sim-copy-bank" style="width:100%;">
        Copiar datos (formato limpio para banco)
      </button>
    `;

    function updateLive() {
      const tUsd = Number(state.checkoutTotalUsd) || 0;
      const pUsd = Number(state.paidCashUsd) || 0;
      const d = tUsd - pUsd;
      const aUsd = Math.abs(d);
      const aBs = aUsd * rate;
      const sEl = stageEl.querySelector("#sim-ck-status");
      const aEl = stageEl.querySelector("#sim-ck-amount");
      if (sEl) sEl.textContent = d < -0.005 ? "VUELTO A ENTREGAR" : (aUsd <= 0.005 ? "CUENTA SALDADA" : "FALTA POR PAGAR");
      if (aEl) aEl.textContent = `$${fmtMoney(aUsd, 2)} / Bs. ${fmtMoney(aBs, 2)}`;
    }

    stageEl.querySelector("#sim-toggle-eye").addEventListener("click", () => { setAutoplay(false); state.hideSensitive = !state.hideSensitive; renderCheckout(); });
    stageEl.querySelector("#sim-ck-total").addEventListener("input", (e) => { setAutoplay(false); state.checkoutTotalUsd = parseFloat(e.target.value) || 0; updateLive(); });
    stageEl.querySelector("#sim-ck-cash").addEventListener("input", (e) => { setAutoplay(false); state.paidCashUsd = parseFloat(e.target.value) || 0; updateLive(); });
    stageEl.querySelector("#sim-copy-bank").addEventListener("click", async () => {
      setAutoplay(false);
      const tUsd = Number(state.checkoutTotalUsd) || 0;
      const pUsd = Number(state.paidCashUsd) || 0;
      const pendingBs = Math.max(0, tUsd - pUsd) * rate;
      const cleanBlock = pendingBs > 0.005
        ? `0134\nV24123456\n04141234567\n${pendingBs.toFixed(2).replace(".", ",")}`
        : "0134\nV24123456\n04141234567";
      try {
        await navigator.clipboard.writeText(cleanBlock);
        showToast("Bloque limpio de 4 líneas copiado para el banco");
      } catch {
        showToast("Listo para pegar en BDV, Banesco, Mercantil o Bancamiga");
      }
    });
  }

  // ---------------- Legal Hub Tabs ----------------
  const legalTabBtns = Array.from(document.querySelectorAll("[data-legal-tab]"));
  const legalPanels = Array.from(document.querySelectorAll("[data-legal-panel]"));
  legalTabBtns.forEach((btn) => {
    btn.addEventListener("click", () => {
      const key = btn.getAttribute("data-legal-tab");
      legalTabBtns.forEach((b) => {
        const active = b.getAttribute("data-legal-tab") === key;
        b.classList.toggle("is-active", active);
        b.setAttribute("aria-selected", String(active));
      });
      legalPanels.forEach((p) => p.classList.toggle("is-active", p.getAttribute("data-legal-panel") === key));
    });
  });

  // ---------------- Boot ----------------
  setAutoplay(state.autoplay);
  selectTab("converter");
  fetchLiveRates();

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
      selectTab(TAB_ORDER[(idx + 1) % TAB_ORDER.length]);
    }
  }, 90);
})();
