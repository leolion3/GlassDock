document.addEventListener("click", (e) => {
  const img = e.target.closest(".demo-card img");
  let overlay = document.querySelector(".gd-lightbox");

  if (img) {
    if (!overlay) {
      overlay = document.createElement("div");
      overlay.className = "gd-lightbox";
      overlay.innerHTML = "<img>";
      document.body.appendChild(overlay);
    }
    overlay.querySelector("img").src = img.src;
    overlay.querySelector("img").alt = img.alt;
    requestAnimationFrame(() => overlay.classList.add("open"));
    return;
  }

  if (overlay && overlay.classList.contains("open")) {
    overlay.classList.remove("open");
  }
});

document.addEventListener("keydown", (e) => {
  if (e.key === "Escape") {
    document.querySelector(".gd-lightbox.open")?.classList.remove("open");
  }
});
