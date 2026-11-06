// Build docs/index.html: every Korean doc in one readable page (served by GitHub Pages).
// Usage: npm install && node build.js   (writes docs/index.html from docs/ko/*.md)
const fs = require('fs');
const path = require('path');
const { Marked } = require('marked');

const DOCS = path.resolve(process.argv[2] || path.join(__dirname, '../../docs'));
const OUT = path.resolve(process.argv[3] || path.join(DOCS, 'index.html'));
const KO = path.join(DOCS, 'ko');
const REPO = 'https://github.com/tsunehimatoi/psd2live';

// icon: shown in the document list ('ref' for formats/internals, 'agent' for MCP).
const GROUPS = [
  { title: '시작하기', docs: ['README', 'CHANGELOG', 'ROADMAP', 'STATUS'] },
  { title: '사용 가이드', docs: ['guide/USER_GUIDE', 'guide/MODIFIERS', 'guide/CANVAS_EDITOR', 'guide/LAYOUT', 'guide/DEPTH_SPLIT', 'guide/SKELETON', 'guide/SWING', 'guide/PHYSICS', 'guide/SIMULATION', 'guide/SIMULATION_TUTORIAL', 'guide/DEFORM_PATHS', 'guide/TEXTURE_UPSCALE'] },
  { title: '빌드와 배포', docs: ['guide/DEVELOPMENT', 'guide/CUBISM_SDK_SETUP', 'guide/CUBISM_CI_RELEASE', 'guide/PSD_IMPORT_PERFORMANCE'] },
  { title: '형식과 참조', icon: 'ref', docs: ['spec/PSD_LAYER_SPEC', 'spec/DEFORMER_AND_PARAMETER_SPEC', 'spec/IMPLEMENTATION_COMPARISON', 'spec/PROJECT_FORMAT', 'spec/LAYOUT_MANIFEST', 'spec/EXPORT_TARGETS', 'spec/RUNTIME', 'spec/P2LRT_V2', 'spec/INTERIOR_SURFACE', 'spec/TEXTURE_STRUCTURE_MESH'] },
  { title: '에이전트 (MCP)', icon: 'agent', docs: ['agent/MCP_AUTHORING', 'agent/AGENT_DESIGN'] },
  { title: '내부 구현', icon: 'ref', docs: ['spec/DOCUMENT_LAYER', 'spec/RIG_PRESETS', 'spec/RIG_MODIFIER_STACK', 'spec/RIG_TEMPLATES_FACE', 'spec/RIG_TEMPLATES_HAIR_BODY', 'spec/RIG_TEMPLATES_PHYSICS_MOTION', 'spec/RIG_TEMPLATES_STRUCTURE', 'spec/MATERIALIZED_RIG', 'spec/MATERIAL_LAYOUT_WORKSPACE', 'spec/LAYOUT_AI_ASSIST', 'spec/HEADLESS_HOST', 'spec/RUNTIME_EXPORT_ARCHITECTURE_AND_GAPS', 'spec/MESH_TOPOLOGY_AND_SPLIT', 'spec/PAINT_SYSTEM_ARCHITECTURE_AND_PRD', 'spec/CANVAS_RENDERER', 'spec/UI_INTERACTION_CONSISTENCY', 'spec/CONTEXT_MENU_REVIEW', 'spec/CONTEXT_MENU_AUDIT'] },
];

// Short sidebar labels for docs whose H1 is long; others use the H1.
const LABELS = {
  README: 'PSD2Live 소개', 'guide/USER_GUIDE': '빠른 시작', 'guide/CANVAS_EDITOR': '캔버스 편집', 'guide/DEPTH_SPLIT': '앞뒤 레이어 분리',
  'guide/SIMULATION_TUTORIAL': '시뮬레이션 튜토리얼', 'guide/CUBISM_SDK_SETUP': 'Cubism SDK 설정', 'guide/CUBISM_CI_RELEASE': 'CI와 릴리스',
  'guide/PSD_IMPORT_PERFORMANCE': 'PSD 가져오기 성능', 'spec/DEFORMER_AND_PARAMETER_SPEC': '디포머와 파라미터', 'spec/EXPORT_TARGETS': '내보내기 대상',
  'spec/INTERIOR_SURFACE': '내부 조형', 'spec/LAYOUT_AI_ASSIST': 'Layout AI 보조', 'spec/MATERIALIZED_RIG': '실체화된 리그',
};

const LANGS = { ko: '한국어', en: 'English', ja: '日本語', zh: '简体中文' };

// Every ko markdown file must be placed in a group.
function listMd(dir) {
  const out = [];
  if (!fs.existsSync(dir)) return out;
  (function walk(d) {
    for (const e of fs.readdirSync(d, { withFileTypes: true })) {
      const p = path.join(d, e.name);
      if (e.isDirectory()) walk(p);
      else if (e.name.endsWith('.md')) out.push(path.relative(dir, p).replace(/\.md$/, '').split(path.sep).join('/'));
    }
  })(dir);
  return out;
}
const all = listMd(KO);
const listed = GROUPS.flatMap(g => g.docs);
const missing = all.filter(d => !listed.includes(d));
const extra = listed.filter(d => !all.includes(d));
if (missing.length || extra.length) {
  console.error('unplaced:', missing, 'nonexistent:', extra);
  process.exit(1);
}
const otherLangs = Object.fromEntries(Object.keys(LANGS).filter(l => l !== 'ko').map(l => [l, new Set(listMd(path.join(DOCS, l)))]));

const esc = s => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
const stripTags = s => s.replace(/<[^>]+>/g, '').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/&amp;/g, '&');
const slugify = t => stripTags(t).trim().toLowerCase().replace(/[^\p{L}\p{M}\p{N}\p{Pc}\- ]/gu, '').replace(/ /g, '-');

function render(docId, md) {
  const seen = new Map();
  const headings = [];
  let h1 = null;
  const marked = new Marked({ gfm: true });
  marked.use({
    renderer: {
      heading(text, level) {
        let s = slugify(text);
        const n = seen.get(s) || 0;
        seen.set(s, n + 1);
        if (n) s = `${s}-${n}`;
        // The first H1 is rendered once in the article header instead.
        if (level === 1 && !h1) { h1 = { id: s, text: stripTags(text) }; return ''; }
        headings.push({ level, id: s, text: stripTags(text) });
        return `<h${level} id="${esc(s)}">${text}<a class="heading-anchor" href="#${esc(s)}" aria-label="이 제목 링크">#</a></h${level}>\n`;
      },
      code(code, lang) {
        if (lang === 'mermaid') return `<pre class="mermaid">${esc(code)}</pre>\n`;
        return `<div class="code-wrap"><pre><code${lang ? ` data-lang="${esc(lang)}"` : ''}>${esc(code)}</code></pre><button class="copy-code" type="button" aria-label="코드 복사">복사</button></div>\n`;
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
    if (target === '../README.md') return `${attr}="${REPO}#readme"`;
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
    .replace(/<img /g, '<img loading="lazy" decoding="async" ')
    .replace(/<table>/g, '<div class="table-scroll" tabindex="0"><table>').replace(/<\/table>/g, '</table></div>')
    .replace(/<blockquote>\s*<p>\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\]\s*/g, (_, k) => {
      const label = { NOTE: '참고', TIP: '팁', IMPORTANT: '중요', WARNING: '주의', CAUTION: '경고' }[k];
      return `<blockquote class="callout callout-${k.toLowerCase()}"><p><strong class="callout-label">${label}</strong>`;
    })
    .replace(/<p>\s*<\/p>/g, '');
  return { html, headings, h1 };
}

const docs = {};
for (const id of listed) {
  const md = fs.readFileSync(path.join(KO, id + '.md'), 'utf8');
  const { html, headings, h1 } = render(id, md);
  const group = GROUPS.find(g => g.docs.includes(id));
  const title = h1 ? h1.text : id;
  docs[id] = {
    id, html, headings, group,
    h1Id: `${id}::${h1 ? h1.id : 'top'}`,
    title: id === 'README' ? 'PSD2Live 소개' : title,
    label: LABELS[id] || title,
    minutes: Math.max(1, Math.ceil(stripTags(html).replace(/\s+/g, ' ').length / 950)),
  };
}

const icon = {
  logo: '<svg viewBox="0 0 32 32" fill="none" aria-hidden="true"><path d="m16 3 13 8v14l-13 6L3 23V9L16 3Z" stroke="currentColor" stroke-width="1.5"/><path d="m3 9 13 8 13-6M16 3v14m0 0v14M3 23l13-6 13 8" stroke="currentColor" stroke-width="1.5"/></svg>',
  search: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></svg>',
};
const href = id => '#' + esc(id);
const gi = g => 'g' + GROUPS.indexOf(g);

/* sidebar */
const navHtml = `<a class="side-home" href="#index" data-doc="index"><span>⌂</span> 문서 홈</a>`
  + `<a class="side-index" href="#index::library"><span>▤</span> 전체 문서 <small>${listed.length}</small></a>`
  + GROUPS.map(g => `<section class="nav-section"><h2>${esc(g.title)}<span>${g.docs.length}</span></h2>${g.docs.map(id => `<a href="${href(id)}" data-doc="${esc(id)}">${esc(docs[id].label)}</a>`).join('')}</section>`).join('')
  + `<div class="side-note"><span class="live-dot"></span> 한국어 문서 ${listed.length}개를 한곳에<br><small>docs/ko 원본 Markdown 전체 수록</small></div>`;

/* home */
const pathCards = [
  ['01', '시작하기', 'PSD 준비부터 첫 모델 내보내기까지.', 'guide/USER_GUIDE'],
  ['02', '편집과 애니메이션', '캔버스, 뼈대, 물리와 시뮬레이션.', 'guide/CANVAS_EDITOR'],
  ['03', '개발과 확장', '파일 형식, 런타임과 MCP 연동.', 'guide/DEVELOPMENT'],
];
const heroArt = `<div class="hero-art" aria-hidden="true"><div class="art-grid"></div><svg viewBox="0 0 320 280" fill="none"><path class="mesh-shadow" d="m155 35 94 43 27 103-107 69-99-48-29-103 114-64Z"/><g stroke="currentColor"><path d="m155 35 94 43 27 103-107 69-99-48-29-103 114-64Z"/><path d="m155 35 14 110 80-67M41 99l128 46-99 57m206-21-107-36v105M155 35 70 202m179-124L169 250M41 99l235 82"/><path d="m102 64 12 62 7 99m79-170-31 90 62 65M55 151l114-6 94-15" opacity=".4"/></g><g fill="currentColor">${[[155, 35], [249, 78], [276, 181], [169, 250], [70, 202], [41, 99], [169, 145]].map(([x, y]) => `<circle cx="${x}" cy="${y}" r="4"/>`).join('')}</g><circle cx="169" cy="145" r="12" stroke="currentColor" opacity=".3"/></svg><span class="art-label">FROM LAYERS TO LIFE</span></div>`;
const langTabs = Object.entries(LANGS).map(([l, name]) => l === 'ko'
  ? `<a class="language-tab active" href="#index::library" aria-current="true">${name}<span>${listed.length}</span></a>`
  : `<a class="language-tab" href="${REPO}/tree/master/docs/${l}" target="_blank" rel="noopener">${name}<span>${otherLangs[l].size}</span></a>`).join('');
const docIcon = g => g.icon === 'ref' ? '⌘' : g.icon === 'agent' ? '⌁' : '▤';

const indexHtml = `<div class="breadcrumb"><span>문서</span><span>/</span>개요</div>`
  + `<section class="hero"><div class="eyebrow"><span class="live-dot"></span> PSD2LIVE DOCUMENTATION</div><h1 id="index::top">상상을 움직이는<br>가장 확실한 시작.</h1><p>첫 PSD 가져오기부터 리깅, 애니메이션, 런타임까지.<br>PSD2Live의 모든 문서를 한곳에서 찾아보세요.</p><div class="hero-actions"><a class="primary-button" href="#guide/USER_GUIDE">빠른 시작 가이드 <span>→</span></a><a class="text-button" href="#index::library">전체 문서 둘러보기 ↓</a></div>${heroArt}</section>`
  + `<div class="intro-meta"><span><strong>${listed.length}</strong> 문서</span><span><strong>${GROUPS.length}</strong> 분류</span><span>사용법부터 내부 구조까지</span><a href="#CHANGELOG">변경 이력 보기 ↗</a></div>`
  + `<section class="pathways" aria-label="추천 시작 경로">${pathCards.map(([n, t, d, id]) => `<a class="path-card" href="${href(id)}"><div class="path-top"><span class="path-number">${n}</span><span>↗</span></div><h2>${t}</h2><p>${d}</p><span class="path-link">가이드 읽기 →</span></a>`).join('')}</section>`
  + `<section class="library" id="index::library"><div class="section-heading"><div class="eyebrow">EXPLORE THE DOCS</div><h2>필요한 문서를 찾아보세요.</h2><p>주제별로 정리된 전체 한국어 문서입니다. 다른 언어는 GitHub에서 볼 수 있습니다.</p></div>`
  + `<nav class="language-tabs" aria-label="문서 언어">${langTabs}</nav>`
  + `<div class="library-toolbar"><div class="category-filters" role="group" aria-label="주제 필터"><button type="button" data-filter="all" aria-pressed="true">전체</button>${GROUPS.map(g => `<button type="button" data-filter="${gi(g)}" aria-pressed="false">${esc(g.title)}</button>`).join('')}</div><label class="list-search">${icon.search}<input id="filter-input" type="search" aria-label="문서 목록 필터" placeholder="목록에서 찾기"></label></div>`
  + `<div class="document-list">${listed.map(id => { const d = docs[id]; return `<a class="document-row" href="${href(id)}" data-category="${gi(d.group)}" data-search="${esc(d.title + ' ' + d.label + ' ' + id)}"><span class="doc-icon">${docIcon(d.group)}</span><span class="doc-info"><strong>${esc(d.label)}</strong><small>ko/${esc(id)}.md</small></span><span class="category-tag">${esc(d.group.title)}</span><span class="row-arrow">↗</span></a>`; }).join('')}</div>`
  + `<p class="empty-state" hidden>일치하는 문서가 없습니다. 다른 검색어로 찾아보세요.</p><div class="library-count" role="status"><span id="visible-count">${listed.length}</span>개 문서 표시</div></section>`
  + `<section class="help-strip"><span class="help-symbol">⌘</span><div><h2>문서의 내용까지 검색하세요.</h2><p>⌘ K 또는 Ctrl K로 모든 가이드와 기술 문서를 바로 찾을 수 있습니다.</p></div><button class="search-trigger" type="button">검색 열기 →</button></section>`;

/* documents */
function tocLinks(d) {
  return d.headings.filter(h => h.level === 2 || h.level === 3)
    .map(h => `<a href="#${esc(d.id)}::${esc(h.id)}" class="depth-${h.level}">${esc(h.text)}</a>`).join('');
}
function article(id, i) {
  const d = docs[id];
  const prev = listed[i - 1], next = listed[i + 1];
  const translations = Object.keys(otherLangs).filter(l => otherLangs[l].has(id));
  const toc = d.headings.filter(h => h.level === 2 || h.level === 3).length >= 2
    ? `<details class="mobile-toc"><summary>이 페이지의 목차</summary><nav>${tocLinks(d)}</nav></details>` : '';
  return `<article id="${esc(id)}" class="doc" hidden data-title="${esc(d.title)}">`
    + `<div class="breadcrumb"><a href="#index">문서</a><span>/</span><a href="#index::library">${esc(d.group.title)}</a><span>/</span>${esc(d.label)}</div>`
    + `<header class="article-header"><div class="eyebrow">${esc(d.group.title)} <span class="language-badge">한국어</span></div><h1 id="${esc(d.h1Id)}">${esc(d.title)}</h1>`
    + `<div class="article-meta"><span>약 ${d.minutes}분 읽기</span><span>·</span><a href="${REPO}/blob/master/docs/ko/${esc(id)}.md" target="_blank" rel="noopener">원문 보기 ↗</a><button class="copy-link" type="button">링크 복사 ⧉</button></div>`
    + (translations.length ? `<div class="translations"><span>다른 언어</span>${translations.map(l => `<a href="${REPO}/blob/master/docs/${l}/${esc(id)}.md" lang="${l}" target="_blank" rel="noopener">${LANGS[l]}</a>`).join('')}</div>` : '')
    + `</header>${toc}<div class="prose">${d.html}</div>`
    + `<nav class="prev-next" aria-label="이전 및 다음 문서">${prev ? `<a href="${href(prev)}"><small>← 이전 문서</small><strong>${esc(docs[prev].label)}</strong></a>` : '<span></span>'}${next ? `<a href="${href(next)}"><small>다음 문서 →</small><strong>${esc(docs[next].label)}</strong></a>` : '<span></span>'}</nav>`
    + `</article>`;
}

const articles = [`<article id="index" class="doc index-page">${indexHtml}</article>`]
  .concat(listed.map(article))
  .join('\n');

const meta = {
  titles: Object.fromEntries([['index', 'PSD2Live 문서'], ...listed.map(id => [id, docs[id].title])]),
  paths: Object.fromEntries(listed.map(id => [id, `ko/${id}.md`])),
};

const template = fs.readFileSync(path.join(__dirname, 'template.html'), 'utf8');
const out = template
  .replace('<!--LOGO-->', () => icon.logo)
  .replace(/<!--SEARCH_ICON-->/g, () => icon.search)
  .replace(/<!--REPO-->/g, () => REPO)
  .replace('<!--NAV-->', () => navHtml)
  .replace('<!--ARTICLES-->', () => articles)
  .replace('/*META*/null', () => JSON.stringify(meta));
fs.writeFileSync(OUT, out);
console.log(`wrote ${OUT} (${(out.length / 1024 / 1024).toFixed(2)} MB, ${listed.length} docs)`);
