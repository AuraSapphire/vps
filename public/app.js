const audio=document.getElementById("audio");
const tracksEl=document.getElementById("tracks");
const emptyEl=document.getElementById("empty");
const titleEl=document.getElementById("title");
const artistEl=document.getElementById("artist");
const seek=document.getElementById("seek");
const elapsed=document.getElementById("elapsed");
const duration=document.getElementById("duration");
const playBtn=document.getElementById("play");
const volume=document.getElementById("volume");
const eq=document.getElementById("eq");
const statusEl=document.getElementById("status");
const footerStatus=document.getElementById("footerStatus");
const search=document.getElementById("search");
const countEl=document.getElementById("count");

let library=[];
let current=-1;
let shuffle=false;
let repeat=false;
let favorites=JSON.parse(localStorage.getItem("aeroplayer-favorites")||"[]");

const fmt=s=>{if(!Number.isFinite(s))return"0:00";return Math.floor(s/60)+":"+String(Math.floor(s%60)).padStart(2,"0")};
const cleanName=name=>name.replace(/\.[^/.]+$/,"").replace(/\s*\(?SPOTISAVER\)?\s*/ig," ").replace(/[_-]+/g," ").replace(/\s+/g," ").trim();

async function load(){
  statusEl.textContent="Loading library…";
  try{
    const res=await fetch("/api/music",{cache:"no-store"});
    if(!res.ok)throw new Error("library request failed");
    library=await res.json();
    countEl.textContent=library.length;
    render();
    statusEl.textContent=library.length?library.length+" tracks":"0 tracks";
    footerStatus.textContent=library.length?"Library ready":"Library empty";
  }catch(e){
    library=[];render();
    statusEl.textContent="Server error";
    footerStatus.textContent="Could not load library";
  }
}
function render(){
  const q=search.value.trim().toLowerCase();
  const onlyFav=document.querySelector(".side-item.active")?.dataset.filter==="favorites";
  const visible=library.filter(t=>(!onlyFav||favorites.includes(t.name))&&(!q||cleanName(t.name).toLowerCase().includes(q)));
  tracksEl.innerHTML="";
  emptyEl.hidden=visible.length>0;
  visible.forEach((t)=>{
    const realIndex=library.indexOf(t);
    const row=document.createElement("button");
    row.className="track"+(realIndex===current?" active":"");
    row.innerHTML=`<span class="track-num">${realIndex===current?"▶":String(realIndex+1).padStart(2,"0")}</span><span class="track-name">${esc(cleanName(t.name))}</span><span class="track-time">${favorites.includes(t.name)?"★":""}</span>`;
    row.onclick=()=>play(realIndex);
    row.ondblclick=()=>play(realIndex);
    tracksEl.appendChild(row);
  });
  document.getElementById("favCount").textContent=favorites.length;
}
function esc(s){return s.replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[c]))}
function play(i){
  if(!library[i])return;
  current=i;
  audio.src=library[i].url;
  audio.play().catch(()=>{});
  titleEl.textContent=cleanName(library[i].name);
  artistEl.textContent="AeroPlayer • VPS library";
  footerStatus.textContent="Playing "+cleanName(library[i].name);
  render();
}
function toggle(){if(!audio.src){if(library.length)play(0);return}audio.paused?audio.play():audio.pause()}
function next(){
  if(!library.length)return;
  let i;
  if(shuffle)i=Math.floor(Math.random()*library.length);
  else i=(current+1)%library.length;
  play(i);
}
function prev(){if(audio.currentTime>3){audio.currentTime=0;return}if(!library.length)return;play((current-1+library.length)%library.length)}
function toggleFav(){
  if(current<0)return;
  const n=library[current].name;
  favorites=favorites.includes(n)?favorites.filter(x=>x!==n):[...favorites,n];
  localStorage.setItem("aeroplayer-favorites",JSON.stringify(favorites));render();
}
playBtn.onclick=toggle;
document.getElementById("next").onclick=next;
document.getElementById("prev").onclick=prev;
document.getElementById("shuffle").onclick=()=>{shuffle=!shuffle;document.getElementById("shuffle").style.color=shuffle?"#69dcff":""};
document.getElementById("repeat").onclick=()=>{repeat=!repeat;document.getElementById("repeat").style.color=repeat?"#69dcff":""};
volume.oninput=()=>audio.volume=Number(volume.value);
audio.volume=.8;
audio.onplay=()=>{playBtn.textContent="❚❚";eq.classList.add("playing")};
audio.onpause=()=>{playBtn.textContent="▶";eq.classList.remove("playing")};
audio.ontimeupdate=()=>{elapsed.textContent=fmt(audio.currentTime);seek.value=audio.duration?audio.currentTime/audio.duration*100:0};
audio.onloadedmetadata=()=>{duration.textContent=fmt(audio.duration)};
audio.onended=()=>repeat?play(current):next();
seek.oninput=()=>{if(audio.duration)audio.currentTime=Number(seek.value)/100*audio.duration};
search.oninput=render;
document.getElementById("refresh").onclick=load;
document.getElementById("refreshFooter").onclick=load;
document.getElementById("libraryBtn").onclick=load;
document.getElementById("nowPlayingBtn").onclick=()=>window.scrollTo({top:0,behavior:"smooth"});
document.getElementById("queueBtn").onclick=()=>{document.querySelectorAll(".side-item").forEach(x=>x.classList.remove("active"));document.getElementById("queueBtn").classList.add("active")};
document.querySelectorAll(".side-item[data-filter]").forEach(btn=>btn.onclick=()=>{document.querySelectorAll(".side-item").forEach(x=>x.classList.remove("active"));btn.classList.add("active");render()});
document.addEventListener("keydown",e=>{if(e.code==="Space"&&e.target.tagName!=="INPUT"){e.preventDefault();toggle()}if(e.code==="ArrowRight"&&e.ctrlKey)next();if(e.code==="ArrowLeft"&&e.ctrlKey)prev()});
load();
