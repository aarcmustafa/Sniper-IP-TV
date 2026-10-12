package com.stitten.stitteniptv.player

/**
 * الحالات الموحدة للمشغل بغض النظر عن المحرك المستخدم.
 */
sealed class PlayerState {
    object Idle : PlayerState()
    object Loading : PlayerState()
    object Playing : PlayerState()
    object Paused : PlayerState()
    object Ended : PlayerState()
    object Error : PlayerState()
    data class Buffering(val percent: Float) : PlayerState()
}
