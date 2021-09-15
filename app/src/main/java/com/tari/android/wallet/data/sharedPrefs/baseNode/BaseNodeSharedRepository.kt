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
package com.tari.android.wallet.data.sharedPrefs.baseNode

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository.Key.currentBaseNodeField
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository.Key.userBaseNodeListField
import com.tari.android.wallet.model.BaseNodeValidationResult

class BaseNodeSharedRepository(
    private val context: Context,
    private val sharedPrefs: SharedPreferences
) {

    private val gson = Gson()

    private object Key {
        const val currentBaseNodeField = "tari_wallet_current_base_node"
        const val userBaseNodeListField = "tari_wallet_user_base_nodes"

        const val baseNodeLastSyncResult = "tari_wallet_base_node_last_sync_result"
    }


    //todo extract to delegate
    var currentBaseNode: BaseNodeDto?
        get() {
            val savedValue = sharedPrefs.getString(currentBaseNodeField, null)
            return if (savedValue == null) null else gson.fromJson(savedValue, BaseNodeDto::class.java) as BaseNodeDto
        }
        set(value) {
            sharedPrefs.edit().run {
                if (value == null) {
                    remove(currentBaseNodeField)
                } else {
                    putString(currentBaseNodeField, gson.toJson(value, BaseNodeDto::class.java))
                    apply()
                }
            }
        }

    var userBaseNodes: BaseNodeList
        get() {
            val savedValue = sharedPrefs.getString(userBaseNodeListField, null)
            return if (savedValue == null) BaseNodeList().apply { userBaseNodes = this } else {
                gson.fromJson(savedValue, BaseNodeList::class.java) as BaseNodeList
            }
        }
        set(value) {
            sharedPrefs.edit().run {
                putString(userBaseNodeListField, gson.toJson(value, BaseNodeList::class.java))
                apply()
            }
        }

    var baseNodeLastSyncResult: BaseNodeValidationResult?
        get() = try {
            BaseNodeValidationResult.map(sharedPrefs.getInt(Key.baseNodeLastSyncResult, -1))
        } catch (exception: Exception) {
            null
        }
        set(value) = sharedPrefs.edit().run {
            putInt(Key.baseNodeLastSyncResult, value?.status ?: -1)
            apply()
        }

    fun deleteUserBaseNode(baseNodeDto: BaseNodeDto) {
        userBaseNodes.apply {
            remove(baseNodeDto)
            userBaseNodes = this
        }
    }

    fun addUserBaseNode(baseNodeDto: BaseNodeDto) {
        userBaseNodes.apply {
            add(baseNodeDto)
            userBaseNodes = this
        }
    }

    fun clear() {
        baseNodeLastSyncResult = null
        currentBaseNode = null
        val baseNodes = userBaseNodes
        baseNodes.clear()
        userBaseNodes = baseNodes
    }
}