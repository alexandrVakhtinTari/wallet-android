package com.tari.android.wallet.ui.component.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialog
import com.tari.android.wallet.ui.dialog.error.ErrorDialog

abstract class CommonView<VM : CommonViewModel, VB : ViewBinding> : LinearLayout {
    lateinit var viewModel: VM

    lateinit var binding: VB
        private set

    abstract fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): VB

    lateinit var viewLifecycle: LifecycleOwner

    constructor(context: Context) : super(context, null) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        binding = bindingInflate(LayoutInflater.from(context), this, true)

        setup()
    }

    abstract fun setup()

    open fun bindViewModel(viewModel: VM) {
        this.viewModel = viewModel

        viewModel.openLink.observe(viewLifecycle) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }

        viewModel.confirmDialog.observe(viewLifecycle) { ConfirmDialog(context, it).show() }

        viewModel.errorDialog.observe(viewLifecycle) { ErrorDialog(context, it).show() }
    }
}