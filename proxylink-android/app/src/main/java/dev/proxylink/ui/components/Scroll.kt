package dev.proxylink.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Modifier.verticalScrollCompat(): Modifier = this.verticalScroll(rememberScrollState())

@Composable
fun Modifier.horizontalScrollCompat(): Modifier = this.horizontalScroll(rememberScrollState())
