package com.tari.android.wallet.ui.fragment.settings.logs.logFiles.adapter

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import java.io.File

class LogFileViewHolderItem(val filename: String, val file: File, val action: (item: LogFileViewHolderItem) -> Unit) : CommonViewHolderItem()