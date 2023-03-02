package com.tari.android.wallet.ui.fragment.contact_book.add

import android.content.ClipboardManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.extension.executeWithError
import com.tari.android.wallet.extension.getWithError
import com.tari.android.wallet.model.Contact
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.User
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.contact_book.data.IContact
import com.tari.android.wallet.ui.fragment.contact_book.root.ContactBookNavigation
import com.tari.android.wallet.ui.fragment.send.addRecepient.recipientList.RecipientHeaderItem
import com.tari.android.wallet.ui.fragment.send.addRecepient.recipientList.RecipientViewHolderItem
import com.tari.android.wallet.util.Build
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.extractEmojis
import com.tari.android.wallet.yat.YatAdapter
import com.tari.android.wallet.yat.YatUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Optional
import javax.inject.Inject


//todo needed to refactor input methods
class AddContactViewModel : CommonViewModel() {

    var tariWalletAddress: TariWalletAddress? = null

    private var recentTxUsersLimit = 3
    private var allTxs = mutableListOf<Tx>()
    private var contacts = mutableListOf<Contact>()

    private val _list: MutableLiveData<MutableList<CommonViewHolderItem>> = MutableLiveData(mutableListOf())
    val list: LiveData<MutableList<CommonViewHolderItem>> = _list

    private val _navigation: SingleLiveEvent<ContactBookNavigation> = SingleLiveEvent()
    val navigation: LiveData<ContactBookNavigation> = _navigation

    private val _showClipboardData: MutableLiveData<TariWalletAddress> = MutableLiveData()
    val showClipboardData: LiveData<TariWalletAddress> = _showClipboardData

    val readyToInteract = MutableLiveData(false)

    val serviceIsReady = MutableLiveData(false)

    val clipboardChecker = MediatorLiveData<Unit>()

    @Inject
    lateinit var yatAdapter: YatAdapter

    @Inject
    lateinit var clipboardManager: ClipboardManager

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var deeplinkHandler: DeeplinkHandler

    init {
        component.inject(this)

        clipboardChecker.addSource(serviceIsReady) { if (readyToInteract.value!! && serviceIsReady.value!!) checkClipboardForValidEmojiId() }

        doOnConnected {
            serviceIsReady.postValue(true)
            viewModelScope.launch(Dispatchers.IO) {
                fetchAllData()
                displayList()
            }
        }
    }

    fun displayList() {
        val recentTxUsers =
            allTxs.asSequence().sortedByDescending { it.timestamp }.distinctBy { it.user }.take(recentTxUsersLimit).map { it.user }.toMutableList()
        val formattedList = mutableListOf<CommonViewHolderItem>()

        if (recentTxUsers.isEmpty() && Build.MOCKED) {
            for (i in 0 until 10) {
                recentTxUsers.add(0, User().apply {
                    tariWalletAddress = TariWalletAddress().apply {
                        hexString = "Hex string ttt"
                        emojiId =
                            "\uD83D\uDC7F\uD83C\uDF54\uD83C\uDF75\uD83C\uDFAD\uD83C\uDFC8\uD83D\uDD2E\uD83D\uDE91\uD83D\uDC1C\uD83C\uDF6F\uD83D\uDC7B\uD83C\uDFBC\uD83C\uDF77\uD83C\uDFAC\uD83D\uDC95\uD83D\uDC51\uD83D\uDDFD\uD83C\uDF5E\uD83C\uDFBC\uD83D\uDE91\uD83C\uDF08\uD83C\uDFBA\uD83D\uDD26\uD83D\uDCC9\uD83C\uDF60\uD83C\uDF4D\uD83C\uDF4A\uD83C\uDF4B\uD83D\uDCDA\uD83C\uDF30\uD83C\uDF6B\uD83D\uDEAB\uD83C\uDF49\uD83C\uDF47"
                    }
                })
            }
        }

        if (recentTxUsers.isNotEmpty()) {
            formattedList.add(RecipientHeaderItem(resourceManager.getString(R.string.add_recipient_recent_tx_contacts), 0))
            formattedList.addAll(recentTxUsers.map { user -> RecipientViewHolderItem(user) })
        }

        if (contacts.isEmpty() && Build.MOCKED) {
            for (i in 0 until 10) {
                contacts.add(0, Contact().apply {
                    alias = "Test user"
                    tariWalletAddress = TariWalletAddress().apply {
                        hexString = "Hex string ttt"
                        emojiId =
                            "\uD83D\uDC7F\uD83C\uDF54\uD83C\uDF75\uD83C\uDFAD\uD83C\uDFC8\uD83D\uDD2E\uD83D\uDE91\uD83D\uDC1C\uD83C\uDF6F\uD83D\uDC7B\uD83C\uDFBC\uD83C\uDF77\uD83C\uDFAC\uD83D\uDC95\uD83D\uDC51\uD83D\uDDFD\uD83C\uDF5E\uD83C\uDFBC\uD83D\uDE91\uD83C\uDF08\uD83C\uDFBA\uD83D\uDD26\uD83D\uDCC9\uD83C\uDF60\uD83C\uDF4D\uD83C\uDF4A\uD83C\uDF4B\uD83D\uDCDA\uD83C\uDF30\uD83C\uDF6B\uD83D\uDEAB\uD83C\uDF49\uD83C\uDF47"
                    }
                })
            }
        }

        if (contacts.isNotEmpty()) {
            formattedList.add(RecipientHeaderItem(resourceManager.getString(R.string.add_recipient_my_contacts), formattedList.size))
            formattedList.addAll(contacts.map { user -> RecipientViewHolderItem(user) })
        }

        _list.postValue(formattedList)
    }

    fun displaySearchList(users: List<User>) {
        _list.postValue(users.map { user -> RecipientViewHolderItem(user) }.toMutableList())
    }

    fun searchAndDisplayRecipients(query: String) {
        // search transaction users
        val filteredTxUsers = allTxs.filter {
            it.user.walletAddress.emojiId.contains(query) || (it.user as? Contact)?.alias?.contains(query, ignoreCase = true) ?: false
        }.map { it.user }.distinct()

        // search contacts (we don't have non-transaction contacts at the moment, but we probably
        // will have them in the future - so this is a safety measure)
        val filteredContacts = contacts.filter { it.walletAddress.emojiId.contains(query) || it.alias.contains(query, ignoreCase = true) }
        val users = (filteredTxUsers + filteredContacts).distinct().sortedWith { o1, o2 ->
            val value1 = ((o1 as? Contact)?.alias) ?: o1.walletAddress.emojiId
            val value2 = ((o2 as? Contact)?.alias) ?: o2.walletAddress.emojiId
            value1.compareTo(value2)
        }

        displaySearchList(users)
    }

    private fun fetchAllData() {
        walletService.executeWithError { error, wallet ->
            contacts = wallet.getContacts(error).orEmpty().toMutableList()
            allTxs.addAll(wallet.getCompletedTxs(error).orEmpty())
            allTxs.addAll(wallet.getPendingInboundTxs(error).orEmpty())
            allTxs.addAll(wallet.getPendingOutboundTxs(error).orEmpty())
            allTxs.addAll(wallet.getCancelledTxs(error).orEmpty())
        }
    }


    fun onContinue() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = contacts.firstOrNull { it.walletAddress == tariWalletAddress } ?: User(tariWalletAddress!!)
            _navigation.postValue(ContactBookNavigation.ToAddContactName(IContact.generateFromUser(user)))
        }
    }

    /**
     * Checks clipboard data for a valid deep link or an emoji id.
     */
    private fun checkClipboardForValidEmojiId() {
        val clipboardString = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: return

        val deepLink = deeplinkHandler.handle(clipboardString) as? DeepLink.Send
        if (deepLink != null) { // there is a deep link in the clipboard
            tariWalletAddress = walletService.getWalletAddressFromHexString(deepLink.walletAddressHex)
        } else { // try to extract a valid emoji id
            val emojis = clipboardString.trim().extractEmojis()
            // search in windows of length = emoji id length
            var currentIndex = emojis.size - Constants.Wallet.emojiIdLength
            while (currentIndex >= 0) {
                val emojiWindow =
                    emojis
                        .subList(currentIndex, currentIndex + Constants.Wallet.emojiIdLength)
                        .joinToString(separator = "")
                // there is a chunked emoji id in the clipboard
                tariWalletAddress = walletService.getWalletAddressFromEmojiId(emojiWindow)
                if (tariWalletAddress != null) {
                    break
                }
                --currentIndex
            }
        }
        if (tariWalletAddress == null) {
            checkForWalletAddressHex(clipboardString)
        }
        tariWalletAddress?.let {
            _showClipboardData.postValue(it)
        }
    }

    /**
     * Checks clipboard data for a public key hex string.
     */
    fun checkForWalletAddressHex(input: String): Boolean {
        val hexStringRegex = Regex("([A-Za-z0-9]{66})")
        var result = hexStringRegex.find(input)
        while (result != null) {
            val hexString = result.value
            tariWalletAddress = walletService.getWalletAddressFromHexString(hexString)
            if (tariWalletAddress != null) {
                return true
            }
            result = result.next()
        }
        return false
    }

    fun getWalletAddressFromHexString(publicKeyHex: String): TariWalletAddress? =
        walletService.getWithError { _, wallet -> wallet.getWalletAddressFromHexString(publicKeyHex) }

    fun getWalletAddressFromEmojiId(emojiId: String): TariWalletAddress? =
        walletService.getWithError { _, wallet -> wallet.getWalletAddressFromEmojiId(emojiId) }
}