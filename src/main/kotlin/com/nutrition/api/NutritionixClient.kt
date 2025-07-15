package com.nutrition.api

import com.google.gson.Gson
import com.nutrition.models.Ingredient
import com.nutrition.models.NutritionixFood
import com.nutrition.models.NutritionixResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class NutritionixClient(
    private val appId: String,
    private val appKey: String
) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()
    
    fun searchFood(query: String): Ingredient? {
        val requestBody = """{"query": "$query"}""".toRequestBody(jsonMediaType)
        
        val request = Request.Builder()
            .url("https://trackapi.nutritionix.com/v2/natural/nutrients")
            .addHeader("x-app-id", appId)
            .addHeader("x-app-key", appKey)
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                println("API Error: ${response.code} - ${response.message}")
                return null
            }
            
            val responseBody = response.body?.string() ?: return null
            val nutritionixResponse = gson.fromJson(responseBody, NutritionixResponse::class.java)
            
            return nutritionixResponse.foods.firstOrNull()?.let { food ->
                Ingredient(
                    name = food.food_name,
                    servingSize = food.serving_qty,
                    servingUnit = food.serving_unit,
                    calories = food.nf_calories,
                    protein = food.nf_protein,
                    fat = food.nf_total_fat,
                    carbs = food.nf_total_carbohydrate
                )
            }
        }
    }
}
