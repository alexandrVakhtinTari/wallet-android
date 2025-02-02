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
package com.tari.android.wallet.ui.fragment.send.addRecepient

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.Activity
import android.content.*
import android.os.*
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.animation.addListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.*
import com.tari.android.wallet.R.dimen.add_recipient_clipboard_emoji_id_container_height
import com.tari.android.wallet.R.dimen.add_recipient_paste_emoji_id_button_visible_top_margin
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.application.DeepLink
import com.tari.android.wallet.application.DeepLink.Type.EMOJI_ID
import com.tari.android.wallet.application.DeepLink.Type.PUBLIC_KEY_HEX
import com.tari.android.wallet.data.network.NetworkRepository
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.databinding.FragmentAddRecipientBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.model.*
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.qr.EXTRA_QR_DATA
import com.tari.android.wallet.ui.fragment.qr.QRScannerActivity
import com.tari.android.wallet.ui.fragment.send.addRecepient.list.RecipientListAdapter
import com.tari.android.wallet.ui.fragment.send.addRecepient.list.RecipientViewHolderItem
import com.tari.android.wallet.util.*
import com.tari.android.wallet.util.Constants.Wallet.emojiFormatterChunkSize
import com.tari.android.wallet.util.Constants.Wallet.emojiIdLength
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.math.min

//todo needed to refactor
class AddRecipientFragment : CommonFragment<FragmentAddRecipientBinding, AddRecipientViewModel>(),
    RecyclerView.OnItemTouchListener,
    TextWatcher {

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var clipboardManager: ClipboardManager

    @Inject
    lateinit var networkRepository: NetworkRepository

    /**
     * List, adapter & layout manager.
     */
    private var recyclerViewAdapter: RecipientListAdapter = RecipientListAdapter()
    private var scrollListener = ScrollListener(this)

    /**
     * Fields related to emoji id input masking.
     */
    private var isDeletingSeparatorAtIndex: Int? = null
    private var textWatcherIsRunning = false
    private var inputNormalLetterSpacing = 0.04f
    private var inputEmojiIdLetterSpacing = 0.27f

    private val textChangedProcessDelayMs = 100L
    private val textChangedProcessHandler = Handler(Looper.getMainLooper())
    private var textChangedProcessRunnable = Runnable { processTextChanged() }

    private var hidePasteEmojiIdViewsOnTextChanged = false

    /**
     * Paste-emoji-id-related views.
     */
    private val dimmerViews
        get() = arrayOf(ui.topDimmerView, ui.middleDimmerView, ui.bottomDimmerView)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentAddRecipientBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appComponent.inject(this)

        val viewModel: AddRecipientViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()
        displayInitialList()
        ui.searchEditText.setRawInputType(InputType.TYPE_CLASS_TEXT)
        ui.searchEditText.addTextChangedListener(this@AddRecipientFragment)
        viewModel.checkClipboardForValidEmojiId()

        if (savedInstanceState == null) {
            tracker.screen(path = "/home/send_tari/add_recipient", title = "Send Tari - Add Recipient")
        }
        subscribeViewModal()
    }

    private fun subscribeViewModal() = with(viewModel) {
        observe(list) { recyclerViewAdapter.update(it) }

        observe(navigation) { processNavigation(it) }

        observe(showClipboardData) { showClipboardData(it) }
    }

    private fun showClipboardData(data: PublicKey) {
        ui.rootView.postDelayed({
            hidePasteEmojiIdViewsOnTextChanged = true
            showPasteEmojiIdViews(data)
            focusEditTextAndShowKeyboard()
        }, 100L)
    }

    private fun processNavigation(navigation: AddRecipientNavigation) {
        val listener = requireActivity() as AddRecipientListener

        when(navigation) {
            is AddRecipientNavigation.ToAmount -> listener.continueToAmount(this, navigation.user)
        }
    }

    private fun setupUI() {
        ui.contactsListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewAdapter.setClickListener(CommonAdapter.ItemClickListener() {
            (it as? RecipientViewHolderItem)?.user?.let { user ->
                (activity as? AddRecipientListener)?.continueToAmount(this, user)
            }
        })
        ui.contactsListRecyclerView.adapter = recyclerViewAdapter
        ui.contactsListRecyclerView.addOnScrollListener(scrollListener)
        ui.contactsListRecyclerView.addOnItemTouchListener(this)
        ui.scrollDepthGradientView.alpha = 0f
        ui.progressBar.setColor(color(add_recipient_prog_bar))
        ui.progressBar.visible()
        ui.continueButton.gone()
        ui.invalidEmojiIdTextView.gone()
        hidePasteEmojiIdViews(animate = false)
        OverScrollDecoratorHelper.setUpOverScroll(ui.emojiIdScrollView)
        OverScrollDecoratorHelper.setUpOverScroll(ui.searchEditTextScrollView)
        ui.searchEditText.inputType = InputType.TYPE_NULL
        ui.backButton.setOnClickListener { onBackButtonClicked(it) }
        ui.qrCodeButton.setOnClickListener { onQRButtonClick(it) }
        ui.continueButton.setOnClickListener { onContinueButtonClicked(it) }
        dimmerViews.forEach { it.setOnClickListener { onEmojiIdDimmerClicked() } }
        ui.pasteEmojiIdButton.setOnClickListener { onPasteEmojiIdButtonClicked() }
        ui.emojiIdTextView.setOnClickListener { onPasteEmojiIdButtonClicked() }
    }

    fun reset() {
        // state is not initial if there's some character in the search input
        if (ui.searchEditText.text.toString().isNotEmpty()) {
            ui.searchEditText.setText("")
            ui.searchEditText.isEnabled = true
            ui.continueButton.gone()
        }
    }

    private fun startQRCodeActivity() {
        val intent = Intent(activity, QRScannerActivity::class.java)
        startActivityForResult(intent, QRScannerActivity.REQUEST_QR_SCANNER)
        activity?.overridePendingTransition(R.anim.slide_up, 0)
    }


    /**
     * Displays paste-emoji-id-related views.
     */
    private fun showPasteEmojiIdViews(publicKey: PublicKey) {
        ui.emojiIdTextView.text = EmojiUtil.getFullEmojiIdSpannable(
            publicKey.emojiId,
            string(emoji_id_chunk_separator),
            color(black),
            color(light_gray)
        )
        ui.emojiIdContainerView.setBottomMargin(-dimenPx(add_recipient_clipboard_emoji_id_container_height))
        ui.emojiIdContainerView.visible()
        dimmerViews.forEach { dimmerView ->
            dimmerView.alpha = 0f
            dimmerView.visible()
        }

        // animate
        val emojiIdAppearAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val animValue = valueAnimator.animatedValue as Float
                dimmerViews.forEach { dimmerView -> dimmerView.alpha = animValue * 0.6f }
                ui.emojiIdContainerView.setBottomMargin((-dimenPx(add_recipient_clipboard_emoji_id_container_height) * (1f - animValue)).toInt())
            }
            interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
            duration = Constants.UI.mediumDurationMs
        }


        // animate and show paste emoji id button
        ui.pasteEmojiIdContainerView.setTopMargin(0)
        val pasteButtonAppearAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                ui.pasteEmojiIdContainerView.setTopMargin(
                    (dimenPx(add_recipient_paste_emoji_id_button_visible_top_margin) * value).toInt()
                )
                ui.pasteEmojiIdContainerView.alpha = value
            }
            addListener(onStart = { ui.pasteEmojiIdContainerView.visible() })
            interpolator = EasingInterpolator(Ease.BACK_OUT)
            duration = Constants.UI.shortDurationMs
        }

        AnimatorSet().apply {
            playSequentially(emojiIdAppearAnim, pasteButtonAppearAnim)
            startDelay = Constants.UI.xShortDurationMs
            start()
        }
    }

    /**
     * Hides paste-emoji-id-related views.
     */
    private fun hidePasteEmojiIdViews(animate: Boolean, onEnd: (() -> (Unit))? = null) {
        if (!animate) {
            ui.pasteEmojiIdContainerView.gone()
            ui.emojiIdContainerView.gone()
            dimmerViews.forEach(View::gone)
            onEnd?.let { it() }
            return
        }
        // animate and hide paste emoji id button
        val pasteButtonDisappearAnim = ValueAnimator.ofFloat(0f, 1f)
        pasteButtonDisappearAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.pasteEmojiIdContainerView.setTopMargin(
                (dimenPx(add_recipient_paste_emoji_id_button_visible_top_margin) * (1 - value)).toInt()
            )
            ui.pasteEmojiIdContainerView.alpha = (1 - value)
        }
        pasteButtonDisappearAnim.addListener(onEnd = { ui.pasteEmojiIdContainerView.gone() })
        pasteButtonDisappearAnim.duration = Constants.UI.shortDurationMs
        // animate and hide emoji id & dimmers
        val emojiIdDisappearAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdDisappearAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            dimmerViews.forEach { dimmerView -> dimmerView.alpha = 0.6f * (1 - value) }
            ui.emojiIdContainerView.setBottomMargin((-dimenPx(add_recipient_clipboard_emoji_id_container_height) * value).toInt())
        }
        emojiIdDisappearAnim.addListener(onEnd = {
            ui.emojiIdContainerView.gone()
            dimmerViews.forEach(View::gone)
        })
        emojiIdDisappearAnim.duration = Constants.UI.shortDurationMs

        // chain anim.s and start
        val animSet = AnimatorSet()
        animSet.playSequentially(pasteButtonDisappearAnim, emojiIdDisappearAnim)
        if (onEnd != null) {
            animSet.addListener(onEnd = { onEnd() })
        }
        animSet.start()
    }

    /**
     * Displays non-search-result list.
     */
    private fun displayInitialList() {
        ui.progressBar.gone()
        ui.contactsListRecyclerView.visible()
        viewModel.displayList()
        focusEditTextAndShowKeyboard()
    }

    private fun focusEditTextAndShowKeyboard() {
        val mActivity = activity ?: return
        ui.searchEditText.requestFocus()
        val imm = mActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    private fun onSearchTextChanged(query: String) {
        if (query.isEmpty()) {
            displayInitialList()
        } else {
            viewModel.searchAndDisplayRecipients(query)
            ui.progressBar.gone()
            ui.contactsListRecyclerView.visible()
        }
    }

    private fun onBackButtonClicked(view: View) {
        view.temporarilyDisableClick()
        activity?.let {
            it.hideKeyboard()
            ui.rootView.postDelayed(200L) { it.onBackPressed() }
        }
    }

    private fun clearSearchResult() {
        ui.progressBar.gone()
        ui.contactsListRecyclerView.visible()
        viewModel.displaySearchList(listOf())
    }

    /**
     * Open QR code scanner on button click.
     */
    private fun onQRButtonClick(view: View) {
        view.temporarilyDisableClick()
        requireActivity().hideKeyboard()
        hidePasteEmojiIdViews(animate = true) {
            ui.rootView.postDelayed(Constants.UI.keyboardHideWaitMs) { startQRCodeActivity() }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == QRScannerActivity.REQUEST_QR_SCANNER
            && resultCode == Activity.RESULT_OK
            && data != null
        ) {
            val qrData = data.getStringExtra(EXTRA_QR_DATA) ?: return
            val deepLink = DeepLink.from(networkRepository, qrData) ?: return
            when (deepLink.type) {
                EMOJI_ID -> {
                    ui.searchEditText.setText(
                        deepLink.identifier,
                        TextView.BufferType.EDITABLE
                    )
                    ui.searchEditText.postDelayed({
                        ui.searchEditTextScrollView.smoothScrollTo(0, 0)
                    }, Constants.UI.mediumDurationMs)
                }
                PUBLIC_KEY_HEX -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val publicKeyHex = deepLink.identifier
                        val publicKey = viewModel.getPublicKeyFromHexString(publicKeyHex)
                        if (publicKey != null) {
                            ui.rootView.post { ui.searchEditText.setText(publicKey.emojiId, TextView.BufferType.EDITABLE) }
                            ui.searchEditText.postDelayed({ ui.searchEditTextScrollView.smoothScrollTo(0, 0) }, Constants.UI.mediumDurationMs)
                        }
                    }
                }
            }
        }
    }

    private fun onContinueButtonClicked(view: View) {
        view.temporarilyDisableClick()
        viewModel.onContinue()
    }

    /**
     * Dimmer clicked - hide paste-related views.
     */
    private fun onEmojiIdDimmerClicked() {
        hidePasteEmojiIdViews(animate = true) {
            requireActivity().hideKeyboard()
            ui.searchEditText.clearFocus()
        }
    }

    /**
     * Paste banner clicked.
     */
    private fun onPasteEmojiIdButtonClicked() {
        hidePasteEmojiIdViewsOnTextChanged = false
        hidePasteEmojiIdViews(animate = true) {
            ui.searchEditText.scaleX = 0f
            ui.searchEditText.scaleY = 0f
            ui.searchEditText.setText(
                viewModel.emojiIdPublicKey!!.emojiId,
                TextView.BufferType.EDITABLE
            )
            ui.searchEditText.setSelection(ui.searchEditText.text?.length ?: 0)
            ui.rootView.postDelayed({ animateEmojiIdPaste() }, Constants.UI.xShortDurationMs)
        }
    }

    private fun animateEmojiIdPaste() {
        // animate text size
        val textAnim = ValueAnimator.ofFloat(0f, 1f)
        textAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.searchEditText.scaleX = value
            ui.searchEditText.scaleY = value
            // searchEditText.translationX = -width * (1f - value) / 2f
        }
        textAnim.duration = Constants.UI.shortDurationMs
        textAnim.interpolator = EasingInterpolator(Ease.BACK_OUT)
        textAnim.start()
        ui.rootView.postDelayed({
            ui.searchEditTextScrollView.smoothScrollTo(0, 0)
        }, Constants.UI.shortDurationMs)
    }

    // region recycler view item touch listener

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        // no-op
    }

    /**
     * Hide keyboard on list touch.
     */
    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        requireActivity().hideKeyboard()
        ui.searchEditText.clearFocus()
        return false
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // no-op
    }

    // endregion

    // region emoji id input masking

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        isDeletingSeparatorAtIndex =
            if (count == 1 && after == 0 && s[start].toString() == string(emoji_id_chunk_separator)) {
                start
            } else {
                null
            }
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        // no-op
    }

    override fun afterTextChanged(editable: Editable) {
        if (hidePasteEmojiIdViewsOnTextChanged) {
            hidePasteEmojiIdViews(animate = true)
            hidePasteEmojiIdViewsOnTextChanged = false
        }
        if (textWatcherIsRunning) {
            return
        }
        textChangedProcessHandler.removeCallbacks(textChangedProcessRunnable)
        textChangedProcessHandler.postDelayed(textChangedProcessRunnable, textChangedProcessDelayMs)
    }

    private fun processTextChanged() {
        textWatcherIsRunning = true
        val editable = ui.searchEditText.editableText
        var text = editable.toString()

        ui.continueButton.gone()
        ui.invalidEmojiIdTextView.gone()

        if (editable.toString().firstNCharactersAreEmojis(emojiFormatterChunkSize)) {
            // if deleting a separator, first get the index of the character before that separator
            // and delete that character
            if (isDeletingSeparatorAtIndex != null) {
                val index =
                    EmojiUtil.getStartIndexOfItemEndingAtIndex(text, isDeletingSeparatorAtIndex!!)
                editable.delete(index, isDeletingSeparatorAtIndex!!)
                isDeletingSeparatorAtIndex = null
                text = editable.toString()
            }

            // delete all separators first
            val separator = string(emoji_id_chunk_separator)
            for ((offset, index) in EmojiUtil.getExistingChunkSeparatorIndices(
                text,
                separator
            ).withIndex()) {
                val target = index - (offset * separator.length)
                editable.delete(target, target + separator.length)
            }
            val textWithoutSeparators = editable.toString()
            ui.searchEditText.textAlignment = View.TEXT_ALIGNMENT_CENTER
            ui.searchEditText.letterSpacing = inputEmojiIdLetterSpacing
            // add separators
            for ((offset, index) in EmojiUtil.getNewChunkSeparatorIndices(textWithoutSeparators)
                .withIndex()) {
                val chunkSeparatorSpannable = EmojiUtil.getChunkSeparatorSpannable(
                    separator,
                    color(light_gray)
                )
                val target = index + (offset * separator.length)
                editable.insert(target, chunkSeparatorSpannable)
            }
            // check if valid emoji - don't search if not
            val numberofEmojis = textWithoutSeparators.numberOfEmojis()
            if (textWithoutSeparators.containsNonEmoji() || numberofEmojis > emojiIdLength) {
                viewModel.emojiIdPublicKey = null
                ui.invalidEmojiIdTextView.text = string(add_recipient_invalid_emoji_id)
                ui.invalidEmojiIdTextView.visible()
                ui.qrCodeButton.visible()
                clearSearchResult()
            } else {
                if (numberofEmojis == emojiIdLength) {
                    if (textWithoutSeparators == sharedPrefsWrapper.emojiId!!) {
                        viewModel.emojiIdPublicKey = null
                        ui.invalidEmojiIdTextView.text = string(add_recipient_own_emoji_id)
                        ui.invalidEmojiIdTextView.visible()
                        ui.qrCodeButton.visible()
                        clearSearchResult()
                    } else {
                        ui.qrCodeButton.gone()
                        // valid emoji id length - clear list, no search, display continue button
                        lifecycleScope.launch(Dispatchers.IO) {
                            viewModel.emojiIdPublicKey = viewModel.getPublicKeyFromEmojiId(textWithoutSeparators)
                            withContext(Dispatchers.Main) {
                                if (viewModel.emojiIdPublicKey == null) {
                                    ui.invalidEmojiIdTextView.text =
                                        string(add_recipient_invalid_emoji_id)
                                    ui.invalidEmojiIdTextView.visible()
                                    clearSearchResult()
                                } else {
                                    ui.invalidEmojiIdTextView.gone()
                                    ui.continueButton.visible()
                                    activity?.hideKeyboard()
                                    ui.searchEditText.clearFocus()
                                    onSearchTextChanged(textWithoutSeparators)
                                    ui.searchEditText.isEnabled = false
                                }
                            }
                        }
                    }
                } else {
                    viewModel.emojiIdPublicKey = null
                    ui.qrCodeButton.visible()
                    onSearchTextChanged(textWithoutSeparators)
                }
            }
        } else {
            viewModel.emojiIdPublicKey = null
            ui.qrCodeButton.visible()
            ui.searchEditText.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            ui.searchEditText.letterSpacing = inputNormalLetterSpacing
            onSearchTextChanged(editable.toString())
        }
        textWatcherIsRunning = false
    }

    // endregion

    // region scroll depth gradient view controls

    private fun onRecyclerViewScrolled(totalDeltaY: Int) {
        ui.scrollDepthGradientView.alpha = min(
            Constants.UI.scrollDepthShadowViewMaxOpacity,
            totalDeltaY / (dimenPx(R.dimen.add_recipient_contact_list_item_height)).toFloat()
        )
    }

    class ScrollListener(fragment: AddRecipientFragment) : RecyclerView.OnScrollListener() {

        private val fragmentWR = WeakReference(fragment)
        private var totalDeltaY = 0

        fun reset() {
            totalDeltaY = 0
        }

        override fun onScrolled(recyclerView: RecyclerView, dX: Int, dY: Int) {
            super.onScrolled(recyclerView, dX, dY)
            totalDeltaY += dY
            fragmentWR.get()?.onRecyclerViewScrolled(totalDeltaY)
        }

    }

    // end region

}