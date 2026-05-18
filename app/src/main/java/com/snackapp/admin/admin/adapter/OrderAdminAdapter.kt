package com.snackapp.admin.admin.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.snackapp.admin.R
import com.snackapp.admin.databinding.ItemOrderAdminBinding
import com.snackapp.admin.model.Order
import java.util.Locale

class OrderAdminAdapter(
    private val onStatusChange: (Order, String) -> Unit
) : ListAdapter<Order, OrderAdminAdapter.VH>(DiffCallback()) {

    inner class VH(val binding: ItemOrderAdminBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemOrderAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val order = getItem(position)
        val context = holder.binding.root.context
        
        with(holder.binding) {
            tvOrderId.text = context.getString(R.string.order_id_format, order.orderId.takeLast(6).uppercase())
            tvCustomerName.text = order.customerName
            tvCustomerPhone.text = order.customerPhone
            tvOrderDate.text = order.orderDate
            tvTotal.text = String.format(Locale("vi", "VN"), "%,.0f₫", order.totalAmount)
            tvAddress.text = order.address
            tvItemCount.text = context.getString(R.string.item_count_format, order.items.size)

            // Trạng thái badge màu
            tvStatus.text = order.status
            val colorRes = if (order.status == Order.STATUS_DELIVERED)
                R.color.color_green
            else
                R.color.color_yellow
            
            tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))

            // Nút đổi trạng thái
            if (order.status == Order.STATUS_PENDING) {
                btnChangeStatus.text = "✓ Xác nhận giao"
                btnChangeStatus.isEnabled = true
                btnChangeStatus.alpha = 1.0f
                btnChangeStatus.setOnClickListener {
                    onStatusChange(order, Order.STATUS_DELIVERED)
                }
            } else {
                btnChangeStatus.text = "Đã giao"
                btnChangeStatus.isEnabled = false
                btnChangeStatus.alpha = 0.5f
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(a: Order, b: Order) = a.orderId == b.orderId
        override fun areContentsTheSame(a: Order, b: Order) = a == b
    }
}
