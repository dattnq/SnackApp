package com.snackapp.admin.customer.fragment
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.snackapp.admin.customer.adapter.OrderHistoryAdapter
import com.snackapp.admin.databinding.FragmentOrderHistoryBinding
import com.snackapp.admin.model.Order

class OrderHistoryFragment: Fragment()  {
    private var _binding: FragmentOrderHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: OrderHistoryAdapter
    private var databaseQuery: Query? = null
    private var valueEventListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = OrderHistoryAdapter()
        binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrders.adapter = adapter
        loadOrders()
    }

    private fun loadOrders() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE

        databaseQuery = FirebaseDatabase.getInstance().getReference("Orders")
            .orderByChild("userId").equalTo(uid)
        
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Kiểm tra an toàn: nếu binding đã bị hủy thì thoát
                if (_binding == null) return

                binding.progressBar.visibility = View.GONE
                val orders = snapshot.children
                    .mapNotNull { it.getValue(Order::class.java) }
                    .sortedByDescending { it.orderDate }

                adapter.submitList(orders.toMutableList())
                binding.tvEmpty.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
                binding.rvOrders.visibility = if (orders.isEmpty()) View.GONE else View.VISIBLE

                // Thống kê chi tiêu
                val total = orders.sumOf { it.totalAmount }
                binding.tvTotalSpent.text = "Tổng chi tiêu: %.0f₫".format(total)
            }

            override fun onCancelled(error: DatabaseError) {
                if (_binding == null) return
                binding.progressBar.visibility = View.GONE
            }
        }
        
        databaseQuery?.addValueEventListener(valueEventListener!!)
    }

    override fun onDestroyView() {
        // Hủy listener khi fragment bị hủy để tránh rò rỉ bộ nhớ và crash
        valueEventListener?.let { databaseQuery?.removeEventListener(it) }
        super.onDestroyView()
        _binding = null
    }

}