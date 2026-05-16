package com.snackapp.admin.customer.fragment
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.database.*
import com.snackapp.admin.customer.ProductDetailActivity
import com.snackapp.admin.customer.adapter.MenuAdapter
import com.snackapp.admin.databinding.FragmentMenuBinding
import com.snackapp.admin.model.Product

class MenuFragment : Fragment(){
    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    private val allProducts = mutableListOf<Product>()
    private val categories  = mutableListOf<String>()
    private lateinit var adapter: MenuAdapter
    private var selectedCategory = "Tất cả"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Grid 2 cột
        adapter = MenuAdapter { product ->
            val intent = Intent(requireContext(), ProductDetailActivity::class.java)
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.productId)
            startActivity(intent)
        }
        binding.rvProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvProducts.adapter = adapter

        // Search
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filterProducts()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        loadProducts()
    }

    private fun loadProducts() {
        binding.progressBar.visibility = View.VISIBLE
        FirebaseDatabase.getInstance().getReference("Products")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    binding.progressBar.visibility = View.GONE
                    allProducts.clear()
                    categories.clear()
                    categories.add("Tất cả")

                    for (child in snapshot.children) {
                        child.getValue(Product::class.java)?.let { p ->
                            allProducts.add(p)
                            if (p.category.isNotEmpty() && !categories.contains(p.category)) {
                                categories.add(p.category)
                            }
                        }
                    }
                    setupCategoryChips()
                    filterProducts()
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.progressBar.visibility = View.GONE
                }
            })
    }

    private fun setupCategoryChips() {
        binding.chipGroupCategory.removeAllViews()
        categories.forEach { cat ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = cat
                isCheckable = true
                isChecked = cat == selectedCategory
                setOnClickListener {
                    selectedCategory = cat
                    filterProducts()
                }
            }
            binding.chipGroupCategory.addView(chip)
        }
    }

    private fun filterProducts() {
        val query = binding.etSearch.text.toString().trim()
        val filtered = allProducts.filter { p ->
            val matchCat = selectedCategory == "Tất cả" || p.category == selectedCategory
            val matchSearch = query.isEmpty() || p.name.contains(query, ignoreCase = true)
            matchCat && matchSearch
        }
        adapter.submitList(filtered.toMutableList())
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}