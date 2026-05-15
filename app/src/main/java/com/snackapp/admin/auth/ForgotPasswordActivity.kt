package com.snackapp.admin.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.snackapp.admin.R
import com.snackapp.admin.databinding.ActivityForgotPasswordBinding

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_forgot_password)

        binding.btnSendOtp.setOnClickListener { sendPasswordResetEmail() }
        
        // Các bước 2 và 3 được ẩn đi vì Firebase xử lý việc đổi mật khẩu qua link Email
        binding.layoutStep2.visibility = View.GONE
        binding.layoutStep3.visibility = View.GONE
    }

    private fun sendPasswordResetEmail() {
        val email = binding.etForgotEmail.text.toString().trim()
        if (email.isEmpty()) {
            binding.etForgotEmail.error = getString(R.string.err_fill_all_fields)
            return
        }

        showLoading(true)
        
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(
                    this, 
                    "Liên kết đặt lại mật khẩu đã được gửi! Vui lòng kiểm tra hộp thư của bạn.", 
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSendOtp.isEnabled = !show
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
