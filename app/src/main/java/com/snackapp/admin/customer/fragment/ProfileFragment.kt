package com.snackapp.admin.customer.fragment
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.snackapp.admin.R
import com.snackapp.admin.databinding.FragmentProfileBinding
import com.snackapp.admin.model.User

class ProfileFragment : Fragment()  {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val uid  get() = auth.currentUser?.uid ?: ""
    private val db   get() = FirebaseDatabase.getInstance().getReference("Users").child(uid)

    private var selectedImageUri: Uri? = null

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivAvatar.setImageURI(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadProfile()

        binding.ivAvatar.setOnClickListener { imagePicker.launch("image/*") }
        binding.btnSave.setOnClickListener { saveProfile() }

        // Mở WishlistFragment khi nhấn nút
        binding.btnWishlist.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.snackapp.admin.R.id.fragmentContainer, WishlistFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun loadProfile() {
        db.get().addOnSuccessListener { snap ->
            val user = snap.getValue(User::class.java) ?: return@addOnSuccessListener
            binding.etFullName.setText(user.fullName)
            binding.etPhone.setText(user.phone)
            binding.tvEmail.text = user.email

            if (user.avatarUrl.isNotEmpty()) {
                Glide.with(this).load(user.avatarUrl)
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(binding.ivAvatar)
            }
        }
    }

    private fun saveProfile() {
        val fullName = binding.etFullName.text.toString().trim()
        val phone    = binding.etPhone.text.toString().trim()

        if (fullName.isEmpty()) { binding.etFullName.error = "Nhập họ tên"; return }

        binding.btnSave.isEnabled = false

        if (selectedImageUri != null) {
            uploadAvatarThenSave(fullName, phone)
        } else {
            saveToDb(fullName, phone, null)
        }
    }

    private fun uploadAvatarThenSave(fullName: String, phone: String) {
        val ref = FirebaseStorage.getInstance()
            .getReference("avatars/$uid.jpg")

        ref.putFile(selectedImageUri!!)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { url ->
                    saveToDb(fullName, phone, url.toString())
                }
            }
            .addOnFailureListener { e ->
                binding.btnSave.isEnabled = true
                Toast.makeText(requireContext(), "Lỗi upload: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveToDb(fullName: String, phone: String, avatarUrl: String?) {
        val updates = mutableMapOf<String, Any>(
            "fullName" to fullName,
            "phone"    to phone
        )
        avatarUrl?.let { updates["avatarUrl"] = it }

        db.updateChildren(updates)
            .addOnSuccessListener {
                binding.btnSave.isEnabled = true
                Toast.makeText(requireContext(), "Đã cập nhật hồ sơ!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                binding.btnSave.isEnabled = true
                Toast.makeText(requireContext(), "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

}