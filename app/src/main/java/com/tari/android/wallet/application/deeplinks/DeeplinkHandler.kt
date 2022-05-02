package com.tari.android.wallet.application.deeplinks

import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository

class DeeplinkHandler(private val networkRepository: NetworkRepository) {

    private val oldDeeplinkFormatter = OldDeeplinkFormatter()
    private val deeplinkFormatter = DeeplinkFormatter(networkRepository)

    fun handle(deepLink: String): DeepLink? = oldDeeplinkFormatter.from(networkRepository, deepLink) ?: deeplinkFormatter.parse(deepLink)

    fun getDeeplink(deeplink: DeepLink): String = deeplinkFormatter.toDeeplink(deeplink)
}