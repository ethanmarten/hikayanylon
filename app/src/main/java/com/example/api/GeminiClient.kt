package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun getChatResponse(prompt: String, history: List<Pair<String, String>> = emptyList()): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "أهلاً بك! يرجى التأكد من تهيئة مفتاح API الخاص بـ Gemini في لوحة الأسرار (Secrets) لتتمكن من التحدث مع مساعد حكاية الذكي.\n\nفي غضون ذلك، إليك أسعارنا الثابتة:\n• نايلون الضفة: 15 شيكل للكيلو (14 شيكل للكميات فوق 10 كيلو)\n• نايلون مصري: 17 شيكل للكيلو (16 شيكل للكميات فوق 10 كيلو)\n• كاسات بلاستيك وسط: 7 شيكل للربطة\n• كاسات كرتون: تواصل معنا لمعرفة السعر الحالي"
        }

        try {
            // Build the system instructions and formatting for the pricing rules
            val systemInstruction = """
                أنت "مساعد حكاية الذكي"، مساعد الذكاء الاصطناعي الراقي لعلامة التعبئة والتغليف الفاخرة "متجر حكاية للنايلون" (Hikaya Nylon Store).
                مهمتك هي الإجابة فوراً وبطريقة مهذبة وفاخرة على أسئلة العملاء بخصوص أسعار المنتجات وكمياتها وسياستنا بناءً على القواعد التالية حصراً:
                
                قواعد التسعير والمنتجات لدينا:
                1. نايلون الضفة (West Bank Nylon):
                   - السعر الأساسي: 15 شيكل للكيلو.
                   - الخصم للكميات الكبيرة: إذا كانت الكمية المطلوبة أكثر من 10 كيلو، يصبح السعر 14 شيكل للكيلو.
                2. نايلون مصري (Egyptian Nylon):
                   - السعر الأساسي: 17 شيكل للكيلو.
                   - الخصم للكميات الكبيرة: إذا كانت الكمية المطلوبة أكثر من 10 كيلو، يصبح السعر 16 شيكل للكيلو.
                3. كاسات بلاستيك وسط (Medium Plastic Cups):
                   - السعر: 7 شيكل لكل ربطة (كمامة أو sleeve).
                4. كاسات كرتون (Paper Cups):
                   - السعر غير ثابت، أخبر العميل بالعبارة التالية تحديداً: "تواصل معنا لمعرفة السعر الحالي".
                
                تصنيف المنتجات المعروضة في الكتالوج:
                - أكياس النايلون الأسود (Black Nylon Bags)
                - الوسط الملون (Colored Medium Bags / Nylon)
                - نايلون البكسة الكبير الأزرق والأسود (Large Box Nylon Blue & Black)
                - كاسات بلاستيك وسط (Medium Plastic Cups)
                - كاسات كرتون (Paper Cups)

                ملاحظة هامة جداً:
                - العملة المستخدمة دائماً هي الشيكل الجديد (NIS).
                - للتسليم والطلب النهائي، يمكن للعميل إرسال الطلب تلقائياً عبر الواتساب إلى رقم المتجر: 972594820775.
                - أجب بلغة عربية فصحى راقية، بأسلوب ترحيبي مهذب يليق بعلامة تجارية فاخرة. لا تذكر أسعاراً خارج هذه القواعد.
                - إذا سأل العميل عن إرسال طلب، أخبره أنه يمكنه استخدام زر "إرسال الطلب عبر الواتساب" في واجهة الكتالوج أو يمكنك مساعدته في كتابة تفاصيل الطلب وتوجيهه لإرساله.
            """.trimIndent()

            val contentsArray = JSONArray()

            // First add the history if any
            for (turn in history) {
                val turnContent = JSONObject().apply {
                    put("role", if (turn.first == "user") "user" else "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", turn.second) })
                    })
                }
                contentsArray.put(turnContent)
            }

            // Then add the current prompt
            val currentContent = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", prompt) })
                })
            }
            contentsArray.put(currentContent)

            // Construct the main request object
            val requestBodyJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestBodyJson.toString().toRequestBody(mediaType)

            val url = "${BASE_URL}?key=${apiKey}"
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Request failed: code=${response.code}, body=$errBody")
                    return@withContext "عذراً، حدث خطأ في الاتصال بالمساعد الذكي. يرجى مراجعة إعدادات الإنترنت والمحاولة لاحقاً."
                }

                val responseBodyStr = response.body?.string() ?: return@withContext "عذراً، لم يتلق المساعد أي استجابة."
                val jsonResponse = JSONObject(responseBodyStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val contentObj = candidate.optJSONObject("content")
                    if (contentObj != null) {
                        val parts = contentObj.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "لم تتوفر إجابة مناسبة.")
                        }
                    }
                }
                "عذراً، لم يستطع المساعد معالجة طلبك حالياً."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during Chat API call", e)
            "حدث خطأ في الاتصال بالمساعد الذكي. يرجى التأكد من تشغيل الإنترنت ومحاولة التحدث مع المساعد مجدداً."
        }
    }
}
