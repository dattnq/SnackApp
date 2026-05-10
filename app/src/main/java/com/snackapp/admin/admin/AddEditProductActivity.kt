package com.snackapp.admin.admin

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.snackapp.admin.R
import com.snackapp.admin.databinding.ActivityAddEditProductBinding
import com.snackapp.admin.model.Product
import java.text.SimpleDateFormat
import java.util.*

class AddEditProductActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PRODUCT_ID = "extra_product_id"
        private const val TAG = "AddEditProduct"
    }

    private lateinit var binding: ActivityAddEditProductBinding
    private val dbRef = FirebaseDatabase.getInstance().getReference("Products")
    private val storageRef = FirebaseStorage.getInstance().getReference("product_images")

    private var selectedImageUri: Uri? = null
    private var existingProduct: Product? = null
    private var existingImageUrl: String = ""
    private val calendar = Calendar.getInstance()

    // Sử dụng Photo Picker hiện đại hơn
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
            binding.etCategory.setText(product.category)
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

        if (name.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.err_fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        val stock = stockStr.toIntOrNull() ?: 0

        // Kiểm tra ảnh nếu là thêm mới
        if (selectedImageUri == null && existingImageUrl.isEmpty()) {
            Toast.makeText(this, getString(R.string.err_pick_image), Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        if (selectedImageUri != null) {
            uploadImageAndSave(name, price, stock, expireDate)
        } else {
            persistProduct(name, price, existingImageUrl, stock, expireDate)
        }
    }

    private fun uploadImageAndSave(name: String, price: Double, stock: Int, expireDate: String) {
        val fileName = "prod_${System.currentTimeMillis()}.jpg"
        val ref = storageRef.child(fileName)

        val uploadTask = ref.putFile(selectedImageUri!!)
        
        uploadTask.addOnProgressListener { taskSnapshot ->
            val total = taskSnapshot.totalByteCount
            if (total > 0) {
                val progress = (100.0 * taskSnapshot.bytesTransferred / total).toInt()
                binding.progressBar.progress = progress
                binding.tvUploadProgress.text = getString(R.string.msg_upload_progress, progress)
            }
        }.addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { uri ->
                persistProduct(name, price, uri.toString(), stock, expireDate)
            }.addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Lỗi lấy URL: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener { e ->
            showLoading(false)
            Log.e(TAG, "Storage Error: ${e.message}")
            Toast.makeText(this, getString(R.string.err_upload_failed, e.message), Toast.LENGTH_LONG).show()
        }
    }

    private fun persistProduct(name: String, price: Double, url: String, stock: Int, expireDate: String) {
        val id = existingProduct?.productId ?: dbRef.push().key ?: UUID.randomUUID().toString()
        val product = Product(
            productId = id,
            name = name,
            price = price,
            imageUrl = url,
            description = binding.etDescription.text.toString().trim(),
            category = binding.etCategory.text.toString().trim(),
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
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
