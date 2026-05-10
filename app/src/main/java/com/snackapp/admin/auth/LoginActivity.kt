package com.snackapp.admin.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.snackapp.admin.R
import com.snackapp.admin.admin.AdminMainActivity
import com.snackapp.admin.databinding.ActivityLoginBinding
import com.snackapp.admin.model.User

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Nếu đã đăng nhập rồi thì kiểm tra role ngay
        auth.currentUser?.let { checkRoleAndNavigate(it.uid) }

        binding.btnLogin.setOnClickListener { doLogin() }
        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun doLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.err_fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                checkRoleAndNavigate(result.user!!.uid)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, getString(R.string.err_login_failed, e.message), Toast.LENGTH_LONG).show()
            }
    }

    private fun checkRoleAndNavigate(uid: String) {
        FirebaseDatabase.getInstance().getReference("Users").child(uid)
            .get()
            .addOnSuccessListener { snap ->
                showLoading(false)
                val user = snap.getValue(User::class.java)
                if (user?.role == "admin") {
                    startActivity(Intent(this, AdminMainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, getString(R.string.msg_welcome_user, user?.fullName ?: "Khách"), Toast.LENGTH_SHORT).show()
                    // Customer — mở CustomerMainActivity (nếu có)
                    // startActivity(Intent(this, CustomerMainActivity::class.java))
                    // finish()
                }
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, getString(R.string.err_load_user), Toast.LENGTH_SHORT).show()
                auth.signOut()
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
    }
}
