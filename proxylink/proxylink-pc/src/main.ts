import { app, BrowserWindow, Menu, Tray, clipboard, dialog, ipcMain, nativeImage, shell } from 'electron'
import { join } from 'node:path'
import os from 'node:os'
import QRCode from 'qrcode'
import { createServer, Server } from './server'

if (!app.requestSingleInstanceLock()) app.quit()

let win: BrowserWindow | null = null
let tray: Tray | null = null
let quitting = false
let srv!: Server

const asset = (f: string) => join(app.getAppPath(), 'assets', f)

function show() {
  if (!win) return
  win.show()
  win.focus()
}

let qrFor = ''
let qrUrl = ''
let gFor = ''
let gUrl = ''
async function full() {
  const s = srv.state()
  if (s.pairUri !== qrFor) {
    qrFor = s.pairUri
    qrUrl = await QRCode.toDataURL(s.pairUri, { margin: 1, width: 220, color: { dark: '#040C24', light: '#FFFFFF' } })
  }
  if (s.guestUri !== gFor) {
    gFor = s.guestUri
    gUrl = await QRCode.toDataURL(s.guestUri, { margin: 1, width: 220, color: { dark: '#040C24', light: '#FFFFFF' } })
  }
  return { ...s, qr: qrUrl, guestQr: gUrl }
}

async function push() {
  if (win && !win.isDestroyed() && srv) win.webContents.send('state', await full())
}

function createWindow() {
  win = new BrowserWindow({
    width: 440,
    height: 680,
    show: false,
    backgroundColor: '#040C24',
    title: 'proxylink',
    autoHideMenuBar: true,
    icon: asset('icon.png'),
    webPreferences: { preload: join(__dirname, 'preload.js'), contextIsolation: true, sandbox: true },
  })
  win.loadFile(asset('index.html'))
  win.webContents.setWindowOpenHandler(() => ({ action: 'deny' }))
  win.webContents.on('will-navigate', e => e.preventDefault())
  win.once('ready-to-show', show)
  win.on('close', e => {
    if (!quitting) { e.preventDefault(); win?.hide() }
  })
}

async function pickFile() {
  const r = await dialog.showOpenDialog({ properties: ['openFile', 'multiSelections'] })
  if (!r.canceled) r.filePaths.forEach(p => srv.sendFile(p))
}

function menu() {
  return Menu.buildFromTemplate([
    { label: 'open proxylink', click: show },
    { label: 'send file...', click: () => { void pickFile() } },
    { label: 'new pairing code', click: () => srv.newCode() },
    {
      label: 'allow unpaired devices',
      type: 'checkbox',
      checked: srv.state().acceptStrangers,
      click: i => srv.setAcceptStrangers(i.checked),
    },
    {
      label: 'start with system',
      type: 'checkbox',
      checked: app.getLoginItemSettings().openAtLogin,
      click: i => app.setLoginItemSettings({ openAtLogin: i.checked }),
    },
    { label: 'open downloads folder', click: () => { void shell.openPath(srv.state().downloadDir) } },
    { type: 'separator' },
    { label: 'quit', click: () => { quitting = true; app.quit() } },
  ])
}

app.on('second-instance', show)
app.on('activate', show)
app.on('window-all-closed', () => {})
app.on('before-quit', () => { quitting = true; srv?.close() })

app.whenReady().then(() => {
  srv = createServer({
    name: os.hostname(),
    dataDir: app.getPath('userData'),
    downloadDir: join(app.getPath('downloads'), 'proxylink'),
    clip: { read: async () => clipboard.readText(), write: async t => clipboard.writeText(t) },
    ask: async q => {
      const r = await dialog.showMessageBox({
        type: 'question', buttons: ['accept', 'deny'], defaultId: 1, cancelId: 1, title: 'proxylink', message: q,
      })
      return r.response === 0
    },
    log: m => console.log(m),
    onChange: () => { void push() },
  })

  createWindow()

  tray = new Tray(nativeImage.createFromPath(asset('icon.png')).resize({ width: 18, height: 18 }))
  tray.setToolTip('proxylink')
  tray.setContextMenu(menu())
  tray.on('click', show)

  ipcMain.handle('get', () => full())
  ipcMain.handle('newCode', () => srv.newCode())
  ipcMain.handle('sendText', (_e, t: unknown) => {
    if (typeof t === 'string' && t.trim()) srv.sendText(t.trim())
  })
  ipcMain.handle('sendFile', () => pickFile())
  ipcMain.handle('strangers', (_e, v: unknown) => {
    srv.setAcceptStrangers(!!v)
    tray?.setContextMenu(menu())
  })
  ipcMain.handle('setIp', (_e, ip: unknown) => srv.setPreferredIp(typeof ip === 'string' ? ip : ''))
  ipcMain.handle('openFolder', () => shell.openPath(srv.state().downloadDir))
})
