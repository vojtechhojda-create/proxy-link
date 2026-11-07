package dev.proxylink.ui.components

fun fmtSize(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1_048_576 -> "%.1f KB".format(b / 1024.0)
    b < 1_073_741_824 -> "%.1f MB".format(b / 1_048_576.0)
    else -> "%.2f GB".format(b / 1_073_741_824.0)
}
