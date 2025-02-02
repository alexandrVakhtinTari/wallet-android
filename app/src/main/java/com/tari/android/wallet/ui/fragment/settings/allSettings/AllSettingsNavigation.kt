package com.tari.android.wallet.ui.fragment.settings.allSettings

sealed class AllSettingsNavigation() {
    object ToBackupSettings : AllSettingsNavigation()
    object ToDeleteWallet : AllSettingsNavigation()
    object ToBackgroundService : AllSettingsNavigation()
    object ToNetworkSelection : AllSettingsNavigation()
    object ToBaseNodeSelection : AllSettingsNavigation()
}