(function () {
  "use strict";

  var header = document.querySelector("[data-header]");
  if (header) {
    var syncHeader = function () { header.classList.toggle("is-stuck", window.scrollY > 6); };
    syncHeader();
    window.addEventListener("scroll", syncHeader, { passive: true });
  }

  var toggle = document.querySelector("[data-nav-toggle]");
  var nav = document.getElementById("mobile-nav");
  if (toggle && nav) {
    var setNav = function (open) {
      nav.classList.toggle("is-open", open);
      toggle.setAttribute("aria-expanded", open ? "true" : "false");
      toggle.setAttribute("aria-label", open ? "Close menu" : "Open menu");
    };
    toggle.addEventListener("click", function () { setNav(!nav.classList.contains("is-open")); });
    nav.addEventListener("click", function (event) { if (event.target.closest("a")) setNav(false); });
    window.addEventListener("keydown", function (event) { if (event.key === "Escape") setNav(false); });
  }

  var revealed = document.querySelectorAll("[data-reveal]");
  revealed.forEach(function (element) { element.classList.add("reveal"); });
  if ("IntersectionObserver" in window) {
    var observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (!entry.isIntersecting) return;
        entry.target.classList.add("is-in");
        observer.unobserve(entry.target);
      });
    }, { threshold: .12 });
    revealed.forEach(function (element) { observer.observe(element); });
  } else {
    revealed.forEach(function (element) { element.classList.add("is-in"); });
  }

  document.querySelectorAll("[data-year]").forEach(function (element) {
    element.textContent = String(new Date().getFullYear());
  });
})();
