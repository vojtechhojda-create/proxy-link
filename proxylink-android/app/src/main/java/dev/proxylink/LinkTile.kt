package dev.proxylink

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState

class LinkTile : TileService() {
    private val listener: (LinkState) -> Unit = { refresh() }

    override fun onStartListening() {
        Link.init(this)
        Link.addListener(listener)
        refresh()
    }

    override fun onStopListening() {
        Link.removeListener(listener)
    }

    override fun onClick() {
        Link.init(this)
        // the background connection lives in the foreground service, without it the process can die
        // while the phone is still searching for the pc. the start can be refused by the system, the tile must not crash
        runCatching { startForegroundService(Intent(this, LinkService::class.java)) }
        Link.toggle()
        refresh()
    }

    // state comes from the real link state. "searching" means the toggle is on but no device is connected yet
    private fun refresh() {
        val t = qsTile ?: return
        val (state, sub) = when {
            Link.state == LinkState.CONNECTED -> Tile.STATE_ACTIVE to Link.pcName
            Link.state == LinkState.CONNECTING -> Tile.STATE_ACTIVE to "connecting"
            Link.reconnecting != null -> Tile.STATE_ACTIVE to "searching"
            Link.state == LinkState.NEEDS_PAIR -> Tile.STATE_INACTIVE to "pair in the app"
            Link.known.isEmpty() -> Tile.STATE_INACTIVE to "no paired device"
            else -> Tile.STATE_INACTIVE to "off"
        }
        t.state = state
        t.label = "proxylink"
        t.subtitle = sub
        t.updateTile()
    }
}
