package com.snackapp.admin.auth

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.snackapp.admin.R
import com.snackapp.admin.databinding.ActivityForgotPasswordBinding
import java.util.*

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val auth = FirebaseAuth.getInstance()
    private var generatedOtp: String = ""
    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_forgot_password)

        binding.btnSendOtp.setOnClickListener { sendOtp() }
        binding.btnVerifyOtp.setOnClickListener { verifyOtp() }
        binding.btnChangePassword.setOnClickListener { resetPassword() }
    }

    private fun sendOtp() {
        userEmail = binding.etForgotEmail.text.toString().trim()
        if (userEmail.isEmpty()) {
            binding.etForgotEmail.error = getString(R.string.err_fill_all_fields)
            return
        }

        showLoading(true)
        
        // Tạo mã OTP 6 số mô phỏng
        generatedOtp = (100000..999999).random().toString()
        
        // Gửi email khôi phục thật của Firebase (dạng link)
        auth.sendPasswordResetEmail(userEmail)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, getString(R.string.msg_otp_sent), Toast.LENGTH_SHORT).show()
                
                // Chuyển sang bước nhập OTP
                binding.layoutStep1.visibility = View.GONE
                binding.layoutStep2.visibility = View.VISIBLE
                
                // Ghi log mã OTP để bạn test (Thực tế mã này sẽ gửi qua API email)
                Log.d("OTP_DEBUG", "Mã OTP test của bạn là: $generatedOtp")
                Toast.makeText(this, "Mã OTP test (xem Logcat): $generatedOtp", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun verifyOtp() {
        val inputOtp = binding.etOtp.text.toString().trim()
        if (inputOtp == generatedOtp) {
            binding.layoutStep2.visibility = View.GONE
            binding.layoutStep3.visibility = View.VISIBLE
        } else {
            Toast.makeText(this, getString(R.string.err_invalid_otp), Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetPassword() {
        val newPassword = binding.etNewPassword.text.toString().trim()
        if (newPassword.length < 6) {
            binding.etNewPassword.error = getString(R.string.err_password_too_short)
            return
        }

        // Vì lý do bảo mật, Firebase yêu cầu người dùng click vào link trong Email 
        // để thực hiện đổi mật khẩu thực sự. 
        Toast.makeText(this, "Vui lòng kiểm tra Email và nhấn vào liên kết để đổi mật khẩu chính thức.", Toast.LENGTH_LONG).show()
        finish()
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
