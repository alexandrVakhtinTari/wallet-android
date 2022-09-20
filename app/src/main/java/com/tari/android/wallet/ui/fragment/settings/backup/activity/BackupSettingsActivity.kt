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
package com.tari.android.wallet.ui.fragment.settings.backup.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ActivityBackupSettingsBinding
import com.tari.android.wallet.ui.common.CommonActivity
import com.tari.android.wallet.ui.fragment.settings.backup.ChangeSecurePasswordFragment
import com.tari.android.wallet.ui.fragment.settings.backup.EnterCurrentPasswordFragment
import com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.BackupSettingsFragment
import com.tari.android.wallet.ui.fragment.settings.backup.verifySeedPhrase.VerifySeedPhraseFragment
import com.tari.android.wallet.ui.fragment.settings.backup.writeDownSeedWords.WriteDownSeedPhraseFragment

class BackupSettingsActivity : CommonActivity<ActivityBackupSettingsBinding, BackupSettingsViewModel>(), BackupSettingsRouter {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityBackupSettingsBinding.inflate(layoutInflater).apply { setContentView(root) }

        val viewModel: BackupSettingsViewModel by viewModels()
        bindViewModel(viewModel)

        setContentView(R.layout.activity_backup_settings)
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        if (savedInstanceState == null) {
            loadBackupSettingsFragment()
        }
    }

    private fun loadBackupSettingsFragment() {
        supportFragmentManager.beginTransaction()
            .add(R.id.settings_fragment_container, BackupSettingsFragment.newInstance())
            .commit()
    }

    override fun toWalletBackupWithRecoveryPhrase(sourceFragment: Fragment) = addFragment(sourceFragment, WriteDownSeedPhraseFragment.newInstance())

    override fun toSeedPhraseVerification(sourceFragment: Fragment, seedWords: List<String>) =
        addFragment(sourceFragment, VerifySeedPhraseFragment.newInstance(seedWords))

    override fun toConfirmPassword(sourceFragment: Fragment) =
        addFragment(sourceFragment, EnterCurrentPasswordFragment.newInstance(), allowStateLoss = true)

    override fun toChangePassword(sourceFragment: Fragment) =
        addFragment(sourceFragment, ChangeSecurePasswordFragment.newInstance(), allowStateLoss = true)

    override fun onPasswordChanged(sourceFragment: Fragment) {
        if (supportFragmentManager.findFragmentByTag(EnterCurrentPasswordFragment::class.java.simpleName) != null) {
            supportFragmentManager.popBackStackImmediate(
                EnterCurrentPasswordFragment::class.java.simpleName,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        } else {
            onBackPressed()
        }
    }

    override fun onSeedPhraseVerificationComplete(sourceFragment: Fragment) {
        supportFragmentManager.popBackStackImmediate(WriteDownSeedPhraseFragment::class.java.simpleName, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }

    // nyarian:
    // allowStateLoss parameter is necessary to resolve device-specific issues like one
    // for samsung devices with biometrics enabled, as after launching the biometric prompt
    // onSaveInstanceState is called, and commit()ing any stuff after onSaveInstanceState is called
    // results into IllegalStateException: Can not perform this action after onSaveInstanceState
    private fun addFragment(sourceFragment: Fragment, fragment: Fragment, allowStateLoss: Boolean = false) {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
            .hide(sourceFragment)
            .add(R.id.settings_fragment_container, fragment, fragment.javaClass.simpleName)
            .addToBackStack(fragment.javaClass.simpleName)
            .apply { if (allowStateLoss) commitAllowingStateLoss() else commit() }
    }


    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, BackupSettingsActivity::class.java))
        }
    }

}
