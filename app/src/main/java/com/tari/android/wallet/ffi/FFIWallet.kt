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
package com.tari.android.wallet.ffi

import com.orhanobut.logger.Logger
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.model.*
import com.tari.android.wallet.model.recovery.WalletRestorationResult
import com.tari.android.wallet.service.seedPhrase.SeedPhraseRepository
import com.tari.android.wallet.util.Constants
import io.sentry.Sentry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Wallet wrapper.
 *
 * @author The Tari Development Team
 */

internal class FFIWallet(
    val sharedPrefsRepository: SharedPrefsRepository,
    val seedPhraseRepository: SeedPhraseRepository,
    commsConfig: FFICommsConfig,
    logPath: String
) : FFIBase() {

    private var balance: BalanceInfo = BalanceInfo()

    companion object {
        private var atomicInstance = AtomicReference<FFIWallet>()
        var instance: FFIWallet?
            get() = atomicInstance.get()
            set(value) = atomicInstance.set(value)
    }

    // region JNI

    private external fun jniCreate(
        commsConfig: FFICommsConfig,
        logPath: String,
        maxNumberOfRollingLogFiles: Int,
        rollingLogFileMaxSizeBytes: Int,
        passphrase: String?,
        seedWords: FFISeedWords?,
        callbackReceivedTx: String,
        callbackReceivedTxSig: String,
        callbackReceivedTxReply: String,
        callbackReceivedTxReplySig: String,
        callbackReceivedFinalizedTx: String,
        callbackReceivedFinalizedTxSig: String,
        callbackTxBroadcast: String,
        callbackTxBroadcastSig: String,
        callbackTxMined: String,
        callbackTxMinedSig: String,
        callbackTxFauxConfirmed: String,
        callbackTxFauxConfirmedSig: String,
        callbackTxFauxUnconfirmed: String,
        callbackTxFauxUnconfirmedSig: String,
        callbackTxMinedUnconfirmed: String,
        callbackTxMinedUnconfirmedSig: String,
        callbackDirectSendResult: String,
        callbackDirectSendResultSig: String,
        callbackStoreAndForwardSendResult: String,
        callbackStoreAndForwardSendResultSig: String,
        callbackTxCancellation: String,
        callbackTxCancellationSig: String,
        callbackTXOValidationComplete: String,
        callbackTXOValidationCompleteSig: String,
        callbackContactsLivenessDataUpdated: String,
        callbackContactsLivenessDataUpdatedSig: String,
        callbackBalanceUpdated: String,
        callbackBalanceUpdatedSig: String,
        callbackTransactionValidationComplete: String,
        callbackTransactionValidationCompleteSig: String,
        callbackConnectivityStatus: String,
        callbackConnectivityStatusSig: String,
        libError: FFIError
    )

    private external fun jniGetBalance(libError: FFIError): FFIPointer

    private external fun jniLogMessage(message: String, libError: FFIError)

    private external fun jniGetPublicKey(libError: FFIError): FFIPointer

    private external fun jniGetContacts(libError: FFIError): FFIPointer

    private external fun jniAddUpdateContact(contactPtr: FFIContact, libError: FFIError): Boolean

    private external fun jniRemoveContact(contactPtr: FFIContact, libError: FFIError): Boolean

    private external fun jniGetCompletedTxs(libError: FFIError): FFIPointer

    private external fun jniGetCancelledTxs(libError: FFIError): FFIPointer

    private external fun jniGetCompletedTxById(id: String, libError: FFIError): FFIPointer

    private external fun jniGetCancelledTxById(id: String, libError: FFIError): FFIPointer

    private external fun jniGetPendingOutboundTxs(libError: FFIError): FFIPointer

    private external fun jniGetPendingOutboundTxById(id: String, libError: FFIError): FFIPointer

    private external fun jniGetPendingInboundTxs(libError: FFIError): FFIPointer

    private external fun jniGetPendingInboundTxById(id: String, libError: FFIError): FFIPointer

    private external fun jniCancelPendingTx(id: String, libError: FFIError): Boolean

    private external fun jniSendTx(
        publicKeyPtr: FFIPublicKey,
        amount: String,
        feePerGram: String,
        message: String,
        oneSided: Boolean,
        libError: FFIError
    ): ByteArray

    private external fun jniCoinSplit(
        amount: String,
        splitCount: String,
        fee: String,
        message: String,
        lockHeight: String,
        libError: FFIError
    ): ByteArray

    private external fun jniSignMessage(message: String, libError: FFIError): String

    private external fun jniVerifyMessageSignature(publicKeyPtr: FFIPublicKey, message: String, signature: String, libError: FFIError): Boolean

    private external fun jniImportUTXO(
        amount: String,
        spendingKey: FFIPrivateKey,
        sourcePublicKey: FFIPublicKey,
        outputFeatures: FFIOutputFeatures,
        tariCommitmentSignature: FFITariCommitmentSignature,
        sourceSenderPublicKey: FFIPublicKey,
        scriptPrivateKey: FFIPrivateKey,
        message: String,
        libError: FFIError
    ): ByteArray

    private external fun jniAddBaseNodePeer(publicKey: FFIPublicKey, address: String, libError: FFIError): Boolean

    private external fun jniStartTXOValidation(libError: FFIError): ByteArray

    private external fun jniStartTxValidation(libError: FFIError): ByteArray

    private external fun jniRestartTxBroadcast(libError: FFIError): ByteArray

    private external fun jniPowerModeNormal(libError: FFIError)

    private external fun jniPowerModeLow(libError: FFIError)

    private external fun jniGetSeedWords(libError: FFIError): FFIPointer

    private external fun jniSetKeyValue(key: String, value: String, libError: FFIError): Boolean

    private external fun jniGetKeyValue(key: String, libError: FFIError): String

    private external fun jniRemoveKeyValue(key: String, libError: FFIError): Boolean

    private external fun jniGetConfirmations(libError: FFIError): ByteArray

    private external fun jniSetConfirmations(number: String, libError: FFIError)

    private external fun jniEstimateTxFee(amount: String, gramFee: String, kernelCount: String, outputCount: String, libError: FFIError): ByteArray

    private external fun jniApplyEncryption(passphrase: String, libError: FFIError)

    private external fun jniRemoveEncryption(libError: FFIError)

    private external fun jniStartRecovery(
        base_node_public_key: FFIPublicKey,
        callback: String,
        callback_sig: String,
        recoveryOutputMessage: String,
        libError: FFIError
    ): Boolean

    private external fun jniDestroy()

    // endregion

    var listener: FFIWalletListener? = null

    // this acts as a constructor would for a normal class since constructors are not allowed for
    // singletons
    init {
        if (pointer == nullptr) { // so it can only be assigned once for the singleton
            val error = FFIError()
            Logger.i("Pre jniCreate.")
            try {
                jniCreate(
                    commsConfig,
                    logPath,
                    Constants.Wallet.maxNumberOfRollingLogFiles,
                    Constants.Wallet.rollingLogFileMaxSizeBytes,
                    sharedPrefsRepository.databasePassphrase,
                    seedPhraseRepository.getPhrase()?.ffiSeedWords,
                    this::onTxReceived.name, "(J)V",
                    this::onTxReplyReceived.name, "(J)V",
                    this::onTxFinalized.name, "(J)V",
                    this::onTxBroadcast.name, "(J)V",
                    this::onTxMined.name, "(J)V",
                    this::onTxMinedUnconfirmed.name, "(J[B)V",
                    this::onTxFauxConfirmed.name, "(J)V",
                    this::onTxFauxUnconfirmed.name, "(J[B)V",
                    this::onDirectSendResult.name, "([BZ)V",
                    this::onStoreAndForwardSendResult.name, "([BZ)V",
                    this::onTxCancelled.name, "(J[B)V",
                    this::onTXOValidationComplete.name, "([BZ)V",
                    this::onContactLivenessDataUpdated.name, "(J)V",
                    this::onBalanceUpdated.name, "(J)V",
                    this::onTxValidationComplete.name, "([BZ)V",
                    this::onConnectivityStatus.name, "([B)V",
                    error
                )
            } catch (e: Throwable) {
                Sentry.captureException(e)
                Logger.i("Post jniCreate with exception: %d.", e)
                throw e
            }

            Logger.i("Post jniCreate with code: %d.", error.code)
            throwIf(error)

            enableEncryption()
        }
    }

    fun enableEncryption() {
        val passphrase = sharedPrefsRepository.databasePassphrase
        if (passphrase == null) {
            Logger.i("Database encryption enable")
            sharedPrefsRepository.generateDatabasePassphrase()
            try {
                setEncryption(sharedPrefsRepository.databasePassphrase.orEmpty())
            } catch (e: Throwable) {
                //Sentry.captureException(e)
                sharedPrefsRepository.databasePassphrase = null
            }
        }
    }

    fun getBalance(): BalanceInfo {
        val error = FFIError()
        val result = jniGetBalance(error)
        throwIf(error)
        val b = FFIBalance(result)
        val bal = BalanceInfo(
            b.getAvailable(),
            b.getIncoming(),
            b.getOutgoing(),
            b.getTimeLocked(),
        )
        b.destroy()
        if (balance != bal) {
            balance = bal
        }
        return balance
    }

    fun getPublicKey(): FFIPublicKey {
        val error = FFIError()
        val result = FFIPublicKey(jniGetPublicKey(error))
        throwIf(error)
        return result
    }

    fun getContacts(): FFIContacts {
        val error = FFIError()
        val result = FFIContacts(jniGetContacts(error))
        throwIf(error)
        return result
    }

    fun addUpdateContact(contact: FFIContact): Boolean {
        val error = FFIError()
        val result = jniAddUpdateContact(contact, error)
        throwIf(error)
        return result
    }

    fun removeContact(contact: FFIContact): Boolean {
        val error = FFIError()
        val result = jniRemoveContact(contact, error)
        throwIf(error)
        return result
    }

    fun getCompletedTxs(): FFICompletedTxs {
        val error = FFIError()
        val result = FFICompletedTxs(jniGetCompletedTxs(error))
        throwIf(error)
        return result
    }

    fun getCancelledTxs(): FFICompletedTxs {
        val error = FFIError()
        val result = FFICompletedTxs(jniGetCancelledTxs(error))
        throwIf(error)
        return result
    }

    fun getCompletedTxById(id: BigInteger): FFICompletedTx {
        val error = FFIError()
        val result = FFICompletedTx(jniGetCompletedTxById(id.toString(), error))
        throwIf(error)
        return result
    }

    fun getCancelledTxById(id: BigInteger): FFICompletedTx {
        val error = FFIError()
        val result = FFICompletedTx(jniGetCancelledTxById(id.toString(), error))
        throwIf(error)
        return result
    }

    fun getPendingOutboundTxs(): FFIPendingOutboundTxs {
        val error = FFIError()
        val result = FFIPendingOutboundTxs(jniGetPendingOutboundTxs(error))
        throwIf(error)
        return result
    }

    fun getPendingOutboundTxById(id: BigInteger): FFIPendingOutboundTx {
        val error = FFIError()
        val result =
            FFIPendingOutboundTx(jniGetPendingOutboundTxById(id.toString(), error))
        throwIf(error)
        return result
    }

    fun getPendingInboundTxs(): FFIPendingInboundTxs {
        val error = FFIError()
        val result = FFIPendingInboundTxs(jniGetPendingInboundTxs(error))
        throwIf(error)
        return result
    }

    fun getPendingInboundTxById(id: BigInteger): FFIPendingInboundTx {
        val error = FFIError()
        val result =
            FFIPendingInboundTx(jniGetPendingInboundTxById(id.toString(), error))
        throwIf(error)
        return result
    }

    fun cancelPendingTx(id: BigInteger): Boolean {
        val error = FFIError()
        val result = jniCancelPendingTx(id.toString(), error)
        throwIf(error)
        return result
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onTxReceived(pendingInboundTxPtr: FFIPointer) {
        Logger.i("Tx received. Pointer: %s", pendingInboundTxPtr.toString())
        val tx = FFIPendingInboundTx(pendingInboundTxPtr)
        val pendingTx = PendingInboundTx(tx)
        GlobalScope.launch { listener?.onTxReceived(pendingTx) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onTxReplyReceived(txPointer: FFIPointer) {
        Logger.i("Tx reply received. Pointer: %s", txPointer.toString())
        val tx = FFICompletedTx(txPointer)
        val pendingOutboundTx = PendingOutboundTx(tx)
        GlobalScope.launch { listener?.onTxReplyReceived(pendingOutboundTx) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onTxFinalized(completedTx: FFIPointer) {
        Logger.i("Tx finalized. Pointer: %s", completedTx.toString())
        val tx = FFICompletedTx(completedTx)
        val pendingInboundTx = PendingInboundTx(tx)
        GlobalScope.launch { listener?.onTxFinalized(pendingInboundTx) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onTxBroadcast(completedTxPtr: FFIPointer) {
        Logger.i("Tx completed. Pointer: %s", completedTxPtr.toString())
        val tx = FFICompletedTx(completedTxPtr)
        when (tx.getDirection()) {
            Tx.Direction.INBOUND -> {
                val pendingInboundTx = PendingInboundTx(tx)
                GlobalScope.launch { listener?.onInboundTxBroadcast(pendingInboundTx) }
            }
            Tx.Direction.OUTBOUND -> {
                val pendingOutboundTx = PendingOutboundTx(tx)
                GlobalScope.launch { listener?.onOutboundTxBroadcast(pendingOutboundTx) }
            }
        }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onTxMined(completedTxPtr: FFIPointer) {
        Logger.i("Tx mined & confirmed. Pointer: %s", completedTxPtr.toString())
        val completed = CompletedTx(completedTxPtr)
        GlobalScope.launch { listener?.onTxMined(completed) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onTxMinedUnconfirmed(completedTxPtr: FFIPointer, confirmationCountBytes: ByteArray) {
        Logger.i("Tx mined & unconfirmed. Pointer: %s", completedTxPtr.toString())
        val confirmationCount = BigInteger(1, confirmationCountBytes).toInt()
        val completed = CompletedTx(completedTxPtr)
        GlobalScope.launch { listener?.onTxMinedUnconfirmed(completed, confirmationCount) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onTxFauxConfirmed(completedTxPtr: FFIPointer) {
        Logger.i("Tx faux confirmed. Pointer: %s", completedTxPtr.toString())
        val completed = CompletedTx(completedTxPtr)
        GlobalScope.launch { listener?.onTxMined(completed) }
    }

    fun onTxFauxUnconfirmed(completedTxPtr: FFIPointer, confirmationCountBytes: ByteArray) {
        Logger.i("Tx faux unconfirmed. Pointer: %s", completedTxPtr.toString())
        val confirmationCount = BigInteger(1, confirmationCountBytes).toInt()
        val completed = CompletedTx(completedTxPtr)
        GlobalScope.launch { listener?.onTxMinedUnconfirmed(completed, confirmationCount) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onDirectSendResult(bytes: ByteArray, success: Boolean) {
        Logger.i("Direct send result received. Success: $success")
        val txId = BigInteger(1, bytes)
        GlobalScope.launch { listener?.onDirectSendResult(txId, success) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onStoreAndForwardSendResult(bytes: ByteArray, success: Boolean) {
        Logger.i("Store and forward send result received. Success: $success")
        val txId = BigInteger(1, bytes)
        GlobalScope.launch { listener?.onStoreAndForwardSendResult(txId, success) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onTxCancelled(completedTx: FFIPointer, rejectionReason: ByteArray) {
        Logger.i("Tx cancelled. Pointer: %s", completedTx.toString())
        val rejectionReasonInt = BigInteger(1, rejectionReason).toInt()

        val tx = FFICompletedTx(completedTx)
        when (tx.getDirection()) {
            Tx.Direction.OUTBOUND -> {
                val cancelledTx = CancelledTx(tx)
                GlobalScope.launch { listener?.onTxCancelled(cancelledTx, rejectionReasonInt) }
            }
        }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onConnectivityStatus(bytes: ByteArray) {
        val connectivityStatus = BigInteger(1, bytes)
        GlobalScope.launch { listener?.onConnectivityStatus(connectivityStatus.toInt()) }
        Logger.i("ConnectivityStatus is [$connectivityStatus]")
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onBalanceUpdated(ptr: FFIPointer) {
        Logger.i("Balance Updated. Pointer: %s", ptr.toString())
        val b = FFIBalance(ptr)
        val balance = BalanceInfo(b.getAvailable(), b.getIncoming(), b.getOutgoing(), b.getTimeLocked())
        b.destroy()
        this.balance = balance
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onTXOValidationComplete(bytes: ByteArray, isSuccess: Boolean) {
        val requestId = BigInteger(1, bytes)
        Logger.i("Invalid TXO validation [$requestId] complete. Result: $isSuccess")
        GlobalScope.launch { listener?.onTXOValidationComplete(requestId, isSuccess) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onTxValidationComplete(bytes: ByteArray, isSuccess: Boolean) {
        val requestId = BigInteger(1, bytes)
        Logger.i("Transaction validation [$requestId] complete. Result: $isSuccess")
        GlobalScope.launch { listener?.onTxValidationComplete(requestId, isSuccess) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onContactLivenessDataUpdated(livedessUpdate: FFIPointer) {
        Logger.i("OnContactLivenessDataUpdated. Pointer: %s", livedessUpdate.toString())
    }

    fun estimateTxFee(amount: BigInteger, gramFee: BigInteger, kernelCount: BigInteger, outputCount: BigInteger): BigInteger {
        val error = FFIError()
        val bytes = jniEstimateTxFee(amount.toString(), gramFee.toString(), kernelCount.toString(), outputCount.toString(), error)
        Logger.d("Tx fee estimate status code (0 means ok): %d", error.code)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun sendTx(destination: FFIPublicKey, amount: BigInteger, feePerGram: BigInteger, message: String, isOneSided: Boolean): BigInteger {
        if (amount < BigInteger.valueOf(0L)) {
            throw FFIException(message = "Amount is less than 0.")
        }
        if (destination == getPublicKey()) {
            throw FFIException(message = "Tx source and destination are the same.")
        }
        val error = FFIError()
        val bytes = jniSendTx(destination, amount.toString(), feePerGram.toString(), message, isOneSided, error)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun coinSplit(amount: BigInteger, count: BigInteger, height: BigInteger, fee: BigInteger, message: String): BigInteger {
        val minimumLibFee = 100L
        if (fee < BigInteger.valueOf(minimumLibFee)) {
            throw FFIException(message = "Fee is less than the minimum of $minimumLibFee taris.")
        }
        if (amount < BigInteger.valueOf(0L)) {
            throw FFIException(message = "Amount is less than 0.")
        }

        val error = FFIError()
        val bytes = jniCoinSplit(amount.toString(), count.toString(), fee.toString(), message, height.toString(), error)
        Logger.d("Coin split code (0 means ok): %d", error.code)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun signMessage(message: String): String {
        val error = FFIError()
        val result = jniSignMessage(message, error)
        throwIf(error)
        return result
    }

    fun verifyMessageSignature(contactPublicKey: FFIPublicKey, message: String, signature: String): Boolean {
        val error = FFIError()
        val result = jniVerifyMessageSignature(contactPublicKey, message, signature, error)
        throwIf(error)
        return result
    }

    fun importUTXO(
        amount: BigInteger,
        message: String,
        spendingKey: FFIPrivateKey,
        sourcePublicKey: FFIPublicKey,
        tariCommitmentSignature: FFITariCommitmentSignature,
        senderPublicKey: FFIPublicKey,
        scriptPrivateKey: FFIPrivateKey,
    ): BigInteger {
        val error = FFIError()
        val bytes = jniImportUTXO(
            amount.toString(),
            spendingKey,
            sourcePublicKey,
            FFIOutputFeatures(),
            tariCommitmentSignature,
            senderPublicKey,
            scriptPrivateKey,
            message,
            error
        )
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun startTXOValidation(): BigInteger {
        val error = FFIError()
        val bytes = jniStartTXOValidation(error)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun startTxValidation(): BigInteger {
        val error = FFIError()
        val bytes = jniStartTxValidation(error)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun restartTxBroadcast(): BigInteger {
        val error = FFIError()
        val bytes = jniRestartTxBroadcast(error)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun setPowerModeNormal() {
        val error = FFIError()
        jniPowerModeNormal(error)
        throwIf(error)
    }

    fun setPowerModeLow() {
        val error = FFIError()
        jniPowerModeLow(error)
        throwIf(error)
    }

    fun getSeedWords(): FFISeedWords {
        val error = FFIError()
        val result = FFISeedWords(jniGetSeedWords(error))
        throwIf(error)
        return result
    }

    fun addBaseNodePeer(
        baseNodePublicKey: FFIPublicKey,
        baseNodeAddress: String
    ): Boolean {
        val error = FFIError()
        val result = jniAddBaseNodePeer(baseNodePublicKey, baseNodeAddress, error)
        throwIf(error)
        return result
    }

    fun setKeyValue(key: String, value: String): Boolean {
        val error = FFIError()
        val result = jniSetKeyValue(key, value, error)
        throwIf(error)
        return result
    }

    fun getKeyValue(key: String): String {
        val error = FFIError()
        val result = jniGetKeyValue(key, error)
        throwIf(error)
        return result
    }

    fun removeKeyValue(key: String): Boolean {
        val error = FFIError()
        val result = jniRemoveKeyValue(key, error)
        throwIf(error)
        return result
    }

    fun logMessage(message: String) {
        val error = FFIError()
        jniLogMessage(message, error)
        throwIf(error)
    }

    fun getRequiredConfirmationCount(): BigInteger {
        val error = FFIError()
        val bytes = jniGetConfirmations(
            error
        )
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun setRequiredConfirmationCount(number: BigInteger) {
        val error = FFIError()
        jniSetConfirmations(
            number.toString(),
            error
        )
        throwIf(error)
    }

    fun setEncryption(passphrase: String) {
        val error = FFIError()
        val result = jniApplyEncryption(passphrase, error)
        throwIf(error)
        return result
    }

    fun removeEncryption() {
        val error = FFIError()
        val result = jniRemoveEncryption(error)
        throwIf(error)
        return result
    }

    fun startRecovery(baseNodePublicKey: FFIPublicKey, recoveryOutputMessage: String): Boolean {
        val error = FFIError()
        val result = jniStartRecovery(baseNodePublicKey, this::onWalletRecovery.name, "(I[B[B)V", recoveryOutputMessage, error)
        throwIf(error)
        return result
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onWalletRecovery(event: Int, firstArg: ByteArray, secondArg: ByteArray) {
        val result = WalletRestorationResult.create(event, firstArg, secondArg)
        Logger.i("Wallet restoration. Result: $result")
        GlobalScope.launch { listener?.onWalletRestoration(result) }
    }

    override fun destroy() {
        listener = null
        jniDestroy()
    }


    fun generateTestData(datastorePath: String): Boolean = false

    fun testBroadcastTx(tx: BigInteger): Boolean = false

    fun testCompleteSentTx(tx: FFIPendingOutboundTx): Boolean = false

    fun testMineTx(tx: BigInteger): Boolean = false

    fun testFinalizeReceivedTx(tx: FFIPendingInboundTx): Boolean = false

    fun testReceiveTx(): Boolean = false
}
