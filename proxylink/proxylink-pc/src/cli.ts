import clipboardy from 'clipboardy'
import { createInterface } from 'node:readline'
import { join } from 'node:path'
import os from 'node:os'
import { createServer } from './server'

const NAME = process.env.PL_NAME ?? os.hostname()
let answer: ((v: string) => void) | null = null
let lastCode = ''

const srv = createServer({
  name: NAME,
  dataDir: process.cwd(),
  downloadDir: join(process.cwd(), 'received'),
  clip: { read: () => clipboardy.read(), write: t => clipboardy.write(t) },
  ask: q => new Promise<boolean>(r => {
    console.log(`${q} - accept? (y/n)`)
    answer = v => r(v.trim().toLowerCase() === 'y')
  }),
  log: m => console.log(m),
  onChange: () => {
    const c = srv.state().code
    if (c !== lastCode) { lastCode = c; console.log(`pairing code: ${c}`) }
  },
})

lastCode = srv.state().code
console.log(`proxylink pc "${NAME}" listening on :8765`)
console.log(`pairing code: ${lastCode}`)
console.log(`certificate fingerprint: ${srv.state().fingerprint}`)
console.log('commands: s <text|link> | f <path> | l | c (new code)')

const rl = createInterface({ input: process.stdin })
rl.on('line', l => {
  if (answer) { const a = answer; answer = null; return a(l) }
  const [c, ...r] = l.trim().split(' ')
  const arg = r.join(' ')
  if (c === 's' && arg) srv.sendText(arg)
  else if (c === 'f' && arg) srv.sendFile(arg.replace(/^"|"$/g, ''))
  else if (c === 'l') console.log(srv.state().devices.map(d => `${d.name} ${d.trusted ? 'paired' : 'unpaired'}`).join('\n') || 'no devices')
  else if (c === 'c') srv.newCode()
})
