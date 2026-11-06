// Build docs/index.html: every Korean doc in one readable page (served by GitHub Pages).
// Usage: npm install && node build.js   (writes docs/index.html from docs/ko/*.md)
const fs = require('fs');
const path = require('path');
const { Marked } = require('marked');

const DOCS = path.resolve(process.argv[2] || path.join(__dirname, '../../docs'));
const OUT = path.resolve(process.argv[3] || path.join(DOCS, 'index.html'));
const KO = path.join(DOCS, 'ko');
const REPO = 'https://github.com/tsunehimatoi/psd2live';

const GROUPS = [
  { title: '시작하기', docs: ['README', 'CHANGELOG', 'ROADMAP', 'STATUS'] },
  { title: '사용 가이드', docs: ['guide/USER_GUIDE', 'guide/MODIFIERS', 'guide/CANVAS_EDITOR', 'guide/LAYOUT', 'guide/DEPTH_SPLIT', 'guide/SKELETON', 'guide/SWING', 'guide/PHYSICS', 'guide/SIMULATION', 'guide/SIMULATION_TUTORIAL', 'guide/DEFORM_PATHS', 'guide/TEXTURE_UPSCALE'] },
  { title: '빌드와 배포', docs: ['guide/DEVELOPMENT', 'guide/CUBISM_SDK_SETUP', 'guide/CUBISM_CI_RELEASE', 'guide/PSD_IMPORT_PERFORMANCE'] },
  { title: '형식과 참조', docs: ['spec/PSD_LAYER_SPEC', 'spec/DEFORMER_AND_PARAMETER_SPEC', 'spec/IMPLEMENTATION_COMPARISON', 'spec/PROJECT_FORMAT', 'spec/LAYOUT_MANIFEST', 'spec/EXPORT_TARGETS', 'spec/RUNTIME', 'spec/P2LRT_V2', 'spec/INTERIOR_SURFACE', 'spec/TEXTURE_STRUCTURE_MESH'] },
  { title: '에이전트 (MCP)', docs: ['agent/MCP_AUTHORING', 'agent/AGENT_DESIGN'] },
  { title: '내부 구현', docs: ['spec/DOCUMENT_LAYER', 'spec/RIG_PRESETS', 'spec/RIG_MODIFIER_STACK', 'spec/RIG_TEMPLATES_FACE', 'spec/RIG_TEMPLATES_HAIR_BODY', 'spec/RIG_TEMPLATES_PHYSICS_MOTION', 'spec/RIG_TEMPLATES_STRUCTURE', 'spec/MATERIALIZED_RIG', 'spec/MATERIAL_LAYOUT_WORKSPACE', 'spec/LAYOUT_AI_ASSIST', 'spec/HEADLESS_HOST', 'spec/RUNTIME_EXPORT_ARCHITECTURE_AND_GAPS', 'spec/MESH_TOPOLOGY_AND_SPLIT', 'spec/PAINT_SYSTEM_ARCHITECTURE_AND_PRD', 'spec/CANVAS_RENDERER', 'spec/UI_INTERACTION_CONSISTENCY', 'spec/CONTEXT_MENU_REVIEW', 'spec/CONTEXT_MENU_AUDIT'] },
];

// Every ko markdown file must be placed in a group.
const all = [];
(function walk(d) {
  for (const e of fs.readdirSync(d, { withFileTypes: true })) {
    const p = path.join(d, e.name);
    if (e.isDirectory()) walk(p);
    else if (e.name.endsWith('.md')) all.push(path.relative(KO, p).replace(/\.md$/, '').split(path.sep).join('/'));
  }
})(KO);
const listed = GROUPS.flatMap(g => g.docs);
const missing = all.filter(d => !listed.includes(d));
const extra = listed.filter(d => !all.includes(d));
if (missing.length || extra.length) {
  console.error('unplaced:', missing, 'nonexistent:', extra);
  process.exit(1);
}

const esc = s => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
const stripTags = s => s.replace(/<[^>]+>/g, '').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/&amp;/g, '&');
const slugify = t => stripTags(t).trim().toLowerCase().replace(/[^\p{L}\p{M}\p{N}\p{Pc}\- ]/gu, '').replace(/ /g, '-');

function render(docId, md) {
  const seen = new Map();
  const headings = [];
  const marked = new Marked({ gfm: true });
  marked.use({
    renderer: {
      heading(text, level) {
        let s = slugify(text);
        const n = seen.get(s) || 0;
        seen.set(s, n + 1);
        if (n) s = `${s}-${n}`;
        headings.push({ level, id: s, text: stripTags(text) });
        return `<h${level} id="${esc(s)}">${text}<a class="hanchor" href="#${esc(s)}" aria-label="이 제목 링크">#</a></h${level}>\n`;
      },
      code(code, lang) {
        if (lang === 'mermaid') return `<pre class="mermaid">${esc(code)}</pre>\n`;
        return `<div class="code"><pre><code${lang ? ` data-lang="${esc(lang)}"` : ''}>${esc(code)}</code></pre><button class="copy" type="button">복사</button></div>\n`;
      },
    },
  });
  let html = marked.parse(md);

  const docDir = path.posix.dirname('ko/' + docId);
  const fix = (attr, raw) => {
    let v;
    try { v = decodeURIComponent(raw.replace(/&amp;/g, '&')); } catch { v = raw; }
    if (/^[a-z][a-z0-9+.-]*:/i.test(v)) return `${attr}="${raw}"`;
    if (v.startsWith('#')) return `${attr}="#${esc(docId)}::${esc(v.slice(1))}"`;
    const [p, anchor] = v.split('#');
    const target = path.posix.normalize(path.posix.join(docDir, p));
    if (target.startsWith('ko/') && target.endsWith('.md')) {
      const id = target.slice(3, -3);
      if (all.includes(id)) return `${attr}="#${esc(id)}${anchor ? '::' + esc(anchor) : ''}"`;
    }
    if (target === 'README.md') return `${attr}="#index"`;
    if (target === '../README.md') return `${attr}="https://github.com/tsunehimatoi/psd2live#readme"`;
    if (attr === 'href') {
      // Anything outside the Korean set (source code, other languages) opens on GitHub,
      // so the page also works when hosted on GitHub Pages.
      const repoPath = path.posix.normalize(path.posix.join('docs', target));
      const url = `${REPO}/${repoPath.endsWith('/') ? 'tree' : 'blob'}/master/${repoPath}`;
      return `href="${esc(url)}${anchor ? '#' + esc(anchor) : ''}" target="_blank" rel="noopener"`;
    }
    return `${attr}="${esc(target)}"`;
  };
  html = html
    .replace(/\b(href|src)="([^"]*)"/g, (_, a, v) => fix(a, v))
    .replace(/\bid="([^"]*)"/g, (_, v) => `id="${esc(docId)}::${v}"`)
    .replace(/<img /g, '<img loading="lazy" ')
    .replace(/<table>/g, '<div class="table-wrap"><table>').replace(/<\/table>/g, '</table></div>')
    .replace(/<blockquote>\s*<p>\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\]\s*/g, (_, k) => {
      const label = { NOTE: '참고', TIP: '팁', IMPORTANT: '중요', WARNING: '주의', CAUTION: '경고' }[k];
      return `<blockquote class="callout callout-${k.toLowerCase()}"><p class="callout-title">${label}</p><p>`;
    })
    .replace(/<p>\s*<\/p>/g, '');
  return { html, headings };
}

function summary(html) {
  const ps = html.match(/<p>[\s\S]*?<\/p>/g) || [];
  for (const p of ps) {
    const linkText = (p.match(/<a [^>]*>[\s\S]*?<\/a>/g) || []).map(stripTags).join('').length;
    const text = stripTags(p).replace(/\s+/g, ' ').trim();
    if (text.length < 40 || linkText > text.length * 0.5) continue;
    return text.length > 140 ? text.slice(0, 138).trimEnd() + '…' : text;
  }
  return '';
}

const docs = {};
for (const id of listed) {
  const md = fs.readFileSync(path.join(KO, id + '.md'), 'utf8');
  const { html, headings } = render(id, md);
  const h1 = headings.find(h => h.level === 1);
  docs[id] = { id, title: h1 ? h1.text.replace(/#$/, '') : id, html, headings, summary: summary(html), lines: md.split('\n').length };
}
docs.README.title = 'PSD2Live 소개';

const navHtml = GROUPS.map(g => `<section class="nav-group"><h2>${esc(g.title)}</h2><ul>${g.docs.map(id => `<li><a href="#${esc(id)}" data-doc="${esc(id)}">${esc(docs[id].title)}</a></li>`).join('')}</ul></section>`).join('');

const totalLines = listed.reduce((n, id) => n + docs[id].lines, 0);
const indexHtml = `<header class="hero"><img src="branding/psd2live-icon-mesh.png" alt="" width="72" height="72"><div><h1 id="index::top">PSD2Live 문서</h1><p>PSD2Live의 한국어 문서 ${listed.length}개를 한곳에 모았습니다. 왼쪽 목록에서 문서를 고르거나, 위쪽 검색창(<kbd>/</kbd> 또는 <kbd>Ctrl</kbd>+<kbd>K</kbd>)으로 모든 문서의 제목과 본문을 찾을 수 있습니다.</p><p class="meta">원본: <code>docs/ko/*.md</code> · 문서 ${listed.length}개 · 약 ${totalLines.toLocaleString('ko-KR')}줄</p></div></header>` +
  GROUPS.map(g => `<section class="group"><h2>${esc(g.title)}</h2><div class="cards">${g.docs.map(id => `<a class="card" href="#${esc(id)}"><strong>${esc(docs[id].title)}</strong><span>${esc(docs[id].summary)}</span><small>${esc(id)}.md</small></a>`).join('')}</div></section>`).join('');

const order = ['index', ...listed];
const articles = [`<article id="index" class="doc index-page">${indexHtml}</article>`]
  .concat(listed.map(id => `<article id="${esc(id)}" class="doc" hidden data-title="${esc(docs[id].title)}"><p class="crumb">${esc(GROUPS.find(g => g.docs.includes(id)).title)} · <code>docs/ko/${esc(id)}.md</code></p>${docs[id].html}</article>`))
  .join('\n');

const meta = { order, titles: Object.fromEntries([['index', 'PSD2Live 문서'], ...listed.map(id => [id, docs[id].title])]) };

const template = fs.readFileSync(path.join(__dirname, 'template.html'), 'utf8');
const out = template
  .replace('<!--NAV-->', () => navHtml)
  .replace('<!--ARTICLES-->', () => articles)
  .replace('/*META*/null', () => JSON.stringify(meta));
fs.writeFileSync(OUT, out);
console.log(`wrote ${OUT} (${(out.length / 1024 / 1024).toFixed(2)} MB, ${listed.length} docs)`);
