package com.snackapp.admin.customer.adapter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.snackapp.admin.R
import com.snackapp.admin.databinding.ItemMenuProductBinding
import com.snackapp.admin.model.Product
import com.snackapp.admin.util.CartManager


class MenuAdapter(
    private val onClick: (Product) -> Unit
) : ListAdapter<Product, MenuAdapter.VH>(DiffCallback())
{
    inner class VH(val binding: ItemMenuProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemMenuProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = getItem(position)
        with(holder.binding) {
            tvName.text  = p.name
            tvPrice.text = "%.0f₫".format(p.price)
            tvCategory.text = p.category

            Glide.with(root.context)
                .load(p.imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .centerCrop()
                .into(ivProduct)

            // Hiển thị icon giỏ hàng nếu đã thêm
            btnAddCart.setImageResource(
                if (CartManager.isInCart(p.productId)) R.drawable.ic_cart_filled
                else R.drawable.ic_cart
            )

            btnAddCart.setOnClickListener {
                CartManager.addItem(p)
                btnAddCart.setImageResource(R.drawable.ic_cart_filled)
            }

            root.setOnClickListener { onClick(p) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(a: Product, b: Product) = a.productId == b.productId
        override fun areContentsTheSame(a: Product, b: Product) = a == b
    }

}