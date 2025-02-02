package com.tari.android.wallet.ui.dialog.backup

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.tari.android.wallet.data.network.NetworkRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefDateTimeDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringSecuredDelegate
import org.joda.time.DateTime

class BackupSettingsRepository(
    private val context: Context,
    private val sharedPrefs: SharedPreferences,
    private val networkRepository: NetworkRepository
) {

    var lastSuccessfulBackupDate: DateTime? by SharedPrefDateTimeDelegate(sharedPrefs, formatKey(Keys.lastSuccessfulBackupDate))

    var backupFailureDate: DateTime? by SharedPrefDateTimeDelegate(sharedPrefs, formatKey(Keys.backupFailureDate))

    var scheduledBackupDate: DateTime? by SharedPrefDateTimeDelegate(sharedPrefs, formatKey(Keys.scheduledBackupDate))

    val backupIsEnabled: Boolean
        get() = (lastSuccessfulBackupDate != null)

    var backupPassword: String? by SharedPrefStringSecuredDelegate(context, sharedPrefs, formatKey(Keys.backupPassword))

    var localBackupFolderURI: Uri? by SharedPrefGsonDelegate(sharedPrefs, formatKey(Keys.localBackupFolderURI), Uri::class.java)

    var lastBackupDialogShown: DateTime? by SharedPrefDateTimeDelegate(sharedPrefs, formatKey(Keys.lastBackupDialogShownTime), null)

    fun isShowHintDialog(): Boolean = with(lastBackupDialogShown) { this == null || !this.plusMinutes(delayTimeInMinutes).isAfterNow }

    fun clear() {
        lastBackupDialogShown = null
        lastSuccessfulBackupDate = null
        backupFailureDate = null
        scheduledBackupDate = null
        backupPassword = null
        localBackupFolderURI = null
    }

    private fun formatKey(key: String): String = key + "_" + networkRepository.currentNetwork!!.network.displayName

    object Keys {
        const val lastSuccessfulBackupDate = "tari_wallet_last_successful_backup_date"
        const val backupFailureDate = "tari_wallet_backup_failure_date"
        const val scheduledBackupDate = "tari_wallet_scheduled_backup_date"
        const val backupPassword = "tari_wallet_last_next_alarm_time"
        const val localBackupFolderURI = "tari_wallet_local_backup_folder_uri"
        const val lastBackupDialogShownTime = "last_shown_time_key"
    }

    companion object {
        const val delayTimeInMinutes = 5
    }
}