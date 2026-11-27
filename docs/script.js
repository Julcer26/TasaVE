(function () {
  "use strict";

  // 1. Theme toggle (defaults to dark Silk Wave)
  var themeBtn = document.getElementById("theme-toggle");
  if (themeBtn) {
    themeBtn.addEventListener("click", function () {
      var cur = document.documentElement.getAttribute("data-theme") === "light" ? "light" : "dark";
      var next = cur === "dark" ? "light" : "dark";
      document.documentElement.setAttribute("data-theme", next);
      var metaScheme = document.querySelector('meta[name="color-scheme"]');
      if (metaScheme) metaScheme.content = next;
      var metaColor = document.querySelector('meta[name="theme-color"]');
      if (metaColor) metaColor.content = next === "dark" ? "#070B11" : "#F4F7FB";
      try {
        localStorage.setItem("tasave_web_theme", next);
      } catch (e) {}
    });
  }

  // 2. Billing Toggle — Defaults to Mensual ($0.99 / mes)
  var billingBtns = document.querySelectorAll(".billing-btn[data-billing]");
  var priceVal = document.getElementById("ultra-price-val");
  var pricePeriod = document.getElementById("ultra-price-period");
  var priceNote = document.getElementById("ultra-price-note");

  function setBilling(mode) {
    billingBtns.forEach(function (btn) {
      var active = btn.getAttribute("data-billing") === mode;
      btn.classList.toggle("is-active", active);
      btn.setAttribute("aria-pressed", active ? "true" : "false");
    });
    if (!priceVal || !pricePeriod || !priceNote) return;
    if (mode === "yearly") {
      priceVal.textContent = "$7.99";
      pricePeriod.textContent = "/ año";
      priceNote.textContent = "Equivale a $0.67/mes · Ahorras 33% frente al plan mensual.";
    } else {
      priceVal.textContent = "$0.99";
      pricePeriod.textContent = "/ mes";
      priceNote.textContent = "Cancela cuando quieras desde Google Play, sin permanencia.";
    }
  }

  billingBtns.forEach(function (btn) {
    btn.addEventListener("click", function () {
      setBilling(btn.getAttribute("data-billing"));
    });
  });

  // 3. Interactive Story Screen Switchers (Perfiles/Tarjeta & Calendario/Flotante)
  document.querySelectorAll(".story-switch").forEach(function (group) {
    var targetId = group.getAttribute("data-target");
    var imgEl = targetId ? document.getElementById(targetId) : null;
    var frameEl = imgEl ? imgEl.closest(".story-shot-frame") : null;
    var buttons = group.querySelectorAll(".story-sw-btn");
    buttons.forEach(function (btn) {
      btn.addEventListener("click", function () {
        buttons.forEach(function (b) {
          b.classList.remove("is-active");
          b.setAttribute("aria-pressed", "false");
        });
        btn.classList.add("is-active");
        btn.setAttribute("aria-pressed", "true");
        if (imgEl) {
          var src = btn.getAttribute("data-src");
          var alt = btn.getAttribute("data-alt");
          if (src) imgEl.src = src;
          if (alt) imgEl.alt = alt;
        }
        if (frameEl) {
          var isCard = btn.getAttribute("data-card") === "true";
          frameEl.classList.toggle("is-card-shot", isCard);
        }
      });
    });
  });

  // 4. FAQ Accordion
  document.querySelectorAll(".faq-trigger").forEach(function (trigger) {
    trigger.addEventListener("click", function () {
      var item = trigger.closest(".faq-item");
      if (!item) return;
      var willOpen = !item.classList.contains("is-open");
      document.querySelectorAll(".faq-item.is-open").forEach(function (openItem) {
        openItem.classList.remove("is-open");
        var t = openItem.querySelector(".faq-trigger");
        if (t) t.setAttribute("aria-expanded", "false");
      });
      if (willOpen) {
        item.classList.add("is-open");
        trigger.setAttribute("aria-expanded", "true");
      }
    });
  });

  // 5. Sticky Mobile Download Bar (appears after scrolling past hero CTA)
  var stickyBar = document.getElementById("mobile-sticky-bar");
  var heroSection = document.getElementById("inicio");
  if (stickyBar && heroSection && "IntersectionObserver" in window) {
    var io = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          stickyBar.classList.toggle("is-visible", !entry.isIntersecting);
        });
      },
      { threshold: 0.12 }
    );
    io.observe(heroSection);
  }
})();
