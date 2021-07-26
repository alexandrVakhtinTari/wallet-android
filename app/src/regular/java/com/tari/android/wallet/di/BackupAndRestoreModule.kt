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
package com.tari.android.wallet.di

import android.content.Context
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.infrastructure.backup.*
import com.tari.android.wallet.infrastructure.backup.GoogleDriveBackupStorage
import com.tari.android.wallet.notification.NotificationHelper
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
internal class BackupAndRestoreModule {

    @Provides
    @Singleton
    fun provideBackupFileProcessor(
        sharedPrefs: SharedPrefsRepository,
        @Named(WalletModule.FieldName.walletFilesDirPath) walletFilesDirPath: String,
        @Named(WalletModule.FieldName.walletDatabaseFilePath) walletDatabaseFilePath: String,
        @Named(WalletModule.FieldName.walletTempDirPath) walletTempDirPath: String
    ): BackupFileProcessor = BackupFileProcessor(
        sharedPrefs,
        walletFilesDirPath,
        walletDatabaseFilePath,
        walletTempDirPath
    )

    @Provides
    @Singleton
    fun provideBackupStorage(
        context: Context,
        sharedPrefs: SharedPrefsRepository,
        @Named(WalletModule.FieldName.walletTempDirPath) walletTempDirPath: String,
        backupFileProcessor: BackupFileProcessor
    ): BackupStorage = GoogleDriveBackupStorage(
        context,
        sharedPrefs,
        walletTempDirPath,
        backupFileProcessor
    )

    @Provides
    @Singleton
    fun provideBackupManager(
        context: Context,
        sharedPrefs: SharedPrefsRepository,
        backupStorage: BackupStorage,
        notificationHelper: NotificationHelper
    ): BackupManager = BackupManager(context, sharedPrefs, backupStorage, notificationHelper)

}
