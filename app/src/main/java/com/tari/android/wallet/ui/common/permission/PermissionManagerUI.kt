package com.tari.android.wallet.ui.common.permission

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.orhanobut.logger.Printer
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.icon.IconModule


class PermissionManagerUI(val fragment: CommonFragment<*, *>) {

    var grantedAction: () -> Unit = {}
    var notGrantedAction: () -> Unit = {}

    private val logger: Printer
        get() = com.orhanobut.logger.Logger.t("permission")

    fun runWithPermissions(vararg permissions: String, openSettings: Boolean = false, callback: () -> Unit): Unit = with(fragment) {
        logger.d("runWithPermissions: start")

        grantedAction = { runWithPermissions(*permissions, openSettings = openSettings, callback = callback) }


        if (this.isDetached) return

        for (permission in permissions) {
            if (isPermissionGranted(permission)) {
                logger.d("permission granted: $permission")
            } else {
                launcher.launch(permission)

                if (shouldShowRequestPermissionRationale(permission)) {
                    launcher.launch(permission)
                    return
                } else {
                    if (openSettings) {
                        showRationalPermissionDialog(permission)
                    }
                }
                return
            }
        }
        callback()
    }

    fun runWithPermission(permission: String, openSettings: Boolean = false, callback: () -> Unit) = with(fragment) {
        logger.d("runWithPermissions: start")

        if (this.isDetached) return

        if (isPermissionGranted(permission)) {
            logger.d("permission granted: $permission")
            callback()
        } else {
            if (shouldShowRequestPermissionRationale(permission)) {
                grantedAction = callback
                launcher.launch(permission)
            } else {
                if (openSettings) {
                    showRationalPermissionDialog(permission)
                }
            }
        }
    }

    private fun showRationalPermissionDialog(permission: String) {
        val permissionName = kotlin.runCatching {
            val packageManager: PackageManager = fragment.requireContext().packageManager
            val permissionInfo = packageManager.getPermissionInfo(permission, 0)
            permissionInfo.labelRes
            fragment.getString(permissionInfo.labelRes)
        }.getOrNull() ?: permission

        val args = ModularDialogArgs(
            DialogArgs(), listOf(
                IconModule(R.drawable.vector_sharing_failed),
                HeadModule(fragment.getString(R.string.common_error_title)),
                BodyModule(fragment.getString(R.string.common_permission_required_dialog_body, permissionName)),
                ButtonModule(fragment.getString(R.string.common_permission_required_button), ButtonStyle.Normal) {
                    openSettings()
                },
                ButtonModule(fragment.getString(R.string.common_cancel), ButtonStyle.Close)
            )
        )
        fragment.viewModel.modularDialog.postValue(args)
    }

    fun isPermissionGranted(permission: String): Boolean = isPermissionNotGranted(permission).not()

    fun isPermissionNotGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(fragment.requireContext(), permission) == PackageManager.PERMISSION_DENIED

    fun openSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", fragment.requireContext().packageName, null)
            ContextCompat.startActivity(fragment.requireContext(), this, Bundle())
        }
    }
}