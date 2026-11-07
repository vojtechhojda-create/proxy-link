import { contextBridge, ipcRenderer } from 'electron'

contextBridge.exposeInMainWorld('pl', {
  get: () => ipcRenderer.invoke('get'),
  onState: (cb: (s: unknown) => void) => { ipcRenderer.on('state', (_e, s) => cb(s)) },
  newCode: () => ipcRenderer.invoke('newCode'),
  sendText: (t: string) => ipcRenderer.invoke('sendText', t),
  sendFile: () => ipcRenderer.invoke('sendFile'),
  strangers: (v: boolean) => ipcRenderer.invoke('strangers', v),
  setIp: (ip: string) => ipcRenderer.invoke('setIp', ip),
  openFolder: () => ipcRenderer.invoke('openFolder'),
})
