package com.snackapp.admin.customer
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.snackapp.admin.customer.adapter.CartAdapter
import com.snackapp.admin.databinding.ActivityCartBinding
import com.snackapp.admin.model.Order
import com.snackapp.admin.model.User
import com.snackapp.admin.util.CartManager
import java.text.SimpleDateFormat
import java.util.*

class CartActivity  : AppCompatActivity(){
    private lateinit var binding: ActivityCartBinding
    private lateinit var adapter: CartAdapter
    private val db   = FirebaseDatabase.getInstance().getReference("Orders")
    private val auth = FirebaseAuth.getInstance()
    private val userDb = FirebaseDatabase.getInstance().getReference("Users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Giỏ hàng"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = CartAdapter(
            onIncrease = { CartManager.increaseQty(it.productId); refreshCart() },
            onDecrease = { CartManager.decreaseQty(it.productId); refreshCart() },
            onRemove   = { CartManager.removeItem(it.productId);   refreshCart() }
        )

        binding.rvCartItems.layoutManager = LinearLayoutManager(this)
        binding.rvCartItems.adapter = adapter

        binding.btnOrder.setOnClickListener { placeOrder() }

        loadUserInfo()
        refreshCart()
    }

    private fun loadUserInfo() {
        val uid = auth.currentUser?.uid ?: return
        userDb.child(uid).get().addOnSuccessListener { snap ->
            val user = snap.getValue(User::class.java)
            user?.let {
                if (it.address.isNotEmpty()) binding.etAddress.setText(it.address)
                if (it.phone.isNotEmpty()) binding.etPhone.setText(it.phone)
            }
        }.addOnFailureListener {
            // Có thể bỏ qua nếu không load được thông tin cá nhân
        }
    }

    private fun refreshCart() {
        val items = CartManager.getItems()
        adapter.submitList(items.toMutableList())
        binding.tvTotal.text = "Tổng: %.0f₫".format(CartManager.getTotalAmount())

        val empty = CartManager.isEmpty()
        binding.layoutEmpty.visibility   = if (empty) View.VISIBLE else View.GONE
        binding.rvCartItems.visibility   = if (empty) View.GONE else View.VISIBLE
        binding.layoutCheckout.visibility= if (empty) View.GONE else View.VISIBLE
    }

    private fun placeOrder() {
        val address = binding.etAddress.text.toString().trim()
        val phone   = binding.etPhone.text.toString().trim()

        if (address.isEmpty()) { 
            binding.etAddress.error = "Vui lòng nhập địa chỉ giao hàng"
            binding.etAddress.requestFocus()
            return 
        }
        if (phone.isEmpty()) { 
            binding.etPhone.error = "Vui lòng nhập số điện thoại"
            binding.etPhone.requestFocus()
            return 
        }
        if (CartManager.isEmpty()) { 
            Toast.makeText(this, "Giỏ hàng của bạn đang trống", Toast.LENGTH_SHORT).show()
            return 
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đặt hàng", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        // Lấy tên khách hàng từ Database, nếu không có dùng tên từ Auth
        userDb.child(currentUser.uid).get().addOnCompleteListener { task ->
            val customerName = if (task.isSuccessful) {
                val user = task.result.getValue(User::class.java)
                if (user != null && user.fullName.isNotEmpty()) user.fullName else (currentUser.displayName ?: "Khách hàng")
            } else {
                currentUser.displayName ?: "Khách hàng"
            }

            val orderId = db.push().key ?: UUID.randomUUID().toString()
            val now = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

            val order = Order(
                orderId      = orderId,
                userId       = currentUser.uid,
                customerName = customerName,
                customerPhone= phone,
                address      = address,
                items        = CartManager.getItems(),
                totalAmount  = CartManager.getTotalAmount(),
                orderDate    = now,
                status       = Order.STATUS_PENDING
            )

            // Lưu đơn hàng vào Firebase
            db.child(orderId).setValue(order)
                .addOnSuccessListener {
                    showLoading(false)
                    CartManager.clear()
                    
                    if (!isFinishing) {
                        AlertDialog.Builder(this@CartActivity)
                            .setTitle("Thành công")
                            .setMessage("🎉 Đặt hàng thành công! Đơn hàng của bạn đã được gửi và đang chờ xử lý.")
                            .setPositiveButton("Xác nhận") { _, _ ->
                                finish()
                            }
                            .setCancelable(false)
                            .show()
                    }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(this@CartActivity, "Lỗi đặt hàng: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnOrder.isEnabled     = !show
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

}