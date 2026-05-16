package com.snackapp.admin.customer.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.snackapp.admin.R
import com.snackapp.admin.databinding.FragmentProfileBinding
import com.snackapp.admin.model.User
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class ProfileFragment : Fragment() {
    companion object {
        private const val TAG = "ProfileFragment"
        private const val IMGBB_API_KEY = "d16845701e4b8a58d8ccc5a4029e82df"
    }

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val uid get() = auth.currentUser?.uid ?: ""
    private val db get() = FirebaseDatabase.getInstance().getReference("Users").child(uid)

    private var selectedImageUri: Uri? = null

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            if (_binding != null) {
                binding.ivAvatar.setImageURI(it)
            }
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

        binding.btnWishlist.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, WishlistFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun loadProfile() {
        db.get().addOnSuccessListener { snap ->
            if (_binding == null) return@addOnSuccessListener
            val user = snap.getValue(User::class.java) ?: return@addOnSuccessListener
            binding.etFullName.setText(user.fullName)
            binding.etPhone.setText(user.phone)
            binding.etAddress.setText(user.address)
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
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()

        if (fullName.isEmpty()) {
            binding.etFullName.error = "Nhập họ tên"
            return
        }

        showLoading(true)

        if (selectedImageUri != null) {
            val base64Image = uriToBase64(selectedImageUri!!)
            if (base64Image != null) {
                uploadAvatarToImgBB(base64Image, fullName, phone, address)
            } else {
                showLoading(false)
                Toast.makeText(context, "Không thể xử lý ảnh", Toast.LENGTH_SHORT).show()
            }
        } else {
            saveToDb(fullName, phone, address, null)
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val ctx = context ?: return null
            val inputStream = ctx.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val scaledBitmap = scaleBitmap(bitmap)

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi chuyển base64: ${e.message}")
            null
        }
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val maxSize = 1024
        var width = bitmap.width
        var height = bitmap.height

        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun uploadAvatarToImgBB(base64Image: String, fullName: String, phone: String, address: String) {
        val url = "https://api.imgbb.com/1/upload?key=$IMGBB_API_KEY"

        if (_binding != null) {
            binding.tvUploadProgress.text = "Đang tải ảnh đại diện..."
        }
        
        val ctx = context ?: return
        val queue = Volley.newRequestQueue(ctx)
        val stringRequest = object : StringRequest(Request.Method.POST, url,
            Response.Listener { response ->
                if (_binding == null) return@Listener
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getBoolean("success")) {
                        val imageUrl = jsonResponse.getJSONObject("data").getString("url")
                        saveToDb(fullName, phone, address, imageUrl)
                    } else {
                        showLoading(false)
                        Toast.makeText(ctx, "Lỗi tải ảnh lên ImgBB", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    showLoading(false)
                    Toast.makeText(ctx, "Lỗi phản hồi server", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                if (_binding == null) return@ErrorListener
                showLoading(false)
                Toast.makeText(ctx, "Lỗi kết nối: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["image"] = base64Image
                return params
            }
        }

        stringRequest.retryPolicy = DefaultRetryPolicy(
            30000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        queue.add(stringRequest)
    }

    private fun saveToDb(fullName: String, phone: String, address: String, avatarUrl: String?) {
        val updates = mutableMapOf<String, Any>(
            "fullName" to fullName,
            "phone" to phone,
            "address" to address
        )
        avatarUrl?.let { updates["avatarUrl"] = it }

        if (_binding != null) {
            binding.tvUploadProgress.text = "Đang lưu thông tin..."
        }

        db.updateChildren(updates)
            .addOnSuccessListener {
                if (_binding == null) return@addOnSuccessListener
                showLoading(false)
                Toast.makeText(context, "Đã cập nhật hồ sơ!", Toast.LENGTH_SHORT).show()
                selectedImageUri = null
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                showLoading(false)
                Toast.makeText(context, "Lỗi cập nhật dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(show: Boolean) {
        if (_binding == null) return
        binding.layoutProgress.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !show
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
