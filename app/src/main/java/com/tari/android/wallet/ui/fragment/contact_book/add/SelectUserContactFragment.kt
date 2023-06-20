package com.tari.android.wallet.ui.fragment.contact_book.add

import android.os.Bundle
import android.view.View
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.contact_book.contactSelection.ContactSelectionFragment
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation

class SelectUserContactFragment : ContactSelectionFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ui.toolbar.ui.toolbarTitle.text = string(R.string.transaction_send_to)
        ui.addFirstNameInput.gone()
        ui.addSurnameInput.gone()

        viewModel.isContactlessPayment.postValue(true)
        viewModel.additionalFilter = { it.contact.getFFIDto() != null }
    }

    override fun goToNext() {
        super.goToNext()

        val user = viewModel.getUserDto()
        viewModel.navigation.postValue(Navigation.TxListNavigation.ToSendTariToUser(user))
    }
}