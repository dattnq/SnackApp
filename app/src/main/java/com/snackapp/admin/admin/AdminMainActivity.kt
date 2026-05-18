package com.snackapp.admin.admin

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.snackapp.admin.R
import com.snackapp.admin.admin.fragment.CustomerManageFragment
import com.snackapp.admin.admin.fragment.DashboardFragment
import com.snackapp.admin.admin.fragment.OrderManageFragment
import com.snackapp.admin.admin.fragment.ProductManageFragment
import com.snackapp.admin.auth.LoginActivity
import com.snackapp.admin.databinding.ActivityAdminMainBinding

class AdminMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        if (savedInstanceState == null) {
            loadFragment(DashboardFragment(), getString(R.string.nav_dashboard))
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment(), getString(R.string.nav_dashboard))
                    true
                }
                R.id.nav_products  -> {
                    loadFragment(ProductManageFragment(), getString(R.string.nav_products))
                    true
                }
                R.id.nav_orders    -> {
                    loadFragment(OrderManageFragment(), getString(R.string.nav_orders))
                    true
                }
                R.id.nav_customers -> {
                    loadFragment(CustomerManageFragment(), getString(R.string.nav_customers))
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment, title: String) {
        supportActionBar?.title = title
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_admin, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_logout) {
            showLogoutConfirmationDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_confirm_logout_title)
            .setMessage(R.string.dialog_confirm_logout_msg)
            .setPositiveButton(R.string.btn_logout_confirm) { _, _ ->
                // 1. Sign out từ Firebase
                FirebaseAuth.getInstance().signOut()

                // 2. Sign out từ Google
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .build()
                GoogleSignIn.getClient(this, gso).signOut()

                // 3. Quay về màn hình Login
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }
}