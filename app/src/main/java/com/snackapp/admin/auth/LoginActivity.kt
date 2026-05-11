package com.snackapp.admin.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.snackapp.admin.R
import com.snackapp.admin.admin.AdminMainActivity
import com.snackapp.admin.databinding.ActivityLoginBinding
import com.snackapp.admin.model.User

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth = FirebaseAuth.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Google Sign-In
        // Lưu ý: R.string.default_web_client_id sẽ tự động có sau khi bạn thêm OAuth Client ID vào Firebase Console 
        // và tải lại file google-services.json. Nếu chưa có, bạn có thể thay bằng chuỗi trực tiếp.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Check current user
        auth.currentUser?.let { checkRoleAndNavigate(it.uid) }

        binding.btnLogin.setOnClickListener { doLogin() }
        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.btnGoogleLogin.setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val firebaseUser = result.user!!
                saveUserToDatabaseIfNew(firebaseUser)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Auth failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserToDatabaseIfNew(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        val db = FirebaseDatabase.getInstance().getReference("Users")
        db.child(firebaseUser.uid).get().addOnSuccessListener { snap ->
            if (!snap.exists()) {
                val user = User(
                    uid = firebaseUser.uid,
                    fullName = firebaseUser.displayName ?: "",
                    email = firebaseUser.email ?: "",
                    role = "customer",
                    avatarUrl = firebaseUser.photoUrl.toString()
                )
                db.child(firebaseUser.uid).setValue(user).addOnSuccessListener {
                    checkRoleAndNavigate(firebaseUser.uid)
                }
            } else {
                checkRoleAndNavigate(firebaseUser.uid)
            }
        }.addOnFailureListener {
            checkRoleAndNavigate(firebaseUser.uid)
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
                    val intent = Intent(this, AdminMainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, getString(R.string.msg_welcome_user, user?.fullName ?: "Khách"), Toast.LENGTH_SHORT).show()
                    // Điều hướng sang màn hình người dùng nếu cần
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
        binding.btnGoogleLogin.isEnabled = !show
    }
}
