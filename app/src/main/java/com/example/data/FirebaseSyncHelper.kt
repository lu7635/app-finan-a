package com.example.data
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.InputStream

data class CloudBackupData(
    val clients: List<Client>,
    val debts: List<Debt>,
    val payments: List<Payment>,
    val products: List<Product>
)

object FirebaseSyncHelper {
    private const val TAG = "FirebaseSyncHelper"

    private const val DEFAULT_API_KEY = "AIzaSyBr04WEd7TCloPW-j0cGEy5FQl6IQRdtyY"
    private const val DEFAULT_PROJECT_ID = "controle-financeiro-68025"
    private const val DEFAULT_APP_ID = "1:81173822027:android:442db94bb03876d7b66bce"
    private const val DEFAULT_WEB_CLIENT_ID = "81173822027-ndbirp6c3rdoqe9vrgorhrcg25iqoblk.apps.googleusercontent.com"

    private fun loadFirebaseConfigFromAssets(context: Context): Triple<String, String, String>? {
        return try {
            val inputStream: InputStream = context.assets.open("google-services.json")
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val jsonString = String(buffer, Charsets.UTF_8)
            val json = JSONObject(jsonString)
            
            val projectInfo = json.getJSONObject("project_info")
            val projectId = projectInfo.getString("project_id")
            
            val clientArray = json.getJSONArray("client")
            if (clientArray.length() > 0) {
                val clientObj = clientArray.getJSONObject(0)
                val clientInfo = clientObj.getJSONObject("client_info")
                val appId = clientInfo.getString("mobilesdk_app_id")
                
                val apiKeyArray = clientObj.getJSONArray("api_key")
                if (apiKeyArray.length() > 0) {
                    val apiKeyObj = apiKeyArray.getJSONObject(0)
                    val apiKey = apiKeyObj.getString("current_key")
                    return Triple(apiKey, projectId, appId)
                }
            }
            null
        } catch (e: Exception) {
            Log.d(TAG, "google-services.json not found in assets, using DEFAULT constants.")
            null
        }
    }

    fun getActiveConfig(context: Context): Triple<String, String, String> {
        val assetConfig = loadFirebaseConfigFromAssets(context)
        return Triple(
            assetConfig?.first ?: DEFAULT_API_KEY,
            assetConfig?.second ?: DEFAULT_PROJECT_ID,
            assetConfig?.third ?: DEFAULT_APP_ID
        )
    }

    fun isInitialized(context: Context): Boolean {
        return try {
            FirebaseApp.getInstance()
            true
        } catch (e: Exception) {
            try {
                val prefs = context.getSharedPreferences("fiado_prefs", Context.MODE_PRIVATE)
                val activeConfig = getActiveConfig(context)

                val apiKey = prefs.getString("firebase_api_key", "")?.trim()?.takeIf { it.isNotEmpty() } ?: activeConfig.first
                val projectId = prefs.getString("firebase_project_id", "")?.trim()?.takeIf { it.isNotEmpty() } ?: activeConfig.second
                val appId = prefs.getString("firebase_app_id", "")?.trim()?.takeIf { it.isNotEmpty() } ?: activeConfig.third

                val options = FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setProjectId(projectId)
                    .setApplicationId(appId)
                    .build()
                FirebaseApp.initializeApp(context, options)
                true
            } catch (ex: Exception) {
                Log.e(TAG, "Failed standard/custom Firebase init", ex)
                false
            }
        }
    }

    fun getFirebaseAuth(context: Context): FirebaseAuth? {
        return if (isInitialized(context)) {
            try {
                FirebaseAuth.getInstance()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun getFirestore(context: Context): FirebaseFirestore? {
        return if (isInitialized(context)) {
            try {
                FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    // Modern Coroutine-based User Registration
    suspend fun registerUser(context: Context, email: String, pass: String): Pair<Boolean, String?> {
        val auth = getFirebaseAuth(context) ?: return Pair(false, "Firebase não inicializado.")
        return try {
            auth.createUserWithEmailAndPassword(email, pass).await()
            Pair(true, null)
        } catch (e: Exception) {
            Log.e(TAG, "Reg error", e)
            val msg = when {
                e.message?.contains("WEAK_PASSWORD", ignoreCase = true) == true || e.message?.contains("password", ignoreCase = true) == true -> "A senha deve conter no mínimo 6 caracteres."
                e.message?.contains("EMAIL_ALREADY_IN_USE", ignoreCase = true) == true || e.message?.contains("already in use", ignoreCase = true) == true -> "Este endereço de e-mail já está em uso."
                e.message?.contains("INVALID_EMAIL", ignoreCase = true) == true -> "Endereço de e-mail inválido."
                else -> e.localizedMessage ?: "Erro ao registrar conta no Firebase."
            }
            Pair(false, msg)
        }
    }

    // Modern Coroutine-based User Sign In
    suspend fun signInUser(context: Context, email: String, pass: String): Pair<Boolean, String?> {
        val auth = getFirebaseAuth(context) ?: return Pair(false, "Firebase não inicializado.")
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Pair(true, null)
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            val msg = when {
                e.message?.contains("user-not-found", ignoreCase = true) == true || e.message?.contains("no user record", ignoreCase = true) == true -> "Nenhuma conta de usuário localizada com este e-mail."
                e.message?.contains("wrong-password", ignoreCase = true) == true || e.message?.contains("invalid-credential", ignoreCase = true) == true -> "E-mail ou senha inválidos."
                else -> e.localizedMessage ?: "Falha ao autenticar no Firebase."
            }
            Pair(false, msg)
        }
    }

    fun getGoogleWebClientId(context: Context): String {
        return try {
            val inputStream: InputStream = context.assets.open("google-services.json")
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val jsonString = String(buffer, Charsets.UTF_8)
            val json = JSONObject(jsonString)
            val clientArray = json.getJSONArray("client")
            if (clientArray.length() > 0) {
                val clientObj = clientArray.getJSONObject(0)
                val oauthArray = clientObj.optJSONArray("oauth_client")
                if (oauthArray != null) {
                    for (i in 0 until oauthArray.length()) {
                        val o = oauthArray.getJSONObject(i)
                        if (o.optInt("client_type") == 3) {
                            val id = o.optString("client_id")
                            if (id.isNotEmpty()) return id
                        }
                    }
                }
            }
            DEFAULT_WEB_CLIENT_ID
        } catch (e: Exception) {
            DEFAULT_WEB_CLIENT_ID
        }
    }

    suspend fun signInWithGoogle(context: Context, idToken: String): Pair<Boolean, String?> {
        val auth = getFirebaseAuth(context) ?: return Pair(false, "Firebase não inicializado.")
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            Pair(true, null)
        } catch (e: Exception) {
            Log.e(TAG, "Google auth error", e)
            Pair(false, e.localizedMessage ?: "Falha ao autenticar com a conta Google.")
        }
    }

    // Sign out
    fun signOutUser(context: Context) {
        getFirebaseAuth(context)?.signOut()
    }

    // Get current email
    fun getCurrentUserEmail(context: Context): String? {
        return getFirebaseAuth(context)?.currentUser?.email
    }

    // Get current UID
    fun getCurrentUserUid(context: Context): String? {
        return getFirebaseAuth(context)?.currentUser?.uid
    }

    // Set custom credential params in Settings
    fun saveCustomCredentials(context: Context, apiKey: String, projectId: String, appId: String) {
        val prefs = context.getSharedPreferences("fiado_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("firebase_api_key", apiKey.trim())
            .putString("firebase_project_id", projectId.trim())
            .putString("firebase_app_id", appId.trim())
            .apply()
        // Reset Apps so we re-initialize on next isInitialized() check
        try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isNotEmpty()) {
                // We cannot easily delete/unregister, but we can restart or re-init if needed
            }
        } catch (e: Exception) {
            Log.e(TAG, "App reset error", e)
        }
    }

    // Backup Room Entities directly to Cloud Firestore as documents
    suspend fun backupDataToCloud(
        context: Context,
        clients: List<Client>,
        debts: List<Debt>,
        payments: List<Payment>,
        products: List<Product>
    ): Boolean {
        val firestore = getFirestore(context) ?: return false
        val uid = getCurrentUserUid(context) ?: "anonymous"

        return try {
            val batch = firestore.batch()

            // 1. Clients
            clients.forEach { client ->
                val docRef = firestore.collection("users").document(uid)
                    .collection("clients").document(client.id.toString())
                val map = hashMapOf(
                    "id" to client.id,
                    "name" to client.name,
                    "phone" to client.phone,
                    "whatsapp" to client.whatsapp,
                    "email" to client.email,
                    "address" to client.address,
                    "notes" to client.notes,
                    "createdAt" to client.createdAt,
                    "lastMovement" to client.lastMovement
                )
                batch.set(docRef, map)
            }

            // 2. Debts
            debts.forEach { debt ->
                val docRef = firestore.collection("users").document(uid)
                    .collection("debts").document(debt.id.toString())
                val map = hashMapOf(
                    "id" to debt.id,
                    "clientId" to debt.clientId,
                    "type" to debt.type,
                    "description" to debt.description,
                    "value" to debt.value,
                    "paidValue" to debt.paidValue,
                    "createdAt" to debt.createdAt,
                    "notes" to debt.notes,
                    "status" to debt.status
                )
                batch.set(docRef, map)
            }

            // 3. Payments
            payments.forEach { payment ->
                val docRef = firestore.collection("users").document(uid)
                    .collection("payments").document(payment.id.toString())
                val map = hashMapOf(
                    "id" to payment.id,
                    "debtId" to payment.debtId,
                    "clientId" to payment.clientId,
                    "value" to payment.value,
                    "createdAt" to payment.createdAt,
                    "notes" to payment.notes
                )
                batch.set(docRef, map)
            }

            // 4. Products
            products.forEach { product ->
                val docRef = firestore.collection("users").document(uid)
                    .collection("products").document(product.id.toString())
                val map = hashMapOf(
                    "id" to product.id,
                    "name" to product.name,
                    "price" to product.price,
                    "createdAt" to product.createdAt,
                    "stockQuantity" to product.stockQuantity
                )
                batch.set(docRef, map)
            }

            batch.commit().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Backup cloud error", e)
            false
        }
    }

    // Restore from Cloud Firestore
    suspend fun restoreDataFromCloud(context: Context): CloudBackupData? {
        val firestore = getFirestore(context) ?: return null
        val uid = getCurrentUserUid(context) ?: return null

        return try {
            // Retrieve Clients
            val clientsSnap = firestore.collection("users").document(uid).collection("clients").get().await()
            val clients = clientsSnap.map { doc ->
                Client(
                    id = (doc.getLong("id") ?: 0L).toInt(),
                    name = doc.getString("name") ?: "",
                    phone = doc.getString("phone") ?: "",
                    whatsapp = doc.getString("whatsapp") ?: "",
                    email = doc.getString("email"),
                    address = doc.getString("address"),
                    notes = doc.getString("notes"),
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    lastMovement = doc.getLong("lastMovement") ?: System.currentTimeMillis()
                )
            }

            // Retrieve Debts
            val debtsSnap = firestore.collection("users").document(uid).collection("debts").get().await()
            val debts = debtsSnap.map { doc ->
                Debt(
                    id = (doc.getLong("id") ?: 0L).toInt(),
                    clientId = (doc.getLong("clientId") ?: 0L).toInt(),
                    type = doc.getString("type") ?: "Produto",
                    description = doc.getString("description") ?: "",
                    value = doc.getDouble("value") ?: 0.0,
                    paidValue = doc.getDouble("paidValue") ?: 0.0,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    notes = doc.getString("notes"),
                    status = doc.getString("status") ?: "Pendente"
                )
            }

            // Retrieve Payments
            val paymentsSnap = firestore.collection("users").document(uid).collection("payments").get().await()
            val payments = paymentsSnap.map { doc ->
                Payment(
                    id = (doc.getLong("id") ?: 0L).toInt(),
                    debtId = (doc.getLong("debtId") ?: 0L).toInt(),
                    clientId = (doc.getLong("clientId") ?: 0L).toInt(),
                    value = doc.getDouble("value") ?: 0.0,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    notes = doc.getString("notes")
                )
            }

            // Retrieve Products
            val productsSnap = firestore.collection("users").document(uid).collection("products").get().await()
            val products = productsSnap.map { doc ->
                Product(
                    id = (doc.getLong("id") ?: 0L).toInt(),
                    name = doc.getString("name") ?: "",
                    price = doc.getDouble("price") ?: 0.0,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    stockQuantity = doc.getLong("stockQuantity")?.toInt()
                )
            }

            CloudBackupData(clients, debts, payments, products)
        } catch (e: Exception) {
            Log.e(TAG, "Restore cloud error", e)
            null
        }
    }
}
