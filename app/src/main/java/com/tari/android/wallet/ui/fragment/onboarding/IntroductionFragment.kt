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
package com.tari.android.wallet.ui.fragment.onboarding

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.animation.addListener
import androidx.fragment.app.Fragment
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.white
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.data.network.NetworkRepository
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.databinding.FragmentIntroductionBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.applyURLStyle
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.service.WalletServiceLauncher
import com.tari.android.wallet.ui.activity.onboarding.OnboardingFlowActivity
import com.tari.android.wallet.ui.activity.restore.WalletRestoreActivity
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.util.Constants
import javax.inject.Inject
import kotlin.math.min

/**
 * onBoarding flow : wallet introduction screen.
 * Private key and wallet do not get created in this fragment.
 * They get created in the next fragment (CreateWalletFragment) after the user clicks the CTA.
 *
 * @author The Tari Development Team
 */
internal class IntroductionFragment : Fragment() {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var networkRepository: NetworkRepository

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var applicationContext: Context

    private var listener: Listener? = null

    private val handler = Handler()

    private var videoViewHasBeenSetup = false
    private var videoViewLastPosition = 0

    private val createWalletArtificialDelay = Constants.UI.CreateWallet.tariTextAnimViewDurationMs

    private lateinit var ui: FragmentIntroductionBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentIntroductionBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appComponent.inject(this)
        setupUi()
        if (savedInstanceState == null) {
            tracker.screen(path = "/onboarding/introduction", title = "Onboarding - Introduction")
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (activity is Listener) {
            listener = activity as Listener
        }
    }

    override fun onDestroy() {
        EventBus.walletState.unsubscribe(this)
        listener = null
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onStart() {
        super.onStart()
        if (videoViewHasBeenSetup) {
            startVideo()
        }
    }

    override fun onResume() {
        super.onResume()
        ui.selectNetworkButton.text = string(introduction_selected_wallet, networkRepository.currentNetwork!!.network.displayName)
    }

    override fun onPause() {
        videoViewLastPosition = ui.rainAnimationVideoView.currentPosition
        ui.rainAnimationVideoView.stopPlayback()
        super.onPause()
    }

    private fun setupUi() {
        ui.createWalletProgressBar.setColor(color(white))
        ui.apply {
            tariLogoLottieAnimationView.alpha = 0f
            networkInfoTextView.alpha = 0f
            smallGemImageView.alpha = 0f
            createWalletContainerView.alpha = 0f
            selectNetworkContainerView.alpha = 0f
            headerLineTopTextView.alpha = 0f
            headerLineBottomTextView.alpha = 0f
            userAgreementAndPrivacyPolicyTextView.alpha = 0f
            restoreWalletCtaView.alpha = 0f
            ui.restoreWalletCtaView.setOnClickListener {
                activity?.let {
                    it.startActivity(
                        WalletRestoreActivity.navigationIntent(it)
                    )
                    it.overridePendingTransition(R.anim.enter_from_bottom, R.anim.exit_to_top)
                }
            }
            val versionInfo = "${networkRepository.currentNetwork!!.network.displayName} ${BuildConfig.VERSION_NAME} b${BuildConfig.VERSION_CODE}"
            networkInfoTextView.text = versionInfo
            // highlight links
            userAgreementAndPrivacyPolicyTextView.text =
                SpannableString(string(create_wallet_user_agreement_and_privacy_policy)).apply {
                    applyURLStyle(
                        string(create_wallet_user_agreement),
                        string(user_agreement_url)
                    )
                    applyURLStyle(
                        string(create_wallet_privacy_policy),
                        string(privacy_policy_url)
                    )
                }
            // make the links clickable
            userAgreementAndPrivacyPolicyTextView.movementMethod = LinkMovementMethod.getInstance()
            rootView.doOnGlobalLayout {
                runStartupAnimation()
                setupAndStartVideo()
            }
            createWalletButton.setOnThrottledClickListener { onCreateWalletClick() }
            selectNetworkContainerView.setOnThrottledClickListener { (requireActivity() as OnboardingFlowActivity).navigateToNetworkSelection() }
        }
    }

    private fun setupAndStartVideo() {
        val size = min(ui.videoOuterContainerView.width, ui.videoOuterContainerView.height)
        ui.videoInnerContainerView.setLayoutSize(size, size)
        ui.rainAnimationVideoView.setVideoURI(requireContext().getResourceUri(R.raw.purple_orb))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ui.rainAnimationVideoView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
        }
        startVideo()
        videoViewHasBeenSetup = true
    }

    private fun startVideo() {
        ui.rainAnimationVideoView.setOnPreparedListener { mp ->
            mp.isLooping = true
            ui.rainAnimationVideoView.start()
            ui.rainAnimationVideoView.seekTo(videoViewLastPosition)
        }
    }

    private fun animateHeaderLineTextView(textView: TextView, startDelay: Long) {
        val height = textView.measuredHeight.toFloat()
        ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                textView.alpha = value
                textView.translationY = (1 - value) * height
            }
            this.startDelay = startDelay
            duration = Constants.UI.mediumDurationMs
            interpolator = EasingInterpolator(Ease.SINE_OUT)
            start()
        }
    }

    private fun runStartupAnimation() {
        animateHeaderLineTextView(ui.headerLineTopTextView, Constants.UI.mediumDurationMs)
        animateHeaderLineTextView(ui.headerLineBottomTextView, Constants.UI.mediumDurationMs + Constants.UI.xShortDurationMs * 2)

        ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                ui.tariLogoLottieAnimationView.alpha = value
                ui.networkInfoTextView.alpha = value
                ui.smallGemImageView.alpha = value
                ui.createWalletContainerView.alpha = value
                ui.selectNetworkContainerView.alpha = value
                ui.userAgreementAndPrivacyPolicyTextView.alpha = value
                ui.restoreWalletCtaView.alpha = value
            }
            startDelay = Constants.UI.mediumDurationMs
            duration = Constants.UI.longDurationMs
            interpolator = EasingInterpolator(Ease.SINE_OUT)
            start()
        }
    }

    private fun onCreateWalletClick() = with(ui) {
        createWalletButton.temporarilyDisableClick()
        restoreWalletCtaView.setOnClickListener(null)
        createWalletButton.gone()
        createWalletProgressBar.visible()
        walletServiceLauncher.start()
        createWalletContainerView.animateClick {
            selectNetworkContainerView.isEnabled = false
            rootView.postDelayed(createWalletArtificialDelay) { startTariWalletViewAnimation() }
        }
    }

    private fun startTariWalletViewAnimation() {
        val metrics = resources.displayMetrics
        val offset = (metrics.heightPixels / 2 - ui.tariLogoLottieAnimationView.height / 2 - ui.tariLogoLottieAnimationView.top).toFloat()
        val tariViewTranslateAnim = ObjectAnimator.ofFloat(
            ui.tariLogoLottieAnimationView,
            View.TRANSLATION_Y,
            offset
        )
        tariViewTranslateAnim.interpolator = EasingInterpolator(Ease.CIRC_IN_OUT)
        tariViewTranslateAnim.duration = Constants.UI.CreateWallet.tariTextAnimViewDurationMs

        tariViewTranslateAnim.addListener(onEnd = { playTariWalletLottieAnimation() })
        ui.tariLogoLottieAnimationView.addAnimatorListener(onEnd = { listener?.continueToCreateWallet() })

        val tariViewScaleAnim = ValueAnimator.ofFloat(ui.tariLogoLottieAnimationView.scale, 1f)
        tariViewScaleAnim.duration = Constants.UI.CreateWallet.tariTextAnimViewDurationMs
        tariViewScaleAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val scale = valueAnimator.animatedValue as Float
            ui.tariLogoLottieAnimationView.scale = scale
        }

        val fadeOutAnim = ValueAnimator.ofFloat(1f, 0f)
        fadeOutAnim.duration = Constants.UI.CreateWallet.viewContainerFadeOutDurationMs
        fadeOutAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            ui.restoreWalletCtaView.alpha = alpha
            ui.videoOuterContainerView.alpha = alpha
            ui.headerLineTopTextView.alpha = alpha
            ui.headerLineBottomTextView.alpha = alpha
            ui.selectNetworkContainerView.alpha = alpha
            ui.createWalletContainerView.alpha = alpha
            ui.userAgreementAndPrivacyPolicyTextView.alpha = alpha
        }
        fadeOutAnim.addListener(onEnd = {
            ui.videoOuterContainerView.invisible()
            ui.headerLineTopTextView.invisible()
            ui.headerLineBottomTextView.invisible()
            ui.selectNetworkContainerView.invisible()
            ui.createWalletContainerView.invisible()
            ui.userAgreementAndPrivacyPolicyTextView.invisible()
        })

        val animSet = AnimatorSet()
        animSet.playTogether(tariViewTranslateAnim, tariViewScaleAnim, fadeOutAnim)
        animSet.start()
    }

    private fun playTariWalletLottieAnimation() {
        ui.tariLogoLottieAnimationView.playAnimation()
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = Constants.UI.mediumDurationMs
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.smallGemImageView.alpha = alpha
                ui.networkInfoTextView.alpha = alpha
            }
            startDelay = Constants.UI.Auth.bottomViewsFadeOutDelay
            start()
        }
    }

    interface Listener {

        fun continueToCreateWallet()

    }

}
