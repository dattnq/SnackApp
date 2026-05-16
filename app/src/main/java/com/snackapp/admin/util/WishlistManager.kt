package com.snackapp.admin.util
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/**
 * Quản lý danh sách yêu thích (Wishlist).
 * Lưu lên Firebase: Users/{uid}/wishlist/{productId} = true
 */

object WishlistManager {
    private val wishlist = mutableSetOf<String>() // productId set
    private val db get() = FirebaseDatabase.getInstance().reference
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    fun load(onDone: () -> Unit) {
        val u = uid ?: return
        db.child("Users").child(u).child("wishlist").get()
            .addOnSuccessListener { snap ->
                wishlist.clear()
                snap.children.forEach { wishlist.add(it.key ?: "") }
                onDone()
            }
    }

    fun toggle(productId: String, onResult: (isFav: Boolean) -> Unit) {
        val u = uid ?: return
        if (wishlist.contains(productId)) {
            wishlist.remove(productId)
            db.child("Users").child(u).child("wishlist").child(productId).removeValue()
            onResult(false)
        } else {
            wishlist.add(productId)
            db.child("Users").child(u).child("wishlist").child(productId).setValue(true)
            onResult(true)
        }
    }

    fun isFav(productId: String) = wishlist.contains(productId)

    fun getAll() = wishlist.toSet()

}