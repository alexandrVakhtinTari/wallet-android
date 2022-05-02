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
package com.tari.android.wallet.model

import android.os.Parcel
import android.os.Parcelable
import com.tari.android.wallet.extension.toMicroTari
import java.io.Serializable
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * This wrapper is needed for amount parameters in AIDL methods.
 *
 * @author The Tari Development Team
 */
class MicroTari() : Parcelable, Comparable<MicroTari>, Serializable {

    var value = BigInteger("0")

    private val million = BigDecimal(1e6)
    val tariValue: BigDecimal
        // Note: BigDecimal keeps track of both precision and scale, 1e6 != 1_000_000 in this case (scale 6, scale 0)
        get() = value.toBigDecimal().divide(million,6,RoundingMode.HALF_UP)

    val formattedTariValue: String
        get() = getFormattedValue(tariValue.toString())

    val formattedValue: String
        get() = getFormattedValue(value.toString())

    private fun getFormattedValue(value: String): String = value.trimEnd { it == '0' }.trimEnd { it == '.' }.trimEnd { it == ',' }

    constructor(
        value: BigInteger
    ) : this() {
        this.value = value
    }

    // region operator overloadings

    operator fun plusAssign(increment: MicroTari) {
        this.value += increment.value
    }

    operator fun plus(increment: MicroTari): MicroTari {
        return MicroTari(this.value + increment.value)
    }

    operator fun plus(increment: Int): MicroTari {
        return this + increment.toMicroTari()
    }

    operator fun plus(increment: Long): MicroTari {
        return this + increment.toMicroTari()
    }

    operator fun minusAssign(decrement: MicroTari) {
        this.value -= decrement.value
    }

    operator fun minus(decrement: MicroTari): MicroTari {
        return MicroTari(this.value - decrement.value)
    }

    operator fun minus(decrement: Int): MicroTari {
        return this - decrement.toMicroTari()
    }

    operator fun minus(decrement: Long): MicroTari {
        return this - decrement.toMicroTari()
    }

    // endregion

    // region Parcelable

    constructor(parcel: Parcel) : this() {
        readFromParcel(parcel)
    }

    companion object CREATOR : Parcelable.Creator<MicroTari> {

        override fun createFromParcel(parcel: Parcel): MicroTari {
            return MicroTari(parcel)
        }

        override fun newArray(size: Int): Array<MicroTari> {
            return Array(size) { MicroTari() }
        }

    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(value)
    }

    private fun readFromParcel(inParcel: Parcel) {
        value = inParcel.readSerializable() as BigInteger
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun compareTo(other: MicroTari): Int {
        return this.value.compareTo(other.value)
    }

    // endregion

}