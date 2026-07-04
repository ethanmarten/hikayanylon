package com.example.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.ui.Product

enum class FirebaseMode {
    REAL,
    SANDBOX
}

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "Regular User" // "Admin" or "Regular User"
)

data class LivePrices(
    val westBankNylon: Double = 15.0,
    val westBankNylonBulk: Double = 14.0,
    val egyptianNylon: Double = 17.0,
    val egyptianNylonBulk: Double = 16.0,
    val plasticCups: Double = 7.0
)

object FirebaseService {
    private const val TAG = "FirebaseService"
    
    private var _mode = FirebaseMode.SANDBOX
    val mode: FirebaseMode get() = _mode

    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    // Helper to normalize user inputs (allows typing just 'Ethan' or 'Khaled')
    fun normalizeEmail(email: String): String {
        val trimmed = email.trim().lowercase()
        return if (!trimmed.contains("@")) {
            "$trimmed@hikaya.com"
        } else {
            trimmed
        }
    }

    // Fallback sandbox memory state with the customized Admin profiles
    private val sandboxUsers = mutableMapOf<String, UserProfile>(
        "ethan@hikaya.com" to UserProfile("sb_ethan", "ethan@hikaya.com", "Ethan", "Admin"),
        "khaled@hikaya.com" to UserProfile("sb_khaled", "khaled@hikaya.com", "Khaled", "Admin"),
        "user@hikaya.com" to UserProfile("sb_user", "user@hikaya.com", "أبو محمد الزبون", "Regular User")
    )
    private val sandboxPasswords = mutableMapOf<String, String>(
        "ethan@hikaya.com" to "EM2006",
        "khaled@hikaya.com" to "Mm123123",
        "user@hikaya.com" to "user123"
    )
    
    private var currentSandboxUser: UserProfile? = null
    private val sandboxPricesFlow = MutableStateFlow(LivePrices())

    fun initialize(context: Context) {
        try {
            // Attempt real Firebase initialization
            val app = FirebaseApp.getInstance()
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            _mode = FirebaseMode.REAL
            Log.i(TAG, "Successfully initialized REAL Firebase mode.")
        } catch (e: Exception) {
            _mode = FirebaseMode.SANDBOX
            Log.w(TAG, "Firebase SDK not initialized (missing google-services.json?). Falling back to SANDBOX mode. Error: ${e.message}")
        }
    }

    fun registerUser(
        email: String,
        password: String,
        name: String,
        role: String,
        onComplete: (Result<UserProfile>) -> Unit
    ) {
        val cleanEmail = normalizeEmail(email)
        val finalRole = if (
            (cleanEmail == "ethan@hikaya.com" && password == "EM2006") ||
            (cleanEmail == "khaled@hikaya.com" && password == "Mm123123")
        ) {
            "Admin"
        } else {
            "Regular User"
        }
        val finalName = if (finalRole == "Admin") {
            if (cleanEmail == "ethan@hikaya.com") "Ethan" else "Khaled"
        } else {
            name
        }

        if (_mode == FirebaseMode.REAL && auth != null && firestore != null) {
            auth!!.createUserWithEmailAndPassword(cleanEmail, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = task.result?.user?.uid ?: ""
                        val profile = UserProfile(uid, cleanEmail, finalName, finalRole)
                        firestore!!.collection("users").document(uid).set(profile)
                            .addOnSuccessListener {
                                onComplete(Result.success(profile))
                            }
                            .addOnFailureListener { e ->
                                // Fallback if writing profile fails but auth succeeded
                                onComplete(Result.success(profile))
                            }
                    } else {
                        onComplete(Result.failure(task.exception ?: Exception("فشل إنشاء الحساب")))
                    }
                }
        } else {
            // Sandbox mode
            if (sandboxUsers.containsKey(cleanEmail)) {
                onComplete(Result.failure(Exception("هذا البريد الإلكتروني مسجل بالفعل")))
                return
            }
            val uid = "sb_" + System.currentTimeMillis()
            val profile = UserProfile(uid, cleanEmail, finalName, finalRole)
            sandboxUsers[cleanEmail] = profile
            sandboxPasswords[cleanEmail] = password
            currentSandboxUser = profile
            onComplete(Result.success(profile))
        }
    }

    fun loginUser(
        email: String,
        password: String,
        onComplete: (Result<UserProfile>) -> Unit
    ) {
        val cleanEmail = normalizeEmail(email)
        val isAdminAccount = (cleanEmail == "ethan@hikaya.com" && password == "EM2006") ||
                             (cleanEmail == "khaled@hikaya.com" && password == "Mm123123")

        if (_mode == FirebaseMode.REAL && auth != null && firestore != null) {
            auth!!.signInWithEmailAndPassword(cleanEmail, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = task.result?.user?.uid ?: ""
                        firestore!!.collection("users").document(uid).get()
                            .addOnSuccessListener { doc ->
                                val profile = doc.toObject(UserProfile::class.java)
                                    ?: UserProfile(uid, cleanEmail, if (isAdminAccount) cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() } else cleanEmail.substringBefore("@"), if (isAdminAccount) "Admin" else "Regular User")
                                onComplete(Result.success(profile))
                            }
                            .addOnFailureListener { e ->
                                // If database fetch fails, login is still valid, return a default profile
                                onComplete(Result.success(UserProfile(uid, cleanEmail, if (isAdminAccount) cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() } else cleanEmail.substringBefore("@"), if (isAdminAccount) "Admin" else "Regular User")))
                            }
                    } else {
                        // If account does not exist but has the valid admin credentials, automatically register it for ease of use
                        if (isAdminAccount) {
                            val name = if (cleanEmail == "ethan@hikaya.com") "Ethan" else "Khaled"
                            registerUser(cleanEmail, password, name, "Admin") { regResult ->
                                onComplete(regResult)
                            }
                        } else {
                            onComplete(Result.failure(task.exception ?: Exception("اسم المستخدم أو كلمة المرور غير صحيحة")))
                        }
                    }
                }
        } else {
            // Sandbox mode
            val user = sandboxUsers[cleanEmail]
            val pass = sandboxPasswords[cleanEmail]
            if (user != null && pass == password) {
                val finalUser = if (isAdminAccount) user.copy(role = "Admin") else user
                currentSandboxUser = finalUser
                onComplete(Result.success(finalUser))
            } else {
                if (isAdminAccount) {
                    val name = if (cleanEmail == "ethan@hikaya.com") "Ethan" else "Khaled"
                    registerUser(cleanEmail, password, name, "Admin") { regResult ->
                        onComplete(regResult)
                    }
                } else {
                    onComplete(Result.failure(Exception("اسم المستخدم أو كلمة المرور غير صحيحة")))
                }
            }
        }
    }

    fun logoutUser() {
        if (_mode == FirebaseMode.REAL && auth != null) {
            auth!!.signOut()
        } else {
            currentSandboxUser = null
        }
    }

    fun getCurrentUser(onComplete: (UserProfile?) -> Unit) {
        if (_mode == FirebaseMode.REAL && auth != null && firestore != null) {
            val firebaseUser = auth!!.currentUser
            if (firebaseUser != null) {
                firestore!!.collection("users").document(firebaseUser.uid).get()
                    .addOnSuccessListener { doc ->
                        val profile = doc.toObject(UserProfile::class.java)
                            ?: UserProfile(firebaseUser.uid, firebaseUser.email ?: "", "", "Regular User")
                        onComplete(profile)
                    }
                    .addOnFailureListener {
                        onComplete(UserProfile(firebaseUser.uid, firebaseUser.email ?: "", "", "Regular User"))
                    }
            } else {
                onComplete(null)
            }
        } else {
            onComplete(currentSandboxUser)
        }
    }

    private var sandboxPricesListener: ((LivePrices) -> Unit)? = null
    private var sandboxPrices = LivePrices()

    fun observePrices(onPricesChanged: (LivePrices) -> Unit) {
        if (_mode == FirebaseMode.REAL && firestore != null) {
            firestore!!.collection("prices").document("config")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen failed for prices", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val prices = snapshot.toObject(LivePrices::class.java)
                        if (prices != null) {
                            onPricesChanged(prices)
                        }
                    } else {
                        // Set default prices in firestore if empty
                        val defaultPrices = LivePrices()
                        firestore!!.collection("prices").document("config").set(defaultPrices)
                        onPricesChanged(defaultPrices)
                    }
                }
        } else {
            // Sandbox mode callback binding
            sandboxPricesListener = onPricesChanged
            onPricesChanged(sandboxPrices)
        }
    }

    fun updatePrices(prices: LivePrices, onComplete: (Result<Unit>) -> Unit) {
        if (_mode == FirebaseMode.REAL && firestore != null) {
            firestore!!.collection("prices").document("config").set(prices)
                .addOnSuccessListener {
                    onComplete(Result.success(Unit))
                }
                .addOnFailureListener { e ->
                    onComplete(Result.failure(e))
                }
        } else {
            // Sandbox mode
            sandboxPrices = prices
            sandboxPricesListener?.invoke(prices)
            onComplete(Result.success(Unit))
        }
    }

    private var sandboxProductsListener: ((List<Product>) -> Unit)? = null
    private val defaultProducts = listOf(
        Product("black_nylon", "أكياس النايلون الأسود", "أكياس متينة وفاخرة مخصصة للاستخدامات التجارية والصناعية الشاقة.", "inventory", "كيلو", "ابتداءً من 15 شيكل/كيلو", hasSubtypes = true),
        Product("colored_nylon", "الوسط الملون", "نايلون ملون وسط بمواصفات ممتازة ومقاومة فائقة وعزل ممتاز.", "shopping_bag", "كيلو", "ابتداءً من 15 شيكل/كيلو", hasSubtypes = true),
        Product("large_box_nylon", "نايلون البكسة الكبير الأزرق والأسود", "أغطية نايلون البكسة الكبير، حماية ممتازة ومقاومة عالية للتمزق والظروف الجوية.", "layers", "كيلو", "ابتداءً من 15 شيكل/كيلو", hasSubtypes = true),
        Product("plastic_cups", "كاسات بلاستيك وسط", "أكواب بلاستيكية متوسطة فاخرة، مثالية للمشروبات الباردة والساخنة.", "local_cafe", "ربطة", "7 شيكل/ربطة"),
        Product("paper_cups", "كاسات كرتون", "أكواب كرتونية فاخرة بتصاميم عصرية وعزل حراري ممتاز للمشروبات.", "coffee", "ربطة", "تواصل معنا لمعرفة السعر الحالي", isContactOnly = true)
    )
    private var sandboxProducts = defaultProducts.toMutableList()

    fun observeProducts(onProductsChanged: (List<Product>) -> Unit) {
        if (_mode == FirebaseMode.REAL && firestore != null) {
            firestore!!.collection("products")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen failed for products", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        val list = snapshot.documents.mapNotNull { it.toObject(Product::class.java) }
                        onProductsChanged(list)
                    } else {
                        // Pre-populate with default products if empty
                        defaultProducts.forEach { product ->
                            firestore!!.collection("products").document(product.id).set(product)
                        }
                        onProductsChanged(defaultProducts)
                    }
                }
        } else {
            sandboxProductsListener = onProductsChanged
            onProductsChanged(sandboxProducts)
        }
    }

    fun saveProduct(product: Product, onComplete: (Result<Unit>) -> Unit) {
        if (_mode == FirebaseMode.REAL && firestore != null) {
            firestore!!.collection("products").document(product.id).set(product)
                .addOnSuccessListener {
                    onComplete(Result.success(Unit))
                }
                .addOnFailureListener { e ->
                    onComplete(Result.failure(e))
                }
        } else {
            val idx = sandboxProducts.indexOfFirst { it.id == product.id }
            if (idx >= 0) {
                sandboxProducts[idx] = product
            } else {
                sandboxProducts.add(product)
            }
            sandboxProductsListener?.invoke(sandboxProducts)
            onComplete(Result.success(Unit))
        }
    }

    fun deleteProduct(productId: String, onComplete: (Result<Unit>) -> Unit) {
        if (_mode == FirebaseMode.REAL && firestore != null) {
            firestore!!.collection("products").document(productId).delete()
                .addOnSuccessListener {
                    onComplete(Result.success(Unit))
                }
                .addOnFailureListener { e ->
                    onComplete(Result.failure(e))
                }
        } else {
            sandboxProducts.removeAll { it.id == productId }
            sandboxProductsListener?.invoke(sandboxProducts)
            onComplete(Result.success(Unit))
        }
    }
}
