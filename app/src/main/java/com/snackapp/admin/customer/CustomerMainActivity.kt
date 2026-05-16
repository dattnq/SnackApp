package com.snackapp.admin.customer

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.snackapp.admin.R
import com.snackapp.admin.auth.LoginActivity
import com.snackapp.admin.customer.fragment.MenuFragment
import com.snackapp.admin.customer.fragment.OrderHistoryFragment
import com.snackapp.admin.customer.fragment.ProfileFragment
import com.snackapp.admin.databinding.ActivityCustomerMainBinding
import com.snackapp.admin.util.CartManager

class CustomerMainActivity : AppCompatActivity(){
    private lateinit var binding: ActivityCustomerMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "SnackApp"

        if (savedInstanceState == null) {
            loadFragment(MenuFragment(), "Thực đơn")
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_menu    -> {
                    loadFragment(MenuFragment(), "Thực đơn")
                    true
                }
                R.id.nav_orders  -> {
                    loadFragment(OrderHistoryFragment(), "Đơn hàng của tôi")
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment(), "Hồ sơ")
                    true
                }
                R.id.nav_cart    -> {
                    startActivity(Intent(this, CartActivity::class.java))
                    false // Return false so the cart item doesn't stay selected
                }
                else -> false
            }
        }

        // Lắng nghe thay đổi giỏ hàng để cập nhật badge
        CartManager.addListener { updateCartBadge() }
        updateCartBadge()
    }

    private fun loadFragment(fragment: Fragment, title: String) {
        supportActionBar?.title = title
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun updateCartBadge() {
        val count = CartManager.getTotalQuantity()
        val badge = binding.bottomNav.getOrCreateBadge(R.id.nav_cart)
        if (count > 0) {
            badge.isVisible = true
            badge.number   = count
        } else {
            badge.isVisible = false
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_customer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_cart -> {
                startActivity(Intent(this, CartActivity::class.java))
            }
            R.id.action_logout -> {
                FirebaseAuth.getInstance().signOut()
                CartManager.clear()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        CartManager.removeListener { updateCartBadge() }
    }

}