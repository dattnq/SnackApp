package com.snackapp.admin.customer.fragment
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.database.*
import com.snackapp.admin.customer.ProductDetailActivity
import com.snackapp.admin.customer.adapter.MenuAdapter
import com.snackapp.admin.databinding.FragmentWishlistBinding
import com.snackapp.admin.model.Product
import com.snackapp.admin.util.WishlistManager

class WishlistFragment : Fragment(){
    private var _binding: FragmentWishlistBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MenuAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWishlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = MenuAdapter { product ->
            val intent = Intent(requireContext(), ProductDetailActivity::class.java)
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.productId)
            startActivity(intent)
        }
        binding.rvWishlist.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvWishlist.adapter = adapter

        // Load wishlist từ Firebase trước, rồi lấy product details
        WishlistManager.load { loadWishlistProducts() }
    }

    private fun loadWishlistProducts() {
        val favIds = WishlistManager.getAll()
        if (favIds.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvWishlist.visibility = View.GONE
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        FirebaseDatabase.getInstance().getReference("Products")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    binding.progressBar.visibility = View.GONE
                    val products = snapshot.children
                        .mapNotNull { it.getValue(Product::class.java) }
                        .filter { favIds.contains(it.productId) }

                    adapter.submitList(products.toMutableList())
                    binding.tvEmpty.visibility   = if (products.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvWishlist.visibility = if (products.isEmpty()) View.GONE else View.VISIBLE
                }
                override fun onCancelled(error: DatabaseError) {
                    binding.progressBar.visibility = View.GONE
                }
            })
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

}