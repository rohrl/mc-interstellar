// Node-only builder. The delivered HTML has no external assets or runtime dependencies.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {diagrams} from './diagrams.mjs';
const dir=path.dirname(fileURLToPath(import.meta.url));
let body=fs.readFileSync(path.join(dir,'chapters.html'),'utf8');
body=body.replace(/<diagram name="([^"]+)"><\/diagram>/g,(_,name)=>{
  if(!diagrams[name])throw new Error(`Unknown diagram: ${name}`);
  return diagrams[name];
});
body=body.replace(/src="assets\/([^"]+)"/g,(_,name)=>`src="data:image/png;base64,${fs.readFileSync(path.join(dir,'assets',name)).toString('base64')}"`);
const sections=[...body.matchAll(/<section id="([^"]+)"[^>]*>\s*<h2>(.*?)<\/h2>/g)];
const nav=sections.map((m,i)=>`<a href="#${m[1]}"><span>${String(i+1).padStart(2,'0')}</span>${m[2].replace(/<[^>]+>/g,'')}</a>`).join('');
const html=`<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="description" content="An illustrated engineering guide to Interstellar's curved-ray Minecraft renderer, its physics, algorithms, optimizations and experiments."><title>Interstellar — How the black-hole renderer works</title><style>${fs.readFileSync(path.join(dir,'style.css'),'utf8')}</style></head><body><a class="skip" href="#guide">Skip navigation</a><aside><a class="wordmark" href="#top">INTERSTELLAR<span>ENGINEERING FIELD GUIDE</span></a><nav aria-label="Chapters">${nav}</nav><button class="print" onclick="window.print()">Print / save as PDF</button><p class="aside-note">Offline edition · 24 September 2026<br>Implementation: fb0c827</p></aside><main id="guide">${body}<footer>Interstellar · An implementation account, not a claim of complete relativistic transport. Written from source and recorded experiments at fb0c827. Diagrams are explanatory unless explicitly identified as measured or numerically integrated.</footer></main><script>${fs.readFileSync(path.join(dir,'interactions.js'),'utf8')}</script></body></html>`;
fs.writeFileSync(path.join(dir,'interstellar-visual-guide.html'),html);
const words=body.replace(/<svg[\s\S]*?<\/svg>/g,' ').replace(/<[^>]+>/g,' ').split(/\s+/).filter(Boolean).length;
console.log(JSON.stringify({chapters:sections.length,words,bytes:Buffer.byteLength(html),output:path.join(dir,'interstellar-visual-guide.html')}));
