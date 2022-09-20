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

/**
 * Wrapper for native private key type.
 *
 * @author The Tari Development Team
 */
class FFIPublicKey() : FFIBase() {

    // region JNI

    private external fun jniGetBytes(
        libError: FFIError
    ): FFIPointer

    private external fun jniDestroy()
    private external fun jniCreate(
        byteVectorPtr: FFIByteVector,
        libError: FFIError
    )

    private external fun jniFromHex(hexStr: String, libError: FFIError)

    private external fun jniFromEmojiId(emoji: String, libError: FFIError)

    private external fun jniFromPrivateKey(
        privateKeyPtr: FFIPrivateKey,
        libError: FFIError
    )

    private external fun jniGetEmojiId(
        libError: FFIError
    ): String

    // endregion

    constructor(pointer: FFIPointer): this() {
        this.pointer = pointer
    }

    constructor(byteVector: FFIByteVector) : this() {
        val error = FFIError()
        jniCreate(byteVector, error)
        throwIf(error)
    }

    constructor(hex: HexString) : this() {
        if (hex.toString().length == 64) {
            val error = FFIError()
            jniFromHex(hex.hex, error)
            throwIf(error)
        } else {
            throw FFIException(message = "HexString is not a valid PublicKey")
        }
    }

    constructor(emojiId: String) : this() {
        val error = FFIError()
        jniFromEmojiId(emojiId, error)
        throwIf(error)
    }

    constructor(privateKey: FFIPrivateKey) : this() {
        val error = FFIError()
        jniFromPrivateKey(privateKey, error)
        throwIf(error)
    }

    fun getBytes(): FFIByteVector {
        val error = FFIError()
        val result = FFIByteVector(jniGetBytes(error))
        throwIf(error)
        return result
    }

    fun getEmojiId(): String {
        val error = FFIError()
        val result = jniGetEmojiId(error)
        throwIf(error)
        return result
    }

    override fun toString(): String {
        val error = FFIError()
        val result = FFIByteVector(jniGetBytes(error)).toString()
        throwIf(error)
        return result
    }

    override fun destroy() {
        jniDestroy()
    }

}
