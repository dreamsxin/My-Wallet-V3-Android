package piuk.blockchain.android.ui.receive

import android.graphics.Bitmap
import android.support.annotation.StringRes
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.ui.base.View
import piuk.blockchain.android.ui.customviews.ToastCustom
import java.util.*

interface ReceiveView : View {

    val isContactsEnabled: Boolean

    val locale: Locale

    fun getQrBitmap(): Bitmap

    fun getContactName(): String

    fun getBtcAmount(): String

    fun showQrLoading()

    fun showQrCode(bitmap: Bitmap?)

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun updateFiatTextField(text: String)

    fun updateBtcTextField(text: String)

    fun startContactSelectionActivity()

    fun updateReceiveAddress(address: String)

    fun hideContactsIntroduction()

    fun showContactsIntroduction()

    fun showWatchOnlyWarning()

    fun updateReceiveLabel(label: String)

    fun showBottomSheet(uri: String)

    fun setSelectedCurrency(cryptoCurrency: CryptoCurrencies)

    fun finishPage()

}
