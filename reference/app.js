/* Sticky Language – lo-fi prototype interactions.
   Nothing here is real: OCR, translation, LLM breakdown and the Anki app hand-off are all faked
   with hard-coded sample data so the interface can be clicked through. */

// ---------- Mock data ----------
const DICT = {
  "저는":   { word: "저", read: "jeo · + 는 (neun)", tags: ["pronoun", "humble", "+ topic particle"],
              defs: ["I, me (humble / polite form of 나)"], form: "저 + 는 → topic marker after a vowel",
              ex: "저는 학생이에요. — I am a student." },
  "어제":   { word: "어제", read: "eo-je", tags: ["noun", "adverb", "time"],
              defs: ["yesterday"], form: "Dictionary form — no change",
              ex: "어제 비가 왔어요. — It rained yesterday." },
  "친구와": { word: "친구", read: "chin-gu · 親舊 · + 와 (wa)", tags: ["noun", "+ particle 와"],
              defs: ["friend"], form: "친구 + 와 → 'with / and' after a vowel (과 after a consonant)",
              ex: "친구와 영화를 봤어요. — I watched a movie with a friend." },
  "함께":   { word: "함께", read: "ham-kke", tags: ["adverb", "JLPT-like: TOPIK I"],
              defs: ["together", "(with 와/과) along with"], form: "Often used as N와/과 함께",
              ex: "같이 = 함께 (같이 is more casual)" },
  "도서관에": { word: "도서관", read: "do-seo-gwan · 圖書館 · + 에 (e)", tags: ["noun", "place", "+ particle 에"],
              defs: ["library"], form: "도서관 + 에 → destination 'to'",
              ex: "도서관에서 공부해요. — I study at the library." },
  "갔어요": { word: "가다", read: "ga-da → gass-eo-yo", tags: ["verb", "past", "polite"],
              defs: ["to go"], form: "가- + 았 (past) + 어요 (polite) → 갔어요 (contracted)",
              ex: "학교에 가요. — I go to school." },
  "책을":   { word: "책", read: "chaek · 冊 · + 을 (eul)", tags: ["noun", "+ object particle"],
              defs: ["book"], form: "책 + 을 → object marker after a consonant",
              ex: "책을 읽어요. — I read a book." },
  "두":     { word: "두 (둘)", read: "du", tags: ["numeral", "native Korean"],
              defs: ["two (before a counter)"], form: "둘 → 두 when followed by a counter",
              ex: "두 사람 — two people" },
  "권":     { word: "권", read: "gwon · 卷", tags: ["counter"],
              defs: ["counter for books / volumes"], form: "number + 권",
              ex: "책 세 권 — three books" },
  "빌렸어요": { word: "빌리다", read: "bil-li-da → bil-lyeoss-eo-yo", tags: ["verb", "past", "polite"],
              defs: ["to borrow", "to rent"], form: "빌리- + 었 + 어요 → 빌렸어요",
              ex: "돈을 빌려 주세요. — Please lend me money." },
};

const SENTENCES = [
  {
    text: "저는 어제 친구와 함께 도서관에 갔어요.",
    trans: "I went to the library with my friend yesterday.",
    chunks: [
      { t: "저는", role: "Topic", morph: "저 (I) + 는 (topic)", style: "p1" },
      { t: "어제", role: "Time", morph: "어제 (yesterday)", style: "p2" },
      { t: "친구와 함께", role: "Companion", morph: "친구 (friend) + 와 함께 (together with)", style: "p3" },
      { t: "도서관에", role: "Destination", morph: "도서관 (library) + 에 (to)", style: "p4" },
      { t: "갔어요", role: "Predicate", morph: "가다 + 았 + 어요", style: "p5" },
    ],
    expl: [
      "Humble 1st-person pronoun + topic particle 은/는 (는 after a vowel).",
      "Time noun used adverbially; no particle needed.",
      "N와/과 함께 = 'together with N'. 와 after a vowel, 과 after a consonant.",
      "Place + 에 marks the destination of a movement verb (가다/오다).",
      "가다 + past 았 + polite ending 어요; 가+았 contracts to 갔.",
    ],
    grammar: [
      { name: "N은/는 — topic particle", body: "Marks the topic. Use 는 after a vowel, 은 after a consonant." },
      { name: "N와/과 함께 — together with", body: "Formal-ish 'together with'. Spoken alternative: N하고 같이." },
      { name: "V-았/었어요 — past polite", body: "Past tense in the polite informal style. 았 after ㅏ/ㅗ, 었 otherwise." },
    ],
  },
  {
    text: "책을 두 권 빌렸어요.",
    trans: "I borrowed two books.",
    chunks: [
      { t: "책을", role: "Object", morph: "책 (book) + 을 (object)", style: "p1" },
      { t: "두 권", role: "Quantity", morph: "두 (two) + 권 (counter: books)", style: "p3" },
      { t: "빌렸어요", role: "Predicate", morph: "빌리다 + 었 + 어요", style: "p5" },
    ],
    expl: [
      "Object particle 을/를 (을 after a consonant).",
      "Noun + native number + counter. 둘 shortens to 두 before counters.",
      "빌리다 + 었 + 어요 → 빌렸어요 (ㅣ+ㅓ contracts to ㅕ).",
    ],
    grammar: [
      { name: "N을/를 — object particle", body: "Marks the direct object. 을 after a consonant, 를 after a vowel." },
      { name: "N + number + counter", body: "Count things with native numbers + counter: 책 두 권, 사람 세 명." },
    ],
  },
  // The rest of the scanned page: text + translation only (breakdown is "generated" on open)
  { text: "도서관은 생각보다 조용했어요.", trans: "The library was quieter than I expected." },
  { text: "우리는 창가 자리에 앉아서 한 시간 동안 책을 읽었어요.", trans: "We sat by the window and read for an hour." },
  { text: "친구는 소설을 읽으면서 계속 웃었어요.", trans: "My friend kept laughing while reading a novel." },
  { text: "집에 돌아갈 때 비가 오기 시작했어요.", trans: "It started to rain when we were going home." },
];

const NOTES = {
  home: "<b>Home.</b> One big entry point: <i>Scan</i>. Secondary: type/paste text or import an image. A status card shows the Anki app is connected (no flashcards live inside this app). Recent scans re-open in the reader.",
  scan: "<b>Scan (Papago-style).</b> On-device OCR draws boxes around detected text. Modes: single photo, live AR overlay, or import a screenshot. Press the shutter to simulate detection.",
  reader: "<b>Reader (Yomitan-style).</b> OCR text becomes tappable. Tapping a word opens a bottom sheet with reading, definitions, dictionary form + conjugation and an example. <b>Export to Anki</b> opens the Anki app already installed on the phone with the word, reading, meaning and source sentence filled in. The user confirms in Anki and comes straight back.",
  breakdown: "<b>Grammar breakdown (Mirinae-style).</b> Built for whole scanned pages: every sentence is listed in full with its translation, so it's easy to find the one you're stuck on. Tap a sentence to expand its breakdown (the LLM generates it on demand) and tap again to collapse. Inside, tap a chunk / tree node for its explanation; words can be looked up and exported to Anki.",
  translate: "<b>Text translate.</b> For typed or pasted text. Same output can be opened in the reader or broken down. History with favourites.",
  settings: "<b>Settings.</b> Target + explanation language, display options, OCR privacy, and which installed Anki app / deck / note type receives exported words.",
};

// ---------- Helpers ----------
const $ = (s, el = document) => el.querySelector(s);
const $$ = (s, el = document) => [...el.querySelectorAll(s)];

let toastTimer;
function toast(msg) {
  const t = $("#toast");
  t.textContent = msg;
  t.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.classList.remove("show"), 1600);
}

// ---------- Navigation ----------
const NO_TABBAR = ["scan"];
function go(id) {
  $$(".screen").forEach(s => s.classList.toggle("active", s.dataset.screen === id));
  $$("#tabbar button").forEach(b => b.classList.toggle("on", b.dataset.go === id));
  $$("#screenList li").forEach(li => li.classList.toggle("on", li.dataset.go === id));
  $("#tabbar").classList.toggle("hidden-bar", NO_TABBAR.includes(id));
  $("#screenNote").innerHTML = NOTES[id] || "";
  closePopup();
  closeHandoff();
  if (id === "scan") resetScan();
}
document.addEventListener("click", e => {
  const t = e.target.closest("[data-go]");
  if (t) go(t.dataset.go);
});

// ---------- Scan ----------
let scanMode = "photo";
function resetScan() {
  const vf = $("#viewfinder");
  vf.classList.remove("detected");
  vf.classList.toggle("live", scanMode === "live");
  $("#vfHint").textContent = scanMode === "live" ? "Live: translations overlaid in place" : "Point at text · hold steady";
}
$("#scanModes").addEventListener("click", e => {
  const s = e.target.closest("span"); if (!s) return;
  $$("#scanModes span").forEach(x => x.classList.toggle("on", x === s));
  scanMode = s.dataset.mode;
  if (scanMode === "screen") toast("Opens screenshot picker");
  resetScan();
});
$("#shutter").addEventListener("click", () => {
  $("#viewfinder").classList.add("detected");
  $("#vfHint").textContent = "3 lines detected · processing…";
  setTimeout(() => go("reader"), 1100);
});
$("#flashBtn").addEventListener("click", () => toast("Flash toggled"));

// ---------- Reader: tappable words ----------
function renderSource() {
  $("#sourceText").innerHTML = SENTENCES.map(s =>
    s.text.split(" ").map(w => {
      const key = w.replace(/[.,!?]/g, "");
      const punct = w.slice(key.length);
      return `<span class="tok" data-k="${key}">${key}</span>${punct}`;
    }).join(" ")
  ).join(" ");
  $("#translationText").textContent = SENTENCES.map(s => s.trans).join(" ");
}
renderSource();

// Translate screen: the source sentence's words are tappable too
$("#transTokens").innerHTML = SENTENCES[0].text.replace(".", "").split(" ")
  .map(w => `<span class="tok" data-k="${w}">${w}</span>`).join(" ");
$("#transTokens").addEventListener("click", e => {
  const tok = e.target.closest(".tok"); if (!tok) return;
  $$(".tok").forEach(t => t.classList.toggle("sel", t === tok));
  openPopup(tok.dataset.k);
});

let currentWord = null;
$("#sourceText").addEventListener("click", e => {
  const tok = e.target.closest(".tok"); if (!tok) return;
  $$(".tok").forEach(t => t.classList.toggle("sel", t === tok));
  openPopup(tok.dataset.k);
});
function openPopup(key) {
  // Words without mock data get a generic placeholder entry
  const d = DICT[key] || { word: key, read: "[ reading ]", tags: ["[ part of speech ]"],
    defs: ["[ dictionary definition ]"], form: "[ dictionary form + conjugation ]", ex: "[ example sentence ]" };
  currentWord = d; currentKey = key;
  $("#popWord").textContent = d.word;
  $("#popRead").textContent = d.read;
  $("#popTags").innerHTML = d.tags.map(t => `<span>${t}</span>`).join("");
  $("#popDefs").innerHTML = d.defs.map(t => `<li>${t}</li>`).join("");
  $("#popForm").textContent = d.form;
  $("#popEx").textContent = d.ex;
  $("#popAnki").innerHTML = '<span class="anki-logo inv">A</span> Export to Anki';
  $("#wordPopup").classList.add("open");
  $("#popupBackdrop").classList.add("open");
}
function closePopup() {
  $("#wordPopup").classList.remove("open");
  $("#popupBackdrop").classList.remove("open");
  $$(".tok").forEach(t => t.classList.remove("sel"));
}
$("#popupBackdrop").addEventListener("click", closePopup);
// ---------- Export to Anki (hand-off to installed app) ----------
let currentKey = null;
function sentenceFor(key) {
  return SENTENCES.find(s => s.text.includes(key))?.text || "";
}
$("#popAnki").addEventListener("click", () => {
  const d = currentWord;
  $("#hoFront").textContent = d.word;
  $("#hoRead").textContent = d.read.split(" ·")[0];
  $("#hoBack").textContent = d.defs.join("; ");
  $("#hoSent").textContent = sentenceFor(currentKey).replace(currentKey, `<b>${currentKey}</b>`);
  const h = $("#handoff");
  h.className = "handoff open opening";
  setTimeout(() => h.classList.replace("opening", "in-anki"), 900);
});
function closeHandoff() { $("#handoff").className = "handoff"; }
function finishHandoff(added) {
  closeHandoff();
  if (added) {
    $("#popAnki").innerHTML = '<span class="anki-logo inv">A</span> ✓ Sent to Anki';
    toast(`Back in Sticky Language · "${currentWord.word}" added in AnkiDroid`);
  } else toast("Cancelled in AnkiDroid");
}
$("#hoAdd").addEventListener("click", () => finishHandoff(true));
$("#hoAddBtn").addEventListener("click", () => finishHandoff(true));
$("#hoCancel").addEventListener("click", () => finishHandoff(false));
$("#importImg").addEventListener("click", () => toast("Opens photo gallery picker"));

// ---------- Breakdown: list of full sentences, each expandable ----------
const openSents = new Set([0]);      // which sentences are expanded
const selChunk = {};                 // sentence index -> selected chunk index
const generated = new Set();         // sentences whose (fake) LLM breakdown has "loaded"

function breakdownBody(s, si) {
  if (!s.chunks) {
    // Sentences without mock data: show a loading state, then a lo-fi placeholder
    if (!generated.has(si)) return `<div class="gen"><span class="spinner sm"></span> Generating breakdown…</div>`;
    return `<div class="chunk-sentence">${s.text.replace(".", "").split(" ")
              .map(w => `<span class="ch p2">${w}<small>role</small></span>`).join("")}</div>
            <div class="skel"></div><div class="skel short"></div><div class="skel"></div>
            <small>[ chunk tree + grammar points, same layout as sentence 1 ]</small>`;
  }
  const sel = selChunk[si] ?? -1;
  return `
    <div class="chunk-sentence">${s.chunks.map((c, i) =>
      `<span class="ch ${c.style} ${i === sel ? "sel" : ""}" data-i="${i}">${c.t}<small>${c.role}</small></span>`).join("")}</div>
    <div class="tree">${s.chunks.map((c, i) => `
      <div class="node ${i === sel ? "open sel" : ""}" data-i="${i}">
        <div class="nh"><b>${c.t}</b><span class="role">${c.role}</span></div>
        <div class="morph">${c.morph}</div>
        <div class="expl">${s.expl[i]}
          <div class="lookups">${c.t.split(" ").filter(w => DICT[w]).map(w => `<span class="lookup" data-k="${w}">🔍 ${w}</span>`).join("")}</div>
        </div>
      </div>`).join("")}</div>
    <div class="gp-title">Grammar points</div>
    ${s.grammar.map(g => `
      <div class="gp">
        <div class="gp-head"><b>${g.name}</b><span>▾</span></div>
        <div class="gp-body">${g.body}</div>
      </div>`).join("")}`;
}

function renderBreakdown() {
  $("#sentCount").textContent = `${SENTENCES.length} sentences · tap one to break it down`;
  $("#toggleAll").textContent = openSents.size === SENTENCES.length ? "Collapse all" : "Expand all";
  $("#sentList").innerHTML = SENTENCES.map((s, si) => `
    <div class="sent ${openSents.has(si) ? "open" : ""}" data-si="${si}">
      <div class="sent-head">
        <span class="num">${si + 1}</span>
        <div class="sent-text"><b>${s.text}</b><small>${s.trans}</small></div>
        <span class="caret">${openSents.has(si) ? "▴" : "▾"}</span>
      </div>
      ${openSents.has(si) ? `<div class="sent-body">${breakdownBody(s, si)}</div>` : ""}
    </div>`).join("");
}

function openSentence(si) {
  openSents.add(si);
  if (!SENTENCES[si].chunks && !generated.has(si)) {
    setTimeout(() => { generated.add(si); renderBreakdown(); }, 900);
  }
}

$("#sentList").addEventListener("click", e => {
  const lk = e.target.closest(".lookup");
  if (lk) { openPopup(lk.dataset.k); return; }
  const gp = e.target.closest(".gp");
  if (gp) { gp.classList.toggle("open"); return; }
  const sent = e.target.closest(".sent"); if (!sent) return;
  const si = +sent.dataset.si;
  if (e.target.closest(".sent-head")) {
    openSents.has(si) ? openSents.delete(si) : openSentence(si);
    renderBreakdown();
    return;
  }
  const ch = e.target.closest("[data-i]");
  if (ch) {
    const i = +ch.dataset.i;
    selChunk[si] = selChunk[si] === i ? -1 : i;
    renderBreakdown();
  }
});
$("#toggleAll").addEventListener("click", () => {
  if (openSents.size === SENTENCES.length) openSents.clear();
  else SENTENCES.forEach((_, si) => openSentence(si));
  renderBreakdown();
});
renderBreakdown();

// ---------- Translate ----------
const input = $("#transInput");
const updateCount = () => $("#charCount").textContent = `${input.value.length} / 5000`;
input.addEventListener("input", updateCount); updateCount();
$("#swapBtn").addEventListener("click", () => {
  const a = $("#langA"), b = $("#langB");
  [a.textContent, b.textContent] = [b.textContent, a.textContent];
});
$("#translateBtn").addEventListener("click", () => {
  const out = $("#transResult");
  out.textContent = "Translating…";
  setTimeout(() => {
    out.textContent = input.value.trim()
      ? "[ LLM translation would appear here ]"
      : "Type something first.";
    if (input.value.trim() === SENTENCES[0].text) out.textContent = SENTENCES[0].trans;
  }, 700);
});

// ---------- Toggles (settings) ----------
document.addEventListener("click", e => {
  if (e.target.classList.contains("toggle")) e.target.classList.toggle("on");
});
// ---------- Start ----------
go("home");
