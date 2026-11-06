import { WebSocketServer, WebSocket } from 'ws'
import { Bonjour } from 'bonjour-service'
import clipboard from 'clipboardy'
import { createHash, createHmac, randomBytes, randomUUID, timingSafeEqual } from 'node:crypto'
import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import { createInterface } from 'node:readline'
import os from 'node:os'

const PORT = 8765
const NAME = process.env.PL_NAME ?? os.hostname()
const FILE = 'state.json'

type State = { id: string; paired: Record<string, { name: string; secret: string }> }
const state: State = existsSync(FILE)
  ? JSON.parse(readFileSync(FILE, 'utf8'))
  : { id: randomUUID(), paired: {} }
const save = () => writeFileSync(FILE, JSON.stringify(state, null, 2))
save()

const sha = (s: string) => createHash('sha256').update(s).digest('hex')
const mac = (secret: string, nonce: string) => createHmac('sha256', secret).update(nonce).digest('hex')
const eq = (a: string, b: string) => a.length === b.length && timingSafeEqual(Buffer.from(a), Buffer.from(b))

// pairing code, rotates after use or 5 wrong attempts
let code = ''
let fails = 0
const newCode = () => {
  code = String(100000 + Math.floor(Math.random() * 900000))
  fails = 0
  console.log(`pairing code: ${code}`)
}
newCode()

interface Sess { ws: WebSocket; nonce: string; id?: string; name?: string; trusted: boolean; allowed: Set<string> }
const sessions = new Set<Sess>()
const outgoing = new Map<string, { kind: string; text: string }>()
let lastHash = ''

const send = (ws: WebSocket, m: object) => { if (ws.readyState === ws.OPEN) ws.send(JSON.stringify(m)) }
const broadcast = (m: object) => { for (const s of sessions) if (s.trusted) send(s.ws, m) }

// single stdin reader: answers pending prompts first, otherwise runs commands
let answer: ((v: string) => void) | null = null
const rl = createInterface({ input: process.stdin })
const ask = (q: string) => new Promise<boolean>(r => {
  console.log(q)
  answer = v => r(v.trim().toLowerCase() === 'y')
})
rl.on('line', l => {
  if (answer) { const a = answer; answer = null; return a(l) }
  command(l)
})

function sendText(text: string) {
  const sid = randomUUID()
  const kind = /^https?:\/\//.test(text) ? 'link' : 'text'
  outgoing.set(sid, { kind, text })
  broadcast({ t: 'share.request', sid, kind, name: text.slice(0, 40), size: text.length })
}

function command(l: string) {
  const [c, ...r] = l.trim().split(' ')
  const arg = r.join(' ')
  if (c === 's' && arg) sendText(arg)
  else if (c === 'l') console.log([...sessions].map(x => `${x.name ?? '?'} ${x.trusted ? 'trusted' : 'stranger'}`).join('\n') || 'no devices')
  else if (c === 'c') newCode()
  else console.log('commands: s <text|link> | l | c (new pairing code)')
}

async function handle(s: Sess, m: any) {
  switch (m.t) {
    case 'hello': {
      s.id = String(m.id)
      s.name = String(m.name ?? 'device').slice(0, 40)
      const p = state.paired[s.id]
      s.trusted = !!(p && typeof m.mac === 'string' && eq(m.mac, mac(p.secret, s.nonce)))
      send(s.ws, { t: 'ready', trusted: s.trusted })
      break
    }
    case 'pair.req': {
      if (!s.id) return
      if (String(m.code) !== code) {
        send(s.ws, { t: 'pair.fail' })
        if (++fails >= 5) newCode()
        return
      }
      const secret = randomBytes(32).toString('hex')
      state.paired[s.id] = { name: s.name ?? 'device', secret }
      save()
      s.trusted = true
      send(s.ws, { t: 'pair.ok', secret })
      console.log(`paired: ${s.name}`)
      newCode()
      break
    }
    case 'share.request': {
      if (!['text', 'link'].includes(m.kind) || !(m.size <= 100_000)) return send(s.ws, { t: 'share.deny', sid: m.sid })
      const ok = s.trusted || await ask(`${s.name} wants to send ${m.kind} "${m.name}" - accept? (y/n)`)
      if (!ok) return send(s.ws, { t: 'share.deny', sid: m.sid })
      s.allowed.add(m.sid)
      send(s.ws, { t: 'share.accept', sid: m.sid })
      break
    }
    case 'share.text': {
      if (!s.allowed.delete(m.sid) || typeof m.text !== 'string') return
      console.log(`[${s.name}] ${m.kind}: ${m.text}`)
      lastHash = sha(m.text)
      await clipboard.write(m.text)
      break
    }
    case 'share.accept': {
      const o = outgoing.get(m.sid)
      if (o && s.trusted) { outgoing.delete(m.sid); send(s.ws, { t: 'share.text', sid: m.sid, ...o }) }
      break
    }
    case 'share.deny': outgoing.delete(m.sid); break
    case 'clip.update': {
      if (!s.trusted || typeof m.text !== 'string' || m.text.length > 100_000) return
      const h = sha(m.text)
      if (h === lastHash) return
      lastHash = h
      await clipboard.write(m.text)
      break
    }
    case 'ping': send(s.ws, { t: 'pong' }); break
  }
}

const wss = new WebSocketServer({ port: PORT, maxPayload: 1 << 20 })
wss.on('connection', ws => {
  const s: Sess = { ws, nonce: randomBytes(16).toString('hex'), trusted: false, allowed: new Set() }
  sessions.add(s)
  send(ws, { t: 'hello', id: state.id, name: NAME, v: 2, nonce: s.nonce })
  ws.on('close', () => sessions.delete(s))
  ws.on('error', () => ws.close())
  ws.on('message', (raw, bin) => {
    if (bin) return
    let m: any
    try { m = JSON.parse(raw.toString()) } catch { return }
    handle(s, m).catch(() => {})
  })
})

// clipboard poll -> trusted devices (hash dedupe prevents echo loops)
setInterval(async () => {
  try {
    const t = await clipboard.read()
    const h = sha(t)
    if (t && h !== lastHash) {
      lastHash = h
      broadcast({ t: 'clip.update', ts: Date.now(), hash: h, text: t.slice(0, 100_000) })
    }
  } catch {}
}, 800)

// mdns: _proxylink._tcp
new Bonjour().publish({
  name: `proxylink-${NAME}`,
  type: 'proxylink',
  protocol: 'tcp',
  port: PORT,
  txt: { id: state.id, v: '2', name: NAME },
})

console.log(`proxylink pc "${NAME}" listening on :${PORT}`)
command('')
