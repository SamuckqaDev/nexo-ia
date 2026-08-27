(function () {
  "use strict";
  const docs = window.NEXO_DOCS || [];
  const nav = document.querySelector("#docs-nav");
  const content = document.querySelector("#docs-content");
  const toc = document.querySelector("#docs-toc");
  const search = document.querySelector("#docs-search");
  const count = document.querySelector("#docs-count");
  if (!nav || !content || !window.NexoMarkdown) return;

  const buttonFor = (doc) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "docs-nav-item";
    button.dataset.doc = doc.id;
    button.textContent = doc.title;
    return button;
  };
  docs.forEach((doc) => nav.append(buttonFor(doc)));
  count.textContent = `${docs.length} documents`;

  function openDocument(id, heading, shouldNavigate = true) {
    const doc = docs.find((item) => item.id === id) || docs[0];
    if (!doc) return;
    const rendered = window.NexoMarkdown.render(doc.markdown);
    content.innerHTML = rendered.html;
    content.dataset.document = doc.id;
    nav.querySelectorAll("button").forEach((button) => button.classList.toggle("active", button.dataset.doc === doc.id));
    toc.innerHTML = "";
    rendered.headings.filter((item) => item.level <= 3).forEach((item) => {
      const link = document.createElement("a");
      link.href = `#doc-${item.id}`;
      link.className = `toc-level-${item.level}`;
      link.textContent = item.label;
      toc.append(link);
    });
    if (shouldNavigate) {
      history.replaceState(null, "", `#docs/${doc.id}${heading ? `/${heading}` : ""}`);
      const target = heading && content.querySelector(`#${CSS.escape(heading)}`);
      (target || document.querySelector("#documentation")).scrollIntoView({ behavior: "smooth", block: "start" });
    }
  }

  nav.addEventListener("click", (event) => {
    const button = event.target.closest("[data-doc]");
    if (button) openDocument(button.dataset.doc);
  });
  toc.addEventListener("click", (event) => {
    const link = event.target.closest("a");
    if (!link) return;
    event.preventDefault();
    const heading = link.hash.replace("#doc-", "");
    content.querySelector(`#${CSS.escape(heading)}`)?.scrollIntoView({ behavior: "smooth", block: "start" });
    history.replaceState(null, "", `#docs/${content.dataset.document}/${heading}`);
  });
  content.addEventListener("click", (event) => {
    const link = event.target.closest("a");
    const file = link?.getAttribute("href")?.match(/(?:\.\.\/)?([A-Za-z0-9_-]+)\.md(?:#(.*))?$/);
    if (!file) return;
    const target = docs.find((doc) => doc.file.toLowerCase() === `${file[1]}.md`.toLowerCase());
    if (target) { event.preventDefault(); openDocument(target.id, file[2]); }
  });
  document.addEventListener("click", (event) => {
    const link = event.target.closest("[data-doc-link]");
    if (!link) return;
    event.preventDefault();
    openDocument(link.dataset.docLink);
  });
  search.addEventListener("input", () => {
    const query = search.value.trim().toLowerCase();
    let visible = 0;
    nav.querySelectorAll("button").forEach((button) => {
      const doc = docs.find((item) => item.id === button.dataset.doc);
      const matches = !query || `${doc.title}\n${doc.markdown}`.toLowerCase().includes(query);
      button.hidden = !matches;
      if (matches) visible += 1;
    });
    count.textContent = query ? `${visible} matching documents` : `${docs.length} documents`;
  });

  const route = location.hash.match(/^#docs\/([^/]+)(?:\/(.+))?$/);
  openDocument(route?.[1] || "product_vision", route?.[2], Boolean(route));
  window.addEventListener("load", () => {
    if (!location.hash || location.hash.startsWith("#docs/")) return;
    document.getElementById(decodeURIComponent(location.hash.slice(1)))?.scrollIntoView({ behavior: "instant", block: "start" });
  }, { once: true });
}());
