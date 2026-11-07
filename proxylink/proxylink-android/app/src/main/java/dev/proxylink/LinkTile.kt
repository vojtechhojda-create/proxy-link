package dev.proxylink

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
        Link.toggle()
        refresh()
    }

    private fun refresh() {
        val t = qsTile ?: return
        val on = Link.state == LinkState.CONNECTED
        t.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        t.label = "proxylink"
        t.subtitle = if (on) Link.pcName else "off"
        t.updateTile()
    }
}
