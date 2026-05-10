package com.snackapp.admin.admin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.snackapp.admin.R
import com.snackapp.admin.databinding.ItemProductAdminBinding
import com.snackapp.admin.model.Product
import java.util.Locale

class ProductAdminAdapter(
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit
) : ListAdapter<Product, ProductAdminAdapter.VH>(DiffCallback()) {

    inner class VH(val binding: ItemProductAdminBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemProductAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = getItem(position)
        val context = holder.binding.root.context
        with(holder.binding) {
            tvProductName.text = p.name
            tvPrice.text = String.format(Locale("vi", "VN"), "%,.0f₫", p.price)
            tvCategory.text = p.category.ifEmpty { "Chưa phân loại" }
            tvStock.text = context.getString(R.string.label_stock_sold_format, p.stock, p.sold)

            Glide.with(root.context)
                .load(p.imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(ivProduct)

            btnEdit.setOnClickListener { onEdit(p) }
            btnDelete.setOnClickListener { onDelete(p) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product) =
            oldItem.productId == newItem.productId
        override fun areContentsTheSame(oldItem: Product, newItem: Product) = oldItem == newItem
    }
}
