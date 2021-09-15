package com.tari.android.wallet.ui.fragment.debug.baseNodeConfig

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.application.WalletManager
import com.tari.android.wallet.application.baseNodes.BaseNodes
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.loadingButton.LoadingButtonState
import com.tari.android.wallet.ui.extension.invisible
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.validator.OnionAddressValidator
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.validator.PublicHexValidator
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.validator.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class BaseNodeConfigViewModel : CommonViewModel() {
    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var walletManager: WalletManager

    @Inject
    lateinit var baseNodes: BaseNodes

    val serviceConnection = TariWalletServiceConnection()
    val walletService
        get() = serviceConnection.currentState.service!!

    private val publicHexValidator = PublicHexValidator()
    private val onionValidator = OnionAddressValidator()

    private val _onionAddressText = MutableLiveData<String>()
    val onionAddressText: LiveData<String> = _onionAddressText

    private val _publicHexText = MutableLiveData<String>()
    val publicHexText : LiveData<String> = _publicHexText

    private val _publicKeyHexValidationState = MutableLiveData<Validator.State>(Validator.State.Neutral)
    val publicKeyHexValidation: LiveData<Validator.State> = _publicKeyHexValidationState

    private val _addressValidationState = MutableLiveData<Validator.State>(Validator.State.Neutral)
    val addressValidationState: LiveData<Validator.State> = _addressValidationState

    private val _saveButtonState = MutableLiveData<LoadingButtonState>()
    val saveButtonState: LiveData<LoadingButtonState> = _saveButtonState

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

    private fun onServiceConnected() {
        Logger.i("onServiceConnected")
    }

    private fun onServiceDisconnected() {
        Logger.i("AddAmountFragment onServiceDisconnected")
    }

    private fun updateCurrentBaseNode() {

    }

    fun save() {
        validate()

        if (addressValidationState.value!! != Validator.State.Valid ||
            addressValidationState.value!! != Validator.State.Valid) return
        val publicKeyHex = ui.publicKeyHexEditText.editableText.toString()
        val address = ui.addressEditText.editableText.toString()
        sharedPrefsWrapper.baseNodeIsUserCustom = true
        sharedPrefsWrapper.baseNodeLastSyncResult = null
        sharedPrefsWrapper.baseNodeName = null
        sharedPrefsWrapper.baseNodePublicKeyHex = publicKeyHex
        sharedPrefsWrapper.baseNodeAddress = address
        ui.saveButton.invisible()
        ui.progressBar.visible()
        lifecycleScope.launch(Dispatchers.IO) {
            addCustomBaseNodePeer(publicKeyHex, address)
        }
    }

    fun validate() {
        _addressValidationState.postValue(onionValidator.validate(_onionAddressText.value!!))
        _publicKeyHexValidationState.postValue(publicHexValidator.validate(publicHexText.value!!))
    }
}