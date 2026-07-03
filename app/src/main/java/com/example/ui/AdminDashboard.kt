package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

@Composable
fun AdminDashboard(viewModel: MainViewModel) {
    val currentPrices by viewModel.livePrices.collectAsState()
    val context = LocalContext.current

    // Admin Inputs
    var westBankBase by remember(currentPrices) { mutableStateOf(currentPrices.westBankNylon.toString()) }
    var westBankBulk by remember(currentPrices) { mutableStateOf(currentPrices.westBankNylonBulk.toString()) }
    var egyptianBase by remember(currentPrices) { mutableStateOf(currentPrices.egyptianNylon.toString()) }
    var egyptianBulk by remember(currentPrices) { mutableStateOf(currentPrices.egyptianNylonBulk.toString()) }
    var plasticCupsPrice by remember(currentPrices) { mutableStateOf(currentPrices.plasticCups.toString()) }

    var isSaving by remember { mutableStateOf(false) }

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
                .padding(bottom = 20.dp)
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
                        text = "لوحة تحكم الأسعار الفورية",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        textAlign = TextAlign.Right
                    )
                    Text(
                        text = "التحديث هنا يعدل الأسعار فوراً عند كافة المستخدمين المتصلين بقاعدة البيانات.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Right,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // Section 1: West Bank Nylon Prices
        PricingCategorySection(
            title = "أكياس نايلون الضفة (شيكل/كغم)",
            baseValue = westBankBase,
            onBaseChange = { westBankBase = it },
            bulkValue = westBankBulk,
            onBulkChange = { westBankBulk = it },
            testTagPrefix = "west_bank"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section 2: Egyptian Nylon Prices
        PricingCategorySection(
            title = "أكياس نايلون مصري (شيكل/كغم)",
            baseValue = egyptianBase,
            onBaseChange = { egyptianBase = it },
            bulkValue = egyptianBulk,
            onBulkChange = { egyptianBulk = it },
            testTagPrefix = "egyptian"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section 3: Plastic Cups
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
                    isSaving = true
                    val updatedLivePrices = LivePrices(
                        westBankNylon = wbB,
                        westBankNylonBulk = wbBulk,
                        egyptianNylon = egB,
                        egyptianNylonBulk = egBulk,
                        plasticCups = cupsPrice
                    )
                    viewModel.updatePrices(updatedLivePrices) { result ->
                        isSaving = false
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
            enabled = !isSaving
        ) {
            if (isSaving) {
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
    }
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
