package app.proxylink

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

data class transfer(
    val id: Long,
    val name: String,
    val incoming: Boolean,
    val size: Long,
    val done: Long,
    val state: String,
)

object transfers {
    private val _list = MutableStateFlow<List<transfer>>(emptyList())
    val list: StateFlow<List<transfer>> = _list.asStateFlow()

    private val counter = AtomicLong(1)
    private val cancelled = ConcurrentHashMap.newKeySet<Long>()
    private val lastreport = ConcurrentHashMap<Long, Long>()

    fun start(name: String, incoming: Boolean, size: Long): Long {
        val id = counter.getAndIncrement()
        _list.update { (listOf(transfer(id, name, incoming, size, 0L, "running")) + it).take(20) }
        return id
    }

    fun progress(id: Long, done: Long) {
        val last = lastreport[id] ?: 0L
        if (done - last < 128 * 1024) return
        lastreport[id] = done
        _list.update { l -> l.map { if (it.id == id) it.copy(done = done) else it } }
    }

    fun finish(id: Long, state: String) {
        lastreport.remove(id)
        cancelled.remove(id)
        _list.update { l -> l.map { if (it.id == id) it.copy(state = state, done = if (state == "done") it.size.coerceAtLeast(it.done) else it.done) else it } }
    }

    fun cancel(id: Long) {
        cancelled.add(id)
    }

    fun canceled(id: Long): Boolean = cancelled.contains(id)

    fun clearfinished() {
        _list.update { l -> l.filter { it.state == "running" } }
    }
}
