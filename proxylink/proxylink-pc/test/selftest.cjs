// end-to-end protocol test against dist/server.js (no electron, no mdns)
const { createServer } = require('../dist/server.js')
const WebSocket = require('ws')
const crypto = require('crypto')
const fs = require('fs')
const os = require('os')
const path = require('path')

const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'pl-'))
let clip = ''
const srv = createServer({
  name: 'testpc', dataDir: tmp, downloadDir: path.join(tmp, 'dl'), mdns: false, port: 18765,
  clip: { read: async () => clip, write: async t => { clip = t } },
  ask: async () => true, log: () => {},
})

const sleep = ms => new Promise(r => setTimeout(r, ms))
const sha = b => crypto.createHash('sha256').update(b).digest('hex')
const assert = (c, m) => { if (!c) { console.error('FAIL: ' + m); process.exit(1) } }

function client() {
  const ws = new WebSocket('wss://127.0.0.1:18765', {
    rejectUnauthorized: false,
    checkServerIdentity: (_h, cert) => {
      if (cert.fingerprint256.replace(/:/g, '').toLowerCase() !== srv.state().fingerprint) return new Error('pin mismatch')
    },
  })
  const msgs = [], bins = []
  ws.on('message', (d, bin) => (bin ? bins.push(Buffer.from(d)) : msgs.push(JSON.parse(d.toString()))))
  const next = async (t, ms = 3000) => {
    const end = Date.now() + ms
    while (Date.now() < end) {
      const i = msgs.findIndex(m => m.t === t)
      if (i >= 0) return msgs.splice(i, 1)[0]
      await sleep(10)
    }
    throw new Error('timeout waiting for ' + t)
  }
  const tx = m => ws.send(JSON.stringify(m))
  const open = new Promise(r => ws.on('open', r))
  return { ws, msgs, bins, next, tx, open }
}

;(async () => {
  const id = crypto.randomUUID()
  let c = client()
  await c.open
  let h = await c.next('hello')
  c.tx({ t: 'hello', id, name: 'phone' })
  assert((await c.next('ready')).trusted === false, 'stranger not trusted')

  c.tx({ t: 'pair.req', code: '000000' })
  await c.next('pair.fail')
  c.tx({ t: 'pair.req', code: srv.state().code })
  const secret = (await c.next('pair.ok')).secret
  assert(secret && secret.length === 64, 'pair secret')
  c.ws.close(); await sleep(100)

  c = client(); await c.open
  h = await c.next('hello')
  const mac = crypto.createHmac('sha256', secret).update(h.nonce).digest('hex')
  c.tx({ t: 'hello', id, name: 'phone', mac })
  assert((await c.next('ready')).trusted === true, 'reconnect trusted')

  // text share phone -> pc
  let sid = crypto.randomUUID()
  c.tx({ t: 'share.request', sid, kind: 'text', name: 'hi', size: 5 })
  await c.next('share.accept')
  c.tx({ t: 'share.text', sid, kind: 'text', text: 'hello' })
  await sleep(100)
  assert(clip === 'hello', 'text landed in clipboard')

  // file phone -> pc (good hash)
  const data = crypto.randomBytes(200000)
  sid = crypto.randomUUID()
  c.tx({ t: 'share.request', sid, kind: 'file', name: 'a.bin', size: data.length, mime: 'application/octet-stream' })
  await c.next('share.accept')
  for (let i = 0; i < data.length; i += 65536)
    c.ws.send(Buffer.concat([Buffer.from(sid), data.subarray(i, i + 65536)]), { binary: true })
  c.tx({ t: 'share.done', sid, sha256: sha(data) })
  assert((await c.next('share.ok')).ok === true, 'file ok')
  assert(sha(fs.readFileSync(path.join(tmp, 'dl', 'a.bin'))) === sha(data), 'file content')

  // file with wrong hash
  sid = crypto.randomUUID()
  c.tx({ t: 'share.request', sid, kind: 'file', name: 'b.bin', size: 3 })
  await c.next('share.accept')
  c.ws.send(Buffer.concat([Buffer.from(sid), Buffer.from('abc')]), { binary: true })
  c.tx({ t: 'share.done', sid, sha256: 'bad' })
  assert((await c.next('share.ok')).ok === false, 'bad hash rejected')
  assert(!fs.existsSync(path.join(tmp, 'dl', 'b.bin')), 'bad file removed')

  // path traversal in name
  sid = crypto.randomUUID()
  c.tx({ t: 'share.request', sid, kind: 'file', name: '../../evil.txt', size: 1 })
  await c.next('share.accept')
  c.ws.send(Buffer.concat([Buffer.from(sid), Buffer.from('x')]), { binary: true })
  c.tx({ t: 'share.done', sid, sha256: sha('x') })
  await c.next('share.ok')
  assert(fs.existsSync(path.join(tmp, 'dl', 'evil.txt')), 'name sanitized into download dir')

  // pc -> phone text
  srv.sendText('https://example.com')
  const rq = await c.next('share.request')
  assert(rq.kind === 'link', 'link kind')
  c.tx({ t: 'share.accept', sid: rq.sid })
  const tx = await c.next('share.text')
  assert(tx.text === 'https://example.com', 'text delivered to phone')

  // pc -> phone file
  const fp = path.join(tmp, 'out.bin')
  const out = crypto.randomBytes(300000)
  fs.writeFileSync(fp, out)
  srv.sendFile(fp)
  const fr = await c.next('share.request')
  assert(fr.kind === 'file' && fr.size === out.length, 'file request')
  c.tx({ t: 'share.accept', sid: fr.sid })
  const done = await c.next('share.done', 5000)
  const got = Buffer.concat(c.bins.map(b => b.subarray(36)))
  assert(sha(got) === done.sha256 && sha(got) === sha(out), 'file delivered intact')
  c.tx({ t: 'share.ok', sid: fr.sid, ok: true })
  await sleep(100)
  assert(srv.state().transfers.find(t => t.sid === fr.sid).status === 'done', 'transfer marked done')

  // clipboard both ways
  clip = 'from-pc'
  assert((await c.next('clip.update', 3000)).text === 'from-pc', 'clip pc->phone')
  c.tx({ t: 'clip.update', ts: Date.now(), hash: 'x', text: 'from-phone' })
  await sleep(200)
  assert(clip === 'from-phone', 'clip phone->pc')


  // ---- guest qr: unpaired phone, one use ----
  const token = new URL(srv.state().guestUri).searchParams.get('t')
  let g = client(); await g.open; await g.next('hello')
  g.tx({ t: 'hello', id: crypto.randomUUID(), name: 'stranger' })
  await g.next('ready')
  g.tx({ t: 'guest.req', token: 'nope' })
  await g.next('guest.fail')
  g.tx({ t: 'guest.req', token })
  await g.next('guest.ok')
  sid = crypto.randomUUID()
  g.tx({ t: 'share.request', sid, kind: 'text', name: 'x', size: 2 })
  await g.next('share.accept')            // auto accepted, no prompt
  g.tx({ t: 'share.text', sid, kind: 'text', text: 'hi' })
  await sleep(100)
  assert(clip === 'hi', 'guest text delivered')
  let g2 = client(); await g2.open; await g2.next('hello')
  g2.tx({ t: 'hello', id: crypto.randomUUID(), name: 'stranger2' }); await g2.next('ready')
  g2.tx({ t: 'guest.req', token })
  await g2.next('guest.fail')             // token already used
  assert(srv.state().guestUri !== '' && new URL(srv.state().guestUri).searchParams.get('t') !== token, 'guest token rotated')

  // ---- relay: phone A -> pc -> phone B ----
  const bid = crypto.randomUUID()
  let b = client(); await b.open; await b.next('hello')
  b.tx({ t: 'hello', id: bid, name: 'phoneB' }); await b.next('ready')
  b.tx({ t: 'pair.req', code: srv.state().code })
  await b.next('pair.ok')
  let found = false
  for (let i = 0; i < 20 && !found; i++) {
    const pr = await c.next('peers'); found = pr.list.some(x => x.id === bid)
  }
  assert(found, 'A sees B in peers')

  sid = crypto.randomUUID()
  c.tx({ t: 'share.request', sid, kind: 'text', name: 'yo', size: 2, to: bid })
  const rr = await b.next('share.request')
  assert(rr.from === 'phone' && rr.sid === sid, 'relay request carries sender name')
  b.tx({ t: 'share.accept', sid })
  await c.next('share.accept')
  c.tx({ t: 'share.text', sid, kind: 'text', text: 'relayed' })
  assert((await b.next('share.text')).text === 'relayed', 'relayed text')

  const rdata = crypto.randomBytes(400000)
  sid = crypto.randomUUID()
  c.tx({ t: 'share.request', sid, kind: 'file', name: 'r.bin', size: rdata.length, to: bid })
  await b.next('share.request')
  b.tx({ t: 'share.accept', sid })
  await c.next('share.accept')
  for (let i = 0; i < rdata.length; i += 65536)
    c.ws.send(Buffer.concat([Buffer.from(sid), rdata.subarray(i, i + 65536)]), { binary: true })
  c.tx({ t: 'share.done', sid, sha256: sha(rdata) })
  const rd = await b.next('share.done', 5000)
  await sleep(100)
  const rgot = Buffer.concat(b.bins.map(x => x.subarray(36)))
  assert(sha(rgot) === rd.sha256 && sha(rgot) === sha(rdata), 'relayed file intact')
  b.tx({ t: 'share.ok', sid, ok: true })
  assert((await c.next('share.ok')).ok === true, 'relay ok reaches sender')
  // B cannot inject into A's route as sender
  b.tx({ t: 'share.text', sid, kind: 'text', text: 'evil' })
  await sleep(100)
  assert(clip !== 'evil', 'no injection through closed route')

  assert(/^proxylink:\/\/pair\?h=.*&p=18765&c=\d{6}&f=[0-9a-f]{64}&i=/.test(srv.state().pairUri), 'pair uri')
  console.log('selftest ok')
  srv.close()
  process.exit(0)
})().catch(e => { console.error('FAIL', e.message); process.exit(1) })
