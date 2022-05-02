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
package com.tari.android.wallet.ui.fragment.restore.restore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsSharedRepository
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.ui.fragment.auth.AuthActivity
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.BaseNodeConfigRouter
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.addBaseNode.AddCustomBaseNodeFragment
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.changeBaseNode.ChangeBaseNodeFragment
import com.tari.android.wallet.ui.fragment.restore.chooseRestoreOption.ChooseRestoreOptionFragment
import com.tari.android.wallet.ui.fragment.restore.enterRestorationPassword.EnterRestorationPasswordFragment
import com.tari.android.wallet.ui.fragment.restore.inputSeedWords.InputSeedWordsFragment
import com.tari.android.wallet.ui.fragment.restore.walletRestoring.WalletRestoringFragment
import com.tari.android.wallet.ui.fragment.restore.walletRestoringFromSeedWords.WalletRestoringFromSeedWordsFragment
import javax.inject.Inject

class WalletRestoreActivity : AppCompatActivity(), WalletRestoreRouter, BaseNodeConfigRouter {

    @Inject
    lateinit var prefs: SharedPrefsRepository

    @Inject
    lateinit var tariSettingsSharedRepository: TariSettingsSharedRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_backup)
        if (savedInstanceState == null) {
            loadChooseBackupOptionFragment()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_from_top, R.anim.exit_to_bottom)
    }

    private fun loadChooseBackupOptionFragment() {
        supportFragmentManager.beginTransaction()
            .add(R.id.backup_fragment_container, ChooseRestoreOptionFragment())
            .commit()
    }

    override fun toEnterRestorePassword() {
        loadFragment(EnterRestorationPasswordFragment.newInstance())
    }

    override fun toRestoreWithRecoveryPhrase() {
        loadFragment(InputSeedWordsFragment.newInstance())
    }

    override fun toRestoreInProgress() {
        loadFragment(WalletRestoringFragment.newInstance())
    }

    override fun toRestoreFromSeedWordsInProgress() {
        loadFragment(WalletRestoringFromSeedWordsFragment.newInstance())
    }

    override fun toBaseNodeSelection() {
        loadFragment(ChangeBaseNodeFragment())
    }

    override fun onRestoreCompleted() {
        // wallet restored, setup shared prefs accordingly
        prefs.onboardingCompleted = true
        prefs.onboardingAuthSetupCompleted = true
        prefs.onboardingDisplayedAtHome = true
        tariSettingsSharedRepository.isRestoredWallet = true

        startActivity(Intent(this, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    override fun toChangeBaseNode() = Unit

    override fun toAddCustomBaseNode() = loadFragment(AddCustomBaseNodeFragment())

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
            .replace(R.id.backup_fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        fun navigationIntent(context: Context) = Intent(context, WalletRestoreActivity::class.java)
    }
}
