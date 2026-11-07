import { WebSocketServer, WebSocket, RawData } from 'ws'
import { Bonjour } from 'bonjour-service'
import { createHash, createHmac, randomBytes, randomInt, randomUUID, timingSafeEqual, Hash, X509Certificate } from 'node:crypto'
import { createServer as createHttps } from 'node:https'
import { networkInterfaces } from 'node:os'
import selfsigned from 'selfsigned'
import {
  createReadStream, createWriteStream, existsSync, mkdirSync, readFileSync,
  statSync, unlinkSync, writeFileSync, WriteStream,
} from 'node:fs'
import { basename, extname, join } from 'node:path'

const MAX_FILE = 4_000_000_000
const MAX_TEXT = 100_000
const CHUNK = 65_536
const SID = /^[0-9a-f-]{36}$/i
const MIME: Record<string, string> = {
  '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.gif': 'image/gif',
  '.webp': 'image/webp', '.mp4': 'video/mp4', '.mp3': 'audio/mpeg', '.pdf': 'application/pdf',
  '.txt': 'text/plain', '.zip': 'application/zip',
}

const sleep = (ms: number) => new Promise<void>(r => setTimeout(r, ms))
const sha = (s: string) => createHash('sha256').update(s).digest('hex')
const mac = (secret: string, nonce: string) => createHmac('sha256', secret).update(nonce).digest('hex')
const eq = (a: string, b: string) => a.length === b.length && timingSafeEqual(Buffer.from(a), Buffer.from(b))
const safeName = (n: unknown) => basename(String(n ?? '')).replace(/[\\/:*?"<>|\x00-\x1f]/g, '_').slice(0, 120) || 'file'
const fmt = (b: number) => (b < 1024 ? `${b} B` : b < 1048576 ? `${(b / 1024).toFixed(1)} KB` : `${(b / 1048576).toFixed(1)} MB`)

function loadCert(dir: string) {
  const certFile = join(dir, 'cert.pem')
  const keyFile = join(dir, 'key.pem')
  if (!existsSync(certFile) || !existsSync(keyFile)) {
    const pems = selfsigned.generate([{ name: 'commonName', value: 'proxylink' }], {
      keySize: 2048, days: 3650, algorithm: 'sha256',
    })
    writeFileSync(certFile, pems.cert)
    writeFileSync(keyFile, pems.private, { mode: 0o600 })
  }
  const cert = readFileSync(certFile, 'utf8')
  const key = readFileSync(keyFile, 'utf8')
  const fingerprint = new X509Certificate(cert).fingerprint256.replace(/:/g, '').toLowerCase()
  return { cert, key, fingerprint }
}

function lanIps(): string[] {
  const out: string[] = []
  for (const list of Object.values(networkInterfaces())) {
    for (const i of list ?? []) if (i.family === 'IPv4' && !i.internal) out.push(i.address)
  }
  const priv = (a: string) => /^(192\.168\.|10\.|172\.(1[6-9]|2\d|3[01])\.)/.test(a)
  return out.sort((a, b) => Number(priv(b)) - Number(priv(a)))
}

function uniquePath(dir: string, name: string) {
  const ext = extname(name)
  const base = name.slice(0, name.length - ext.length)
  let p = join(dir, name)
  let i = 1
  while (existsSync(p)) p = join(dir, `${base} (${i++})${ext}`)
  return p
}

export type TStatus = 'waiting' | 'active' | 'done' | 'failed' | 'denied'
export interface Transfer { sid: string; name: string; size: number; bytes: number; outgoing: boolean; status: TStatus; device: string }
export interface Device { id: string; name: string; trusted: boolean; guest: boolean }
export interface ServerState {
  id: string; name: string; code: string; acceptStrangers: boolean
  devices: Device[]; transfers: Transfer[]; downloadDir: string
  fingerprint: string; ips: string[]; ip: string; pairUri: string; guestUri: string
}
export interface Clip { read(): Promise<string>; write(t: string): Promise<void> }
export interface Opts {
  name: string
  dataDir: string
  downloadDir: string
  clip: Clip
  ask(q: string): Promise<boolean>
  log(m: string): void
  onChange?(): void
  port?: number
  mdns?: boolean
}

interface Persist { id: string; paired: Record<string, { name: string; secret: string }>; acceptStrangers: boolean }
interface InFile { stream: WriteStream; hash: Hash; bytes: number; size: number; path: string; tr: Transfer }
interface Sess {
  ws: WebSocket; nonce: string; id?: string; name?: string; trusted: boolean; guest?: boolean
  allowed: Set<string>; files: Map<string, InFile>
}
interface OutFile { path: string; name: string; size: number; tr: Transfer; sess: Sess }

export function createServer(opts: Opts) {
  const port = opts.port ?? 8765
  mkdirSync(opts.dataDir, { recursive: true })
  mkdirSync(opts.downloadDir, { recursive: true })

  const stateFile = join(opts.dataDir, 'state.json')
  const st: Persist = existsSync(stateFile)
    ? JSON.parse(readFileSync(stateFile, 'utf8'))
    : { id: '', paired: {}, acceptStrangers: true }
  if (!st.id) st.id = randomUUID()
  st.paired ??= {}
  st.acceptStrangers ??= true
  const save = () => writeFileSync(stateFile, JSON.stringify(st, null, 2))
  save()

  const tls = loadCert(opts.dataDir)
  let code = String(randomInt(100000, 1000000))
  let fails = 0
  let lastHash = ''
  let lastNotify = 0
  let guestToken = randomBytes(9).toString('hex')
  let guestExpires = Date.now() + 5 * 60_000
  let preferredIp = ''
  const routes = new Map<string, { a: Sess; b: Sess }>()   // phone -> pc -> phone relay, by sid

  const sessions = new Set<Sess>()
  const transfers: Transfer[] = []
  const outgoing = new Map<string, { kind: string; text: string }>()
  const outFiles = new Map<string, OutFile>()

  const notify = (force = true) => {
    const n = Date.now()
    if (!force && n - lastNotify < 200) return
    lastNotify = n
    opts.onChange?.()
  }
  const send = (ws: WebSocket, m: object) => { if (ws.readyState === ws.OPEN) ws.send(JSON.stringify(m)) }
  const broadcast = (m: object) => { for (const s of sessions) if (s.trusted) send(s.ws, m) }
  const addTransfer = (t: Transfer) => {
    transfers.unshift(t)
    if (transfers.length > 20) transfers.length = 20
    return t
  }

  const hostIp = () => (preferredIp && lanIps().includes(preferredIp) ? preferredIp : lanIps()[0] ?? '')

  function newGuest() {
    guestToken = randomBytes(9).toString('hex')
    guestExpires = Date.now() + 5 * 60_000
    notify()
  }
  const guestTimer = setInterval(() => { if (Date.now() > guestExpires) newGuest() }, 15_000)

  function sendPeers() {
    const trusted = [...sessions].filter(x => x.trusted && x.id)
    for (const s of trusted) {
      send(s.ws, { t: 'peers', list: trusted.filter(o => o !== s).map(o => ({ id: o.id, name: o.name })) })
    }
  }

  function newCode() {
    code = String(randomInt(100000, 1000000))
    fails = 0
    notify()
  }

  function sendText(text: string) {
    const sid = randomUUID()
    const kind = /^https?:\/\//.test(text) ? 'link' : 'text'
    outgoing.set(sid, { kind, text })
    broadcast({ t: 'share.request', sid, kind, name: text.slice(0, 40), size: text.length })
  }

  function sendFile(path: string) {
    let size = 0
    try {
      const s = statSync(path)
      if (!s.isFile()) return opts.log('not a file')
      size = s.size
    } catch { return opts.log(`file not found: ${path}`) }
    const targets = [...sessions].filter(x => x.trusted)
    if (!targets.length) return opts.log('no trusted device connected')
    const name = basename(path)
    for (const s of targets) {
      const sid = randomUUID()
      const tr = addTransfer({ sid, name, size, bytes: 0, outgoing: true, status: 'waiting', device: s.name ?? '?' })
      outFiles.set(sid, { path, name, size, tr, sess: s })
      send(s.ws, { t: 'share.request', sid, kind: 'file', name, size, mime: MIME[extname(name).toLowerCase()] ?? 'application/octet-stream' })
    }
    notify()
  }

  async function pump(s: Sess, sid: string, f: OutFile) {
    const hash = createHash('sha256')
    const sidb = Buffer.from(sid, 'ascii')
    let n = 0
    f.tr.status = 'active'
    notify()
    try {
      const rs = createReadStream(f.path, { highWaterMark: CHUNK }) as unknown as AsyncIterable<Buffer>
      for await (const chunk of rs) {
        hash.update(chunk)
        while (s.ws.bufferedAmount > 4_000_000) await sleep(15)
        if (s.ws.readyState !== s.ws.OPEN) throw new Error('closed')
        s.ws.send(Buffer.concat([sidb, chunk]), { binary: true })
        n += chunk.length
        f.tr.bytes = n
        notify(false)
      }
      send(s.ws, { t: 'share.done', sid, sha256: hash.digest('hex') })
    } catch {
      f.tr.status = 'failed'
    }
    notify()
  }

  function abortFile(s: Sess, sid: string, f: InFile, tell: boolean) {
    s.files.delete(sid)
    f.stream.destroy()
    try { unlinkSync(f.path) } catch {}
    f.tr.status = 'failed'
    if (tell) send(s.ws, { t: 'share.ok', sid, ok: false })
    notify()
  }

  function onBinary(s: Sess, raw: RawData) {
    const b = Buffer.isBuffer(raw) ? raw : Array.isArray(raw) ? Buffer.concat(raw) : Buffer.from(raw as ArrayBuffer)
    if (b.length <= 36) return
    const sid = b.subarray(0, 36).toString('ascii')
    const rt = routes.get(sid)
    if (rt) {
      if (s === rt.a && rt.b.ws.readyState === rt.b.ws.OPEN) {
        rt.b.ws.send(b, { binary: true })
        if (rt.b.ws.bufferedAmount > 4_000_000) {
          s.ws.pause()
          const iv = setInterval(() => {
            if (rt.b.ws.bufferedAmount < 1_000_000 || rt.b.ws.readyState !== rt.b.ws.OPEN) { clearInterval(iv); s.ws.resume() }
          }, 20)
        }
      }
      return
    }
    const f = s.files.get(sid)
    if (!f) return
    const data = b.subarray(36)
    if (f.bytes + data.length > f.size) return abortFile(s, sid, f, true)
    f.hash.update(data)
    f.bytes += data.length
    f.stream.write(data)
    f.tr.bytes = f.bytes
    notify(false)
  }

  async function handle(s: Sess, m: any) {
    const rsid = typeof m.sid === 'string' ? m.sid : ''
    const route = rsid ? routes.get(rsid) : undefined
    if (route && ['share.accept', 'share.deny', 'share.text', 'share.done', 'share.ok'].includes(m.t)) {
      const fromReceiver = ['share.accept', 'share.deny', 'share.ok'].includes(m.t)
      const ok = fromReceiver ? s === route.b : s === route.a
      if (ok) {
        send(fromReceiver ? route.a.ws : route.b.ws, m)
        if (m.t === 'share.deny' || m.t === 'share.ok') routes.delete(rsid)
      }
      return
    }
    switch (m.t) {
      case 'hello': {
        const id = String(m.id ?? '').slice(0, 64)
        if (!id) return
        s.id = id
        s.name = String(m.name ?? 'device').slice(0, 40)
        const p = st.paired[id]
        s.trusted = !!(p && typeof m.mac === 'string' && eq(m.mac, mac(p.secret, s.nonce)))
        send(s.ws, { t: 'ready', trusted: s.trusted })
        notify()
        if (s.trusted) sendPeers()
        break
      }
      case 'guest.req': {
        if (!s.id || s.trusted) return
        if (String(m.token) !== guestToken || Date.now() > guestExpires) { send(s.ws, { t: 'guest.fail' }); return }
        s.guest = true
        send(s.ws, { t: 'guest.ok' })
        opts.log(`guest connected: ${s.name}`)
        newGuest()
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
        st.paired[s.id] = { name: s.name ?? 'device', secret }
        save()
        s.trusted = true
        send(s.ws, { t: 'pair.ok', secret })
        sendPeers()
        opts.log(`paired: ${s.name}`)
        newCode()
        break
      }
      case 'share.request': {
        const sid = String(m.sid)
        const kind = String(m.kind)
        const size = Number(m.size)
        const deny = () => send(s.ws, { t: 'share.deny', sid })
        if (!SID.test(sid) || !['text', 'link', 'file'].includes(kind)) return deny()
        if (!Number.isFinite(size) || size < 0) return deny()
        if (kind === 'file' ? size > MAX_FILE : size > MAX_TEXT) return deny()
        if (m.to !== undefined) {
          // relay to another trusted device (phone -> pc -> phone)
          if (!s.trusted) return deny()
          const t = [...sessions].find(x => x.trusted && x.id === String(m.to) && x !== s)
          if (!t) return deny()
          routes.set(sid, { a: s, b: t })
          send(t.ws, { t: 'share.request', sid, kind, name: m.name, size, mime: m.mime, from: s.name })
          return
        }
        if (!s.trusted && !s.guest && !st.acceptStrangers) return deny()
        if (s.guest && kind === 'file' && size > 500_000_000) return deny()
        const label = kind === 'file' ? `${safeName(m.name)} (${fmt(size)})` : `"${String(m.name ?? '').slice(0, 40)}"`
        const ok = s.trusted || !!s.guest || await opts.ask(`${s.name} wants to send ${kind} ${label}`)
        if (!ok) return deny()
        if (kind === 'file') {
          const path = uniquePath(opts.downloadDir, safeName(m.name))
          const tr = addTransfer({ sid, name: basename(path), size, bytes: 0, outgoing: false, status: 'active', device: s.name ?? '?' })
          const stream = createWriteStream(path)
          const f: InFile = { stream, hash: createHash('sha256'), bytes: 0, size, path, tr }
          stream.on('error', () => abortFile(s, sid, f, true))
          s.files.set(sid, f)
          notify()
        } else {
          s.allowed.add(sid)
        }
        send(s.ws, { t: 'share.accept', sid })
        break
      }
      case 'share.text': {
        const sid = String(m.sid)
        if (!s.allowed.delete(sid) || typeof m.text !== 'string') return
        opts.log(`[${s.name}] ${m.kind}: ${m.text}`)
        lastHash = sha(m.text)
        await opts.clip.write(m.text)
        break
      }
      case 'share.done': {
        const sid = String(m.sid)
        const f = s.files.get(sid)
        if (!f) return
        s.files.delete(sid)
        f.stream.end(() => {
          const ok = f.bytes === f.size && f.hash.digest('hex') === m.sha256
          if (!ok) { try { unlinkSync(f.path) } catch {} }
          f.tr.status = ok ? 'done' : 'failed'
          if (ok) f.tr.bytes = f.size
          send(s.ws, { t: 'share.ok', sid, ok })
          opts.log(ok ? `received file: ${f.path}` : `file failed: ${f.tr.name}`)
          notify()
        })
        break
      }
      case 'share.accept': {
        const sid = String(m.sid)
        if (!s.trusted) return
        const o = outgoing.get(sid)
        if (o) { outgoing.delete(sid); send(s.ws, { t: 'share.text', sid, ...o }) }
        const f = outFiles.get(sid)
        if (f && f.sess === s) { outFiles.delete(sid); void pump(s, sid, f) }
        break
      }
      case 'share.deny': {
        const sid = String(m.sid)
        outgoing.delete(sid)
        const f = outFiles.get(sid)
        if (f && f.sess === s) { outFiles.delete(sid); f.tr.status = 'denied'; notify() }
        break
      }
      case 'share.ok': {
        const tr = transfers.find(t => t.sid === String(m.sid))
        if (tr && tr.outgoing) {
          tr.status = m.ok ? 'done' : 'failed'
          if (m.ok) tr.bytes = tr.size
          notify()
        }
        break
      }
      case 'clip.update': {
        if (!s.trusted || typeof m.text !== 'string' || m.text.length > MAX_TEXT) return
        const h = sha(m.text)
        if (h === lastHash) return
        lastHash = h
        await opts.clip.write(m.text)
        break
      }
      case 'ping': send(s.ws, { t: 'pong' }); break
    }
  }

  const https = createHttps({ cert: tls.cert, key: tls.key, minVersion: 'TLSv1.2' })
  https.on('error', e => opts.log(`server error: ${e.message}`))
  https.listen(port)
  const wss = new WebSocketServer({ server: https, maxPayload: 1 << 20 })
  wss.on('error', e => opts.log(`server error: ${e.message}`))
  wss.on('connection', ws => {
    const s: Sess = { ws, nonce: randomBytes(16).toString('hex'), trusted: false, allowed: new Set(), files: new Map() }
    sessions.add(s)
    send(ws, { t: 'hello', id: st.id, name: opts.name, v: 2, nonce: s.nonce })
    ws.on('close', () => {
      for (const [sid, f] of [...s.files]) abortFile(s, sid, f, false)
      for (const [sid, f] of [...outFiles]) if (f.sess === s) { outFiles.delete(sid); f.tr.status = 'failed' }
      for (const [sid, r] of [...routes]) if (r.a === s || r.b === s) routes.delete(sid)
      sessions.delete(s)
      sendPeers()
      notify()
    })
    ws.on('error', () => ws.close())
    ws.on('message', (raw, bin) => {
      if (bin) return onBinary(s, raw)
      let m: any
      try { m = JSON.parse(raw.toString()) } catch { return }
      handle(s, m).catch(() => {})
    })
  })

  // clipboard poll -> trusted devices (hash dedupe prevents echo loops)
  const poll = setInterval(async () => {
    try {
      const t = await opts.clip.read()
      const h = sha(t)
      if (t && h !== lastHash) {
        lastHash = h
        broadcast({ t: 'clip.update', ts: Date.now(), hash: h, text: t.slice(0, MAX_TEXT) })
      }
    } catch {}
  }, 800)

  let bonjour: Bonjour | null = null
  if (opts.mdns !== false) {
    try {
      bonjour = new Bonjour()
      bonjour.publish({
        name: `proxylink-${opts.name}`,
        type: 'proxylink',
        protocol: 'tcp',
        port,
        txt: { id: st.id, v: '2', name: opts.name, fp: tls.fingerprint },
      })
    } catch (e) {
      opts.log(`mdns unavailable: ${(e as Error).message}`)
    }
  }

  return {
    state: (): ServerState => ({
      id: st.id,
      name: opts.name,
      code,
      acceptStrangers: st.acceptStrangers,
      devices: [...sessions].filter(s => s.id).map(s => ({ id: s.id!, name: s.name ?? '?', trusted: s.trusted, guest: !!s.guest })),
      transfers: transfers.map(t => ({ ...t })),
      downloadDir: opts.downloadDir,
      fingerprint: tls.fingerprint,
      ips: lanIps(),
      ip: hostIp(),
      pairUri: `proxylink://pair?h=${hostIp()}&p=${port}&c=${code}&f=${tls.fingerprint}&i=${st.id}&n=${encodeURIComponent(opts.name)}`,
      guestUri: `proxylink://guest?h=${hostIp()}&p=${port}&t=${guestToken}&f=${tls.fingerprint}&n=${encodeURIComponent(opts.name)}`,
    }),
    newCode,
    sendText,
    sendFile,
    setPreferredIp(ip: string) { preferredIp = ip; notify() },
    setAcceptStrangers(v: boolean) { st.acceptStrangers = v; save(); notify() },
    close() {
      clearInterval(poll)
      clearInterval(guestTimer)
      for (const s of sessions) s.ws.terminate()
      wss.close()
      https.close()
      try { bonjour?.unpublishAll(() => bonjour?.destroy()) } catch {}
    },
  }
}

export type Server = ReturnType<typeof createServer>
