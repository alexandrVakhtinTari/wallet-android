package com.tari.android.wallet.ui.fragment.debug.baseNodeConfig

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.application.WalletManager
import com.tari.android.wallet.application.baseNodes.BaseNodes
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.model.BaseNodeValidationResult
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import javax.inject.Inject

internal class BaseNodeConfigViewModel : CommonViewModel() {

    @Inject
    lateinit var baseNodeSharedRepository: BaseNodeSharedRepository

    @Inject
    lateinit var walletManager: WalletManager

    @Inject
    lateinit var baseNodes: BaseNodes

    val serviceConnection = TariWalletServiceConnection()
    val walletService
        get() = serviceConnection.currentState.service!!

    private val _navigation = SingleLiveEvent<BaseNodeConfigNavigation>()
    val navigation: LiveData<BaseNodeConfigNavigation> = _navigation

    private val _currentBaseNode = MutableLiveData<BaseNodeDto>()
    val currentBaseNode: LiveData<BaseNodeDto> = _currentBaseNode

    private val _syncStatus = MutableLiveData<String>()
    val syncStatus: LiveData<String> = _syncStatus

    init {
        component?.inject(this)

        EventBus.baseNodeState.subscribe(this) { updateCurrentBaseNode() }

        serviceConnection.connection.subscribe {
            when (it.status) {
                TariWalletServiceConnection.ServiceConnectionStatus.CONNECTED -> onServiceConnected()
                TariWalletServiceConnection.ServiceConnectionStatus.DISCONNECTED -> onServiceDisconnected()
                else -> Unit
            }
        }.addTo(compositeDisposable)
    }

    fun navigateToAdd() = _navigation.postValue(BaseNodeConfigNavigation.ToAddCustomBaseNode)

    fun navigateToChange() = _navigation.postValue(BaseNodeConfigNavigation.ToChangeBaseNode)

    private fun onServiceConnected() = Logger.i("onServiceConnected")

    private fun onServiceDisconnected() = Logger.i("AddAmountFragment onServiceDisconnected")

    private fun updateCurrentBaseNode() {
        val syncStatus = when (baseNodeSharedRepository.baseNodeLastSyncResult) {
            null -> resourceManager.getString(R.string.debug_base_node_syncing)
            BaseNodeValidationResult.SUCCESS -> resourceManager.getString(R.string.debug_base_node_sync_successful)
            else -> resourceManager.getString(R.string.debug_base_node_sync_failed)
        }
        _syncStatus.postValue(syncStatus)
        _currentBaseNode.postValue(baseNodeSharedRepository.currentBaseNode)
    }
}