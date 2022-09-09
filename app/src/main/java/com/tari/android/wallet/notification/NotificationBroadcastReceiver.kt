/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.orhanobut.logger.Logger
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.model.recovery.WalletRestorationResult
import com.tari.android.wallet.ui.activity.SplashActivity
import com.tari.android.wallet.ui.activity.home.HomeActivity
import com.tari.android.wallet.ui.fragment.restore.restore.WalletRestoreActivity

class NotificationBroadcastReceiver : BroadcastReceiver() {

    private val logger
        get() = Logger.t(NotificationBroadcastReceiver::class.simpleName)

    override fun onReceive(context: Context, intent: Intent) {
        logger.d("NotificationBroadcastReceiver received")
        val restorationState = EventBus.walletRestorationState.publishSubject.value
        val newIntent: Intent = if (restorationState != null && restorationState !is WalletRestorationResult.Completed) {
            Intent(context, WalletRestoreActivity::class.java)
        } else {
            if (HomeActivity.instance.get() != null) {
                Intent(context, HomeActivity::class.java)
            } else {
                Intent(context, SplashActivity::class.java)
            }
        }
        context.startActivity(newIntent.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.extras?.let { putExtras(it) }
        })
    }
}