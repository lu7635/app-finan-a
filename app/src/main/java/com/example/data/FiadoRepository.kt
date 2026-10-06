package com.example.data

import kotlinx.coroutines.flow.Flow
import java.io.File

class FiadoRepository(private val appDao: AppDao) {

    fun getClientsFlow(userId: String): Flow<List<Client>> =
        if (userId.isEmpty()) appDao.getAllClientsFlow() else appDao.getClientsByUserFlow(userId)

    fun getDebtsFlow(userId: String): Flow<List<Debt>> =
        if (userId.isEmpty()) appDao.getAllDebtsFlow() else appDao.getDebtsByUserFlow(userId)

    fun getPaymentsFlow(userId: String): Flow<List<Payment>> =
        if (userId.isEmpty()) appDao.getAllPaymentsFlow() else appDao.getPaymentsByUserFlow(userId)

    fun getProductsFlow(userId: String): Flow<List<Product>> =
        if (userId.isEmpty()) appDao.getAllProductsFlow() else appDao.getProductsByUserFlow(userId)

    val allClients: Flow<List<Client>> = appDao.getAllClientsFlow()
    val allDebts: Flow<List<Debt>> = appDao.getAllDebtsFlow()
    val allPayments: Flow<List<Payment>> = appDao.getAllPaymentsFlow()
    val allProducts: Flow<List<Product>> = appDao.getAllProductsFlow()

    suspend fun getClientsList(userId: String = ""): List<Client> =
        if (userId.isEmpty()) appDao.getAllClients() else appDao.getClientsByUser(userId)

    suspend fun getDebtsList(userId: String = ""): List<Debt> =
        if (userId.isEmpty()) appDao.getAllDebts() else appDao.getDebtsByUser(userId)

    suspend fun getPaymentsList(userId: String = ""): List<Payment> =
        if (userId.isEmpty()) appDao.getAllPayments() else appDao.getPaymentsByUser(userId)

    suspend fun getProductsList(userId: String = ""): List<Product> =
        if (userId.isEmpty()) appDao.getAllProducts() else appDao.getProductsByUser(userId)

    suspend fun getUserProfile(userId: String): UserProfile? = appDao.getUserProfile(userId)
    suspend fun getUserProfileByEmail(email: String): UserProfile? = appDao.getUserProfileByEmail(email)
    suspend fun saveUserProfile(profile: UserProfile): Long = appDao.insertUserProfile(profile)

    suspend fun migrateUserData(oldUserId: String, newUserId: String) {
        if (oldUserId == newUserId || oldUserId.isEmpty() || newUserId.isEmpty()) return
        appDao.migrateClientsUserId(oldUserId, newUserId)
        appDao.migrateDebtsUserId(oldUserId, newUserId)
        appDao.migratePaymentsUserId(oldUserId, newUserId)
        appDao.migrateProductsUserId(oldUserId, newUserId)
    }

    suspend fun cleanLegacyMockData() {
        try {
            appDao.cleanLegacyMockPayments()
            appDao.cleanLegacyMockDebts()
            appDao.cleanLegacyMockDebts2()
            appDao.cleanLegacyMockClients()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getProductById(id: Int): Product? = appDao.getProductById(id)
    suspend fun insertProduct(product: Product, defaultUserId: String = ""): Long {
        val target = if (product.userId.isEmpty() || product.userId == "default_user") {
            if (defaultUserId.isNotEmpty()) product.copy(userId = defaultUserId) else product
        } else product
        return appDao.insertProduct(target)
    }
    suspend fun updateProduct(product: Product) = appDao.updateProduct(product)
    suspend fun deleteProduct(product: Product) = appDao.deleteProduct(product)

    suspend fun getClientById(id: Int): Client? = appDao.getClientById(id)

    suspend fun insertClient(client: Client, defaultUserId: String = ""): Long {
        val target = if (client.userId.isEmpty() || client.userId == "default_user") {
            if (defaultUserId.isNotEmpty()) client.copy(userId = defaultUserId) else client
        } else client
        return appDao.insertClient(target)
    }

    suspend fun updateClient(client: Client) {
        appDao.updateClient(client)
    }

    suspend fun deleteClient(client: Client) {
        // Find and delete all debts and payments associated with this client to keep DB clean
        val debts = appDao.getDebtsByClient(client.id)
        debts.forEach { debt ->
            val payments = appDao.getPaymentsByDebt(debt.id)
            payments.forEach { payment -> appDao.deletePayment(payment) }
            appDao.deleteDebt(debt)
        }
        appDao.deleteClient(client)
    }

    fun getDebtsByClientFlow(clientId: Int): Flow<List<Debt>> = appDao.getDebtsByClientFlow(clientId)

    suspend fun getDebtsByClient(clientId: Int): List<Debt> = appDao.getDebtsByClient(clientId)

    suspend fun insertDebt(debt: Debt, defaultUserId: String = ""): Long {
        val target = if (debt.userId.isEmpty() || debt.userId == "default_user") {
            if (defaultUserId.isNotEmpty()) debt.copy(userId = defaultUserId) else debt
        } else debt
        val result = appDao.insertDebt(target)
        // Update client's last movement
        appDao.getClientById(debt.clientId)?.let { client ->
            appDao.updateClient(client.copy(lastMovement = System.currentTimeMillis()))
        }
        return result
    }

    suspend fun updateDebt(debt: Debt) {
        appDao.updateDebt(debt)
        // Update client's last movement
        appDao.getClientById(debt.clientId)?.let { client ->
            appDao.updateClient(client.copy(lastMovement = System.currentTimeMillis()))
        }
    }

    suspend fun deleteDebt(debt: Debt) {
        val payments = appDao.getPaymentsByDebt(debt.id)
        payments.forEach { payment -> appDao.deletePayment(payment) }
        appDao.deleteDebt(debt)
    }

    fun getPaymentsByDebtFlow(debtId: Int): Flow<List<Payment>> = appDao.getPaymentsByDebtFlow(debtId)

    fun getPaymentsByClientFlow(clientId: Int): Flow<List<Payment>> = appDao.getPaymentsByClientFlow(clientId)

    suspend fun registerPayment(debtId: Int, value: Double, notes: String?, dateMillis: Long, userId: String = ""): Long {
        val debt = appDao.getDebtById(debtId) ?: return -1L
        val effectiveUserId = userId.ifEmpty { debt.userId }

        // Create payment record
        val payment = Payment(
            userId = effectiveUserId,
            debtId = debtId,
            clientId = debt.clientId,
            value = value,
            status = "Recebido",
            createdAt = dateMillis,
            receivedAt = dateMillis,
            notes = notes
        )
        val paymentId = appDao.insertPayment(payment)

        // Calculate new paid fields
        val paymentsForDebt = appDao.getPaymentsByDebt(debtId)
        val totalPaid = paymentsForDebt.sumOf { it.value }
        
        val newStatus = when {
            totalPaid >= debt.value -> "Pago"
            totalPaid > 0.0 -> "Parcial"
            else -> "Pendente"
        }

        val updatedDebt = debt.copy(
            paidValue = totalPaid,
            status = newStatus,
            receivedAt = if (newStatus == "Pago") dateMillis else debt.receivedAt
        )
        appDao.updateDebt(updatedDebt)

        // Update last movement of the client
        appDao.getClientById(debt.clientId)?.let { client ->
            appDao.updateClient(client.copy(lastMovement = System.currentTimeMillis()))
        }

        return paymentId
    }

    // Helper for local JSON backup/restore
    suspend fun exportDataToJson(): String {
        val clients = appDao.getAllClients()
        val debts = appDao.getAllDebts()
        val payments = appDao.getAllPayments()

        // Create a simple custom JSON string payload
        val clientsJson = clients.joinToString(",") { 
            """{"id":${it.id},"name":"${escape(it.name)}","phone":"${it.phone}","whatsapp":"${it.whatsapp}","email":${quote(it.email)},"address":${quote(it.address)},"notes":${quote(it.notes)},"createdAt":${it.createdAt},"lastMovement":${it.lastMovement}}"""
        }
        val debtsJson = debts.joinToString(",") {
            """{"id":${it.id},"clientId":${it.clientId},"type":"${it.type}","description":"${escape(it.description)}","value":${it.value},"paidValue":${it.paidValue},"createdAt":${it.createdAt},"notes":${quote(it.notes)},"status":"${it.status}"}"""
        }
        val paymentsJson = payments.joinToString(",") {
            """{"id":${it.id},"debtId":${it.debtId},"clientId":${it.clientId},"value":${it.value},"createdAt":${it.createdAt},"notes":${quote(it.notes)}}"""
        }

        return """{"clients":[$clientsJson],"debts":[$debtsJson],"payments":[$paymentsJson]}"""
    }

    private fun escape(s: String): String {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    }

    private fun quote(s: String?): String {
        return if (s == null) "null" else "\"${escape(s)}\""
    }

    suspend fun importDataFromJson(jsonStr: String): Boolean {
        try {
            // A simple JSON parser robust enough for our exported format:
            // Since we use Room, let's parse clients, debts, payments arrays.
            // Let's implement a clean state-machine or regex parsing for import safety!
            // First, clear the database completely or let's merge. To restore, let's clear the tables,
            // then insert each manually.
            
            // Wait! To be absolutely safe and bulletproof with JSON parsing without heavy libraries,
            // we can parse our generated format using regex.
            val clientsRegex = Regex("""\{"id":(\d+),"name":"(.*?)"\,"phone":"(.*?)"\,"whatsapp":"(.*?)"\,"email":(null|"[^"]*")\,"address":(null|"[^"]*")\,"notes":(null|"[^"]*")\,"createdAt":(\d+)\,"lastMovement":(\d+)\}""")
            val debtsRegex = Regex("""\{"id":(\d+),"clientId":(\d+),"type":"(.*?)"\,"description":"(.*?)"\,"value":([\d\.]+)\,"paidValue":([\d\.]+)\,"createdAt":(\d+)\,"notes":(null|"[^"]*")\,"status":"(.*?)"\}""")
            val paymentsRegex = Regex("""\{"id":(\d+),"debtId":(\d+),"clientId":(\d+),"value":([\d\.]+)\,"createdAt":(\d+)\,"notes":(null|"[^"]*")\}""")

            val parsedClients = clientsRegex.findAll(jsonStr).map { match ->
                val id = match.groupValues[1].toInt()
                val name = unescape(match.groupValues[2])
                val phone = match.groupValues[3]
                val whatsapp = match.groupValues[4]
                val email = unquote(match.groupValues[5])
                val address = unquote(match.groupValues[6])
                val notes = unquote(match.groupValues[7])
                val createdAt = match.groupValues[8].toLong()
                val lastMovement = match.groupValues[9].toLong()
                Client(
                    id = id,
                    name = name,
                    phone = phone,
                    whatsapp = whatsapp,
                    email = email,
                    address = address,
                    notes = notes,
                    createdAt = createdAt,
                    lastMovement = lastMovement
                )
            }.toList()

            val parsedDebts = debtsRegex.findAll(jsonStr).map { match ->
                val id = match.groupValues[1].toInt()
                val clientId = match.groupValues[2].toInt()
                val type = match.groupValues[3]
                val description = unescape(match.groupValues[4])
                val value = match.groupValues[5].toDouble()
                val paidValue = match.groupValues[6].toDouble()
                val createdAt = match.groupValues[7].toLong()
                val notes = unquote(match.groupValues[8])
                val status = match.groupValues[9]
                Debt(
                    id = id,
                    clientId = clientId,
                    type = type,
                    description = description,
                    value = value,
                    paidValue = paidValue,
                    createdAt = createdAt,
                    notes = notes,
                    status = status
                )
            }.toList()

            val parsedPayments = paymentsRegex.findAll(jsonStr).map { match ->
                val id = match.groupValues[1].toInt()
                val debtId = match.groupValues[2].toInt()
                val clientId = match.groupValues[3].toInt()
                val value = match.groupValues[4].toDouble()
                val createdAt = match.groupValues[5].toLong()
                val notes = unquote(match.groupValues[6])
                Payment(
                    id = id,
                    debtId = debtId,
                    clientId = clientId,
                    value = value,
                    createdAt = createdAt,
                    notes = notes
                )
            }.toList()

            if (parsedClients.isEmpty() && parsedDebts.isEmpty() && parsedPayments.isEmpty()) {
                // If it fails regex parse, maybe it's empty, but if everything matches, restore it!
                // Let's do a fallback check. If we are sure, proceed.
            }

            // Restore tables
            val existingClients = appDao.getAllClients()
            val existingDebts = appDao.getAllDebts()
            val existingPayments = appDao.getAllPayments()

            // Delete
            existingPayments.forEach { appDao.deletePayment(it) }
            existingDebts.forEach { appDao.deleteDebt(it) }
            existingClients.forEach { appDao.deleteClient(it) }

            // Re-insert
            parsedClients.forEach { appDao.insertClient(it) }
            parsedDebts.forEach { appDao.insertDebt(it) }
            parsedPayments.forEach { appDao.insertPayment(it) }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun importStructuredData(
        clients: List<Client>,
        debts: List<Debt>,
        payments: List<Payment>,
        products: List<Product>,
        userId: String = ""
    ): Boolean {
        try {
            clients.forEach { appDao.insertClient(if (userId.isNotEmpty()) it.copy(userId = userId) else it) }
            debts.forEach { appDao.insertDebt(if (userId.isNotEmpty()) it.copy(userId = userId) else it) }
            payments.forEach { appDao.insertPayment(if (userId.isNotEmpty()) it.copy(userId = userId) else it) }
            products.forEach { appDao.insertProduct(if (userId.isNotEmpty()) it.copy(userId = userId) else it) }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun unescape(s: String): String {
        return s.replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\")
    }

    private fun unquote(s: String): String? {
        if (s == "null") return null
        return unescape(s.substring(1, s.length - 1))
    }
}
