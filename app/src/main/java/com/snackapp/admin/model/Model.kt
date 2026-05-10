package com.snackapp.admin.model

import com.google.firebase.database.IgnoreExtraProperties

// ==================== USER ====================
@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "customer",        // "admin" | "customer"
    val avatarUrl: String = "",
    val phone: String = ""
) {
    // Firebase requires a no-arg constructor
    constructor() : this("", "", "", "customer", "", "")
}

// ==================== PRODUCT ====================
@IgnoreExtraProperties
data class Product(
    val productId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val description: String = "",
    val category: String = "",
    val stock: Int = 0,                   // Tồn kho
    val expireDate: String = "",          // Hạn sử dụng (dd/MM/yyyy)
    val sold: Int = 0                     // Số lượng đã bán (tổng hợp từ orders)
) {
    constructor() : this("", "", 0.0, "", "", "", 0, "", 0)
}

// ==================== CART ITEM ====================
@IgnoreExtraProperties
data class CartItem(
    val productId: String = "",
    val productName: String = "",
    val imageUrl: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1
) {
    constructor() : this("", "", "", 0.0, 1)

    fun subtotal(): Double = price * quantity
}

// ==================== ORDER ====================
@IgnoreExtraProperties
data class Order(
    val orderId: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val address: String = "",
    val items: List<CartItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val orderDate: String = "",           // "dd/MM/yyyy HH:mm"
    val status: String = STATUS_PENDING   // "Đang xử lý" | "Đã giao"
) {
    constructor() : this("", "", "", "", "", emptyList(), 0.0, "", STATUS_PENDING)

    companion object {
        const val STATUS_PENDING = "Đang xử lý"
        const val STATUS_DELIVERED = "Đã giao"
    }
}