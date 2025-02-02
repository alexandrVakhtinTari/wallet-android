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
package com.tari.android.wallet.ui.activity.debug

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ActivityDebugBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.ui.activity.debug.adapter.DebugViewPagerAdapter
import com.tari.android.wallet.ui.extension.addEnterLeftAnimation
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.BaseNodeConfigRouter
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.addBaseNode.AddCustomBaseNodeFragment
import com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.changeBaseNode.ChangeBaseNodeFragment

/**
 * Contains debug fragments.
 *
 * @author The Tari Development Team
 */
internal class DebugActivity : AppCompatActivity(), BaseNodeConfigRouter {

    private lateinit var pagerAdapter: DebugViewPagerAdapter

    private lateinit var ui: ActivityDebugBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityDebugBinding.inflate(layoutInflater).apply { setContentView(root) }
        appComponent.inject(this)
        pagerAdapter = DebugViewPagerAdapter(this)
        ui.viewPager.adapter = pagerAdapter
        ui.viewPager.offscreenPageLimit = 2
        TabLayoutMediator(ui.tabLayout, ui.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> string(R.string.debug_log_files_title)
                1 -> string(R.string.debug_base_node_title)
                2 -> string(R.string.debug_about_title)
                else -> throw RuntimeException("Unexpected position: $position")
            }
        }.attach()
        ui.backButton.setOnClickListener { onBackPressed() }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }

    override fun toAddCustomBaseNode() = loadFragment(AddCustomBaseNodeFragment.getNewInstance())

    override fun toChangeBaseNode() = loadFragment(ChangeBaseNodeFragment.getNewInstance())

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .addEnterLeftAnimation()
            .apply { supportFragmentManager.fragments.forEach { hide(it) } }
            .add(R.id.debug_root, fragment)
            .addToBackStack(null)
            .commit()
    }
}
