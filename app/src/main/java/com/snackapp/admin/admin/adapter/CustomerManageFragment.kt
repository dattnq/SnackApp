package com.snackapp.admin.admin.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.database.*
import com.snackapp.admin.R
import com.snackapp.admin.admin.adapter.CustomerAdminAdapter
import com.snackapp.admin.databinding.DialogEditCustomerBinding
import com.snackapp.admin.databinding.FragmentCustomerManageBinding
import com.snackapp.admin.model.User

class CustomerManageFragment : Fragment() {

    private var _binding: FragmentCustomerManageBinding? = null
    private val binding get() = _binding!!

    private val usersRef = FirebaseDatabase.getInstance().getReference("Users")
    private val allCustomers = mutableListOf<User>()
    private lateinit var adapter: CustomerAdminAdapter
    private var userListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerManageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupSearch()
        listenCustomers()
    }

    // ──────────────────────────────────────────────
    // Setup
    // ──────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = CustomerAdminAdapter(
            onEdit = { user -> showEditDialog(user) },
            onDelete = { user -> confirmDelete(user) }
        )
        binding.rvCustomers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCustomers.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filterCustomers()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // ──────────────────────────────────────────────
    // Firebase listener
    // ──────────────────────────────────────────────

    private fun listenCustomers() {
        userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                allCustomers.clear()
                for (child in snapshot.children) {
                    child.getValue(User::class.java)?.let { user ->
                        // Chỉ lấy khách hàng (role = customer)
                        if (user.role == "customer") allCustomers.add(user)
                    }
                }
                filterCustomers()
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Lỗi: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        usersRef.addValueEventListener(userListener!!)
    }

    private fun filterCustomers() {
        val query = binding.etSearch.text?.toString()?.trim() ?: ""
        val filtered = if (query.isEmpty()) {
            allCustomers.toList()
        } else {
            allCustomers.filter { user ->
                user.fullName.contains(query, ignoreCase = true) ||
                        user.email.contains(query, ignoreCase = true) ||
                        user.phone.contains(query, ignoreCase = true)
            }
        }

        adapter.submitList(filtered.toMutableList())

        // Update count label
        binding.tvCustomerCount.text =
            if (query.isEmpty()) "Tổng cộng ${allCustomers.size} khách hàng"
            else "Tìm thấy ${filtered.size} / ${allCustomers.size} khách hàng"

        // Empty state
        binding.layoutEmpty.visibility =
            if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    // ──────────────────────────────────────────────
    // Edit dialog
    // ──────────────────────────────────────────────

    private fun showEditDialog(user: User) {
        val dialogBinding = DialogEditCustomerBinding.inflate(layoutInflater)

        // Pre-fill fields
        dialogBinding.etFullName.setText(user.fullName)
        dialogBinding.etPhone.setText(user.phone)
        dialogBinding.etAddress.setText(user.address)
        dialogBinding.tvEmailReadonly.text = "✉️  ${user.email.ifEmpty { "Không có email" }}"

        // Load avatar
        if (user.avatarUrl.isNotEmpty()) {
            Glide.with(requireContext())
                .load(user.avatarUrl)
                .placeholder(R.drawable.ic_person)
                .circleCrop()
                .into(dialogBinding.ivDialogAvatar)
        } else {
            dialogBinding.ivDialogAvatar.setImageResource(R.drawable.ic_person)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("✏️  Chỉnh sửa khách hàng")
            .setView(dialogBinding.root)
            .setPositiveButton("Lưu") { _, _ ->
                val newName = dialogBinding.etFullName.text?.toString()?.trim() ?: ""
                val newPhone = dialogBinding.etPhone.text?.toString()?.trim() ?: ""
                val newAddress = dialogBinding.etAddress.text?.toString()?.trim() ?: ""

                if (newName.isEmpty()) {
                    Toast.makeText(requireContext(), "Tên không được để trống", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                saveCustomerChanges(user.uid, newName, newPhone, newAddress)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun saveCustomerChanges(uid: String, name: String, phone: String, address: String) {
        val updates = mapOf(
            "fullName" to name,
            "phone" to phone,
            "address" to address
        )
        usersRef.child(uid).updateChildren(updates)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(requireContext(), "✅ Đã cập nhật thông tin khách hàng", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(requireContext(), "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // ──────────────────────────────────────────────
    // Delete
    // ──────────────────────────────────────────────

    private fun confirmDelete(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️  Xác nhận xóa")
            .setMessage(
                "Bạn có chắc muốn xóa tài khoản của\n" +
                        "\"${user.fullName.ifEmpty { user.email }}\"?\n\n" +
                        "Hành động này không thể hoàn tác."
            )
            .setPositiveButton("Xóa") { _, _ -> deleteCustomer(user) }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteCustomer(user: User) {
        usersRef.child(user.uid).removeValue()
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "🗑️ Đã xóa tài khoản ${user.fullName.ifEmpty { user.email }}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(requireContext(), "Xóa thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // ──────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────

    override fun onDestroyView() {
        userListener?.let { usersRef.removeEventListener(it) }
        super.onDestroyView()
        _binding = null
    }
}