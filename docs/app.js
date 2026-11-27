/* ==========================================================================
   Tasa VE — Lightweight UI Controller (Theme, Screenshot Galleries, Plans & Legal)
   100% Informative Site — Zero Web Calculators / Zero External API Calls
   ========================================================================== */

(() => {
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
      const show = window.scrollY > 460;
      stickyBar.classList.toggle("is-visible", show);
      stickyBar.setAttribute("aria-hidden", String(!show));
    }, { passive: true });
  }

  // ---------------- Hero Device Screenshot Gallery ----------------
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
})();
