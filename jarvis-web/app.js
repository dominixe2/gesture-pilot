const $ = (id) => document.getElementById(id);
const chat = $("chat");
const promptBox = $("prompt");
const state = $("state");
const keyInput = $("apiKey");
const keyStatus = $("keyStatus");
let apiKey = "";
let recognition = null;

function setState(text) { state.textContent = text; }
function addMessage(role, text) {
  const item = document.createElement("div");
  item.className = "message " + role;
  const label = document.createElement("b");
  label.textContent = role === "user" ? "TY" : "JARVIS";
  const body = document.createElement("p");
  body.textContent = text;
  item.append(label, body);
  chat.appendChild(item);
  chat.scrollTop = chat.scrollHeight;
  return body;
}
function speak(text) {
  if (!("speechSynthesis" in window)) return;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = "cs-CZ";
  const voices = window.speechSynthesis.getVoices();
  utterance.voice = voices.find(v => v.lang.toLowerCase().startsWith("cs")) || null;
  window.speechSynthesis.speak(utterance);
}
function localAction(text) {
  const t = text.toLowerCase();
  const sites = [
    {terms:["youtube","písnič","hudbu","hudba","music"], url:"https://www.youtube.com/"},
    {terms:["spotify"], url:"https://open.spotify.com/"},
    {terms:["instagram"], url:"https://www.instagram.com/"},
    {terms:["tiktok"], url:"https://www.tiktok.com/"},
    {terms:["github"], url:"https://github.com/"}
  ];
  const match = sites.find(s => s.terms.some(term => t.includes(term)));
  if (match && /otev|pusť|spusť|zapni|jdi na|ukaž/.test(t)) {
    window.open(match.url, "_blank", "noopener,noreferrer");
    return "Otevírám " + new URL(match.url).hostname + ". Přehrávání a ovládání dalších aplikací zatím vyžaduje lokálního pomocníka pro Windows.";
  }
  return null;
}
async function askGemini(text, target) {
  if (!apiKey) {
    target.textContent = "Nejdřív otevři Nastavení Gemini a vlož API klíč. Klíč mi neposílej do chatu.";
    setState("CHYBÍ KLÍČ");
    return;
  }
  setState("PŘEMÝŠLÍM");
  try {
    const response = await fetch("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + encodeURIComponent(apiKey), {
      method: "POST",
      headers: {"Content-Type":"application/json"},
      body: JSON.stringify({systemInstruction:{parts:[{text:"Jsi JARVIS, osobní asistent. Odpovídej přirozeně česky, stručně a užitečně. Neříkej, že jsi provedl akci v počítači, pokud jsi ji skutečně neprovedl. Tento webový prototyp může otevřít několik webových stránek, ale nemá přímý přístup k aplikacím Windows."}]}, contents:[{role:"user",parts:[{text}]}]})
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error?.message || "Gemini API vrátilo chybu " + response.status);
    const answer = data.candidates?.[0]?.content?.parts?.map(p=>p.text||"").join("").trim() || "Nedostal jsem textovou odpověď.";
    target.textContent = answer;
    speak(answer);
    setState("PŘIPRAVEN");
  } catch (err) {
    target.textContent = "Nepodařilo se spojit s Gemini: " + err.message + ". Zkontroluj klíč, připojení a dostupnost modelu.";
    setState("CHYBA");
  }
}
$("composer").addEventListener("submit", async event => {
  event.preventDefault();
  const text = promptBox.value.trim();
  if (!text) return;
  promptBox.value = "";
  addMessage("user", text);
  const response = addMessage("assistant", "...");
  const action = localAction(text);
  if (action) {
    response.textContent = action;
    speak(action);
    setState("PŘIPRAVEN");
    return;
  }
  await askGemini(text, response);
});
$("saveKey").addEventListener("click", () => {
  apiKey = keyInput.value.trim();
  keyStatus.textContent = apiKey ? "Klíč je aktivní jen pro tuto otevřenou relaci." : "Klíč není nastaven.";
  if (apiKey) keyInput.value = "";
});
$("voice").addEventListener("click", () => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) {
    addMessage("assistant", "Tento prohlížeč nepodporuje rozpoznávání řeči. Zkus aktuální Chrome nebo Edge.");
    return;
  }
  if (!recognition) {
    recognition = new SpeechRecognition();
    recognition.lang = "cs-CZ";
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;
    recognition.onstart = () => setState("NASLOUCHÁM");
    recognition.onerror = event => { setState("PŘIPRAVEN"); addMessage("assistant", "Hlas se nepodařilo rozpoznat: " + event.error); };
    recognition.onend = () => { if (state.textContent === "NASLOUCHÁM") setState("PŘIPRAVEN"); };
    recognition.onresult = event => {
      promptBox.value = event.results[0][0].transcript;
      $("composer").requestSubmit();
    };
  }
  try { recognition.start(); } catch { addMessage("assistant", "Rozpoznávání řeči už běží. Zkus to za chvíli znovu."); }
});
if ("speechSynthesis" in window) window.speechSynthesis.getVoices();