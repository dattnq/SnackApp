package com.snackapp.admin.customer

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
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

class CustomerMainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCustomerMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupNavigation()
        
        // Mặc định mở Menu
        if (savedInstanceState == null) {
            replaceFragment(MenuFragment())
        }

        CartManager.addListener { updateCartBadge() }
        updateCartBadge()
    }

    private fun setupNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_menu    -> replaceFragment(MenuFragment())
                R.id.nav_orders  -> replaceFragment(OrderHistoryFragment())
                R.id.nav_profile -> replaceFragment(ProfileFragment())
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun updateCartBadge() {
        val count = CartManager.getTotalQuantity()
        val badge = binding.bottomNav.getOrCreateBadge(R.id.nav_menu) // Hoặc vị trí khác tùy layout
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
                showLogoutConfirmation()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận đăng xuất")
            .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi ứng dụng không?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun performLogout() {
        FirebaseAuth.getInstance().signOut()
        CartManager.clear()
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }

    override fun onDestroy() {
        super.onDestroy()
        CartManager.removeListener { updateCartBadge() }
    }
}
