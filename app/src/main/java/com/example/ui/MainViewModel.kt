package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Data model for products in catalog
data class Product(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val unit: String,
    val priceInfo: String,
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

    // Tab states: 0 = Catalog & Order, 1 = AI Assistant Chat
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
                text = "مرحباً بك في متجر حكاية للنايلون الفاخر ✨. أنا مساعد حكاية الذكي، ومستعد لمساعدتك في الاستفسار عن تفاصيل المنتجات، الأسعار، وحساب الكميات المطلوبة.\n\nكيف يمكنني خدمتك اليوم؟",
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

    // Pricing Calculation Rules
    fun calculatePrice(productId: String, subtype: String?, quantity: Double): Pair<Double, Double> {
        if (productId == "paper_cups") {
            return Pair(0.0, 0.0) // Contact only
        }
        if (productId == "plastic_cups") {
            // Plastic cups: 7 NIS per sleeve (ربطة)
            return Pair(7.0, 7.0 * quantity)
        }

        // Nylon products pricing
        val basePrice = if (subtype == "مصري") 17.0 else 15.0 // "ضفة" is default and has price 15
        val discountedPrice = if (quantity > 10.0) (basePrice - 1.0) else basePrice
        return Pair(discountedPrice, discountedPrice * quantity)
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
            // Construct conversation history for Gemini API
            val history = _chatMessages.value.filter { it.id != "welcome" && it.id != userMsg.id }.map {
                Pair(it.sender, it.text)
            }

            // Call Gemini
            val botResponse = GeminiClient.getChatResponse(text, history)

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
