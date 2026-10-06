package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.viewmodel.FiadoViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthPersistenceRobolectricTest {

    private lateinit var app: Application
    private lateinit var database: AppDatabase
    private lateinit var dao: AppDao
    private lateinit var repository: FiadoRepository

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(app)
        dao = database.appDao()
        repository = FiadoRepository(dao)
    }

    @Test
    fun testNoDataLostOnLogoutAndLoginAndMultiUserIsolation() = runBlocking {
        val user1Email = "luishenriquesilrib2006@gmail.com"
        val user1Name = "Luis Henrique"
        val user1Uid = "user_luishenriquesilrib2006_gmail_com"

        val user2Email = "maria@gmail.com"
        val user2Name = "Maria Silva"
        val user2Uid = "user_maria_gmail_com"

        // 1. Setup User 1 profile and settings
        val user1Profile = UserProfile(
            userId = user1Uid,
            name = user1Name,
            email = user1Email,
            languageCode = "pt",
            currencyCode = "BRL",
            primaryColorHex = "#6750A4",
            themeMode = "dark",
            userPlan = "Pro"
        )
        repository.saveUserProfile(user1Profile)

        // 2. Add clients and debts for User 1
        val c1Id = repository.insertClient(
            Client(
                userId = user1Uid,
                name = "Cliente Exclusivo do Luis",
                phone = "11999990001",
                whatsapp = "11999990001"
            ),
            user1Uid
        ).toInt()

        val d1Id = repository.insertDebt(
            Debt(
                userId = user1Uid,
                clientId = c1Id,
                type = "Produto",
                description = "Venda Especial",
                value = 500.0,
                paidValue = 200.0,
                status = "Parcial"
            ),
            user1Uid
        ).toInt()

        repository.registerPayment(
            debtId = d1Id,
            value = 200.0,
            notes = "Adiantamento",
            dateMillis = System.currentTimeMillis(),
            userId = user1Uid
        )

        val p1Id = repository.insertProduct(
            Product(
                userId = user1Uid,
                name = "Produto Luis 1",
                price = 100.0,
                stockQuantity = 25
            ),
            user1Uid
        ).toInt()

        // Verify User 1 data exists in Room
        val user1Clients = repository.getClientsList(user1Uid)
        assertEquals(1, user1Clients.size)
        assertEquals("Cliente Exclusivo do Luis", user1Clients.first().name)

        val user1Debts = repository.getDebtsList(user1Uid)
        assertEquals(1, user1Debts.size)
        assertEquals(500.0, user1Debts.first().value, 0.001)

        val user1Payments = repository.getPaymentsList(user1Uid)
        assertEquals(1, user1Payments.size)
        assertEquals(200.0, user1Payments.first().value, 0.001)

        val user1Products = repository.getProductsList(user1Uid)
        assertEquals(1, user1Products.size)
        assertEquals("Produto Luis 1", user1Products.first().name)

        // 3. Simulate Logout: ensure database records are NOT erased
        val viewModel = FiadoViewModel(app)
        viewModel.authenticateUser(user1Name, user1Email, user1Uid)
        assertEquals(true, viewModel.isLoggedIn)
        assertEquals(user1Uid, viewModel.currentUserId)

        // Trigger Logout
        viewModel.firebaseLogout()
        assertEquals(false, viewModel.isLoggedIn)

        // Critical Verification: Database data for User 1 must still exist completely intact!
        val clientsAfterLogout = repository.getClientsList(user1Uid)
        assertEquals("Logout must NOT delete user clients", 1, clientsAfterLogout.size)
        assertEquals("Cliente Exclusivo do Luis", clientsAfterLogout.first().name)

        val debtsAfterLogout = repository.getDebtsList(user1Uid)
        assertEquals("Logout must NOT delete user debts", 1, debtsAfterLogout.size)

        val paymentsAfterLogout = repository.getPaymentsList(user1Uid)
        assertEquals("Logout must NOT delete user payments", 1, paymentsAfterLogout.size)

        val profileAfterLogout = repository.getUserProfile(user1Uid)
        assertNotNull("Logout must NOT delete user profile", profileAfterLogout)
        assertEquals("dark", profileAfterLogout?.themeMode)
        assertEquals("Pro", profileAfterLogout?.userPlan)

        // 4. Simulate login with Second Account (Maria)
        viewModel.authenticateUser(user2Name, user2Email, user2Uid)
        assertEquals(true, viewModel.isLoggedIn)
        assertEquals(user2Uid, viewModel.currentUserId)
        assertEquals(user2Email, viewModel.loggedInEmail)

        // Confirm User 1's data does NOT leak or appear in User 2's session
        val user2ClientsInitial = repository.getClientsList(user2Uid)
        assertEquals("User 2 must start with no clients from User 1", 0, user2ClientsInitial.size)

        val user2DebtsInitial = repository.getDebtsList(user2Uid)
        assertEquals("User 2 must start with no debts from User 1", 0, user2DebtsInitial.size)

        // 5. Add custom data for User 2 (Maria)
        val c2Id = repository.insertClient(
            Client(
                userId = user2Uid,
                name = "Padaria da Maria",
                phone = "11888880002",
                whatsapp = "11888880002"
            ),
            user2Uid
        ).toInt()

        repository.insertDebt(
            Debt(
                userId = user2Uid,
                clientId = c2Id,
                type = "Serviço",
                description = "Fornecimento Pães",
                value = 150.0,
                status = "Pendente"
            ),
            user2Uid
        )

        // Update User 2 theme to light
        viewModel.updateThemeMode("light")
        viewModel.updateCurrency("USD")

        val user2Clients = repository.getClientsList(user2Uid)
        assertEquals(1, user2Clients.size)
        assertEquals("Padaria da Maria", user2Clients.first().name)

        // Logout User 2
        viewModel.firebaseLogout()
        assertEquals(false, viewModel.isLoggedIn)

        // 6. Log back into User 1 (Luis Henrique)
        viewModel.authenticateUser(user1Name, user1Email, user1Uid)
        assertEquals(true, viewModel.isLoggedIn)
        assertEquals(user1Uid, viewModel.currentUserId)
        assertEquals(user1Email, viewModel.loggedInEmail)

        // VERIFY: All User 1 data is 100% restored exactly as it was before logout!
        val user1ClientsRestored = repository.getClientsList(user1Uid)
        assertEquals("All of User 1 clients must remain intact", 1, user1ClientsRestored.size)
        assertEquals("Cliente Exclusivo do Luis", user1ClientsRestored.first().name)

        val user1DebtsRestored = repository.getDebtsList(user1Uid)
        assertEquals("All of User 1 debts must remain intact", 1, user1DebtsRestored.size)
        assertEquals(500.0, user1DebtsRestored.first().value, 0.001)
        assertEquals(200.0, user1DebtsRestored.first().paidValue, 0.001)

        val user1PaymentsRestored = repository.getPaymentsList(user1Uid)
        assertEquals("All of User 1 payments must remain intact", 1, user1PaymentsRestored.size)
        assertEquals(200.0, user1PaymentsRestored.first().value, 0.001)

        val user1ProfileRestored = repository.getUserProfile(user1Uid)
        assertNotNull(user1ProfileRestored)
        assertEquals("dark", user1ProfileRestored?.themeMode)
        assertEquals("Pro", user1ProfileRestored?.userPlan)
        assertEquals("#6750A4", user1ProfileRestored?.primaryColorHex)

        // VERIFY: User 2's data was not mixed or lost either
        val user2ClientsFinal = repository.getClientsList(user2Uid)
        assertEquals(1, user2ClientsFinal.size)
        assertEquals("Padaria da Maria", user2ClientsFinal.first().name)
    }
}
