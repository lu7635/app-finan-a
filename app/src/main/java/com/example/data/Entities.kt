package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String = "default_user",
    val name: String,
    val phone: String,
    val whatsapp: String,
    val email: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMovement: Long = System.currentTimeMillis()
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String = "default_user",
    val name: String,
    val price: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val stockQuantity: Int? = null
)

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String = "default_user",
    val clientId: Int,
    val type: String, // "Produto" or "Serviço"
    val description: String,
    val value: Double,
    val paidValue: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val receivedAt: Long? = null,
    val notes: String? = null,
    val status: String = "Pendente", // "Pendente", "Parcial", "Pago"
    val productId: Int? = null,
    val productPrice: Double? = null,
    val quantity: Int? = null
)

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String = "default_user",
    val debtId: Int,
    val clientId: Int,
    val value: Double,
    val status: String = "Recebido",
    val createdAt: Long = System.currentTimeMillis(),
    val receivedAt: Long = createdAt,
    val notes: String? = null
)

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val userId: String,
    val name: String,
    val email: String,
    val languageCode: String = "pt",
    val currencyCode: String = "BRL",
    val primaryColorHex: String = "#6750A4",
    val themeMode: String = "light",
    val userPlan: String = "Gratuito",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis()
)
