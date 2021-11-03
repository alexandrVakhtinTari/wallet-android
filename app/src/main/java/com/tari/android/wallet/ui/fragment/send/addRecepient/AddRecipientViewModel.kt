package com.tari.android.wallet.ui.fragment.send.addRecepient

import android.content.ClipboardManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.application.DeepLink
import com.tari.android.wallet.data.network.NetworkRepository
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.extension.executeWithError
import com.tari.android.wallet.extension.getWithError
import com.tari.android.wallet.model.Contact
import com.tari.android.wallet.model.PublicKey
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.User
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.send.addRecepient.list.RecipientHeaderItem
import com.tari.android.wallet.ui.fragment.send.addRecepient.list.RecipientViewHolderItem
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.extractEmojis
import com.tari.android.wallet.yat.YatAdapter
import com.tari.android.wallet.yat.YatUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import yat.android.data.YatRecordType
import javax.inject.Inject

//todo needed to refactor input methods
class AddRecipientViewModel() : CommonViewModel() {

    var emojiIdPublicKey: PublicKey? = null

    private var recentTxUsersLimit = 3
    private var allTxs = mutableListOf<Tx>()
    private var contacts = mutableListOf<Contact>()

    private val _list: MutableLiveData<MutableList<CommonViewHolderItem>> = MutableLiveData(mutableListOf())
    val list: LiveData<MutableList<CommonViewHolderItem>> = _list

    private val _navigation: SingleLiveEvent<AddRecipientNavigation> = SingleLiveEvent()
    val navigation: LiveData<AddRecipientNavigation> = _navigation

    private val _showClipboardData: SingleLiveEvent<PublicKey> = SingleLiveEvent()
    val showClipboardData: LiveData<PublicKey> = _showClipboardData

    @Inject
    lateinit var yatAdapter: YatAdapter

    @Inject
    lateinit var clipboardManager: ClipboardManager

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var networkRepository: NetworkRepository

    private val serviceConnection = TariWalletServiceConnection()
    private val walletService
        get() = serviceConnection.currentState.service!!

    init {
        component.inject(this)

        serviceConnection.connection.subscribe {
            if (it.status == TariWalletServiceConnection.ServiceConnectionStatus.CONNECTED) {
                viewModelScope.launch(Dispatchers.IO) {
                    fetchAllData()
                    displayList()
                }
            }
        }.addTo(compositeDisposable)
    }

    fun displayList() {
        val recentTxUsers = allTxs.sortedByDescending { it.timestamp }.distinctBy { it.user }.take(recentTxUsersLimit).map { it.user }
        val formattedList = mutableListOf<CommonViewHolderItem>()

        if (recentTxUsers.isNotEmpty()) {
            formattedList.add(RecipientHeaderItem(resourceManager.getString(R.string.add_recipient_recent_tx_contacts), 0))
            formattedList.addAll(recentTxUsers.map { user -> RecipientViewHolderItem(user) })
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
            it.user.publicKey.emojiId.contains(query) || (it.user as? Contact)?.alias?.contains(query, ignoreCase = true) ?: false
        }.map { it.user }.distinct()

        // search contacts (we don't have non-transaction contacts at the moment, but we probably
        // will have them in the future - so this is a safety measure)
        val filteredContacts = contacts.filter { it.publicKey.emojiId.contains(query) || it.alias.contains(query, ignoreCase = true) }
        val users = (filteredTxUsers + filteredContacts).distinct().sortedWith { o1, o2 ->
            val value1 = ((o1 as? Contact)?.alias) ?: o1.publicKey.emojiId
            val value2 = ((o2 as? Contact)?.alias) ?: o2.publicKey.emojiId
            value1.compareTo(value2)
        }

        displaySearchList(users)

        viewModelScope.launch(Dispatchers.IO) {
            val searchResult = yatAdapter.searchYats(query)
            Logger.i(searchResult.status.toString())
            Logger.i(searchResult.error.toString())
            Logger.i(searchResult.result.toString())
            if (searchResult.status) {
                val list = _list.value!!
                val records = searchResult.result.orEmpty().filter { it.type == YatRecordType.TARI_PUBKEY }
                    .map { result -> YatUser(walletService.getPublicKeyFromHexString(result.data)).apply { yat = query } }
                    .map { user -> RecipientViewHolderItem(user) }
                list.addAll(0, records)
                _list.postValue(list)
            }
        }
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
            val user = contacts.firstOrNull { it.publicKey == emojiIdPublicKey } ?: User(emojiIdPublicKey!!)
            _navigation.postValue(AddRecipientNavigation.ToAmount(user))
        }
    }

    /**
     * Checks clipboard data for a valid deep link or an emoji id.
     */
    fun checkClipboardForValidEmojiId() {
        val clipboardString = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: return

        val deepLink = DeepLink.from(networkRepository, clipboardString)
        if (deepLink != null) { // there is a deep link in the clipboard
            emojiIdPublicKey = when (deepLink.type) {
                DeepLink.Type.EMOJI_ID -> walletService.getPublicKeyFromEmojiId(deepLink.identifier)
                DeepLink.Type.PUBLIC_KEY_HEX -> walletService.getPublicKeyFromHexString(deepLink.identifier)
            }
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
                emojiIdPublicKey = walletService.getPublicKeyFromEmojiId(emojiWindow)
                if (emojiIdPublicKey != null) {
                    break
                }
                --currentIndex
            }
        }
        if (emojiIdPublicKey == null) {
            checkClipboardForPublicKeyHex(clipboardString)
        }
        emojiIdPublicKey?.let {
            if (it.emojiId != sharedPrefsWrapper.emojiId!!) {
                _showClipboardData.postValue(it)
            }
        }
    }

    /**
     * Checks clipboard data for a public key hex string.
     */
    private fun checkClipboardForPublicKeyHex(clipboardString: String) {
        val hexStringRegex = Regex("([A-Za-z0-9]{64})")
        var result = hexStringRegex.find(clipboardString)
        while (result != null) {
            val hexString = result.value
            emojiIdPublicKey = walletService.getPublicKeyFromHexString(hexString)
            if (emojiIdPublicKey != null) {
                return
            }
            result = result.next()
        }
    }

    fun getPublicKeyFromHexString(publicKeyHex: String) = walletService.getWithError { _, wallet -> wallet.getPublicKeyFromHexString(publicKeyHex) }

    fun getPublicKeyFromEmojiId(emojiId: String) = walletService.getWithError { _, wallet -> wallet.getPublicKeyFromEmojiId(emojiId) }
}

