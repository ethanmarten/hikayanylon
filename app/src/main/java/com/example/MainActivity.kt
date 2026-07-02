package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

// Utility function to generate and launch WhatsApp message URL
fun sendWhatsAppMessage(context: Context, userName: String, cart: List<CartItem>, singleItem: CartItem? = null) {
    val phone = "972594820775"
    val intro = "✨ *طلب جديد من تطبيق متجر حكاية للنايلون* ✨\n\n"
    val nameStr = "👤 *إسم العميل:* ${userName.ifBlank { "عميل راقٍ" }}\n\n"
    
    val itemsStr = StringBuilder("📦 *المنتجات المطلوبة:*\n")
    var overallTotal = 0.0

    if (singleItem != null) {
        val typeStr = if (singleItem.subtype != null) " (${singleItem.subtype})" else ""
        itemsStr.append("• *${singleItem.productName}*$typeStr\n")
        itemsStr.append("  - الكمية: ${singleItem.quantity} ${singleItem.unit}\n")
        if (singleItem.totalPrice > 0) {
            itemsStr.append("  - السعر: ${singleItem.totalPrice} شيكل (بمعدل ${singleItem.unitPrice} شيكل/${singleItem.unit})\n")
            overallTotal = singleItem.totalPrice
        } else {
            itemsStr.append("  - السعر: تواصل معنا لتأكيد السعر الحالي\n")
        }
    } else {
        cart.forEachIndexed { index, item ->
            val typeStr = if (item.subtype != null) " (${item.subtype})" else ""
            itemsStr.append("${index + 1}. *${item.productName}*$typeStr\n")
            itemsStr.append("   - الكمية: ${item.quantity} ${item.unit}\n")
            if (item.totalPrice > 0) {
                itemsStr.append("   - السعر: ${item.totalPrice} شيكل (بمعدل ${item.unitPrice} شيكل/${item.unit})\n")
                overallTotal += item.totalPrice
            } else {
                itemsStr.append("   - السعر: تواصل معنا لتأكيد السعر الحالي\n")
            }
            itemsStr.append("\n")
        }
    }
    
    val totalStr = if (overallTotal > 0) "\n💰 *السعر الإجمالي الكلي:* $overallTotal شيكل جديد\n\n" else "\n"
    val footer = "✨ تم إنشاء هذا الطلب تلقائياً عبر تطبيق حكاية الفاخر للتغليف. أرجو تأكيد الطلب والتوصيل."
    
    val fullText = "$intro$nameStr$itemsStr$totalStr$footer"
    val encodedText = Uri.encode(fullText)
    val url = "https://api.whatsapp.com/send?phone=$phone&text=$encodedText"

    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "عذراً، تطبيق الواتساب غير مثبت على هذا الجهاز.", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun MainAppScreen() {
    val viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val activePopup by viewModel.activeProductPopup.collectAsState()
    val context = LocalContext.current

    val productsList = remember {
        listOf(
            Product("black_nylon", "أكياس النايلون الأسود", "أكياس متينة وفاخرة مخصصة للاستخدامات التجارية والصناعية الشاقة.", "inventory", "كيلو", "ابتداءً من 15 شيكل/كيلو", hasSubtypes = true),
            Product("colored_nylon", "الوسط الملون", "نايلون ملون وسط بمواصفات ممتازة ومقاومة فائقة وعزل ممتاز.", "shopping_bag", "كيلو", "ابتداءً من 15 شيكل/كيلو", hasSubtypes = true),
            Product("large_box_nylon", "نايلون البكسة الكبير الأزرق والأسود", "أغطية نايلون البكسة الكبير، حماية ممتازة ومقاومة عالية للتمزق والظروف الجوية.", "layers", "كيلو", "ابتداءً من 15 شيكل/كيلو", hasSubtypes = true),
            Product("plastic_cups", "كاسات بلاستيك وسط", "أكواب بلاستيكية متوسطة فاخرة، مثالية للمشروبات الباردة والساخنة.", "local_cafe", "ربطة", "7 شيكل/ربطة"),
            Product("paper_cups", "كاسات كرتون", "أكواب كرتونية فاخرة بتصاميم عصرية وعزل حراري ممتاز للمشروبات.", "coffee", "ربطة", "تواصل معنا لمعرفة السعر الحالي", isContactOnly = true)
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDeep),
        topBar = { TopBrandingBar() },
        bottomBar = {
            LuxuryBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Brush.verticalGradient(listOf(NavyDeep, NavySurface)))
        ) {
            // Main views with simple tab navigation
            when (selectedTab) {
                0 -> CatalogScreen(
                    productsList = productsList,
                    onProductClick = { viewModel.openProductPopup(it) }
                )
                1 -> ChatScreen(viewModel = viewModel)
            }

            // Floating Cart Indicator & Drawer
            CartDrawer(viewModel = viewModel)

            // Dynamic Popup for Configurable Products
            activePopup?.let { product ->
                ProductConfigPopup(
                    product = product,
                    onDismiss = { viewModel.closeProductPopup() },
                    onConfirm = { subtype, qty, unitPrice, totalPrice ->
                        viewModel.addToCart(
                            productName = product.title,
                            subtype = subtype,
                            quantity = qty,
                            unit = product.unit,
                            unitPrice = unitPrice,
                            totalPrice = totalPrice
                        )
                        viewModel.closeProductPopup()
                        Toast.makeText(context, "تمت إضافة المنتج إلى السلة ✨", Toast.LENGTH_SHORT).show()
                    },
                    onQuickWhatsApp = { subtype, qty, unitPrice, totalPrice ->
                        val singleItem = CartItem(
                            id = "temp",
                            productName = product.title,
                            subtype = subtype,
                            quantity = qty,
                            unit = product.unit,
                            unitPrice = unitPrice,
                            totalPrice = totalPrice
                        )
                        viewModel.closeProductPopup()
                        sendWhatsAppMessage(context, viewModel.customerName.value, emptyList(), singleItem)
                    }
                )
            }
        }
    }
}

@Composable
fun TopBrandingBar() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = NavyDeep,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "Premium Brand Logo",
                    tint = GoldPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "متجر حكاية للنايلون",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = "العلامة الفاخرة للتعبئة والتغليف والتسوق الراقي",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, GoldPrimary, Color.Transparent)))
            )
        }
    }
}

@Composable
fun LuxuryBottomNavigation(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp)
            .navigationBarsPadding(),
        containerColor = NavyDeep,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            label = {
                Text(
                    text = "المنتجات والطلب",
                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp,
                    color = if (selectedTab == 0) GoldPrimary else TextMuted
                )
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.MenuBook,
                    contentDescription = "الكتالوج والطلب",
                    tint = if (selectedTab == 0) GoldPrimary else TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = NavyLight
            ),
            modifier = Modifier.testTag("tab_catalog")
        )

        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            label = {
                Text(
                    text = "مساعد حكاية الذكي",
                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp,
                    color = if (selectedTab == 1) GoldPrimary else TextMuted
                )
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Chat,
                    contentDescription = "مساعد حكاية الذكي",
                    tint = if (selectedTab == 1) GoldPrimary else TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = NavyLight
            ),
            modifier = Modifier.testTag("tab_chat")
        )
    }
}

@Composable
fun CatalogScreen(
    productsList: List<Product>,
    onProductClick: (Product) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Image Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(GoldPrimary, GoldDark))), RoundedCornerShape(16.dp))
                    .shadow(8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_brand_hero),
                    contentDescription = "Brand Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Dark overlay with gradient for typography readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x99000000), Color(0xDD070E1A))
                            )
                        )
                )
                // Arabic Greetings over Banner
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "منتجات التغليف والنايلون الفاخرة",
                        color = GoldAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "اختر المنتجات الفاخرة التي تلبي تطلعاتك وسيتم تجهيز فاتورتك فوراً",
                        color = TextOnNavy,
                        fontSize = 12.sp,
                        maxLines = 2,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Section Title
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp, 24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(GoldPrimary)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "كتالوج المنتجات الفاخرة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextOnNavy
                )
            }
        }

        // Products Catalog
        items(productsList) { product ->
            ProductCard(
                product = product,
                onClick = { onProductClick(product) }
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit
) {
    val icon: ImageVector = when (product.iconName) {
        "inventory" -> Icons.Default.Inventory
        "shopping_bag" -> Icons.Default.ShoppingBag
        "layers" -> Icons.Default.Layers
        "local_cafe" -> Icons.Default.LocalCafe
        else -> Icons.Default.Coffee
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .shadow(4.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Icon Container styled in premium gold circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(NavyLight, NavyDeep)))
                    .border(BorderStroke(1.dp, GoldPrimary), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = product.title,
                    tint = GoldPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Info Column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextOnNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.description,
                    fontSize = 12.sp,
                    color = TextMuted,
                    maxLines = 2,
                    lineHeight = 16.sp,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Price Tag Badge in metallic colors
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Premium Badge",
                        tint = GoldPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = product.priceInfo,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GoldPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Elegant Order Arrow/Indicator
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NavyLight)
                    .border(BorderStroke(1.dp, BorderGold), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "طلب المنتج",
                    tint = GoldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ProductConfigPopup(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (subtype: String?, qty: Double, unitPrice: Double, totalPrice: Double) -> Unit,
    onQuickWhatsApp: (subtype: String?, qty: Double, unitPrice: Double, totalPrice: Double) -> Unit
) {
    var selectedSubtype by remember { mutableStateOf(if (product.hasSubtypes) "ضفة" else null) }
    var quantityText by remember { mutableStateOf("5") } // Default starting quantity
    val quantity = quantityText.toDoubleOrNull() ?: 0.0

    // Pricing calculation helper
    val viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val (unitPrice, totalPrice) = viewModel.calculatePrice(product.id, selectedSubtype, quantity)

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(BorderStroke(2.dp, Brush.horizontalGradient(listOf(GoldPrimary, GoldDark))), RoundedCornerShape(20.dp))
                .shadow(24.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = TextMuted
                        )
                    }
                    Text(
                        text = "تفاصيل الطلب الفاخر",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                    Spacer(modifier = Modifier.size(32.dp)) // Spacer to align title
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Product Title
                Text(
                    text = product.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextOnNavy,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = product.description,
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (product.isContactOnly) {
                    // Paper Cups Message
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NavyLight)
                            .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "تواصل معنا",
                                tint = GoldPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "تواصل معنا لمعرفة السعر الحالي",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "أسعار الأكواب الكرتونية تتغير باستمرار. اضغط بالأسفل لطلب تسعير فوري عبر الواتساب أو التحدث للمساعد الذكي.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Contact Only action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                onQuickWhatsApp(null, 0.0, 0.0, 0.0)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تواصل واتساب 📞", color = NavyDeep, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Configurable Section (Nylon Subtypes & Stepper)
                    if (product.hasSubtypes) {
                        Text(
                            text = "اختر مصدر أو نوع التصنيع:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextOnNavy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            textAlign = TextAlign.Right
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf("ضفة", "مصري").forEach { source ->
                                val isSelected = selectedSubtype == source
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) NavyLight else NavyDeep)
                                        .border(
                                            BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) GoldPrimary else BorderGold
                                            ),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedSubtype = source }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = source,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) GoldPrimary else TextMuted,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = if (source == "مصري") "17 شيكل/كيلو" else "15 شيكل/كيلو",
                                            fontSize = 11.sp,
                                            color = if (isSelected) GoldAccent else TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Stepper Quantity Input
                    Text(
                        text = "الكمية المطلوبة (${product.unit}):",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextOnNavy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        textAlign = TextAlign.Right
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Minus Button
                        IconButton(
                            onClick = {
                                val currentVal = quantityText.toDoubleOrNull() ?: 0.0
                                if (currentVal > 1) {
                                    quantityText = (currentVal - 1).toInt().toString()
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(NavyLight)
                                .border(BorderStroke(1.dp, BorderGold), CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "تقليل", tint = GoldPrimary)
                        }

                        // Direct TextField Input
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.toDoubleOrNull() != null) {
                                    quantityText = input
                                }
                            },
                            modifier = Modifier
                                .width(110.dp)
                                .padding(horizontal = 12.dp),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = GoldPrimary
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = BorderGold,
                                focusedContainerColor = NavyDeep,
                                unfocusedContainerColor = NavyDeep
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        // Plus Button
                        IconButton(
                            onClick = {
                                val currentVal = quantityText.toDoubleOrNull() ?: 0.0
                                quantityText = (currentVal + 1).toInt().toString()
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(NavyLight)
                                .border(BorderStroke(1.dp, BorderGold), CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "زيادة", tint = GoldPrimary)
                        }
                    }

                    // Bulk discount indicator banner
                    if (product.hasSubtypes && quantity > 10.0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2C2515))
                                .border(BorderStroke(1.dp, GoldPrimary), RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = "خصم الكيلو", tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تم خصم 1 شيكل/كيلو تلقائياً لتجاوزك كمية 10 كيلو! 🔥",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Total Calculation Panel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NavyDeep)
                            .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "${unitPrice} شيكل", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                                Text(text = "سعر الوحدة الحالي:", fontSize = 14.sp, color = TextMuted)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = BorderGold, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${totalPrice} شيكل جديد",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                                Text(text = "السعر الإجمالي الكلي:", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextOnNavy)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons row
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Quick Whatsapp
                        Button(
                            onClick = {
                                onQuickWhatsApp(selectedSubtype, quantity, unitPrice, totalPrice)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = NavyDeep)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إرسال طلب سريع بالواتساب 📞", color = NavyDeep, fontWeight = FontWeight.Bold)
                        }

                        // Add to Order Cart
                        OutlinedButton(
                            onClick = {
                                onConfirm(selectedSubtype, quantity, unitPrice, totalPrice)
                            },
                            border = BorderStroke(1.5.dp, GoldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إضافة إلى سلة الطلبات الحالية", fontWeight = FontWeight.Bold, color = GoldPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartDrawer(viewModel: MainViewModel) {
    val cart by viewModel.cart.collectAsState()
    val customerName by viewModel.customerName.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (cart.isEmpty()) return

    val totalSum = cart.sumOf { it.totalPrice }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cart_drawer_container"),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Overlay when expanded
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .clickable { isExpanded = false }
            )
        }

        // Cart Drawer Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .border(BorderStroke(2.dp, GoldPrimary), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .shadow(24.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Drag / Tap indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(GoldPrimary)
                        .clickable { isExpanded = !isExpanded }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Summary Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cart.size.toString(),
                                fontWeight = FontWeight.Bold,
                                color = NavyDeep,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "سلة طلباتك الحالية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextOnNavy
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$totalSum شيكل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = GoldPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "توسيع",
                            tint = GoldPrimary
                        )
                    }
                }

                // Expanded Section showing items and inputs
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Divider(color = BorderGold, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Customer Name Input Field
                        Text(
                            text = "الرجاء كتابة إسمك الكريم لتأكيد الفاتورة:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            textAlign = TextAlign.Right
                        )
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { viewModel.setCustomerName(it) },
                            placeholder = { Text("إسم العميل الكريم...", color = TextMuted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("name_input"),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right, color = TextOnNavy),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = BorderGold,
                                focusedContainerColor = NavyDeep,
                                unfocusedContainerColor = NavyDeep
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Added Items List
                        Text(
                            text = "الأصناف المضافة:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextOnNavy,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                                .padding(vertical = 8.dp)
                        ) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(cart) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NavyDeep)
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.removeFromCart(item.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف الصنف",
                                                tint = Color(0xFFEF9A9A),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            val typeLabel = if (item.subtype != null) " (${item.subtype})" else ""
                                            Text(
                                                text = "${item.productName}$typeLabel",
                                                color = TextOnNavy,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                textAlign = TextAlign.Right
                                            )
                                            Text(
                                                text = "${item.quantity} ${item.unit} × ${item.unitPrice} شيكل",
                                                color = TextMuted,
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Right
                                            )
                                        }

                                        Text(
                                            text = "${item.totalPrice} شيكل",
                                            color = GoldPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }

                        Divider(color = BorderGold, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Clear Cart and Order Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.clearCart() },
                                border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF9A9A)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "تفريغ السلة")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تفريغ", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (customerName.isBlank()) {
                                        Toast.makeText(context, "الرجاء كتابة إسمك الكريم لتسهيل معالجة الطلب.", Toast.LENGTH_LONG).show()
                                    } else {
                                        sendWhatsAppMessage(context, customerName, cart)
                                        viewModel.clearCart()
                                        isExpanded = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("submit_order_button")
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = NavyDeep)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إرسال الطلب عبر الواتساب 📞", color = NavyDeep, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val isBotResponding by viewModel.isBotResponding.collectAsState()
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Suggestions chips list for premium user UX
    val suggestions = remember {
        listOf(
            "ما هي أسعار النايلون؟",
            "كم سعر كاسات الكرتون؟",
            "هل تتوفر خدمة توصيل؟",
            "أريد طلب نايلون ضفة"
        )
    }

    // Automatically scroll to bottom whenever messages list size changes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Chat List Space
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message = message)
            }

            if (isBotResponding) {
                item {
                    BotTypingIndicator()
                }
            }
        }

        // Suggestions Horizontal Row
        Text(
            text = "إستفسارات شائعة مقترحة:",
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            textAlign = TextAlign.Right
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            items(suggestions) { question ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(NavyLight)
                        .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(16.dp))
                        .clickable {
                            viewModel.sendChatMessage(question)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = question,
                        color = GoldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Send Input Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Send Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(GoldPrimary, GoldDark)))
                    .clickable {
                        if (textInput.isNotBlank()) {
                            viewModel.sendChatMessage(textInput)
                            textInput = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "إرسال",
                    tint = NavyDeep,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Text Input Field with Gold borders
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        text = "اسأل مساعد حكاية الذكي بخصوص الأسعار...",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Right
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right, color = TextOnNavy, fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = BorderGold,
                    focusedContainerColor = NavySurface,
                    unfocusedContainerColor = NavySurface
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                singleLine = false
            )
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isBot = message.sender == "bot"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isBot) Alignment.End else Alignment.Start
    ) {
        // Sender Badge Header
        Text(
            text = if (isBot) "✨ مساعد حكاية الذكي" else "العميل",
            fontSize = 10.sp,
            color = if (isBot) GoldAccent else TextMuted,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )

        // Bubble Box
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isBot) 16.dp else 2.dp,
                        bottomEnd = if (isBot) 2.dp else 16.dp
                    )
                )
                .background(
                    if (isBot) Brush.verticalGradient(listOf(NavySurface, NavyLight))
                    else Brush.verticalGradient(listOf(GoldPrimary, GoldDark))
                )
                .border(
                    BorderStroke(
                        width = 1.dp,
                        color = if (isBot) BorderGold else Color.Transparent
                    ),
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isBot) 16.dp else 2.dp,
                        bottomEnd = if (isBot) 2.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                color = if (isBot) TextOnNavy else NavyDeep,
                fontSize = 14.sp,
                fontWeight = if (isBot) FontWeight.Normal else FontWeight.Medium,
                lineHeight = 20.sp,
                textAlign = TextAlign.Right
            )
        }
    }
}

@Composable
fun BotTypingIndicator() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "✨ جاري التفكير...",
            fontSize = 10.sp,
            color = GoldAccent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(NavySurface)
                .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "جاري صياغة الرد الفاخر من حكاية...",
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Right
                )
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = GoldPrimary
                )
            }
        }
    }
}
