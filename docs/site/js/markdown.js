(function () {
  "use strict";

  const escapeHtml = (value) => value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");

  const slugCounts = new Map();
  const slugify = (value) => {
    const base = value.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "")
      .replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "") || "section";
    const count = slugCounts.get(base) || 0;
    slugCounts.set(base, count + 1);
    return count ? `${base}-${count + 1}` : base;
  };

  const inline = (source) => {
    let text = escapeHtml(source);
    const code = [];
    text = text.replace(/`([^`]+)`/g, (_, value) => {
      code.push(`<code>${value}</code>`);
      return `\u0000CODE${code.length - 1}\u0000`;
    });
    text = text.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (_, label, href) => {
      const safeHref = /^(https?:|\.\.\/|\.\/|#|[A-Za-z0-9_.\-/]+$)/.test(href) ? href : "#";
      const external = /^https?:/.test(safeHref) ? ' target="_blank" rel="noreferrer"' : "";
      return `<a href="${escapeHtml(safeHref)}"${external}>${label}</a>`;
    });
    text = text.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>");
    text = text.replace(/\*([^*]+)\*/g, "<em>$1</em>");
    return text.replace(/\u0000CODE(\d+)\u0000/g, (_, index) => code[Number(index)]);
  };

  function render(markdown) {
    slugCounts.clear();
    const lines = markdown.replace(/\r\n/g, "\n").split("\n");
    const html = [];
    const headings = [];
    let paragraph = [];
    let list = null;
    let quote = [];
    let inCode = false;
    let codeLanguage = "";
    let codeLines = [];

    const flushParagraph = () => {
      if (paragraph.length) html.push(`<p>${inline(paragraph.join(" "))}</p>`);
      paragraph = [];
    };
    const flushList = () => {
      if (list) html.push(`<${list.type}>${list.items.map((item) => `<li>${inline(item)}</li>`).join("")}</${list.type}>`);
      list = null;
    };
    const flushQuote = () => {
      if (quote.length) html.push(`<blockquote>${inline(quote.join(" "))}</blockquote>`);
      quote = [];
    };
    const flushAll = () => { flushParagraph(); flushList(); flushQuote(); };

    for (let index = 0; index < lines.length; index += 1) {
      const line = lines[index];
      if (line.startsWith("```")) {
        if (inCode) {
          html.push(`<pre><code class="language-${escapeHtml(codeLanguage)}">${escapeHtml(codeLines.join("\n"))}</code></pre>`);
          inCode = false; codeLanguage = ""; codeLines = [];
        } else {
          flushAll(); inCode = true; codeLanguage = line.slice(3).trim();
        }
        continue;
      }
      if (inCode) { codeLines.push(line); continue; }
      if (!line.trim()) { flushAll(); continue; }

      const heading = line.match(/^(#{1,6})\s+(.+)$/);
      if (heading) {
        flushAll();
        const level = heading[1].length;
        const label = heading[2].trim();
        const id = slugify(label);
        headings.push({ level, label, id });
        html.push(`<h${level} id="${id}">${inline(label)}<a class="heading-link" href="#doc-${id}" aria-label="Link to ${escapeHtml(label)}">#</a></h${level}>`);
        continue;
      }

      const nextLine = lines[index + 1] || "";
      if (line.includes("|") && /^\s*\|?\s*:?-+/.test(nextLine)) {
        flushAll();
        const rows = [];
        const cells = (row) => row.trim().replace(/^\||\|$/g, "").split("|").map((cell) => cell.trim());
        const headers = cells(line);
        index += 2;
        while (index < lines.length && lines[index].includes("|") && lines[index].trim()) {
          rows.push(cells(lines[index])); index += 1;
        }
        index -= 1;
        html.push(`<div class="md-table-wrap"><table><thead><tr>${headers.map((cell) => `<th>${inline(cell)}</th>`).join("")}</tr></thead><tbody>${rows.map((row) => `<tr>${row.map((cell) => `<td>${inline(cell)}</td>`).join("")}</tr>`).join("")}</tbody></table></div>`);
        continue;
      }

      const bullet = line.match(/^\s*[-*]\s+(.+)$/);
      const ordered = line.match(/^\s*\d+\.\s+(.+)$/);
      if (bullet || ordered) {
        flushParagraph(); flushQuote();
        const type = ordered ? "ol" : "ul";
        if (list && list.type !== type) flushList();
        list ||= { type, items: [] };
        list.items.push((bullet || ordered)[1]);
        continue;
      }

      if (line.startsWith(">")) {
        flushParagraph(); flushList(); quote.push(line.replace(/^>\s?/, "")); continue;
      }
      if (/^(-{3,}|\*{3,})$/.test(line.trim())) { flushAll(); html.push("<hr>"); continue; }
      paragraph.push(line.trim());
    }
    flushAll();
    if (inCode) html.push(`<pre><code>${escapeHtml(codeLines.join("\n"))}</code></pre>`);
    return { html: html.join("\n"), headings };
  }

  window.NexoMarkdown = { render };
}());
