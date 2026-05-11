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
import com.snackapp.admin.databinding.ActivityRegisterBinding
import com.snackapp.admin.model.User

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().getReference("Users")
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
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnRegister.setOnClickListener { doRegister() }
        binding.tvLogin.setOnClickListener { finish() }
        binding.btnGoogleRegister.setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                saveUserToDatabaseIfNew(result.user!!)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Auth failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserToDatabaseIfNew(firebaseUser: com.google.firebase.auth.FirebaseUser) {
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

    private fun doRegister() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirm = binding.etConfirmPassword.text.toString().trim()

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.err_fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }
        if (password != confirm) {
            binding.etConfirmPassword.error = getString(R.string.err_password_mismatch)
            return
        }
        if (password.length < 6) {
            binding.etPassword.error = getString(R.string.err_password_too_short)
            return
        }

        showLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user!!.uid
                val user = User(
                    uid = uid,
                    fullName = fullName,
                    email = email,
                    role = "customer"
                )
                db.child(uid).setValue(user)
                    .addOnSuccessListener {
                        showLoading(false)
                        Toast.makeText(this, getString(R.string.msg_register_success), Toast.LENGTH_SHORT).show()
                        checkRoleAndNavigate(uid)
                    }
                    .addOnFailureListener { e ->
                        showLoading(false)
                        Toast.makeText(this, getString(R.string.err_save_failed, e.message), Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, getString(R.string.err_register_failed, e.message), Toast.LENGTH_LONG).show()
            }
    }

    private fun checkRoleAndNavigate(uid: String) {
        db.child(uid).get().addOnSuccessListener { snap ->
            showLoading(false)
            val user = snap.getValue(User::class.java)
            if (user?.role == "admin") {
                val intent = Intent(this, AdminMainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, getString(R.string.msg_welcome_user, user?.fullName ?: "Khách"), Toast.LENGTH_SHORT).show()
                finish()
            }
        }.addOnFailureListener {
            showLoading(false)
            auth.signOut()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !show
        binding.btnGoogleRegister.isEnabled = !show
    }
}
