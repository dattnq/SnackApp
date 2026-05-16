package com.snackapp.admin.customer
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.snackapp.admin.customer.adapter.CartAdapter
import com.snackapp.admin.databinding.ActivityCartBinding
import com.snackapp.admin.model.Order
import com.snackapp.admin.util.CartManager
import java.text.SimpleDateFormat
import java.util.*

class CartActivity  : AppCompatActivity(){
    private lateinit var binding: ActivityCartBinding
    private lateinit var adapter: CartAdapter
    private val db   = FirebaseDatabase.getInstance().getReference("Orders")
    private val auth = FirebaseAuth.getInstance()

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

        refreshCart()
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

        if (address.isEmpty()) { binding.etAddress.error = "Nhập địa chỉ giao hàng"; return }
        if (phone.isEmpty())   { binding.etPhone.error = "Nhập số điện thoại"; return }
        if (CartManager.isEmpty()) { Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show(); return }

        val uid  = auth.currentUser?.uid ?: return
        val name = auth.currentUser?.displayName ?: "Khách hàng"

        showLoading(true)

        // Lấy fullName từ DB
        FirebaseDatabase.getInstance().getReference("Users").child(uid).child("fullName").get()
            .addOnSuccessListener { snap ->
                val customerName = snap.getValue(String::class.java) ?: name
                val orderId = db.push().key ?: UUID.randomUUID().toString()
                val now = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

                val order = Order(
                    orderId      = orderId,
                    userId       = uid,
                    customerName = customerName,
                    customerPhone= phone,
                    address      = address,
                    items        = CartManager.getItems(),
                    totalAmount  = CartManager.getTotalAmount(),
                    orderDate    = now,
                    status       = Order.STATUS_PENDING
                )

                db.child(orderId).setValue(order)
                    .addOnSuccessListener {
                        showLoading(false)
                        CartManager.clear()
                        Toast.makeText(this, "🎉 Đặt hàng thành công!", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        showLoading(false)
                        Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnOrder.isEnabled     = !show
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

}