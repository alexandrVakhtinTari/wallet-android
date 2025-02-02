package com.tari.android.wallet.yat

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import com.orhanobut.logger.Logger
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.ui.fragment.send.common.TransactionData
import com.tari.android.wallet.ui.fragment.send.finalize.FinalizeSendTxViewModel
import com.tari.android.wallet.ui.fragment.send.finalize.YatFinalizeSendTxActivity
import yat.android.api.lookup.LookupEmojiIdWithSymbolResponse
import yat.android.data.YatRecord
import yat.android.data.YatRecordType
import yat.android.lib.YatConfiguration
import yat.android.lib.YatIntegration
import yat.android.ui.transactions.outcoming.YatLibOutcomingTransactionData
import java.io.Serializable

class YatAdapter(
    private val yatSharedRepository: YatSharedRepository,
    private val commonRepository: SharedPrefsRepository
) : YatIntegration.Delegate {
    fun initYat() {
        val config = YatConfiguration(BuildConfig.YAT_ORGANIZATION_RETURN_URL, BuildConfig.YAT_ORGANIZATION_NAME, BuildConfig.YAT_ORGANIZATION_KEY)
        YatIntegration.setup(config, YatIntegration.ColorMode.LIGHT, this)
    }

    suspend fun searchYats(query: String) : LookupEmojiIdWithSymbolResponse = YatIntegration.yatApi.lookupEmojiIdWithSymbol(query, "XTR")

    fun openOnboarding(context: Context) {
        val address = commonRepository.publicKeyHexString.orEmpty()
        YatIntegration.showOnboarding(context, listOf(YatRecord(YatRecordType.TARI_PUBKEY, data = address)))
    }

    fun showOutcomingFinalizeActivity(activity: Activity, transactionData: TransactionData) {
        val yatUser = transactionData.recipientUser as YatUser
        val data = YatLibOutcomingTransactionData(transactionData.amount!!.tariValue.toDouble(), "Tari", yatUser.yat)

        val intent = Intent(activity, YatFinalizeSendTxActivity::class.java)
        intent.putExtra("YatLibDataKey", data)
        intent.putExtra(FinalizeSendTxViewModel.transactionDataKey, transactionData as Serializable)
        intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
        activity.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity).toBundle())
    }

    override fun onYatIntegrationComplete(yat: String) {
        Logger.d("Yat integration completed.")
        yatSharedRepository.saveYat(yat)
    }

    override fun onYatIntegrationFailed(failureType: YatIntegration.FailureType) {
        Logger.d("Yat integration failed.$failureType")
    }
}