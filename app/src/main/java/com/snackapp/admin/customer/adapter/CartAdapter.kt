package com.snackapp.admin.customer.adapter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.snackapp.admin.R
import com.snackapp.admin.databinding.ItemCartBinding
import com.snackapp.admin.model.CartItem

class CartAdapter(
    private val onIncrease: (CartItem) -> Unit,
    private val onDecrease: (CartItem) -> Unit,
    private val onRemove:   (CartItem) -> Unit
) : ListAdapter<CartItem, CartAdapter.VH>(DiffCallback())
{
    inner class VH(val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            tvName.text     = item.productName
            tvPrice.text    = "%.0f₫".format(item.price)
            tvSubtotal.text = "%.0f₫".format(item.subtotal())
            tvQuantity.text = item.quantity.toString()

            Glide.with(root.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .centerCrop()
                .into(ivProduct)

            btnIncrease.setOnClickListener { onIncrease(item) }
            btnDecrease.setOnClickListener { onDecrease(item) }
            btnRemove.setOnClickListener   { onRemove(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(a: CartItem, b: CartItem) = a.productId == b.productId
        override fun areContentsTheSame(a: CartItem, b: CartItem) = a == b
    }

}