package com.tari.android.wallet.ui.fragment.restore.chooseRestoreOption

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.application.WalletManager
import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ffi.FFIException
import com.tari.android.wallet.infrastructure.backup.*
import com.tari.android.wallet.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

internal class ChooseRestoreOptionViewModel : CommonViewModel() {

    @Inject
    lateinit var backupStorage: BackupStorage

    @Inject
    lateinit var walletManager: WalletManager

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    init {
        component?.inject(this)
    }

    private val _state = SingleLiveEvent<ChooseRestoreOptionState>()
    val state: LiveData<ChooseRestoreOptionState> = _state

    private val _navigation = SingleLiveEvent<ChooseRestoreOptionNavigation>()
    val navigation: LiveData<ChooseRestoreOptionNavigation> = _navigation

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                backupStorage.onSetupActivityResult(requestCode, resultCode, data)
                restoreFromBackup()
            } catch (exception: Exception) {
                Logger.e("Backup storage setup failed: $exception")
                backupStorage.signOut()
                _state.postValue(ChooseRestoreOptionState.EndProgress)
                showAuthFailedDialog()
            }
        }
    }

    private suspend fun restoreFromBackup() {
        try {
            // try to restore with no password
            backupStorage.restoreLatestBackup()
            EventBus.walletState.subscribe(this) {
                when (it) {
                    WalletState.Initializing,
                    WalletState.NotReady -> Unit
                    WalletState.Running -> _navigation.postValue(ChooseRestoreOptionNavigation.ToRestoreInProgress)
                    is WalletState.Failed -> viewModelScope.launch(Dispatchers.IO) {
                        handleException(WalletStartFailedException(it.exception))
                    }
                }
            }
            viewModelScope.launch(Dispatchers.Main) {
                walletServiceLauncher.start()
            }
        } catch (exception: Exception) {
            handleException(exception)
        }
    }

    private suspend fun handleException(exception: java.lang.Exception) {
        when (exception) {
            is BackupStorageAuthRevokedException -> {
                Logger.e(exception, "Auth revoked.")
                backupStorage.signOut()
                showAuthFailedDialog()
            }
            is BackupStorageTamperedException -> { // backup file not found
                Logger.e(exception, "Backup file not found.")
                backupStorage.signOut()
                showBackupFileNotFoundDialog()
            }
            is BackupFileIsEncryptedException -> {
                //todo check for launch wallet after relaunch app
                _navigation.postValue(ChooseRestoreOptionNavigation.ToEnterRestorePassword)
            }
            is WalletStartFailedException -> {
                Logger.e(exception, "Restore failed: wallet start failed")
                viewModelScope.launch(Dispatchers.Main) {
                    walletServiceLauncher.stopAndDelete()
                }
                val cause = exception.cause
                if (cause is FFIException && cause.error?.code == 114) {
                    showRestoreFailedDialog(resourceManager.getString(R.string.restore_wallet_error_file_not_supported))
                } else {
                    showRestoreFailedDialog(cause?.message)
                }
            }
            is IOException -> {
                Logger.e(exception, "Restore failed: network connection.")
                backupStorage.signOut()
                showRestoreFailedDialog(resourceManager.getString(R.string.error_no_connection_title))
            }
            else -> {
                Logger.e(exception, "Restore failed: $exception")
                backupStorage.signOut()
                showRestoreFailedDialog(exception.message)
            }
        }

        _state.postValue(ChooseRestoreOptionState.EndProgress)
    }

    private fun showBackupFileNotFoundDialog() {
        val args = ErrorDialogArgs(
            resourceManager.getString(R.string.restore_wallet_error_title),
            resourceManager.getString(R.string.restore_wallet_error_file_not_found),
            onClose = { _backPressed.call() })
        _errorDialag.postValue(args)
    }

    private fun showRestoreFailedDialog(message: String? = null) {
        val args = ErrorDialogArgs(
            resourceManager.getString(R.string.restore_wallet_error_title),
            message ?: resourceManager.getString(R.string.restore_wallet_error_desc)
        )
        _errorDialag.postValue(args)
    }

    private fun showAuthFailedDialog() {
        val args = ErrorDialogArgs(
            resourceManager.getString(R.string.restore_wallet_error_title),
            resourceManager.getString(R.string.back_up_wallet_storage_setup_error_desc)
        )
        _errorDialag.postValue(args)
    }
}