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
package com.tari.android.wallet.infrastructure.backup

import com.orhanobut.logger.Logger
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.extension.compress
import com.tari.android.wallet.extension.encrypt
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.infrastructure.backup.compress.CompressionMethod
import com.tari.android.wallet.infrastructure.security.encryption.SymmetricEncryptionAlgorithm
import com.tari.android.wallet.ui.fragment.settings.backup.BackupSettingsRepository
import org.joda.time.DateTime
import java.io.File

/**
 * Utility functions to prepares the backup or restoration files.
 *
 * @author The Tari Development Team
 */
class BackupFileProcessor(
    private val backupSettingsRepository: BackupSettingsRepository,
    private val walletConfig: WalletConfig,
    private val namingPolicy: BackupNamingPolicy,
) {
    private val logger
        get() = Logger.t(BackupFileProcessor::class.simpleName)

    fun generateBackupFile(newPassword: CharArray? = null): Triple<File, DateTime, String> {
        // decrypt database
        FFIWallet.instance?.removeEncryption()
        backupSettingsRepository.backupPassword = null

        // create partial backup in temp folder if password not set
        val databaseFile = File(walletConfig.walletDatabaseFilePath)
        val backupPassword = newPassword ?: backupSettingsRepository.backupPassword?.toCharArray()
        val backupDate = DateTime.now()
        // zip the file
        val compressionMethod = CompressionMethod.zip()
        var mimeType = compressionMethod.mimeType
        val backupFileName = namingPolicy.getBackupFileName(backupDate)
        val compressedFile = File(
            walletConfig.getWalletTempDirPath(),
            "$backupFileName.${compressionMethod.extension}"
        )
        var fileToBackup = listOf(databaseFile).compress(
            CompressionMethod.zip(),
            compressedFile.absolutePath
        )
        // encrypt the file if password is set
        val encryptionAlgorithm = SymmetricEncryptionAlgorithm.aes()
        if (backupPassword != null) {
            val targetFileName = "$backupFileName.${encryptionAlgorithm.extension}"
            fileToBackup = fileToBackup.encrypt(
                encryptionAlgorithm,
                backupPassword,
                File(walletConfig.getWalletTempDirPath(), targetFileName).absolutePath
            )
            mimeType = encryptionAlgorithm.mimeType
        }
        // encrypt after finish backup
        FFIWallet.instance?.enableEncryption()
        logger.i("Backup files was generated")

        return Triple(fileToBackup, backupDate, mimeType)
    }

    fun restoreBackupFile(file: File, password: String? = null) {
        val walletFilesDir = File(walletConfig.getWalletFilesDirPath())
        val compressionMethod = CompressionMethod.zip()
        // encrypt file if password is supplied
        if (!password.isNullOrEmpty()) {
            val unencryptedCompressedFile = File(
                walletConfig.getWalletTempDirPath(),
                "temp_" + System.currentTimeMillis() + "." + compressionMethod.extension
            )
            unencryptedCompressedFile.createNewFile()
            SymmetricEncryptionAlgorithm.aes().decrypt(
                password.toCharArray(),
                { file.inputStream() },
                { unencryptedCompressedFile.outputStream() }
            )
            CompressionMethod.zip().uncompress(
                unencryptedCompressedFile,
                walletFilesDir
            )
            if (!File(walletConfig.walletDatabaseFilePath).exists()) {
                // delete uncompressed files
                walletFilesDir.deleteRecursively()
                throw BackupStorageTamperedException("Invalid encrypted backup.")
            }
            logger.i("Backup file was restored")
        } else {
            CompressionMethod.zip().uncompress(file, walletFilesDir)
            // check if wallet database file exists
            if (!File(walletConfig.walletDatabaseFilePath).exists()) {
                walletFilesDir.listFiles()?.let { files ->
                    // delete uncompressed files
                    for (extractedFile in files) {
                        if (extractedFile.isFile) {
                            extractedFile.delete()
                        }
                    }
                }
                logger.i("Backup file is encrypted")
                // throw exception
                throw BackupFileIsEncryptedException("Cannot uncompress. Restored file is encrypted.")
            }
        }
    }

    fun clearTempFolder() {
        try {
            File(walletConfig.getWalletTempDirPath()).listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            logger.e(e, "Cleaning temp files")
        }
    }

}