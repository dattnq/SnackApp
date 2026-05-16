package com.snackapp.admin.util
import com.snackapp.admin.model.CartItem
import com.snackapp.admin.model.Product

/**
 * Singleton quản lý giỏ hàng — tồn tại suốt vòng đời app.
 * Dùng object thay vì class để đảm bảo chỉ có 1 instance.
 */

class CartManager {
    private val items = mutableListOf<CartItem>()

    /** Thêm sản phẩm vào giỏ. Nếu đã có thì tăng số lượng. */
    fun addItem(product: Product, quantity: Int = 1) {
        val existing = items.find { it.productId == product.productId }
        if (existing != null) {
            val idx = items.indexOf(existing)
            items[idx] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            items.add(
                CartItem(
                    productId   = product.productId,
                    productName = product.name,
                    imageUrl    = product.imageUrl,
                    price       = product.price,
                    quantity    = quantity
                )
            )
        }
        notifyListeners()
    }

    /** Tăng số lượng 1 item */
    fun increaseQty(productId: String) {
        val idx = items.indexOfFirst { it.productId == productId }
        if (idx != -1) {
            items[idx] = items[idx].copy(quantity = items[idx].quantity + 1)
            notifyListeners()
        }
    }

    /** Giảm số lượng. Nếu về 0 thì xóa khỏi giỏ. */
    fun decreaseQty(productId: String) {
        val idx = items.indexOfFirst { it.productId == productId }
        if (idx != -1) {
            if (items[idx].quantity <= 1) items.removeAt(idx)
            else items[idx] = items[idx].copy(quantity = items[idx].quantity - 1)
            notifyListeners()
        }
    }

    /** Xóa hẳn 1 item */
    fun removeItem(productId: String) {
        items.removeAll { it.productId == productId }
        notifyListeners()
    }

    /** Xóa toàn bộ giỏ hàng */
    fun clear() {
        items.clear()
        notifyListeners()
    }

    fun getItems(): List<CartItem> = items.toList()

    fun getTotalAmount(): Double = items.sumOf { it.subtotal() }

    fun getTotalQuantity(): Int = items.sumOf { it.quantity }

    fun isEmpty(): Boolean = items.isEmpty()

    fun isInCart(productId: String): Boolean = items.any { it.productId == productId }

    // ── Observer pattern đơn giản ──────────────────────────────
    private val listeners = mutableListOf<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) = listeners.remove(listener)

    private fun notifyListeners() = listeners.forEach { it() }

}