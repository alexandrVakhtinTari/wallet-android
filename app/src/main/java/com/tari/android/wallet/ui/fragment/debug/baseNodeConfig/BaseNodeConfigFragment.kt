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
package com.tari.android.wallet.ui.fragment.debug.baseNodeConfig

import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.white
import com.tari.android.wallet.R.drawable.base_node_config_edit_text_bg
import com.tari.android.wallet.R.drawable.base_node_config_edit_text_invalid_bg
import com.tari.android.wallet.application.WalletManager
import com.tari.android.wallet.application.baseNodes.BaseNodes
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.databinding.FragmentBaseNodeConfigBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ffi.FFIPublicKey
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.model.BaseNodeValidationResult
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.validator.BaseNodeAddressValidator
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.validator.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Base node configuration debug fragment.
 * If you reach this fragment with valid base node data in the clipboard in format
 * PUBLIC_KEY_HEX::ADDRESS then the fragment will split the clipboard data and paste the values
 * to input fields.
 *
 * @author The Tari Development Team
 */

//todo replaced buttons
//todo extract baseNodeRepo
//todo extract onionValidator
internal class BaseNodeConfigFragment : CommonFragment<FragmentBaseNodeConfigBinding, BaseNodeConfigViewModel>() {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var walletManager: WalletManager

    @Inject
    lateinit var baseNodes: BaseNodes

    private lateinit var walletService: TariWalletService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentBaseNodeConfigBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appComponent.inject(this)
        val viewModel: BaseNodeConfigViewModel by viewModels()
        bindViewModel(viewModel)
        setupUI()
        observeUI()
    }

    override fun onStart() {
        super.onStart()
        checkClipboardForValidBaseNodeData()
    }

    private fun setupUI() = with(ui) {
        progressBar.setColor(color(white))
        updateCurrentBaseNode()
        progressBar.gone()
        invalidPublicKeyHexTextView.invisible()
        invalidAddressTextView.invisible()
        resetButton.setOnClickListener { resetButtonClicked(it) }
        saveButton.setOnClickListener { saveButtonClicked(it) }
        publicKeyHexEditText.addTextChangedListener(onTextChanged = { _, _, _, _ -> onPublicKeyHexChanged() })
        addressEditText.addTextChangedListener(onTextChanged = { _, _, _, _ -> onAddressChanged() })
    }

    private fun observeUI() = with(viewModel) {

        observe(publicKeyHexValidation) {
            ui.publicKeyHexEditText.background = getValidBg(it == Validator.State.Invalid)
            ui.invalidPublicKeyHexTextView.setVisible(it == Validator.State.Invalid, View.INVISIBLE)
        }

        observe(publicKeyHexValidation) {
            ui.addressEditText.background = getValidBg(it == Validator.State.Invalid)
            ui.invalidAddressTextView.setVisible(it == Validator.State.Invalid, View.INVISIBLE)
        }
    }

    private fun getValidBg(isInvalid : Boolean) = drawable(if (isInvalid) base_node_config_edit_text_invalid_bg else base_node_config_edit_text_bg)

    private fun updateCurrentBaseNode() {
        val syncSuccessful = sharedPrefsWrapper.baseNodeLastSyncResult
        ui.syncStatusTextView.text = when (syncSuccessful) {
            null -> string(R.string.debug_base_node_syncing)
            BaseNodeValidationResult.SUCCESS -> string(R.string.debug_base_node_sync_successful)
            else -> string(R.string.debug_base_node_sync_failed)
        }
        if (sharedPrefsWrapper.baseNodeIsUserCustom) {
            ui.nameTextView.text = string(R.string.debug_base_node_custom)
            ui.resetButton.visible()
        } else {
            ui.nameTextView.text = sharedPrefsWrapper.baseNodeName
            ui.resetButton.gone()
        }
        ui.publicKeyHexTextView.text = sharedPrefsWrapper.baseNodePublicKeyHex
        ui.addressTextView.text = sharedPrefsWrapper.baseNodeAddress
    }

    private fun onPublicKeyHexChanged() {
        ui.publicKeyHexEditText.background = drawable(base_node_config_edit_text_bg)
        ui.invalidPublicKeyHexTextView.invisible()
    }

    private fun onAddressChanged() {
        ui.addressEditText.background = drawable(base_node_config_edit_text_bg)
        ui.invalidAddressTextView.invisible()
    }

    /**
     * Checks whether a the public key and address are in the clipboard in the expected format.
     */
    private fun checkClipboardForValidBaseNodeData() {
        val clipboardManager = (activity?.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager) ?: return
        val clipboardString = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: return
        // if clipboard contains at least 1 emoji, then display paste emoji banner
        if (BaseNodeAddressValidator().validate(clipboardString) == Validator.State.Valid) {
            val input = clipboardString.split("::")
            ui.publicKeyHexEditText.setText(input[0])
            ui.addressEditText.setText(input[1])
        }
    }

    private fun resetButtonClicked(view: View) {
        view.temporarilyDisableClick()
        sharedPrefsWrapper.baseNodeIsUserCustom = false
        sharedPrefsWrapper.baseNodeLastSyncResult = null
        lifecycleScope.launch(Dispatchers.IO) {
            baseNodes.setNextBaseNode()
            walletService.startBaseNodeSync(WalletError())
            withContext(Dispatchers.Main) {
                updateCurrentBaseNode()
            }
        }
    }

    private fun saveButtonClicked(view: View) {
        view.temporarilyDisableClick()
        if (!validate()) return
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

    private suspend fun addCustomBaseNodePeer(publicKeyHex: String, address: String) {
        val baseNodeKeyFFI = FFIPublicKey(HexString(publicKeyHex))
        val success = try {
            val wallet = FFIWallet.instance!!
            wallet.addBaseNodePeer(baseNodeKeyFFI, address)
            true
        } catch (exception: Exception) {
            false
        }
        baseNodeKeyFFI.destroy()
        if (success) {
            withContext(Dispatchers.Main) {
                addBaseNodePeerSuccessful(publicKeyHex, address)
            }
        } else {
            baseNodes.setNextBaseNode()
            walletService.startBaseNodeSync(WalletError())
            withContext(Dispatchers.Main) {
                addBaseNodePeerFailed()
            }
        }
    }

    private fun addBaseNodePeerSuccessful(publicKeyHex: String, address: String) {
        val mActivity = activity ?: return
        // clear input
        ui.publicKeyHexEditText.setText("")
        ui.addressEditText.setText("")
        // update UI
        ui.publicKeyHexTextView.text = publicKeyHex
        ui.addressTextView.text = address
        // update app-wide variables
        sharedPrefsWrapper.baseNodePublicKeyHex = publicKeyHex
        sharedPrefsWrapper.baseNodeAddress = address
        // UI
        ui.saveButton.visible()
        ui.progressBar.gone()
        // show toast
        Toast.makeText(
            mActivity,
            R.string.debug_edit_base_node_successful,
            Toast.LENGTH_LONG
        ).show()
        updateCurrentBaseNode()
    }

    private fun addBaseNodePeerFailed() {
        val mActivity = activity ?: return
        // UI update
        ui.saveButton.visible()
        ui.progressBar.gone()
        // show toast
        Toast.makeText(
            mActivity,
            R.string.debug_edit_base_node_failed,
            Toast.LENGTH_LONG
        ).show()
        updateCurrentBaseNode()
    }
}