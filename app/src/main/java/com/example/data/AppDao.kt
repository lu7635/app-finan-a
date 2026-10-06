package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Clients
    @Query("SELECT * FROM clients WHERE userId = :userId ORDER BY name ASC")
    fun getClientsByUserFlow(userId: String): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE userId = :userId ORDER BY name ASC")
    suspend fun getClientsByUser(userId: String): List<Client>

    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClientsFlow(): Flow<List<Client>>

    @Query("SELECT * FROM clients ORDER BY name ASC")
    suspend fun getAllClients(): List<Client>

    @Query("SELECT * FROM clients WHERE id = :clientId")
    suspend fun getClientById(clientId: Int): Client?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)

    // Debts
    @Query("SELECT * FROM debts WHERE userId = :userId ORDER BY createdAt DESC")
    fun getDebtsByUserFlow(userId: String): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getDebtsByUser(userId: String): List<Debt>

    @Query("SELECT * FROM debts ORDER BY createdAt DESC")
    fun getAllDebtsFlow(): Flow<List<Debt>>

    @Query("SELECT * FROM debts ORDER BY createdAt DESC")
    suspend fun getAllDebts(): List<Debt>

    @Query("SELECT * FROM debts WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun getDebtsByClientFlow(clientId: Int): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE clientId = :clientId ORDER BY createdAt DESC")
    suspend fun getDebtsByClient(clientId: Int): List<Debt>

    @Query("SELECT * FROM debts WHERE id = :debtId")
    suspend fun getDebtById(debtId: Int): Debt?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt): Long

    @Update
    suspend fun updateDebt(debt: Debt)

    @Delete
    suspend fun deleteDebt(debt: Debt)

    // Payments
    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY createdAt DESC")
    fun getPaymentsByUserFlow(userId: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getPaymentsByUser(userId: String): List<Payment>

    @Query("SELECT * FROM payments ORDER BY createdAt DESC")
    fun getAllPaymentsFlow(): Flow<List<Payment>>

    @Query("SELECT * FROM payments ORDER BY createdAt DESC")
    suspend fun getAllPayments(): List<Payment>

    @Query("SELECT * FROM payments WHERE debtId = :debtId ORDER BY createdAt DESC")
    fun getPaymentsByDebtFlow(debtId: Int): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE debtId = :debtId ORDER BY createdAt DESC")
    suspend fun getPaymentsByDebt(debtId: Int): List<Payment>

    @Query("SELECT * FROM payments WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun getPaymentsByClientFlow(clientId: Int): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Delete
    suspend fun deletePayment(payment: Payment)

    // Products
    @Query("SELECT * FROM products WHERE userId = :userId ORDER BY name ASC")
    fun getProductsByUserFlow(userId: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE userId = :userId ORDER BY name ASC")
    suspend fun getProductsByUser(userId: String): List<Product>

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllProducts(): List<Product>

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: Int): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    // User Profile
    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    suspend fun getUserProfile(userId: String): UserProfile?

    @Query("SELECT * FROM user_profiles WHERE email = :email LIMIT 1")
    suspend fun getUserProfileByEmail(email: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile): Long

    @Update
    suspend fun updateUserProfile(profile: UserProfile)

    // User Data Migration & Integrity
    @Query("UPDATE clients SET userId = :newUserId WHERE userId = :oldUserId")
    suspend fun migrateClientsUserId(oldUserId: String, newUserId: String)

    @Query("UPDATE debts SET userId = :newUserId WHERE userId = :oldUserId")
    suspend fun migrateDebtsUserId(oldUserId: String, newUserId: String)

    @Query("UPDATE payments SET userId = :newUserId WHERE userId = :oldUserId")
    suspend fun migratePaymentsUserId(oldUserId: String, newUserId: String)

    @Query("UPDATE products SET userId = :newUserId WHERE userId = :oldUserId")
    suspend fun migrateProductsUserId(oldUserId: String, newUserId: String)

    // Legacy Mock Data Purge (removes invented/nonexistent payment and paid debt records)
    @Query("DELETE FROM payments WHERE notes IN ('PIX confirmado hoje', 'Transferência bancária', 'Cartão de Crédito', 'PIX integral', 'PIX à vista', 'Cartão de Débito', 'PIX liquidado', 'Dinheiro no balcão', 'PIX transferido', 'Venda de Kit Cosméticos (152 un)', 'Venda de Troca de Óleo e Filtros (128 un)', 'Venda de Acessórios e Ferramentas (96 un)', 'Venda de Lote de Peças Automotivas (74 un)')")
    suspend fun cleanLegacyMockPayments()

    @Query("DELETE FROM debts WHERE description IN ('Kit Cosméticos & Cuidados', 'Troca de Óleo e Filtros', 'Acessórios e Ferramentas', 'Lote de Peças Automotivas') AND status = 'Pago' AND quantity IS NOT NULL")
    suspend fun cleanLegacyMockDebts()

    @Query("DELETE FROM debts WHERE description IN ('Revisão e Manutenção', 'Revisão Rápida', 'Venda de Produtos') AND notes LIKE '%PIX%' AND status = 'Pago'")
    suspend fun cleanLegacyMockDebts2()

    @Query("DELETE FROM clients WHERE name IN ('Bruno Martins', 'Roberto', 'Mariana', 'Lucas', 'Fernanda', 'Ricardo', 'Camila')")
    suspend fun cleanLegacyMockClients()
}
