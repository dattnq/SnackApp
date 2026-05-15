package com.snackapp.admin.admin

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import com.snackapp.admin.R
import com.snackapp.admin.databinding.ActivityAddEditProductBinding
import com.snackapp.admin.model.Product
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddEditProductActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PRODUCT_ID = "extra_product_id"
        private const val TAG = "AddEditProduct"
        private const val IMGBB_API_KEY = "d16845701e4b8a58d8ccc5a4029e82df"
    }

    private lateinit var binding: ActivityAddEditProductBinding
    private val dbRef = FirebaseDatabase.getInstance().getReference("Products")

    private var selectedImageUri: Uri? = null
    private var existingProduct: Product? = null
    private var existingImageUrl: String = ""
    private val calendar = Calendar.getInstance()
    
    private val categories = arrayOf("Đồ ăn", "Đồ uống", "Snack")

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivProductImage.setImageURI(it)
            binding.tvPickImage.text = getString(R.string.hint_change_image)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupCategoryDropdown()

        val productId = intent.getStringExtra(EXTRA_PRODUCT_ID)
        if (productId != null) {
            loadProductForEdit(productId)
        } else {
            supportActionBar?.title = getString(R.string.title_add_product)
        }

        binding.cardPickImage.setOnClickListener {
            imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        
        binding.etExpireDate.setOnClickListener { showDatePicker() }
        binding.etExpireDate.isFocusable = false

        binding.btnSave.setOnClickListener { saveProduct() }
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, categories)
        binding.actvCategory.setAdapter(adapter)
    }

    private fun showDatePicker() {
        DatePickerDialog(this, { _, year, month, day ->
            calendar.set(year, month, day)
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.etExpireDate.setText(format.format(calendar.time))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadProductForEdit(productId: String) {
        showLoading(true)
        dbRef.child(productId).get().addOnSuccessListener { snap ->
            showLoading(false)
            val product = snap.getValue(Product::class.java) ?: return@addOnSuccessListener
            existingProduct = product
            existingImageUrl = product.imageUrl

            binding.etName.setText(product.name)
            binding.etPrice.setText(String.format(Locale.US, "%.0f", product.price))
            binding.etDescription.setText(product.description)
            binding.actvCategory.setText(product.category, false) // false to prevent filtering
            binding.etStock.setText(product.stock.toString())
            binding.etExpireDate.setText(product.expireDate)

            if (product.imageUrl.isNotEmpty()) {
                Glide.with(this).load(product.imageUrl).into(binding.ivProductImage)
                binding.tvPickImage.text = getString(R.string.hint_change_image)
            }
        }.addOnFailureListener { e ->
            showLoading(false)
            Toast.makeText(this, getString(R.string.err_load_data), Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProduct() {
        val name = binding.etName.text.toString().trim()
        val priceStr = binding.etPrice.text.toString().trim()
        val stockStr = binding.etStock.text.toString().trim()
        val expireDate = binding.etExpireDate.text.toString().trim()
        val category = binding.actvCategory.text.toString().trim()

        if (name.isEmpty() || priceStr.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, getString(R.string.err_fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        val stock = stockStr.toIntOrNull() ?: 0

        if (selectedImageUri == null && existingImageUrl.isEmpty()) {
            Toast.makeText(this, getString(R.string.err_pick_image), Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        if (selectedImageUri != null) {
            val base64Image = uriToBase64(selectedImageUri!!)
            if (base64Image != null) {
                uploadImageToImgBB(base64Image, name, price, stock, expireDate, category)
            } else {
                showLoading(false)
                Toast.makeText(this, "Không thể xử lý ảnh", Toast.LENGTH_SHORT).show()
            }
        } else {
            persistProduct(name, price, existingImageUrl, stock, expireDate, category)
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
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

    private fun uploadImageToImgBB(base64Image: String, name: String, price: Double, stock: Int, expireDate: String, category: String) {
        val url = "https://api.imgbb.com/1/upload?key=$IMGBB_API_KEY"
        
        binding.layoutProgress.visibility = View.VISIBLE
        binding.tvUploadProgress.text = "Đang tải ảnh lên ImgBB..."
        binding.progressBar.isIndeterminate = true
        
        val queue = Volley.newRequestQueue(this)
        val stringRequest = object : StringRequest(Request.Method.POST, url,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getBoolean("success")) {
                        val imageUrl = jsonResponse.getJSONObject("data").getString("url")
                        persistProduct(name, price, imageUrl, stock, expireDate, category)
                    } else {
                        showLoading(false)
                        Toast.makeText(this, "ImgBB error: success=false", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    showLoading(false)
                    Log.e(TAG, "Lỗi phân tích JSON: ${e.message}")
                    Toast.makeText(this, "Lỗi phản hồi từ server", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                showLoading(false)
                val errorMessage = error.networkResponse?.let { "Code: ${it.statusCode}" } ?: error.message
                Log.e(TAG, "Lỗi Volley: $errorMessage")
                Toast.makeText(this, "Lỗi kết nối: $errorMessage", Toast.LENGTH_SHORT).show()
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

    private fun persistProduct(name: String, price: Double, url: String, stock: Int, expireDate: String, category: String) {
        val id = existingProduct?.productId ?: dbRef.push().key ?: UUID.randomUUID().toString()
        val product = Product(
            productId = id,
            name = name,
            price = price,
            imageUrl = url,
            description = binding.etDescription.text.toString().trim(),
            category = category,
            stock = stock,
            expireDate = expireDate,
            sold = existingProduct?.sold ?: 0
        )

        dbRef.child(id).setValue(product).addOnSuccessListener {
            showLoading(false)
            Toast.makeText(this, if (existingProduct == null) getString(R.string.msg_product_added) else getString(R.string.msg_product_updated), Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener { e ->
            showLoading(false)
            Toast.makeText(this, getString(R.string.err_save_failed, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.layoutProgress.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !show
        if (!show) {
            binding.progressBar.isIndeterminate = false
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
