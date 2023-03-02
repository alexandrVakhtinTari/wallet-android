package com.tari.android.wallet.ui.fragment.contact_book.root

import androidx.fragment.app.FragmentActivity
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.IContact

interface ContactBookRouter {

    fun toContactDetails(contact: ContactDto)

    fun toSendTariToContact(contact: ContactDto)

    fun toRequestTariFromContact(contact: ContactDto)

    fun toAddContact()

    fun toAddContactName(contact: IContact)

    fun toFinalizeAddingContact()

    fun toLinkContact(contact: ContactDto)

    companion object {
        fun processNavigation(appCompatActivity: FragmentActivity, navigation: ContactBookNavigation) {
            val router = appCompatActivity as ContactBookRouter
            when(navigation) {
                is ContactBookNavigation.ToAddContact -> router.toAddContact()
                is ContactBookNavigation.ToContactDetails -> router.toContactDetails(navigation.contact)
                is ContactBookNavigation.ToRequestTari -> router.toRequestTariFromContact(navigation.contact)
                is ContactBookNavigation.ToSendTari -> router.toSendTariToContact(navigation.contact)
                is ContactBookNavigation.ToAddContactName -> router.toAddContactName(navigation.contact)
                is ContactBookNavigation.ToFinalizeAddingContact -> router.toFinalizeAddingContact()
                is ContactBookNavigation.ToLinkContact -> router.toLinkContact(navigation.contact)
                is ContactBookNavigation.ToExternalWallet -> Unit //todo
            }
        }
    }
}