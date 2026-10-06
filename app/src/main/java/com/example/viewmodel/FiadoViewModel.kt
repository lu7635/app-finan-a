package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed interface SelectedDayStatus {
    object None : SelectedDayStatus
    data class Exists(
        val dateStr: String,
        val clientName: String,
        val value: Double,
        val type: String,
        val time: String,
        val status: String
    ) : SelectedDayStatus
}

sealed interface Screen {
    object Home : Screen
    object Clients : Screen
    object Calendar : Screen
    object Reports : Screen
    object Account : Screen
    object Products : Screen
    data class ClientDetails(val clientId: Int) : Screen
    object AddClient : Screen
    data class AddDebt(val clientId: Int) : Screen
    object SyncSettings : Screen
}

data class DebtorSummary(
    val client: Client,
    val totalPending: Double,
    val oldestDebtDays: Long,
    val pendingDebtsCount: Int,
    val oldestDebtDescription: String
)

data class DayFinanceSummary(
    val dayNumber: Int,
    val dateMillis: Long,
    val formattedDate: String,
    val totalReceived: Double,
    val totalPending: Double,
    val totalEarned: Double,
    val clientsCount: Int,
    val payments: List<Payment>,
    val debts: List<Debt>
)

data class BestSellingProduct(
    val product: Product,
    val totalQuantitySold: Int,
    val totalRevenue: Double,
    val rank: Int = 1
)

class FiadoViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Context = application.applicationContext
    private val db = AppDatabase.getDatabase(appContext)
    val repository = FiadoRepository(db.appDao())

    // Shared Preferences
    private val prefs = appContext.getSharedPreferences("fiado_prefs", Context.MODE_PRIVATE)

    // Configuration preferences (saved to SharedPreferences)
    var themeMode by mutableStateOf(
        prefs.getString("theme_mode", "light")?.let {
            if (it == "auto") "light" else it
        } ?: "light"
    )
        private set

    var languageCode by mutableStateOf("pt")
        private set

    var currencyCode by mutableStateOf("BRL")
        private set

    var primaryColorHex by mutableStateOf(prefs.getString("primary_color_hex", "#6750A4") ?: "#6750A4")
        private set

    // Login state & User identification
    var isLoggedIn by mutableStateOf(prefs.getBoolean("is_logged_in", true))
        private set

    var loggedInName by mutableStateOf(prefs.getString("logged_in_name", "Luis Henrique") ?: "Luis Henrique")
        private set

    var loggedInEmail by mutableStateOf(prefs.getString("logged_in_email", "luishenriquesilrib2006@gmail.com") ?: "luishenriquesilrib2006@gmail.com")
        private set

    var userPlan by mutableStateOf(prefs.getString("user_plan", "Gratuito") ?: "Gratuito")
        private set

    fun generateDeterministicUserId(email: String): String {
        val clean = email.trim().lowercase()
        return if (clean.isEmpty()) "default_user" else "user_" + clean.replace(Regex("[^a-zA-Z0-9]"), "_")
    }

    var currentUserId by mutableStateOf(
        prefs.getString("current_user_id", "")?.takeIf { it.isNotEmpty() }
            ?: generateDeterministicUserId(prefs.getString("logged_in_email", "luishenriquesilrib2006@gmail.com") ?: "luishenriquesilrib2006@gmail.com")
    )
        private set

    val currentUserIdFlow = MutableStateFlow(currentUserId)

    // Screen stack for simple state-based custom navigation
    val screenStack = mutableStateListOf<Screen>(Screen.Home)
    
    val currentScreen: Screen
        get() = screenStack.lastOrNull() ?: Screen.Home

    // In-memory filters & states
    val searchPattern = MutableStateFlow("")
    private val calendarInit = Calendar.getInstance()
    var calendarYear by mutableStateOf(calendarInit.get(Calendar.YEAR))
    var calendarMonth by mutableStateOf(calendarInit.get(Calendar.MONTH) + 1)
    var selectedCalendarDay by mutableStateOf<Int?>(calendarInit.get(Calendar.DAY_OF_MONTH))
    var calendarViewMode by mutableStateOf("Mês") // "Dia", "Semana", "Mês"

    fun setCalendarView(mode: String) {
        calendarViewMode = mode
    }

    fun previousMonth() {
        if (calendarMonth == 1) {
            calendarMonth = 12
            calendarYear -= 1
        } else {
            calendarMonth -= 1
        }
        selectedCalendarDay = 1
    }

    fun nextMonth() {
        if (calendarMonth == 12) {
            calendarMonth = 1
            calendarYear += 1
        } else {
            calendarMonth += 1
        }
        selectedCalendarDay = 1
    }

    fun getMonthNameFormatted(): String {
        val months = listOf(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        )
        val mIndex = (calendarMonth - 1).coerceIn(0, 11)
        return "${months[mIndex]} $calendarYear"
    }

    // Debtors sorted strictly by oldest pending debt (tempo da pendência)
    fun getDebtorsSortedByDelay(clients: List<Client>, debts: List<Debt>): List<DebtorSummary> {
        val now = System.currentTimeMillis()
        val oneDayLog = 24 * 60 * 60 * 1000L

        return clients.mapNotNull { client ->
            val pendingDebts = debts.filter { it.clientId == client.id && it.status != "Pago" && (it.value - it.paidValue) > 0 }
            if (pendingDebts.isEmpty()) return@mapNotNull null

            val totalPending = pendingDebts.sumOf { it.value - it.paidValue }
            val oldestDebt = pendingDebts.minByOrNull { it.createdAt } ?: return@mapNotNull null
            val daysDelay = ((now - oldestDebt.createdAt) / oneDayLog).coerceAtLeast(0)

            DebtorSummary(
                client = client,
                totalPending = totalPending,
                oldestDebtDays = daysDelay,
                pendingDebtsCount = pendingDebts.size,
                oldestDebtDescription = oldestDebt.description
            )
        }.sortedByDescending { it.oldestDebtDays }
    }

    // Dynamic calculations for Main Card - exactly calculated from actual user payments and debts
    /**
     * TOTAL GANHO (Mês Atual):
     * Representa exclusivamente o valor recebido dentro do mês atual.
     * - Somar somente os valores que foram efetivamente recebidos/pagos dentro do mês atual.
     * - O cálculo considera a data em que o valor foi recebido (receivedAt / createdAt).
     * - Quando o mês mudar, o Total Ganho zera automaticamente para o novo mês (inicia nova contagem do zero).
     * - Os valores recebidos nos meses anteriores não continuam aparecendo no Total Ganho do novo mês.
     * - Não apaga dados históricos: meses anteriores continuam armazenados no banco de dados.
     */
    fun calculateTotalGanhoCurrentMonth(payments: List<Payment>): Double {
        val nowCal = Calendar.getInstance()
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentMonth = nowCal.get(Calendar.MONTH) // 0-indexed (Janeiro = 0)

        return payments.filter { payment ->
            val pTime = if (payment.receivedAt > 0) payment.receivedAt else payment.createdAt
            val pCal = Calendar.getInstance().apply { timeInMillis = pTime }
            pCal.get(Calendar.YEAR) == currentYear && pCal.get(Calendar.MONTH) == currentMonth
        }.sumOf { it.value }
    }

    /**
     * Lista de pagamentos recebidos no mês atual
     */
    fun getPaymentsForCurrentMonth(payments: List<Payment>): List<Payment> {
        val nowCal = Calendar.getInstance()
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentMonth = nowCal.get(Calendar.MONTH)

        return payments.filter { payment ->
            val pTime = if (payment.receivedAt > 0) payment.receivedAt else payment.createdAt
            val pCal = Calendar.getInstance().apply { timeInMillis = pTime }
            pCal.get(Calendar.YEAR) == currentYear && pCal.get(Calendar.MONTH) == currentMonth
        }.sortedByDescending { if (it.receivedAt > 0) it.receivedAt else it.createdAt }
    }

    /**
     * A RECEBER:
     * - Não reinicia quando o mês mudar.
     * - Continua mostrando o total de valores que ainda estão pendentes de recebimento,
     *   independentemente da mudança de mês.
     * - Quando um cliente realiza um pagamento, remove o respectivo valor de 'A Receber' e adiciona a 'Total Ganho'.
     * - Valores pendentes de meses anteriores continuam contabilizados em 'A Receber' até serem pagos.
     * - A mudança de mês não altera, zera ou recalcula indevidamente o valor pendente.
     */
    fun calculateTotalAReceber(debts: List<Debt>): Double {
        return debts
            .filter { it.status != "Pago" }
            .sumOf { (it.value - it.paidValue).coerceAtLeast(0.0) }
    }

    /**
     * Lista de dívidas pendentes de recebimento (sem filtro de mês)
     */
    fun getPendingDebts(debts: List<Debt>): List<Debt> {
        return debts
            .filter { it.status != "Pago" && (it.value - it.paidValue) > 0 }
            .sortedBy { it.createdAt }
    }

    /**
     * Total geral histórico de pagamentos recebidos (todo o histórico)
     */
    fun calculateTotalReceivedHistory(payments: List<Payment>): Double {
        return payments.sumOf { it.value }
    }

    fun calculateTodayReceived(payments: List<Payment>): Double {
        return calculateReceivedForPeriod(payments, "Hoje")
    }

    fun calculateReceivedForPeriod(payments: List<Payment>, period: String): Double {
        if (payments.isEmpty()) return 0.0

        val nowCal = Calendar.getInstance()
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentMonth = nowCal.get(Calendar.MONTH)
        val currentDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)

        return when (period.trim().lowercase()) {
            "hoje", "dia" -> {
                payments.filter { payment ->
                    val pTime = if (payment.receivedAt > 0) payment.receivedAt else payment.createdAt
                    val pCal = Calendar.getInstance().apply { timeInMillis = pTime }
                    pCal.get(Calendar.YEAR) == currentYear && pCal.get(Calendar.DAY_OF_YEAR) == currentDayOfYear
                }.sumOf { it.value }
            }
            "esta semana", "semana" -> {
                // Início da semana atual (Segunda-feira 00:00:00.000)
                val startOfWeek = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                    val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                    add(Calendar.DAY_OF_MONTH, -daysFromMonday)
                }.timeInMillis

                val endOfWeek = Calendar.getInstance().apply {
                    timeInMillis = startOfWeek
                    add(Calendar.DAY_OF_MONTH, 7)
                    add(Calendar.MILLISECOND, -1)
                }.timeInMillis

                payments.filter { payment ->
                    val pTime = if (payment.receivedAt > 0) payment.receivedAt else payment.createdAt
                    pTime in startOfWeek..endOfWeek
                }.sumOf { it.value }
            }
            "esse mês", "este mês", "mês", "mes" -> {
                calculateTotalGanhoCurrentMonth(payments)
            }
            "esse ano", "este ano", "ano" -> {
                payments.filter { payment ->
                    val pTime = if (payment.receivedAt > 0) payment.receivedAt else payment.createdAt
                    val pCal = Calendar.getInstance().apply { timeInMillis = pTime }
                    pCal.get(Calendar.YEAR) == currentYear
                }.sumOf { it.value }
            }
            else -> calculateTotalReceivedHistory(payments)
        }
    }

    fun calculateMonthGrowth(payments: List<Payment>): Double {
        val thisMonthPayments = calculateTotalGanhoCurrentMonth(payments)

        val lastMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val targetYear = lastMonthCal.get(Calendar.YEAR)
        val targetMonth = lastMonthCal.get(Calendar.MONTH)

        val lastMonthPayments = payments.filter { payment ->
            val pTime = if (payment.receivedAt > 0) payment.receivedAt else payment.createdAt
            val pCal = Calendar.getInstance().apply { timeInMillis = pTime }
            pCal.get(Calendar.YEAR) == targetYear && pCal.get(Calendar.MONTH) == targetMonth
        }.sumOf { it.value }

        return if (lastMonthPayments > 0.0) {
            ((thisMonthPayments - lastMonthPayments) / lastMonthPayments) * 100.0
        } else if (thisMonthPayments > 0.0) {
            100.0
        } else {
            0.0
        }
    }

    /**
     * Ranking dinâmico dos produtos mais vendidos ordenados em ordem decrescente de quantidade.
     * Suporta filtros de período: Hoje, Esta semana, Este mês, Este ano e Todo o período.
     */
    fun getBestSellingProducts(
        products: List<Product>,
        debts: List<Debt>,
        period: String = "Todo o período"
    ): List<BestSellingProduct> {
        val now = System.currentTimeMillis()
        val startOfToday = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfWeek = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
            add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        }.timeInMillis

        val startOfMonth = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfYear = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfToday = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        // Apenas dívidas/vendas válidas que não foram canceladas
        val validDebts = debts.filter { it.status != "Cancelado" && (it.type == "Produto" || it.productId != null) }

        val periodDebts = when (period.trim().lowercase()) {
            "hoje", "dia" -> validDebts.filter { it.createdAt in startOfToday..endOfToday }
            "esta semana", "semana" -> validDebts.filter { it.createdAt in startOfWeek..endOfToday }
            "este mês", "esse mês", "mês", "mes" -> validDebts.filter { it.createdAt in startOfMonth..endOfToday }
            "este ano", "esse ano", "ano" -> validDebts.filter { it.createdAt in startOfYear..endOfToday }
            else -> validDebts // "Todo o período"
        }

        return products.mapNotNull { prod ->
            val productDebts = periodDebts.filter { debt ->
                (debt.productId != null && debt.productId == prod.id) ||
                (debt.type == "Produto" && debt.description.trim().equals(prod.name.trim(), ignoreCase = true))
            }

            if (productDebts.isEmpty()) return@mapNotNull null

            var totalQty = 0
            var totalRev = 0.0

            productDebts.forEach { debt ->
                val qty = if (debt.quantity != null && debt.quantity > 0) {
                    debt.quantity
                } else if (prod.price > 0 && debt.value > 0) {
                    (debt.value / prod.price).toInt().coerceAtLeast(1)
                } else 1
                totalQty += qty
                totalRev += debt.value
            }

            if (totalQty <= 0) return@mapNotNull null

            BestSellingProduct(
                product = prod,
                totalQuantitySold = totalQty,
                totalRevenue = totalRev,
                rank = 1
            )
        }
        .sortedWith(compareByDescending<BestSellingProduct> { it.totalQuantitySold }.thenByDescending { it.totalRevenue })
        .mapIndexed { index, item ->
            item.copy(rank = index + 1)
        }
    }

    // Financial summaries for calendar days
    fun getDayFinanceSummary(
        dayNumber: Int,
        clients: List<Client>,
        debts: List<Debt>,
        payments: List<Payment>
    ): DayFinanceSummary {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, calendarYear)
            set(Calendar.MONTH, calendarMonth - 1)
            set(Calendar.DAY_OF_MONTH, dayNumber)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val targetMillis = cal.timeInMillis

        val dayPayments = payments.filter {
            val pCal = Calendar.getInstance().apply { timeInMillis = it.createdAt }
            pCal.get(Calendar.YEAR) == calendarYear &&
            pCal.get(Calendar.MONTH) == (calendarMonth - 1) &&
            pCal.get(Calendar.DAY_OF_MONTH) == dayNumber
        }

        val dayDebts = debts.filter {
            val dCal = Calendar.getInstance().apply { timeInMillis = it.createdAt }
            dCal.get(Calendar.YEAR) == calendarYear &&
            dCal.get(Calendar.MONTH) == (calendarMonth - 1) &&
            dCal.get(Calendar.DAY_OF_MONTH) == dayNumber
        }

        val totalReceived = dayPayments.sumOf { it.value }
        val totalPending = dayDebts.filter { it.status != "Pago" }.sumOf { (it.value - it.paidValue).coerceAtLeast(0.0) }
        val totalEarned = totalReceived // Valor recebido efetivamente no dia
        val involvedClientIds = (dayPayments.map { it.clientId } + dayDebts.map { it.clientId }).distinct()

        val formattedDate = String.format("%02d/%02d/%d", dayNumber, calendarMonth, calendarYear)

        return DayFinanceSummary(
            dayNumber = dayNumber,
            dateMillis = targetMillis,
            formattedDate = formattedDate,
            totalReceived = totalReceived,
            totalPending = totalPending,
            totalEarned = totalEarned,
            clientsCount = involvedClientIds.size,
            payments = dayPayments,
            debts = dayDebts
        )
    }

    // Days in current calendar month
    fun getDaysInCurrentMonth(): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, calendarYear)
            set(Calendar.MONTH, calendarMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Database flows scoped reactively to the current active user
    @OptIn(ExperimentalCoroutinesApi::class)
    val clientsFlow: StateFlow<List<Client>> = currentUserIdFlow
        .flatMapLatest { uid -> repository.getClientsFlow(uid) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val debtsFlow: StateFlow<List<Debt>> = currentUserIdFlow
        .flatMapLatest { uid -> repository.getDebtsFlow(uid) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val paymentsFlow: StateFlow<List<Payment>> = currentUserIdFlow
        .flatMapLatest { uid -> repository.getPaymentsFlow(uid) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val productsFlow: StateFlow<List<Product>> = currentUserIdFlow
        .flatMapLatest { uid -> repository.getProductsFlow(uid) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            // Purge any legacy invented/fake payments or paid debts
            repository.cleanLegacyMockData()

            val uid = currentUserId

            // Migrate legacy "default_user" data if this is the active user
            if (uid != "default_user") {
                val defaultClients = repository.getClientsList("default_user")
                if (defaultClients.isNotEmpty()) {
                    val userClients = repository.getClientsList(uid)
                    if (userClients.isEmpty()) {
                        repository.migrateUserData("default_user", uid)
                    }
                }
            }

            val existingProfile = repository.getUserProfile(uid)
                ?: repository.getUserProfileByEmail(loggedInEmail)

            if (existingProfile != null) {
                languageCode = existingProfile.languageCode
                currencyCode = existingProfile.currencyCode
                primaryColorHex = existingProfile.primaryColorHex
                themeMode = existingProfile.themeMode
                userPlan = existingProfile.userPlan
                loggedInName = existingProfile.name
                loggedInEmail = existingProfile.email
            }

            val existingClients = repository.getClientsList(uid)
            val existingProducts = repository.getProductsList(uid)
            val allClientsInDb = repository.getClientsList("")

            // ONLY seed mock data if the entire database is empty (first install of the app)
            if (allClientsInDb.isEmpty() && existingClients.isEmpty() && existingProducts.isEmpty()) {
                seedMockData(uid)
                seedProductsData(uid)
                if (existingProfile == null) {
                    repository.saveUserProfile(
                        UserProfile(
                            userId = uid,
                            name = loggedInName,
                            email = loggedInEmail,
                            languageCode = languageCode,
                            currencyCode = currencyCode,
                            primaryColorHex = primaryColorHex,
                            themeMode = themeMode,
                            userPlan = userPlan
                        )
                    )
                }
            } else if (existingProfile == null && isLoggedIn) {
                repository.saveUserProfile(
                    UserProfile(
                        userId = uid,
                        name = loggedInName,
                        email = loggedInEmail,
                        languageCode = languageCode,
                        currencyCode = currencyCode,
                        primaryColorHex = primaryColorHex,
                        themeMode = themeMode,
                        userPlan = userPlan
                    )
                )
            }

            try {
                if (FirebaseSyncHelper.isInitialized(appContext)) {
                    val auth = FirebaseSyncHelper.getFirebaseAuth(appContext)
                    auth?.currentUser?.let { user ->
                        val email = user.email ?: loggedInEmail
                        val name = user.displayName ?: user.email?.substringBefore("@") ?: loggedInName
                        val targetUid = user.uid
                        onUserAuthenticated(name, email, targetUid)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun seedMockData(userId: String = currentUserId) {
        val now = System.currentTimeMillis()
        val oneDayLog = 24 * 60 * 60 * 1000L

        // Calendar anchor for September 2026
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.SEPTEMBER)
            set(Calendar.DAY_OF_MONTH, 15)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 30)
        }
        val septBase = cal.timeInMillis

        // 1. João Silva (R$ 850,00 pendente - 32 dias em atraso)
        val c1Id = repository.insertClient(
            Client(
                name = "João Silva",
                phone = "(11) 98765-1101",
                whatsapp = "5511987651101",
                email = "joao.silva@email.com",
                address = "Rua do Comércio, 405",
                notes = "Cliente regular - prefere pagamento por PIX.",
                createdAt = now - 35 * oneDayLog,
                lastMovement = now - 32 * oneDayLog
            )
        ).toInt()

        repository.insertDebt(
            Debt(
                clientId = c1Id,
                type = "Serviço",
                description = "Manutenção Mecânica e Peças",
                value = 550.0,
                paidValue = 0.0,
                createdAt = now - 32 * oneDayLog,
                status = "Pendente"
            )
        )
        repository.insertDebt(
            Debt(
                clientId = c1Id,
                type = "Produto",
                description = "Troca de Óleo e Filtros",
                value = 300.0,
                paidValue = 0.0,
                createdAt = now - 15 * oneDayLog,
                status = "Pendente"
            )
        )

        // 2. Maria Oliveira (R$ 420,00 pendente - 24 dias em atraso)
        val c2Id = repository.insertClient(
            Client(
                name = "Maria Oliveira",
                phone = "(11) 97654-2202",
                whatsapp = "5511976542202",
                email = "maria.oliveira@email.com",
                address = "Av. Paulista, 1500 - Ap 42",
                notes = "Sempre pontual, atraso pontual.",
                createdAt = now - 30 * oneDayLog,
                lastMovement = now - 24 * oneDayLog
            )
        ).toInt()

        repository.insertDebt(
            Debt(
                clientId = c2Id,
                type = "Produto",
                description = "Kit Cosméticos & Cuidados",
                value = 420.0,
                paidValue = 0.0,
                createdAt = now - 24 * oneDayLog,
                status = "Pendente"
            )
        )

        // 3. Carlos Santos (R$ 310,00 pendente - 18 dias em atraso)
        val c3Id = repository.insertClient(
            Client(
                name = "Carlos Santos",
                phone = "(11) 96543-3303",
                whatsapp = "5511965433303",
                email = "carlos.santos@email.com",
                address = "Rua das Flores, 88",
                notes = "Instalação concluída com sucesso.",
                createdAt = now - 25 * oneDayLog,
                lastMovement = now - 18 * oneDayLog
            )
        ).toInt()

        repository.insertDebt(
            Debt(
                clientId = c3Id,
                type = "Serviço",
                description = "Instalação Elétrica & Peças",
                value = 310.0,
                paidValue = 0.0,
                createdAt = now - 18 * oneDayLog,
                status = "Pendente"
            )
        )

        // 4. Ana Paula Ferreira (R$ 195,00 pendente - 9 dias em atraso)
        val c4Id = repository.insertClient(
            Client(
                name = "Ana Paula Ferreira",
                phone = "(11) 95432-4404",
                whatsapp = "5511954324404",
                email = "ana.paula@email.com",
                address = "Rua Bela Vista, 210",
                notes = "Pagamento agendado para o fim do mês.",
                createdAt = now - 12 * oneDayLog,
                lastMovement = now - 9 * oneDayLog
            )
        ).toInt()

        repository.insertDebt(
            Debt(
                clientId = c4Id,
                type = "Produto",
                description = "Consultoria & Acessórios",
                value = 195.0,
                paidValue = 0.0,
                createdAt = now - 9 * oneDayLog,
                status = "Pendente"
            )
        )
    }

    private fun getMillisForDay(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        return cal.timeInMillis
    }

    private suspend fun seedProductsData(userId: String = currentUserId) {
        val existingProducts = repository.getProductsList(userId)
        if (existingProducts.isNotEmpty()) return

        val now = System.currentTimeMillis()
        val oneDayLog = 24 * 60 * 60 * 1000L

        val p1 = repository.insertProduct(Product(userId = userId, name = "Kit Cosméticos & Cuidados", price = 30.0, stockQuantity = 120), userId).toInt()
        val p2 = repository.insertProduct(Product(userId = userId, name = "Troca de Óleo e Filtros", price = 30.0, stockQuantity = 85), userId).toInt()
        val p3 = repository.insertProduct(Product(userId = userId, name = "Acessórios e Ferramentas", price = 25.0, stockQuantity = 60), userId).toInt()
        val p4 = repository.insertProduct(Product(userId = userId, name = "Lote de Peças Automotivas", price = 50.0, stockQuantity = 40), userId).toInt()
        // Produto 5 sem vendas para ficar apenas em "Todos os produtos"
        repository.insertProduct(Product(userId = userId, name = "Filtro de Ar de Alta Performance", price = 45.0, stockQuantity = 35), userId)
    }

    // Navigation stack control
    fun navigateTo(screen: Screen) {
        if (screen is Screen.Home || screen is Screen.Clients || screen is Screen.Calendar || screen is Screen.Products || screen is Screen.Reports || screen is Screen.Account) {
            screenStack.clear()
        }
        screenStack.add(screen)
    }

    fun navigateBack(): Boolean {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.lastIndex)
            return true
        }
        return false
    }

    // Configuration setters with database persistence
    fun updateThemeMode(mode: String) {
        themeMode = mode
        prefs.edit().putString("theme_mode", mode).apply()
        persistUserProfile()
    }

    fun updateLanguage(lang: String) {
        languageCode = lang
        prefs.edit().putString("language", lang).apply()
        persistUserProfile()
    }

    fun updateCurrency(curr: String) {
        currencyCode = curr
        prefs.edit().putString("currency", curr).apply()
        persistUserProfile()
    }

    fun updatePrimaryColor(hex: String) {
        primaryColorHex = hex
        prefs.edit().putString("primary_color_hex", hex).apply()
        persistUserProfile()
    }

    fun updateUserProfile(name: String, email: String, plan: String = userPlan) {
        loggedInName = name
        loggedInEmail = email
        userPlan = plan
        prefs.edit().apply {
            putString("logged_in_name", name)
            putString("logged_in_email", email)
            putString("user_plan", plan)
        }.apply()
        persistUserProfile()
    }

    fun persistUserProfile() {
        viewModelScope.launch {
            val uid = currentUserId
            if (uid.isNotEmpty()) {
                val profile = UserProfile(
                    userId = uid,
                    name = loggedInName,
                    email = loggedInEmail,
                    languageCode = languageCode,
                    currencyCode = currencyCode,
                    primaryColorHex = primaryColorHex,
                    themeMode = themeMode,
                    userPlan = userPlan,
                    lastLogin = System.currentTimeMillis()
                )
                repository.saveUserProfile(profile)
            }
        }
    }

    suspend fun authenticateUser(name: String, email: String, targetUid: String? = null): Boolean {
        val cleanEmail = email.trim().lowercase()

        // 1. Identify user: Check if this email already exists in user_profiles
        val existingProfileByEmail = if (cleanEmail.isNotEmpty()) repository.getUserProfileByEmail(cleanEmail) else null
        val resolvedUserId = when {
            existingProfileByEmail != null -> existingProfileByEmail.userId
            !targetUid.isNullOrBlank() -> targetUid
            cleanEmail.isNotEmpty() -> generateDeterministicUserId(cleanEmail)
            else -> currentUserId.ifEmpty { "default_user" }
        }

        // 2. Fetch existing data for this user from Room
        val existingProfile = repository.getUserProfile(resolvedUserId) ?: existingProfileByEmail

        // 3. Load user profile & preferences (NEVER overwrite existing data with defaults)
        val finalName = if (existingProfile != null && existingProfile.name.isNotEmpty()) {
            existingProfile.name
        } else if (name.isNotBlank()) {
            name
        } else {
            cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        }

        val finalEmail = if (existingProfile != null && existingProfile.email.isNotEmpty()) {
            existingProfile.email
        } else {
            cleanEmail
        }

        if (existingProfile != null) {
            languageCode = existingProfile.languageCode
            currencyCode = existingProfile.currencyCode
            primaryColorHex = existingProfile.primaryColorHex
            themeMode = existingProfile.themeMode
            userPlan = existingProfile.userPlan
        } else {
            // New user: persist initial profile
            val newProfile = UserProfile(
                userId = resolvedUserId,
                name = finalName,
                email = finalEmail,
                languageCode = languageCode,
                currencyCode = currencyCode,
                primaryColorHex = primaryColorHex,
                themeMode = themeMode,
                userPlan = userPlan
            )
            repository.saveUserProfile(newProfile)
        }

        // 4. Update active session state & trigger reactive flows for this account
        currentUserId = resolvedUserId
        currentUserIdFlow.value = resolvedUserId
        isLoggedIn = true
        loggedInName = finalName
        loggedInEmail = finalEmail

        // Persist session cache in SharedPreferences
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("current_user_id", resolvedUserId)
            putString("logged_in_name", finalName)
            putString("logged_in_email", finalEmail)
            putString("language", languageCode)
            putString("currency", currencyCode)
            putString("primary_color_hex", primaryColorHex)
            putString("theme_mode", themeMode)
            putString("user_plan", userPlan)
        }.apply()

        // 5. Cloud sync if available without overwriting local data
        try {
            if (FirebaseSyncHelper.isInitialized(appContext)) {
                val localClients = repository.getClientsList(resolvedUserId)
                if (localClients.isEmpty()) {
                    val cloudData = FirebaseSyncHelper.restoreDataFromCloud(appContext)
                    if (cloudData != null && (cloudData.clients.isNotEmpty() || cloudData.products.isNotEmpty())) {
                        repository.importStructuredData(
                            clients = cloudData.clients,
                            debts = cloudData.debts,
                            payments = cloudData.payments,
                            products = cloudData.products,
                            userId = resolvedUserId
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return true
    }

    fun onUserAuthenticated(name: String, email: String, targetUid: String? = null, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = authenticateUser(name, email, targetUid)
            onComplete?.invoke(success)
        }
    }

    fun loginWithAccount(name: String, email: String, onComplete: (Boolean) -> Unit = {}) {
        onUserAuthenticated(name, email, null) {
            onComplete(true)
        }
    }

    fun updateLoginState(loggedIn: Boolean, name: String, email: String) {
        if (loggedIn) {
            onUserAuthenticated(name, email, null)
        } else {
            firebaseLogout()
        }
    }

    // DB Mutators wrapped nicely with explicit user isolation
    fun registerClient(
        name: String,
        phone: String,
        whatsapp: String,
        email: String?,
        address: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            val formatWhatsApp = whatsapp.filter { it.isDigit() }
            val client = Client(
                userId = currentUserId,
                name = name,
                phone = phone,
                whatsapp = if (formatWhatsApp.isEmpty()) phone.filter { it.isDigit() } else formatWhatsApp,
                email = if (email.isNullOrBlank()) null else email,
                address = if (address.isNullOrBlank()) null else address,
                notes = if (notes.isNullOrBlank()) null else notes
            )
            repository.insertClient(client, currentUserId)
        }
    }

    fun addDebtToClient(
        clientId: Int,
        type: String,
        description: String,
        value: Double,
        notes: String?,
        createdAt: Long = System.currentTimeMillis(),
        dueDate: Long? = null,
        productId: Int? = null,
        productPrice: Double? = null,
        quantity: Int? = null
    ) {
        viewModelScope.launch {
            val debt = Debt(
                userId = currentUserId,
                clientId = clientId,
                type = type,
                description = description,
                value = value,
                notes = if (notes.isNullOrBlank()) null else notes,
                status = "Pendente",
                createdAt = createdAt,
                dueDate = dueDate,
                productId = productId,
                productPrice = productPrice,
                quantity = quantity
            )
            repository.insertDebt(debt, currentUserId)
        }
    }

    fun addProduct(name: String, price: Double, stockQuantity: Int? = null) {
        viewModelScope.launch {
            repository.insertProduct(Product(userId = currentUserId, name = name, price = price, stockQuantity = stockQuantity), currentUserId)
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(if (product.userId.isEmpty()) product.copy(userId = currentUserId) else product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun addPaymentToDebt(
        debtId: Int,
        value: Double,
        notes: String?,
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.registerPayment(
                debtId = debtId,
                value = value,
                notes = if (notes.isNullOrBlank()) null else notes,
                dateMillis = dateMillis,
                userId = currentUserId
            )
        }
    }

    fun registerGenericClientPayment(
        clientId: Int,
        amount: Double,
        notes: String?,
        dateMillis: Long = System.currentTimeMillis()
    ) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            var remaining = amount
            val pendingDebts = repository.getDebtsByClient(clientId)
                .filter { it.status != "Pago" }
                .sortedBy { it.createdAt }

            for (debt in pendingDebts) {
                if (remaining <= 0.0) break
                val remainingDebt = debt.value - debt.paidValue
                if (remainingDebt > 0.0) {
                    val toPay = minOf(remaining, remainingDebt)
                    repository.registerPayment(
                        debtId = debt.id,
                        value = toPay,
                        notes = if (notes.isNullOrBlank()) "Pagamento recebido" else notes,
                        dateMillis = dateMillis,
                        userId = currentUserId
                    )
                    remaining -= toPay
                }
            }
        }
    }

    fun removeClient(client: Client) {
        viewModelScope.launch {
            repository.deleteClient(client)
        }
    }

    fun removeDebt(debt: Debt) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
        }
    }

    // Helper functions for formatting Currency, Date, Reminders
    fun formatCurrency(value: Double): String {
        return String.format(Locale("pt", "BR"), "R$ %,.2f", value)
    }

    fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatDateWithTime(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun getDayOfMonth(millis: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        return cal.get(Calendar.DAY_OF_MONTH)
    }

    // Backup & Restore
    fun triggerBackupLocal(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val json = repository.exportDataToJson()
                prefs.edit().putString("cloud_backup_json", json).apply()
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun triggerRestoreLocal(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val backup = prefs.getString("cloud_backup_json", "") ?: ""
            if (backup.isNotEmpty()) {
                val success = repository.importDataFromJson(backup)
                onComplete(success)
            } else {
                onComplete(false)
            }
        }
    }

    fun triggerBackupCloud(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val uid = currentUserId
                val clients = repository.getClientsList(uid)
                val debts = repository.getDebtsList(uid)
                val payments = repository.getPaymentsList(uid)
                val products = repository.getProductsList(uid)
                val success = FirebaseSyncHelper.backupDataToCloud(appContext, clients, debts, payments, products)
                onComplete(success)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun triggerRestoreCloud(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val data = FirebaseSyncHelper.restoreDataFromCloud(appContext)
                if (data != null) {
                    val success = repository.importStructuredData(
                        clients = data.clients,
                        debts = data.debts,
                        payments = data.payments,
                        products = data.products,
                        userId = currentUserId
                    )
                    onComplete(success)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun firebaseRegister(email: String, pass: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val (success, errorMsg) = FirebaseSyncHelper.registerUser(appContext, email, pass)
            if (success) {
                val authEmail = FirebaseSyncHelper.getCurrentUserEmail(appContext) ?: email
                val targetUid = FirebaseSyncHelper.getCurrentUserUid(appContext)
                onUserAuthenticated(authEmail.substringBefore("@"), authEmail, targetUid) {
                    onComplete(true, null)
                }
            } else {
                onComplete(false, errorMsg)
            }
        }
    }

    fun firebaseLogin(email: String, pass: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val (success, errorMsg) = FirebaseSyncHelper.signInUser(appContext, email, pass)
            if (success) {
                val authEmail = FirebaseSyncHelper.getCurrentUserEmail(appContext) ?: email
                val targetUid = FirebaseSyncHelper.getCurrentUserUid(appContext)
                onUserAuthenticated(authEmail.substringBefore("@"), authEmail, targetUid) {
                    onComplete(true, null)
                }
            } else {
                onComplete(false, errorMsg)
            }
        }
    }

    fun loginWithGoogle(idToken: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val (success, errorMsg) = FirebaseSyncHelper.signInWithGoogle(appContext, idToken)
            if (success) {
                val authEmail = FirebaseSyncHelper.getCurrentUserEmail(appContext) ?: "gmail@google.com"
                val name = FirebaseSyncHelper.getFirebaseAuth(appContext)?.currentUser?.displayName 
                    ?: authEmail.substringBefore("@")
                val targetUid = FirebaseSyncHelper.getCurrentUserUid(appContext)
                onUserAuthenticated(name, authEmail, targetUid) {
                    onComplete(true, null)
                }
            } else {
                onComplete(false, errorMsg)
            }
        }
    }

    fun getGoogleWebClientId(): String {
        return FirebaseSyncHelper.getGoogleWebClientId(appContext)
    }

    fun firebaseLogout() {
        try {
            FirebaseSyncHelper.signOutUser(appContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isLoggedIn = false
        prefs.edit().putBoolean("is_logged_in", false).apply()
    }

    fun getFirebaseConfig(): Triple<String, String, String> {
        val customKey = prefs.getString("firebase_api_key", "") ?: ""
        val customProj = prefs.getString("firebase_project_id", "") ?: ""
        val customApp = prefs.getString("firebase_app_id", "") ?: ""
        
        if (customKey.isNotEmpty() && customProj.isNotEmpty() && customApp.isNotEmpty()) {
            return Triple(customKey, customProj, customApp)
        }
        
        val active = FirebaseSyncHelper.getActiveConfig(appContext)
        return Triple(
            customKey.ifEmpty { active.first },
            customProj.ifEmpty { active.second },
            customApp.ifEmpty { active.third }
        )
    }

    fun saveFirebaseConfig(apiKey: String, projectId: String, appId: String) {
        FirebaseSyncHelper.saveCustomCredentials(appContext, apiKey, projectId, appId)
    }

    fun isFirebaseReady(): Boolean = FirebaseSyncHelper.isInitialized(appContext)

    // Localization strings map for Portuguese, English, and Spanish (dynamic translate)
    private val locales = mapOf(
        "pt" to mapOf(
            "app_title" to "Controle financeiro",
            "tab_home" to "Início",
            "tab_clients" to "Clientes",
            "tab_products" to "Produtos",
            "tab_account" to "Conta",
            "no_clients" to "Nenhum cliente cadastrado",
            "no_debts" to "Nenhum débito cadastrado",
            "due" to "Devido",
            "paid" to "Pago",
            "balance" to "Saldo",
            "pending" to "Pendente",
            "partial" to "Parcial",
            "paid_status" to "Pago",
            "overdue_30" to "Clientes devendo há mais de 1 mês",
            "overdue_14" to "Clientes devendo há mais de 2 semanas",
            "no_overdue" to "Tudo em dia! Sem pendências longas.",
            "create_client" to "Novo Cliente",
            "search_hint" to "Pesquisar por nome, tel, produto...",
            "phone" to "Telefone",
            "whatsapp" to "WhatsApp",
            "email" to "E-mail (opcional)",
            "address" to "Endereço (opcional)",
            "notes" to "Observações",
            "save" to "Salvar Cliente",
            "add_debt" to "Adicionar Débito",
            "register_payment" to "Registrar Pagamento",
            "last_mov" to "Última movimentação",
            "description" to "Descrição",
            "value" to "Valor",
            "type" to "Tipo",
            "product" to "Produto",
            "service" to "Serviço",
            "view_ficha" to "Ver Ficha",
            "statistics" to "Estatísticas",
            "total_rec_month" to "Recebido no mês",
            "total_pend_amt" to "Total pendente",
            "client_qty" to "Quantidade de clientes",
            "debt_qty" to "Quantidade de débitos",
            "default_logged_in" to "Usuário Iniciado",
            "appearance" to "Aparência",
            "theme_light" to "Claro",
            "theme_dark" to "Escuro",
            "theme_auto" to "Automático",
            "lang" to "Idioma",
            "currency" to "Moeda",
            "backup_sync" to "Backup e Sincronização",
            "cloud_sync" to "Nuvem integrada",
            "cloud_sync_desc" to "Faça e restaure backups instantaneamente",
            "do_backup" to "Fazer Backup na Nuvem",
            "do_restore" to "Restaurar Dados",
            "share_on_whatsapp" to "Enviar Cobrança",
            "debt_time" to "há %d dias",
            "pago_on" to "Pago em",
            "partial_pay" to "Pago parcial de: %s",
            "rem_payment" to "Excluir Débito",
            "rem_client" to "Excluir Ficha",
            "add_payment_for" to "Registrar recebimento",
            "payment_val" to "Valor recebido",
            "payment_obs" to "Observação (ex: Dinheiro, PIX, etc.)",
            "pay_full" to "Quitar totalmente",
            "login_header" to "Acesse sua conta",
            "login_desc" to "Seus dados sincronizados entre dispositivos",
            "login_google" to "Entrar com Google",
            "login_email" to "Conectar com E-mail e Senha",
            "logout" to "Desconectar Conta",
            "notification_panel" to "Alertas de Cobrança Automáticos",
            "smart_notif_7" to "Cobrança amigável (7 dias de atraso)",
            "smart_notif_14" to "Lembrete formal (14 dias de atraso)",
            "smart_notif_30" to "Aviso de débito estruturado (30 dias)",
            "notif_sent" to "Notificação enviada!",
            "report_inad" to "Relatórios de Inadimplência",
            "create_bill" to "Criar cobrança"
        ),
        "en" to mapOf(
            "app_title" to "Fiado Ledger",
            "tab_home" to "Home",
            "tab_clients" to "Clients",
            "tab_products" to "Products",
            "tab_account" to "Account",
            "no_clients" to "No clients registered yet",
            "no_debts" to "No debts registered",
            "due" to "Due",
            "paid" to "Paid",
            "balance" to "Balance",
            "pending" to "Pending",
            "partial" to "Partial",
            "paid_status" to "Paid",
            "overdue_30" to "Clients owing for over 1 month",
            "overdue_14" to "Clients owing for over 2 weeks",
            "no_overdue" to "All good! No long overdue debts.",
            "create_client" to "New Client",
            "search_hint" to "Search by name, phone, item...",
            "phone" to "Phone",
            "whatsapp" to "WhatsApp",
            "email" to "Email (optional)",
            "address" to "Address (optional)",
            "notes" to "Notes",
            "save" to "Save Client",
            "add_debt" to "Add Debt",
            "register_payment" to "Register Payment",
            "last_mov" to "Last activity",
            "description" to "Description",
            "value" to "Value",
            "type" to "Type",
            "product" to "Product",
            "service" to "Service",
            "view_ficha" to "View Details",
            "statistics" to "Statistics",
            "total_rec_month" to "Received this month",
            "total_pend_amt" to "Total pending",
            "client_qty" to "Total clients",
            "debt_qty" to "Total debts",
            "default_logged_in" to "Logged User",
            "appearance" to "Appearance",
            "theme_light" to "Light",
            "theme_dark" to "Dark",
            "theme_auto" to "Automatic",
            "lang" to "Language",
            "currency" to "Currency",
            "backup_sync" to "Backup & Sync",
            "cloud_sync" to "Integrated Cloud",
            "cloud_sync_desc" to "Perform and restore backups instantly",
            "do_backup" to "Cloud Backup Now",
            "do_restore" to "Restore Data",
            "share_on_whatsapp" to "Send Friendly Reminder",
            "debt_time" to "%d days ago",
            "pago_on" to "Paid on",
            "partial_pay" to "Partial paid: %s",
            "rem_payment" to "Delete Debt",
            "rem_client" to "Delete Client Profile",
            "add_payment_for" to "Record receipt",
            "payment_val" to "Amount received",
            "payment_obs" to "Method (e.g. Cash, Card, Bank transfer)",
            "pay_full" to "Settle in full",
            "login_header" to "Sign In",
            "login_desc" to "Synchronize your data across devices",
            "login_google" to "Sign in with Google",
            "login_email" to "Sign in with Email",
            "logout" to "Sign Out",
            "notification_panel" to "Automated Billing Reminders",
            "smart_notif_7" to "Friendly check-in (7 days delay)",
            "smart_notif_14" to "Official follow-up (14 days delay)",
            "smart_notif_30" to "Structured payment request (30 days)",
            "notif_sent" to "Notification sent!",
            "report_inad" to "Delinquency Reports",
            "create_bill" to "Create bill"
        ),
        "es" to mapOf(
            "app_title" to "Control de Deudas",
            "tab_home" to "Inicio",
            "tab_clients" to "Clientes",
            "tab_products" to "Productos",
            "tab_account" to "Cuenta",
            "no_clients" to "Ningún cliente registrado",
            "no_debts" to "Ninguna deuda registrada",
            "due" to "Debe",
            "paid" to "Pagado",
            "balance" to "Saldo",
            "pending" to "Pendiente",
            "partial" to "Parcial",
            "paid_status" to "Pagado",
            "overdue_30" to "Clientes debiendo más de 1 mes",
            "overdue_14" to "Clientes debiendo más de 2 semanas",
            "no_overdue" to "¡Todo al día! Sin deudas largas.",
            "create_client" to "Nuevo Cliente",
            "search_hint" to "Buscar por nombre, tel, artículo...",
            "phone" to "Teléfono",
            "whatsapp" to "WhatsApp",
            "email" to "E-mail (opcional)",
            "address" to "Dirección (opcional)",
            "notes" to "Observaciones",
            "save" to "Guardar Cliente",
            "add_debt" to "Agregar Deuda",
            "register_payment" to "Registrar Pago",
            "last_mov" to "Última actividad",
            "description" to "Descripción",
            "value" to "Valor",
            "type" to "Tipo",
            "product" to "Producto",
            "service" to "Servicio",
            "view_ficha" to "Ver Ficha",
            "statistics" to "Estadísticas",
            "total_rec_month" to "Recibido este mes",
            "total_pend_amt" to "Total pendiente",
            "client_qty" to "Cantidad de clientes",
            "debt_qty" to "Cantidad de deudas",
            "default_logged_in" to "Usuario Iniciado",
            "appearance" to "Apariencia",
            "theme_light" to "Claro",
            "theme_dark" to "Oscuro",
            "theme_auto" to "Automático",
            "lang" to "Idioma",
            "currency" to "Moneda",
            "backup_sync" to "Copia de Seguridad",
            "cloud_sync" to "Nube integrada",
            "cloud_sync_desc" to "Haga y restaure copias al instante",
            "do_backup" to "Hacer Copia de Seguridad",
            "do_restore" to "Restaurar de la Nube",
            "share_on_whatsapp" to "Enviar Mensaje de Cobro",
            "debt_time" to "hace %d días",
            "pago_on" to "Pagado el",
            "partial_pay" to "Pago parcial de: %s",
            "rem_payment" to "Eliminar Deuda",
            "rem_client" to "Eliminar Ficha",
            "add_payment_for" to "Registrar cobro",
            "payment_val" to "Monto recibido",
            "payment_obs" to "Forma de pago (ej: Efectivo, Transferencia)",
            "pay_full" to "Liquidar por completo",
            "login_header" to "Inicie sesión",
            "login_desc" to "Sincronice sus datos entre dispositivos",
            "login_google" to "Entrar con Google",
            "login_email" to "Entrar con Correo",
            "logout" to "Desconectar Cuenta",
            "notification_panel" to "Alertas de Cobro Automáticas",
            "smart_notif_7" to "Cobro amigable (7 días de retraso)",
            "smart_notif_14" to "Recordatorio formal (14 días)",
            "smart_notif_30" to "Aviso de deuda estructurado (30 días)",
            "notif_sent" to "¡Notificación enviada!",
            "report_inad" to "Informes de Morosidad",
            "create_bill" to "Crear cobro"
        )
    )

    fun t(key: String): String {
        val strings = locales[languageCode] ?: locales["pt"]!!
        return strings[key] ?: key
    }
}
