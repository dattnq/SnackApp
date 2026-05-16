package com.snackapp.admin.customer

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import com.snackapp.admin.R
import com.snackapp.admin.databinding.ActivityProductDetailBinding
import com.snackapp.admin.model.Product
import com.snackapp.admin.util.CartManager
import com.snackapp.admin.util.WishlistManager

class ProductDetailActivity : AppCompatActivity(){
    companion object {
        const val EXTRA_PRODUCT_ID = "extra_product_id"
    }

    private lateinit var binding: ActivityProductDetailBinding
    private var product: Product? = null
    private var quantity = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val productId = intent.getStringExtra(EXTRA_PRODUCT_ID) ?: run { finish(); return }

        loadProduct(productId)

        binding.btnDecrease.setOnClickListener {
            if (quantity > 1) { quantity--; updateQtyDisplay() }
        }
        binding.btnIncrease.setOnClickListener {
            quantity++; updateQtyDisplay()
        }

        binding.btnAddToCart.setOnClickListener {
            product?.let { p ->
                repeat(quantity) { CartManager.addItem(p) }
                Toast.makeText(this, "Đã thêm $quantity ${p.name} vào giỏ", Toast.LENGTH_SHORT).show()
                binding.btnAddToCart.text = "✓ Đã thêm vào giỏ"
            }
        }

        binding.btnWishlist.setOnClickListener {
            product?.let { p ->
                WishlistManager.toggle(p.productId) { isFav ->
                    binding.btnWishlist.setImageResource(
                        if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart
                    )
                    Toast.makeText(
                        this,
                        if (isFav) "Đã thêm vào yêu thích" else "Đã xóa khỏi yêu thích",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun loadProduct(productId: String) {
        binding.progressBar.visibility = View.VISIBLE
        FirebaseDatabase.getInstance().getReference("Products").child(productId).get()
            .addOnSuccessListener { snap ->
                binding.progressBar.visibility = View.GONE
                product = snap.getValue(Product::class.java) ?: return@addOnSuccessListener
                product?.let { displayProduct(it) }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Không tải được sản phẩm", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayProduct(p: Product) {
        supportActionBar?.title = p.name

        Glide.with(this).load(p.imageUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .into(binding.ivProduct)

        binding.tvName.text        = p.name
        binding.tvPrice.text       = "%.0f₫".format(p.price)
        binding.tvCategory.text    = p.category
        binding.tvDescription.text = p.description.ifEmpty { "Chưa có mô tả" }
        binding.tvStock.text       = "Còn lại: ${p.stock} sản phẩm"

        if (p.expireDate.isNotEmpty()) {
            binding.tvExpire.text    = "HSD: ${p.expireDate}"
            binding.tvExpire.visibility = View.VISIBLE
        }

        // Wishlist icon
        binding.btnWishlist.setImageResource(
            if (WishlistManager.isFav(p.productId)) R.drawable.ic_heart_filled
            else R.drawable.ic_heart
        )

        // Cart button state
        if (CartManager.isInCart(p.productId)) {
            binding.btnAddToCart.text = "✓ Đã thêm vào giỏ"
        }

        updateQtyDisplay()
    }

    private fun updateQtyDisplay() {
        binding.tvQuantity.text = quantity.toString()
        val p = product ?: return
        binding.tvSubtotal.text = "Tạm tính: %.0f₫".format(p.price * quantity)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_customer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.action_cart -> {
                startActivity(Intent(this, CartActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}