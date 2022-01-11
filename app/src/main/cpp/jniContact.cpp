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

#include <jni.h>
#include <android/log.h>
#include <wallet.h>
#include <string>
#include <cmath>
#include <android/log.h>
#include "jniCommon.cpp"

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIContact_jniCreate(
        JNIEnv *jEnv,
        jobject jThis,
        jstring jAlias,
        jobject jPublicKey,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    const char *pAlias = jEnv->GetStringUTFChars(jAlias, JNI_FALSE);
    jlong lPublicKey = GetPointerField(jEnv, jPublicKey);
    auto *pPublicKey = reinterpret_cast<TariPublicKey *>(lPublicKey);
    TariContact *pContact = contact_create(pAlias, pPublicKey, errorCodePointer);
    setErrorCode(jEnv, error, errorCode);
    jEnv->ReleaseStringUTFChars(jAlias, pAlias);
    SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(pContact));
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_tari_android_wallet_ffi_FFIContact_jniGetAlias(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    jlong lContact = GetPointerField(jEnv, jThis);
    auto *pContact = reinterpret_cast<TariContact *>(lContact);
    const char *pAlias = contact_get_alias(pContact, errorCodePointer);
    setErrorCode(jEnv, error, errorCode);
    jstring result = jEnv->NewStringUTF(pAlias);
    string_destroy(const_cast<char *>(pAlias));
    return result;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_tari_android_wallet_ffi_FFIContact_jniGetPublicKey(
        JNIEnv *jEnv,
        jobject jThis,
        jobject error) {
    int errorCode = 0;
    int *errorCodePointer = &errorCode;
    jlong lContact = GetPointerField(jEnv, jThis);
    auto *pContact = reinterpret_cast<TariContact *>(lContact);
    auto result = reinterpret_cast<jlong>(contact_get_public_key(pContact, errorCodePointer));
    setErrorCode(jEnv, error, errorCode);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_tari_android_wallet_ffi_FFIContact_jniDestroy(
        JNIEnv *jEnv,
        jobject jThis) {
    jlong lContact = GetPointerField(jEnv, jThis);
    contact_destroy(reinterpret_cast<TariContact *>(lContact));
    SetPointerField(jEnv, jThis, reinterpret_cast<jlong>(nullptr));
}
