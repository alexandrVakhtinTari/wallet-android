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
package com.tari.android.wallet.application.deeplinks

import com.tari.android.wallet.model.MicroTari
import java.math.BigInteger

/**
 * Parses a deep link and contains the structured deep link details.
 *
 * @author The Tari Development Team
 */
sealed class DeepLink {

    open fun getParams(): Map<String, String> = emptyMap()
    open fun getCommand(): String = ""

    class Send(val publicKeyHex: String = "", val amount: MicroTari? = null, val note: String = "") : DeepLink() {

        constructor(params: Map<String, String>) : this(
            params[publicKeyKey].orEmpty(),
            params[amountKey]?.let { if (it.isEmpty()) null else MicroTari(BigInteger(it)) },
            params[noteKey].orEmpty()
        )

        override fun getParams(): Map<String, String> = hashMapOf<String, String>().apply {
            put(publicKeyKey, publicKeyHex)
            put(amountKey, amount?.formattedValue.orEmpty())
            put(noteKey, note)
        }

        override fun getCommand(): String = sendCommand

        companion object {
            const val sendCommand = "transactions/send"
            const val publicKeyKey = "publicKey"
            const val amountKey = "amount"
            const val noteKey = "note"
        }
    }

    class AddBaseNode(val name: String = "", val peer: String = "") : DeepLink() {

        constructor(params: Map<String, String>) : this(
            params[nameKey].orEmpty(),
            params[peerKey].orEmpty(),
        )

        override fun getParams(): Map<String, String> = hashMapOf<String, String>().apply {
            put(nameKey, name)
            put(peerKey, peer)
        }

        override fun getCommand(): String = addNodeCommand

        companion object {
            const val addNodeCommand = "base_nodes/add"
            const val nameKey = "name"
            const val peerKey = "peer"
        }
    }

    companion object {

        fun getByCommand(command: String, params: Map<String, String>): DeepLink? {
            return when (command) {
                Send.sendCommand -> Send(params)
                AddBaseNode.addNodeCommand -> AddBaseNode(params)
                else -> null
            }
        }
    }
}