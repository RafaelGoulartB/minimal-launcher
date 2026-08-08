/* Minimal Launcher — landing page interactions */
(() => {
  const nav = document.getElementById("nav");
  if (nav) {
    const onScroll = () => nav.classList.toggle("is-scrolled", window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
  }

  const menuButton = document.getElementById("menuBtn");
  const mobileMenu = document.getElementById("mobileMenu");
  if (menuButton && mobileMenu) {
    menuButton.addEventListener("click", () => {
      const isOpen = !mobileMenu.classList.contains("hidden");
      mobileMenu.classList.toggle("hidden", isOpen);
      menuButton.setAttribute("aria-label", isOpen ? "Open menu" : "Close menu");
    });

    mobileMenu.querySelectorAll("a").forEach((link) => {
      link.addEventListener("click", () => {
        mobileMenu.classList.add("hidden");
        menuButton.setAttribute("aria-label", "Open menu");
      });
    });
  }

  const revealTargets = document.querySelectorAll("section h2, section h3");
  revealTargets.forEach((element) => element.classList.add("reveal"));

  if ("IntersectionObserver" in window) {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add("is-visible");
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.15, rootMargin: "0px 0px -10% 0px" },
    );
    revealTargets.forEach((element) => observer.observe(element));
  } else {
    revealTargets.forEach((element) => element.classList.add("is-visible"));
  }

  document.querySelectorAll('a[href^="#"]').forEach((link) => {
    link.addEventListener("click", (event) => {
      const id = link.getAttribute("href");
      if (!id || id === "#") return;
      const target = document.querySelector(id);
      if (!target) return;
      event.preventDefault();
      target.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  });

  const formatCount = (count) => {
    if (typeof count !== "number" || !Number.isFinite(count)) return null;
    if (count >= 1000) return `${(count / 1000).toFixed(count >= 10000 ? 0 : 1).replace(/\.0$/, "")}k`;
    return String(count);
  };

  fetch("https://api.github.com/repos/RafaelGoulartB/minimal-launcher", {
    headers: { Accept: "application/vnd.github+json" },
  })
    .then((response) => (response.ok ? response.json() : null))
    .then((data) => {
      const count = formatCount(data?.stargazers_count);
      if (!count) return;
      const desktop = document.getElementById("starCount");
      const mobile = document.getElementById("starCountMobile");
      if (desktop) desktop.textContent = `★ ${count}`;
      if (mobile) mobile.textContent = count;
    })
    .catch(() => {});
})();
