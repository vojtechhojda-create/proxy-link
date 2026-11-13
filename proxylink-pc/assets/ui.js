const $ = id => document.getElementById(id)

const fmt = b => (b < 1024 ? b + ' B' : b < 1048576 ? (b / 1024).toFixed(1) + ' KB' : (b / 1048576).toFixed(1) + ' MB')

function el(parent, cls, text) {
  const d = document.createElement('div')
  d.className = cls
  if (text !== undefined) d.textContent = text
  parent.appendChild(d)
  return d
}

function render(s) {
  if (s.qr) $('qr').src = s.qr
  if (s.guestQr) $('gqr').src = s.guestQr
  const sel = $('ip')
  const key = s.ips.join(',')
  if (sel.dataset.key !== key) {
    sel.dataset.key = key
    sel.textContent = ''
    s.ips.forEach(ip => { const o = document.createElement('option'); o.value = ip; o.textContent = ip; sel.appendChild(o) })
  }
  sel.value = s.ip
  $('code').textContent = s.code.replace(/(\d{3})(\d{3})/, '$1 $2')

  const dv = $('devices')
  dv.textContent = ''
  if (!s.devices.length) el(dv, 'muted', 'none connected')
  s.devices.forEach(d => {
    el(dv, 'item', d.name + (d.guest ? ' · guest' : d.trusted ? ' · paired' : ' · unpaired') + (d.app ? ' · app ' + d.app : ''))
    // the state of the versions in words; the protocol numbers are in the details only
    const note = d.compat === 'update-mobile' ? 'requires a mobile update, nothing is sent to or accepted from this device'
      : d.compat === 'update-pc' ? 'requires a pc update for the newest features, basic transfers work'
      : d.compat === 'unknown' ? 'version not announced, basic transfers only'
      : ''
    if (note) el(dv, 'muted', note)
  })
  $('ver').textContent = s.version ? 'pc app ' + s.version + ' · protocol ' + s.protocol : 'protocol ' + s.protocol

  const tr = $('transfers')
  tr.textContent = ''
  if (!s.transfers.length) el(tr, 'muted', 'none yet')
  s.transfers.slice(0, 6).forEach(t => {
    const it = el(tr, 'item')
    el(it, 'name', (t.outgoing ? 'to ' : 'from ') + t.device + ' · ' + t.name)
    if (t.status === 'active' || t.status === 'waiting') {
      const bar = el(it, 'bar')
      const fill = el(bar, 'fill')
      fill.style.width = (t.size > 0 ? Math.min(100, (t.bytes / t.size) * 100) : 0) + '%'
    }
    el(it, 'muted' + (t.status === 'failed' ? ' failed' : ''), t.status + ' · ' + fmt(t.bytes) + ' / ' + fmt(t.size))
  })

  $('strangers').checked = s.acceptStrangers
}

window.pl.get().then(render)
window.pl.onState(render)

$('newCode').onclick = () => window.pl.newCode()
$('sendFile').onclick = () => window.pl.sendFile()
$('folder').onclick = () => window.pl.openFolder()
$('strangers').onchange = e => window.pl.strangers(e.target.checked)
$('sendText').onclick = () => {
  const v = $('text').value
  if (v.trim()) { window.pl.sendText(v); $('text').value = '' }
}
$('text').onkeydown = e => { if (e.key === 'Enter') $('sendText').click() }

$('ip').onchange = e => window.pl.setIp(e.target.value)
