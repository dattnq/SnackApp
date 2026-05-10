package com.snackapp.admin.admin.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.database.*
import com.snackapp.admin.R
import com.snackapp.admin.databinding.FragmentDashboardBinding
import com.snackapp.admin.model.Order
import com.snackapp.admin.model.Product
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val fullSdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val dateOnlySdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentDashboardBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadStats()
    }

    private fun loadStats() {
        val db = FirebaseDatabase.getInstance()

        db.getReference("Orders").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return
                val orders = snapshot.children.mapNotNull { it.getValue(Order::class.java) }
                
                // Chỉ tính doanh thu từ đơn ĐÃ GIAO
                val deliveredOrders = orders.filter { it.status == Order.STATUS_DELIVERED }
                val totalRevenue = deliveredOrders.sumOf { it.totalAmount }
                
                binding.tvTotalRevenue.text = getString(R.string.revenue_format, totalRevenue)
                binding.tvTotalOrders.text = getString(R.string.label_total_orders_count, orders.size)
                binding.tvDelivered.text = getString(R.string.label_delivered_count, deliveredOrders.size)
                binding.tvPending.text = getString(R.string.label_pending_count, orders.count { it.status == Order.STATUS_PENDING })

                setupOrderStatusPieChart(deliveredOrders.size, orders.count { it.status == Order.STATUS_PENDING })
                setupRevenueBarChart(deliveredOrders)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        db.getReference("Products").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding == null) return
                val products = snapshot.children.mapNotNull { it.getValue(Product::class.java) }
                binding.tvTotalProducts.text = getString(R.string.label_total_products_count, products.size)

                val lowStock = products.filter { it.stock in 1..5 }
                binding.tvLowStock.text = if (lowStock.isEmpty()) getString(R.string.msg_stock_ok) else
                    lowStock.joinToString(", ") { "${it.name}(${it.stock})" }

                val today = Calendar.getInstance().apply { 
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
                
                val soon = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }.time
                
                val nearExpiry = products.filter {
                    it.expireDate.isNotEmpty() && runCatching {
                        val d = dateOnlySdf.parse(it.expireDate)!!
                        !d.before(today) && d.before(soon)
                    }.getOrDefault(false)
                }
                binding.tvNearExpiry.text = if (nearExpiry.isEmpty()) getString(R.string.msg_no_near_expiry) else
                    nearExpiry.joinToString(", ") { it.name }

                setupTopSellingBarChart(products)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupOrderStatusPieChart(delivered: Int, pending: Int) {
        val entries = mutableListOf<PieEntry>()
        if (delivered > 0) entries.add(PieEntry(delivered.toFloat(), getString(R.string.status_delivered)))
        if (pending > 0) entries.add(PieEntry(pending.toFloat(), getString(R.string.status_pending)))
        
        if (entries.isEmpty()) {
            binding.pieOrderStatus.clear()
            return
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(Color.parseColor("#4CAF50"), Color.parseColor("#FF9800"))
            valueTextSize = 12f
            valueTextColor = Color.WHITE
        }
        binding.pieOrderStatus.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            legend.isEnabled = true
            setHoleColor(Color.TRANSPARENT)
            animateY(800)
            invalidate()
        }
    }

    private fun setupRevenueBarChart(orders: List<Order>) {
        val labelSdf = SimpleDateFormat("dd/MM", Locale.getDefault())
        val days = (6 downTo 0).map { offset ->
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
        }
        
        val labels = days.map { labelSdf.format(it.time) }
        val revenueEntries = days.mapIndexed { index, cal ->
            val dateStr = dateOnlySdf.format(cal.time)
            val daySum = orders.filter { 
                runCatching { dateOnlySdf.format(fullSdf.parse(it.orderDate)!!) }.getOrDefault("") == dateStr
            }.sumOf { it.totalAmount }
            BarEntry(index.toFloat(), daySum.toFloat())
        }

        val dataSet = BarDataSet(revenueEntries, getString(R.string.label_total_revenue)).apply {
            color = Color.parseColor("#FF6B35")
            valueTextSize = 9f
        }
        binding.barRevenue.apply {
            data = BarData(dataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            axisRight.isEnabled = false
            description.isEnabled = false
            animateY(800)
            invalidate()
        }
    }

    private fun setupTopSellingBarChart(products: List<Product>) {
        val top5 = products.filter { it.sold > 0 }.sortedByDescending { it.sold }.take(5)
        if (top5.isEmpty()) {
            binding.barTopSelling.clear()
            return
        }

        val entries = top5.mapIndexed { i, p -> BarEntry(i.toFloat(), p.sold.toFloat()) }
        val labels = top5.map { if (it.name.length > 8) it.name.take(8) + ".." else it.name }

        val dataSet = BarDataSet(entries, getString(R.string.nav_products)).apply {
            colors = ColorTemplate.COLORFUL_COLORS.toList()
        }
        binding.barTopSelling.apply {
            data = BarData(dataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            axisRight.isEnabled = false
            description.isEnabled = false
            animateY(800)
            invalidate()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
