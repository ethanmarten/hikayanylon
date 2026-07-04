package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.firebase.FirebaseService
import com.example.firebase.LivePrices
import com.example.firebase.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Data model for products in catalog
data class Product(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val iconName: String = "",
    val unit: String = "",
    val priceInfo: String = "",
    val hasSubtypes: Boolean = false,
    val isContactOnly: Boolean = false
)

// Data model for order / cart items
data class CartItem(
    val id: String,
    val productName: String,
    val subtype: String? = null, // "ضفة" or "مصري"
    val quantity: Double,
    val unit: String,
    val unitPrice: Double,
    val totalPrice: Double
)

// Data model for Chat messages
data class ChatMessage(
    val id: String,
    val text: String,
    val sender: String, // "user" or "bot"
    val timestamp: Long = System.currentTimeMillis()
)

class MainViewModel : ViewModel() {

    // Auth and User States
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _showAuthModal = MutableStateFlow(false)
    val showAuthModal: StateFlow<Boolean> = _showAuthModal.asStateFlow()

    fun showAuth() {
        _showAuthModal.value = true
    }

    fun hideAuth() {
        _showAuthModal.value = false
    }

    // Live Prices synced with Firestore
    private val _livePrices = MutableStateFlow(LivePrices())
    val livePrices: StateFlow<LivePrices> = _livePrices.asStateFlow()

    // Tab states: 0 = Catalog & Order, 1 = AI Assistant Chat, 2 = Admin Settings (Admin only)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Customer name for invoices
    private val _customerName = MutableStateFlow("")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    // Active order / cart items
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    // Chat history
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "welcome",
                text = "مرحباً بك في متجر حكاية للنايلون الفاخر ✨. أنا مساعد حكاية الذكي، ومستعد لمساعدتك في الاستفسار عن تفاصيل المنتجات، الأسعار الحالية للفئات المختلفة، وحساب تكاليف الفاتورة بدقة.\n\nكيف يمكنني مساعدتك اليوم؟",
                sender = "bot"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // Is Gemini responding/thinking
    private val _isBotResponding = MutableStateFlow(false)
    val isBotResponding: StateFlow<Boolean> = _isBotResponding.asStateFlow()

    // Current product configuration popup state (null means closed)
    private val _activeProductPopup = MutableStateFlow<Product?>(null)
    val activeProductPopup: StateFlow<Product?> = _activeProductPopup.asStateFlow()

    // Dynamic Products list synced with Firestore
    private val _productsList = MutableStateFlow<List<Product>>(emptyList())
    val productsList: StateFlow<List<Product>> = _productsList.asStateFlow()

    init {
        // Fetch current active user on load
        checkActiveUser()

        // Sync and listen to dynamic Firestore prices
        FirebaseService.observePrices { updatedPrices ->
            _livePrices.value = updatedPrices
        }

        // Sync and listen to dynamic Firestore products
        FirebaseService.observeProducts { updatedProducts ->
            _productsList.value = updatedProducts
        }
    }

    fun saveProduct(product: Product, onComplete: (Result<Unit>) -> Unit) {
        FirebaseService.saveProduct(product) { result ->
            onComplete(result)
        }
    }

    fun deleteProduct(productId: String, onComplete: (Result<Unit>) -> Unit) {
        FirebaseService.deleteProduct(productId) { result ->
            onComplete(result)
        }
    }

    private fun checkActiveUser() {
        FirebaseService.getCurrentUser { profile ->
            _currentUser.value = profile
            if (profile != null) {
                _customerName.value = profile.name
            }
        }
    }

    fun login(email: String, password: String) {
        _authLoading.value = true
        _authError.value = null
        FirebaseService.loginUser(email, password) { result ->
            _authLoading.value = false
            result.fold(
                onSuccess = { profile ->
                    _currentUser.value = profile
                    _customerName.value = profile.name
                    _showAuthModal.value = false
                },
                onFailure = { error ->
                    _authError.value = error.message
                }
            )
        }
    }

    fun register(email: String, password: String, name: String, role: String) {
        _authLoading.value = true
        _authError.value = null
        FirebaseService.registerUser(email, password, name, role) { result ->
            _authLoading.value = false
            result.fold(
                onSuccess = { profile ->
                    _currentUser.value = profile
                    _customerName.value = profile.name
                    _showAuthModal.value = false
                },
                onFailure = { error ->
                    _authError.value = error.message
                }
            )
        }
    }

    fun logout() {
        FirebaseService.logoutUser()
        _currentUser.value = null
        _customerName.value = ""
        _selectedTab.value = 0
        clearCart()
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun updatePrices(prices: LivePrices, onComplete: (Result<Unit>) -> Unit) {
        FirebaseService.updatePrices(prices) { result ->
            if (result.isSuccess) {
                _livePrices.value = prices
            }
            onComplete(result)
        }
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun setCustomerName(name: String) {
        _customerName.value = name
    }

    fun openProductPopup(product: Product) {
        _activeProductPopup.value = product
    }

    fun closeProductPopup() {
        _activeProductPopup.value = null
    }

    // Dynamic Pricing Calculation Rules using Live Firestore Prices
    fun calculatePrice(productId: String, subtype: String?, quantity: Double): Pair<Double, Double> {
        val targetProduct = _productsList.value.find { it.id == productId }
        if (targetProduct?.isContactOnly == true || productId == "paper_cups") {
            return Pair(0.0, 0.0) // Contact only
        }
        
        val prices = _livePrices.value
        if (productId == "plastic_cups" || targetProduct?.unit == "ربطة") {
            val cupPrice = prices.plasticCups
            return Pair(cupPrice, cupPrice * quantity)
        }

        // Nylon products pricing
        val basePrice = if (subtype == "مصري") prices.egyptianNylon else prices.westBankNylon
        val bulkPrice = if (subtype == "مصري") prices.egyptianNylonBulk else prices.westBankNylonBulk
        
        val currentPrice = if (quantity > 10.0) bulkPrice else basePrice
        return Pair(currentPrice, currentPrice * quantity)
    }

    // Add configured item to cart
    fun addToCart(productName: String, subtype: String?, quantity: Double, unit: String, unitPrice: Double, totalPrice: Double) {
        val newItem = CartItem(
            id = System.currentTimeMillis().toString(),
            productName = productName,
            subtype = subtype,
            quantity = quantity,
            unit = unit,
            unitPrice = unitPrice,
            totalPrice = totalPrice
        )
        _cart.value = _cart.value + newItem
    }

    fun removeFromCart(itemId: String) {
        _cart.value = _cart.value.filter { it.id != itemId }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    // Send chat message to Gemini
    fun sendChatMessage(text: String) {
        if (text.isBlank()) return

        val userMsg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            text = text,
            sender = "user"
        )
        _chatMessages.value = _chatMessages.value + userMsg
        _isBotResponding.value = true

        viewModelScope.launch {
            // Construct context for the AI assistant including current database prices
            val prices = _livePrices.value
            val pricingContext = """
                معلومات الأسعار الحالية لمتجر حكاية المسترجعة من قاعدة البيانات:
                - أكياس نايلون أسود / ملون / بكسة (الضفة): سعر الكيلو العادي ${prices.westBankNylon} شيكل، والبيع بالجملة (فوق 10 كيلو) بسعر ${prices.westBankNylonBulk} شيكل/كيلو.
                - أكياس نايلون أسود / ملون / بكسة (المصري): سعر الكيلو العادي ${prices.egyptianNylon} شيكل، والبيع بالجملة بسعر ${prices.egyptianNylonBulk} شيكل/كيلو.
                - كاسات بلاستيك وسط: السعر الحالي ${prices.plasticCups} شيكل لكل ربطة.
                - كاسات كرتون: السعر يتغير باستمرار ومتاح فقط بالتواصل الهاتفي أو واتساب.
                
                من فضلك أجب باللغة العربية بأسلوب لبق وفاخر ومهني. ساعد العميل في حساب كميات فواتيره وحفزه للاستفادة من خصم الجملة (شراء أكثر من 10 كغم).
            """.trimIndent()

            val history = _chatMessages.value.filter { it.id != "welcome" && it.id != userMsg.id }.map {
                Pair(it.sender, it.text)
            }

            // Call Gemini with current dynamic database pricing context injection
            val promptWithContext = "$pricingContext\n\nسؤال العميل الحالي: $text"
            val botResponse = GeminiClient.getChatResponse(promptWithContext, history)

            val botMsg = ChatMessage(
                id = (System.currentTimeMillis() + 1).toString(),
                text = botResponse,
                sender = "bot"
            )
            _chatMessages.value = _chatMessages.value + botMsg
            _isBotResponding.value = false
        }
    }
}
