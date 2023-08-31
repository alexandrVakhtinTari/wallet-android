package com.tari.android.wallet.ui.dialog.modular.modules.customBaseNodeBody

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewCustomBaseNodeFullDescriptionBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import yat.android.ui.extension.HtmlHelper

@SuppressLint("ViewConstructor")
class CustomBaseNodeBodyModuleView(context: Context, module: CustomBaseNodeBodyModule) : CommonView<CommonViewModel, ViewCustomBaseNodeFullDescriptionBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): ViewCustomBaseNodeFullDescriptionBinding =
        ViewCustomBaseNodeFullDescriptionBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        val peer = module.baseNodeInfo.publicKeyHex + "::" + module.baseNodeInfo.address
        val pattern = HtmlHelper.getSpannedText(context.getString(R.string.home_custom_base_node_full_description, module.baseNodeInfo.name, peer))
        ui.descriptionText.text = pattern
    }
}