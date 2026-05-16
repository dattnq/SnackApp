package com.snackapp.admin.customer.adapter
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.snackapp.admin.databinding.ItemOrderHistoryBinding
import com.snackapp.admin.model.Order

class OrderHistoryAdapter : ListAdapter<Order, OrderHistoryAdapter.VH>(DiffCallback()){

    inner class VH(val binding: ItemOrderHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemOrderHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val order = getItem(position)
        with(holder.binding) {
            tvOrderId.text   = "#${order.orderId.takeLast(6).uppercase()}"
            tvDate.text      = order.orderDate
            tvTotal.text     = "%.0f₫".format(order.totalAmount)
            tvItemCount.text = "${order.items.size} món"
            tvAddress.text   = order.address
            tvStatus.text    = order.status

            tvStatus.setBackgroundColor(
                if (order.status == Order.STATUS_DELIVERED)
                    Color.parseColor("#4CAF50")
                else Color.parseColor("#FF9800")
            )

            // Liệt kê tên các món
            tvItems.text = order.items.joinToString(" • ") {
                "${it.productName} x${it.quantity}"
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(a: Order, b: Order) = a.orderId == b.orderId
        override fun areContentsTheSame(a: Order, b: Order) = a == b
    }
}