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
import com.snackapp.admin.model.Product
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
        // Chỉ cập nhật kho khi chuyển từ trạng thái khác sang "Đã giao"
        if (newStatus == Order.STATUS_DELIVERED && order.status != Order.STATUS_DELIVERED) {
            updateProductInventory(order)
        }

        db.child(order.orderId).child("status").setValue(newStatus)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateProductInventory(order: Order) {
        val productRef = FirebaseDatabase.getInstance().getReference("Products")
        
        order.items.forEach { cartItem ->
            productRef.child(cartItem.productId).runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val p = currentData.getValue(Product::class.java) 
                        ?: return Transaction.success(currentData)
                    
                    // Trừ tồn kho và cộng số lượng đã bán
                    val newStock = (p.stock - cartItem.quantity).coerceAtLeast(0)
                    val newSold = p.sold + cartItem.quantity
                    
                    currentData.child("stock").value = newStock
                    currentData.child("sold").value = newSold
                    
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    // Xử lý lỗi nếu cần
                }
            })
        }
    }

    override fun onDestroyView() {
        removeListener()
        super.onDestroyView()
        _binding = null
    }
}
