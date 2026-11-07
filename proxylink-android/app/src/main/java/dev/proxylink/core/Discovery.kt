package dev.proxylink.core

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import java.net.Inet4Address

class Discovery(ctx: Context) {
    private val nsd = ctx.getSystemService(NsdManager::class.java)
    private val lock = ctx.getSystemService(WifiManager::class.java)
        .createMulticastLock("proxylink").apply { setReferenceCounted(false) }
    private val queue = ArrayDeque<NsdServiceInfo>()
    private var busy = false
    private lateinit var found: (Peer) -> Unit

    fun start(onFound: (Peer) -> Unit, onLost: (String) -> Unit) {
        found = onFound
        lock.acquire()
        nsd.discoverServices("_proxylink._tcp.", NsdManager.PROTOCOL_DNS_SD, object : NsdManager.DiscoveryListener {
            override fun onServiceFound(s: NsdServiceInfo) { synchronized(queue) { queue.add(s) }; next() }
            override fun onServiceLost(s: NsdServiceInfo) = onLost(s.serviceName)
            override fun onDiscoveryStarted(t: String) {}
            override fun onDiscoveryStopped(t: String) {}
            override fun onStartDiscoveryFailed(t: String, e: Int) {}
            override fun onStopDiscoveryFailed(t: String, e: Int) {}
        })
    }

    // nsd allows one resolve at a time
    private fun next() {
        val s = synchronized(queue) {
            if (busy) null else queue.removeFirstOrNull()?.also { busy = true }
        } ?: return
        nsd.resolveService(s, object : NsdManager.ResolveListener {
            override fun onResolveFailed(i: NsdServiceInfo, e: Int) { done() }
            override fun onServiceResolved(i: NsdServiceInfo) {
                val ip = (i.host as? Inet4Address)?.hostAddress
                if (ip != null) {
                    val a = i.attributes
                    found(Peer(
                        key = i.serviceName,
                        id = a["id"]?.decodeToString() ?: "",
                        name = a["name"]?.decodeToString() ?: i.serviceName,
                        host = ip,
                        port = i.port,
                        fp = a["fp"]?.decodeToString() ?: "",
                    ))
                }
                done()
            }
        })
    }

    private fun done() { synchronized(queue) { busy = false }; next() }
}
