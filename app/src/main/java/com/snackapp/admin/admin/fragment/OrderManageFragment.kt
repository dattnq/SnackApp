package com.snackapp.admin.admin.fragment

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.snackapp.admin.admin.adapter.OrderAdminAdapter
import com.snackapp.admin.databinding.FragmentOrderManageBinding
import com.snackapp.admin.model.Order
import java.text.SimpleDateFormat
import java.util.*

class OrderManageFragment : Fragment() {

    private var _binding: FragmentOrderManageBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseDatabase.getInstance().getReference("Orders")
    private lateinit var adapter: OrderAdminAdapter
    
    private var currentQuery: Query? = null
    private var currentListener: ValueEventListener? = null
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentOrderManageBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = OrderAdminAdapter { order, newStatus ->
            updateOrderStatus(order, newStatus)
        }
        binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrders.adapter = adapter

        // Filter chips
        binding.chipAll.setOnClickListener { loadOrders(null) }
        binding.chipPending.setOnClickListener { loadOrders(Order.STATUS_PENDING) }
        binding.chipDelivered.setOnClickListener { loadOrders(Order.STATUS_DELIVERED) }

        loadOrders(null)
    }

    private fun loadOrders(filterStatus: String?) {
        // Gỡ bỏ listener cũ trước khi tạo query mới
        removeListener()

        val query: Query = if (filterStatus != null)
            db.orderByChild("status").equalTo(filterStatus)
        else db.orderByChild("orderDate")

        currentQuery = query
        currentListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children
                    .mapNotNull { it.getValue(Order::class.java) }
                    .sortedByDescending { 
                        // Sửa lỗi sắp xếp: Chuyển String sang Date để so sánh chính xác
                        try { dateFormat.parse(it.orderDate) } catch (e: Exception) { Date(0) }
                    }
                adapter.submitList(list.toMutableList())
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
            override fun onCancelled(error: DatabaseError) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Lỗi tải đơn hàng: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        currentQuery?.addValueEventListener(currentListener!!)
    }

    private fun removeListener() {
        currentListener?.let { currentQuery?.removeEventListener(it) }
        currentListener = null
        currentQuery = null
    }

    private fun updateOrderStatus(order: Order, newStatus: String) {
        db.child(order.orderId).child("status").setValue(newStatus)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        removeListener()
        super.onDestroyView()
        _binding = null
    }
}
