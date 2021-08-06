package com.tari.android.wallet.ui.component.tooltip
import android.app.ActionBar
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import com.orhanobut.logger.Logger
import com.tari.android.wallet.databinding.TooltipLayoutBinding

class TooltipWindow(ctx: Context, text: String?) {
    private val binding: TooltipLayoutBinding
    private val tipWindow: PopupWindow?

    init {
        Logger.i("tooltip_window_1")

        tipWindow = PopupWindow(ctx)
        binding = TooltipLayoutBinding.inflate(LayoutInflater.from(ctx), null, false)
        binding.tooltipText.text = text
    }

    fun showToolTip(anchor: View) = with(tipWindow) {
        Logger.i("tooltip_window_2")

        tipWindow!!.height = ActionBar.LayoutParams.WRAP_CONTENT
        tipWindow.width = ActionBar.LayoutParams.WRAP_CONTENT
        tipWindow.isOutsideTouchable = true
        tipWindow.isTouchable = false
        tipWindow.isFocusable = false
        tipWindow.setBackgroundDrawable(BitmapDrawable())
        tipWindow.contentView = binding.root

        val anchor_rect = Rect(
            screen_pos.get(0), screen_pos.get(1), screen_pos.get(0)
                    + anchor.width, screen_pos.get(1) + anchor.height
        )

        val contentViewHeight = binding.root.measuredHeight
        val contentViewWidth = binding.root.measuredWidth

        var position_x = 0
        var position_y = 0

        position_x = anchor_rect.centerX() - (contentViewWidth - contentViewWidth / 2)
        position_y = anchor_rect.bottom - anchor_rect.height() / 2 + 10
//            DRAW_TOP -> {
//                position_x = anchor_rect.centerX() - (contentViewWidth - contentViewWidth / 2)
//                position_y = anchor_rect.top - anchor_rect.height()
//            }
        }

        tipWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, 0, 0)

        Logger.i("tooltip_window_3")
    }

    val isTooltipShown: Boolean
        get() = tipWindow != null && tipWindow.isShowing

    fun dismissTooltip() {
        if (tipWindow != null && tipWindow.isShowing) tipWindow.dismiss()
    }
}