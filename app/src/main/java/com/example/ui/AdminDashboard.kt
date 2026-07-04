package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firebase.FirebaseService
import com.example.firebase.LivePrices
import com.example.ui.theme.*

fun getProductIcon(iconName: String): ImageVector {
    return when (iconName) {
        "inventory" -> Icons.Default.Inventory
        "shopping_bag" -> Icons.Default.ShoppingBag
        "layers" -> Icons.Default.Layers
        "local_cafe" -> Icons.Default.LocalCafe
        "restaurant" -> Icons.Default.Restaurant
        "shopping_cart" -> Icons.Default.ShoppingCart
        else -> Icons.Default.Coffee
    }
}

@Composable
fun AdminDashboard(viewModel: MainViewModel) {
    val currentPrices by viewModel.livePrices.collectAsState()
    val productsList by viewModel.productsList.collectAsState()
    val context = LocalContext.current

    // Active Tab state: 0 = الأسعار, 1 = إدارة الكاتالوج
    var activeAdminTab by remember { mutableStateOf(0) }

    // Admin Inputs for Prices
    var westBankBase by remember(currentPrices) { mutableStateOf(currentPrices.westBankNylon.toString()) }
    var westBankBulk by remember(currentPrices) { mutableStateOf(currentPrices.westBankNylonBulk.toString()) }
    var egyptianBase by remember(currentPrices) { mutableStateOf(currentPrices.egyptianNylon.toString()) }
    var egyptianBulk by remember(currentPrices) { mutableStateOf(currentPrices.egyptianNylonBulk.toString()) }
    var plasticCupsPrice by remember(currentPrices) { mutableStateOf(currentPrices.plasticCups.toString()) }

    var isSavingPrices by remember { mutableStateOf(false) }

    // Dialog state variables for Products Management
    var showEditDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var productToDeleteId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcome Admin Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(GoldPrimary, GoldDark))), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(GoldPrimary, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = NavyDeep)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "بوابة الإدارة الفاخرة حكاية 👑",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        textAlign = TextAlign.Right
                    )
                    Text(
                        text = "أهلاً بك يا مديرنا الفاضل. يمكنك التحكم بالأسعار وإدارة أصناف الكاتالوج بنقرة واحدة.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Right,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // Custom Tab Swapper
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .background(NavyDeep, RoundedCornerShape(14.dp))
                .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(14.dp))
                .padding(4.dp)
        ) {
            val activeProductsBg = if (activeAdminTab == 1) Modifier.background(Brush.horizontalGradient(listOf(GoldPrimary, GoldDark))) else Modifier
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .then(activeProductsBg)
                    .clickable { activeAdminTab = 1 }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "إدارة أصناف الكاتالوج 📦",
                    fontWeight = FontWeight.Bold,
                    color = if (activeAdminTab == 1) NavyDeep else GoldPrimary,
                    fontSize = 13.sp
                )
            }

            val activePricesBg = if (activeAdminTab == 0) Modifier.background(Brush.horizontalGradient(listOf(GoldPrimary, GoldDark))) else Modifier
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .then(activePricesBg)
                    .clickable { activeAdminTab = 0 }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "تحديث تسعير النايلون الكلي 💰",
                    fontWeight = FontWeight.Bold,
                    color = if (activeAdminTab == 0) NavyDeep else GoldPrimary,
                    fontSize = 13.sp
                )
            }
        }

        if (activeAdminTab == 0) {
            // Price Management Form
            PricingCategorySection(
                title = "أكياس نايلون الضفة (شيكل/كغم)",
                baseValue = westBankBase,
                onBaseChange = { westBankBase = it },
                bulkValue = westBankBulk,
                onBulkChange = { westBankBulk = it },
                testTagPrefix = "west_bank"
            )

            Spacer(modifier = Modifier.height(16.dp))

            PricingCategorySection(
                title = "أكياس نايلون مصري (شيكل/كغم)",
                baseValue = egyptianBase,
                onBaseChange = { egyptianBase = it },
                bulkValue = egyptianBulk,
                onBulkChange = { egyptianBulk = it },
                testTagPrefix = "egyptian"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Plastic Cups Price
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = NavySurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "كاسات بلاستيك وسط (شيكل/ربطة)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GoldPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        textAlign = TextAlign.Right
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = plasticCupsPrice,
                            onValueChange = { plasticCupsPrice = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("plastic_cups_input"),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = TextOnNavy),
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
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "سعر الربطة الحالي:", fontSize = 12.sp, color = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save Price Settings Button
            Button(
                onClick = {
                    val wbB = westBankBase.toDoubleOrNull()
                    val wbBulk = westBankBulk.toDoubleOrNull()
                    val egB = egyptianBase.toDoubleOrNull()
                    val egBulk = egyptianBulk.toDoubleOrNull()
                    val cupsPrice = plasticCupsPrice.toDoubleOrNull()

                    if (wbB == null || wbBulk == null || egB == null || egBulk == null || cupsPrice == null) {
                        Toast.makeText(context, "الرجاء إدخال أرقام صحيحة وصالحة لكافة خانات التسعير.", Toast.LENGTH_LONG).show()
                    } else {
                        isSavingPrices = true
                        val updatedLivePrices = LivePrices(
                            westBankNylon = wbB,
                            westBankNylonBulk = wbBulk,
                            egyptianNylon = egB,
                            egyptianNylonBulk = egBulk,
                            plasticCups = cupsPrice
                        )
                        viewModel.updatePrices(updatedLivePrices) { result ->
                            isSavingPrices = false
                            result.fold(
                                onSuccess = {
                                    Toast.makeText(context, "تم حفظ وتعميم الأسعار الجديدة بنجاح ✨", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { e ->
                                    Toast.makeText(context, "فشل حفظ الأسعار: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_prices_button"),
                enabled = !isSavingPrices
            ) {
                if (isSavingPrices) {
                    CircularProgressIndicator(color = NavyDeep, modifier = Modifier.size(24.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = NavyDeep)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حفظ وتعميم التعديلات فورياً 🚀",
                            color = NavyDeep,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        } else {
            // Product Catalog Management Tab
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        productToEdit = null
                        showEditDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NavyDeep)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة صنف جديد للكاتالوج ➕", color = NavyDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                if (productsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("الكاتالوج فارغ حالياً! اضغط لإضافة أول منتج.", color = TextMuted, fontSize = 13.sp)
                    }
                } else {
                    productsList.forEach { product ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = NavySurface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Icons Actions Block
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = {
                                                productToEdit = product
                                                showEditDialog = true
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(NavyLight, CircleShape)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = GoldAccent, modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                productToDeleteId = product.id
                                                showDeleteConfirm = true
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(NavyLight, CircleShape)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF9A9A), modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    // Product Title & Icon Info
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(text = product.title, color = TextOnNavy, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Right)
                                            Text(text = "الوحدة: ${product.unit}", color = TextMuted, fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(GoldPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(getProductIcon(product.iconName), contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = product.description,
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        if (product.hasSubtypes) {
                                            Text("🏷️ يحتوي فئات", color = GoldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        if (product.isContactOnly) {
                                            Text("📞 تواصل فقط", color = Color(0xFF90CAF9), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Text(
                                        text = "توضيح السعر: ${product.priceInfo}",
                                        color = GoldPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Products Add/Edit Modal
    if (showEditDialog) {
        ProductEditDialog(
            product = productToEdit,
            onDismiss = { showEditDialog = false },
            onSave = { updatedProduct ->
                showEditDialog = false
                viewModel.saveProduct(updatedProduct) { result ->
                    result.fold(
                        onSuccess = {
                            Toast.makeText(context, "تم حفظ المنتج بنجاح ✨", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { e ->
                            Toast.makeText(context, "فشل الحفظ: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        )
    }

    // Delete Confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("تأكيد الحذف ⚠️", color = Color(0xFFEF9A9A), fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
            text = { Text("هل أنت متأكد من رغبتك في حذف هذا الصنف نهائياً من الكاتالوج؟ لا يمكن التراجع عن هذا الإجراء.", color = TextOnNavy, fontSize = 13.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        productToDeleteId?.let { id ->
                            viewModel.deleteProduct(id) { result ->
                                result.fold(
                                    onSuccess = {
                                        Toast.makeText(context, "تم حذف الصنف بنجاح.", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { e ->
                                        Toast.makeText(context, "فشل حذف الصنف: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF9A9A))
                ) {
                    Text("نعم، احذف الصنف", color = NavyDeep, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("إلغاء", color = TextOnNavy)
                }
            },
            containerColor = NavySurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ProductEditDialog(
    product: Product?,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var title by remember { mutableStateOf(product?.title ?: "") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var iconName by remember { mutableStateOf(product?.iconName ?: "inventory") }
    var unit by remember { mutableStateOf(product?.unit ?: "كيلو") }
    var priceInfo by remember { mutableStateOf(product?.priceInfo ?: "") }
    var hasSubtypes by remember { mutableStateOf(product?.hasSubtypes ?: false) }
    var isContactOnly by remember { mutableStateOf(product?.isContactOnly ?: false) }

    val iconsList = listOf("inventory", "shopping_bag", "layers", "local_cafe", "coffee", "restaurant", "shopping_cart")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (product == null) "إضافة صنف جديد للكاتالوج ➕" else "تعديل بيانات الصنف 📝",
                fontWeight = FontWeight.Bold,
                color = GoldPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                Text("اسم الصنف أو المنتج:", fontSize = 11.sp, color = TextMuted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("مثال: أكياس نايلون أسود ثقيل", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
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

                // Description
                Text("وصف الصنف والمواصفات:", fontSize = 11.sp, color = TextMuted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("تفاصيل الصنف والاستخدامات...", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right, color = TextOnNavy),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = BorderGold,
                        focusedContainerColor = NavyDeep,
                        unfocusedContainerColor = NavyDeep
                    ),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2
                )

                // Unit
                Text("وحدة البيع والتسعير:", fontSize = 11.sp, color = TextMuted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    placeholder = { Text("مثال: كيلو أو ربطة", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
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

                // Price Info
                Text("توضيح السعر للعميل:", fontSize = 11.sp, color = TextMuted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                OutlinedTextField(
                    value = priceInfo,
                    onValueChange = { priceInfo = it },
                    placeholder = { Text("مثال: ابتداءً من 15 شيكل/كيلو", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
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

                // Icon selection
                Text("أيقونة الصنف البصرية:", fontSize = 11.sp, color = TextMuted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    iconsList.forEach { name ->
                        val selected = iconName == name
                        IconButton(
                            onClick = { iconName = name },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (selected) GoldPrimary else NavyLight)
                                .border(BorderStroke(1.dp, if (selected) GoldAccent else BorderGold), CircleShape)
                        ) {
                            Icon(
                                imageVector = getProductIcon(name),
                                contentDescription = null,
                                tint = if (selected) NavyDeep else GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Checkboxes Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isContactOnly,
                            onCheckedChange = { isContactOnly = it },
                            colors = CheckboxDefaults.colors(checkedColor = GoldPrimary, uncheckedColor = BorderGold)
                        )
                        Text("للتواصل فقط 📞", color = TextOnNavy, fontSize = 11.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = hasSubtypes,
                            onCheckedChange = { hasSubtypes = it },
                            colors = CheckboxDefaults.colors(checkedColor = GoldPrimary, uncheckedColor = BorderGold)
                        )
                        Text("يحتوي فئات (ضفة/مصري)؟ 🏷️", color = TextOnNavy, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val finalId = product?.id ?: "product_${System.currentTimeMillis()}"
                        onSave(
                            Product(
                                id = finalId,
                                title = title,
                                description = description,
                                iconName = iconName,
                                unit = unit,
                                priceInfo = priceInfo,
                                hasSubtypes = hasSubtypes,
                                isContactOnly = isContactOnly
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                shape = RoundedCornerShape(10.dp),
                enabled = title.isNotBlank()
            ) {
                Text("حفظ التغييرات ✨", color = NavyDeep, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color(0xFFEF9A9A))
            }
        },
        containerColor = NavySurface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun PricingCategorySection(
    title: String,
    baseValue: String,
    onBaseChange: (String) -> Unit,
    bulkValue: String,
    onBulkChange: (String) -> Unit,
    testTagPrefix: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = NavySurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = GoldPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Right
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bulk Price Field
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "جملة (فوق 10 كغم):",
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        textAlign = TextAlign.Right
                    )
                    OutlinedTextField(
                        value = bulkValue,
                        onValueChange = onBulkChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${testTagPrefix}_bulk_input"),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = TextOnNavy),
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
                }

                // Base Price Field
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مفرد (أساسي):",
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        textAlign = TextAlign.Right
                    )
                    OutlinedTextField(
                        value = baseValue,
                        onValueChange = onBaseChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${testTagPrefix}_base_input"),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = TextOnNavy),
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
                }
            }
        }
    }
}
