package com.snackapp.admin.admin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.snackapp.admin.R
import com.snackapp.admin.databinding.ItemCustomerAdminBinding
import com.snackapp.admin.model.User

class CustomerAdminAdapter(
    private val onEdit: (User) -> Unit,
    private val onDelete: (User) -> Unit
) : ListAdapter<User, CustomerAdminAdapter.VH>(DiffCallback()) {

    inner class VH(val binding: ItemCustomerAdminBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemCustomerAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = getItem(position)
        with(holder.binding) {
            // Name
            tvFullName.text = user.fullName.ifEmpty { "Chưa cập nhật" }

            // Email
            tvEmail.text = user.email.ifEmpty { "—" }

            // Phone
            tvPhone.text = if (user.phone.isNotEmpty()) "📞 ${user.phone}" else "📞 Chưa có số"

            // UID short
            tvUid.text = "ID: ${user.uid.take(12)}…"

            // Avatar
            if (user.avatarUrl.isNotEmpty()) {
                Glide.with(root.context)
                    .load(user.avatarUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivAvatar)
            } else {
                ivAvatar.setImageResource(R.drawable.ic_person)
            }

            btnEdit.setOnClickListener { onEdit(user) }
            btnDelete.setOnClickListener { onDelete(user) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) =
            oldItem.uid == newItem.uid

        override fun areContentsTheSame(oldItem: User, newItem: User) =
            oldItem == newItem
    }
}