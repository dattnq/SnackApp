package com.snackapp.admin.admin.fragment

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.snackapp.admin.R
import com.snackapp.admin.admin.AddEditProductActivity
import com.snackapp.admin.admin.adapter.ProductAdminAdapter
import com.snackapp.admin.databinding.FragmentProductManageBinding
import com.snackapp.admin.model.Product

class ProductManageFragment : Fragment() {

    private var _binding: FragmentProductManageBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseDatabase.getInstance().getReference("Products")
    private val allProducts = mutableListOf<Product>()
    private lateinit var adapter: ProductAdminAdapter
    
    private var productListener: ValueEventListener? = null
    private var currentCategory: String = "Tất cả"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProductManageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ProductAdminAdapter(
            onEdit = { product ->
                val intent = Intent(requireContext(), AddEditProductActivity::class.java)
                intent.putExtra(AddEditProductActivity.EXTRA_PRODUCT_ID, product.productId)
                startActivity(intent)
            },
            onDelete = { product -> deleteProduct(product) }
        )

        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter

        binding.fabAddProduct.setOnClickListener {
            startActivity(Intent(requireContext(), AddEditProductActivity::class.java))
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filterProducts()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        setupCategoryFilter()
        listenProducts()
    }

    private fun setupCategoryFilter() {
        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chipAll
            currentCategory = when (checkedId) {
                R.id.chipFood -> "Đồ ăn"
                R.id.chipDrink -> "Đồ uống"
                R.id.chipSnack -> "Snack"
                else -> "Tất cả"
            }
            filterProducts()
        }
    }

    private fun listenProducts() {
        productListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                allProducts.clear()
                for (child in snapshot.children) {
                    child.getValue(Product::class.java)?.let { allProducts.add(it) }
                }
                filterProducts()
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Lỗi: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        db.addValueEventListener(productListener!!)
    }

    private fun filterProducts() {
        val query = binding.etSearch.text.toString().trim()
        
        val filtered = allProducts.filter { product ->
            val matchSearch = product.name.contains(query, ignoreCase = true)
            val matchCategory = if (currentCategory == "Tất cả") true 
                               else product.category == currentCategory
            matchSearch && matchCategory
        }

        adapter.submitList(filtered.toMutableList())
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun deleteProduct(product: Product) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc muốn xóa \"${product.name}\"?")
            .setPositiveButton("Xóa") { _, _ ->
                db.child(product.productId).removeValue()
                    .addOnSuccessListener {
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() {
        productListener?.let { db.removeEventListener(it) }
        super.onDestroyView()
        _binding = null
    }
}
