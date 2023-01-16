package com.programmersbox.common

import androidx.compose.runtime.Composable

public actual fun getPlatformName(): String {
    return "JavaScript"
}

@Composable
public fun UIShow() {
    App()
}