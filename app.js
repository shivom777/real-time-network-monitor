const $ = (id) => document.getElementById(id);
const modal = $('modal');
$('openModal').onclick = () => modal.classList.add('show');
$('closeModal').onclick = () => modal.classList.remove('show');
modal.addEventListener('click', e => { if(e.target === modal) modal.classList.remove('show'); });
$('type').addEventListener('change', () => {
  const tcp = $('type').value === 'TCP';
  $('addressLabel').childNodes[0].textContent = tcp ? 'Host / IP address' : 'Endpoint URL';
  $('address').placeholder = tcp ? 'example.com or 127.0.0.1' : 'https://example.com';
  $('portLabel').classList.toggle('hidden', !tcp);
  $('address').value = '';
});
async function api(path, options={}) { const r=await fetch(path,{headers:{'Content-Type':'application/json'},...options}); const data=await r.json().catch(()=>({})); if(!r.ok)throw new Error(data.error||'Request failed'); return data; }
function esc(s=''){return String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
function timeAgo(value){if(!value)return 'Not checked';const d=new Date(value);return isNaN(d)?'Recently':d.toLocaleTimeString([], {hour:'2-digit',minute:'2-digit',second:'2-digit'});}
async function refresh(){
  try{
    const [targets,events,stats]=await Promise.all([api('/api/targets'),api('/api/events'),api('/api/stats')]);
    $('total').textContent=stats.total;$('up').textContent=stats.up;$('down').textContent=stats.down;$('availability').innerHTML=stats.availability+'<small>%</small>';$('avgResponse').textContent=stats.avgResponse+' ms';$('ringValue').textContent=stats.availability+'%';$('ring').style.background=`conic-gradient(var(--green) ${stats.availability*3.6}deg,#24314a 0deg)`;
    $('targetRows').innerHTML=targets.length?targets.map(t=>`<tr><td><div class="target-name">${esc(t.name)}</div><div class="target-address">${esc(t.address)}${t.type==='TCP'?':'+t.port:''}</div></td><td>${t.type}</td><td><span class="pill ${t.status}">${t.status}</span></td><td>${t.responseTime<0?'—':t.responseTime+' ms'}</td><td>${t.availability}%</td><td><button class="delete" title="Remove target" data-delete="${esc(t.id)}">×</button></td></tr>`).join(''):'<tr><td colspan="6" class="empty">No targets yet. Add a website or server to begin.</td></tr>';
    $('eventCount').textContent=events.length+' EVENTS';
    $('eventRows').innerHTML=events.length?events.slice(0,30).map(e=>`<div class="event"><i class="event-dot ${e.status}"></i><b>${esc(e.target)}</b><span class="pill ${e.status}">${e.status}</span><small>${esc(e.message)} · ${timeAgo(e.time)}</small></div>`).join(''):'<div class="empty">No events recorded yet. Events appear when a target changes status.</div>';
  }catch(e){console.error(e);}
}
$('targetRows').addEventListener('click',async e=>{const id=e.target.dataset.delete;if(!id)return;if(confirm('Remove this monitored target?')){try{await api('/api/targets?id='+encodeURIComponent(id),{method:'DELETE'});refresh();}catch(err){alert(err.message);}}});
$('targetForm').addEventListener('submit',async e=>{e.preventDefault();$('formError').textContent='';const f=new FormData(e.target);const payload={name:f.get('name').trim(),type:f.get('type'),address:f.get('address').trim(),port:f.get('port')||'0'};try{await api('/api/targets',{method:'POST',body:JSON.stringify(payload)});e.target.reset();$('portLabel').classList.add('hidden');modal.classList.remove('show');refresh();}catch(err){$('formError').textContent=err.message;}});
$('refresh').onclick=refresh;refresh();setInterval(refresh,4000);
