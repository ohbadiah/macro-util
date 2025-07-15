package com.nutrition.api

import com.google.gson.Gson
import com.nutrition.models.Ingredient
import com.nutrition.models.NutritionixFood
import com.nutrition.models.NutritionixResponse
import com.nutrition.models.NutritionixInstantResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class NutritionixClient(
    private val appId: String,
    private val appKey: String,
) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    fun searchFoodOptionsInstant(query: String): List<Ingredient> {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://trackapi.nutritionix.com/v2/search/instant?query=$encodedQuery"
        
        val request = Request.Builder()
            .url(url)
            .addHeader("x-app-id", appId)
            .addHeader("x-app-key", appKey)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return emptyList() // Silent failure, will fall back to natural search
            }

            val responseBody = response.body?.string() ?: return emptyList()
            val instantResponse = gson.fromJson(responseBody, NutritionixInstantResponse::class.java)
            
            val results = mutableListOf<Ingredient>()
            
            // Add branded foods first (more specific)
            instantResponse.branded.take(5).forEach { branded ->
                val displayName = if (!branded.brand_name.isNullOrBlank()) {
                    "${branded.food_name} (${branded.brand_name})"
                } else {
                    branded.food_name
                }
                
                // Create a temporary ingredient with basic info - will get full nutrition later
                results.add(Ingredient(
                    name = displayName,
                    servingSize = branded.serving_qty ?: 1.0,
                    servingUnit = branded.serving_unit ?: "serving",
                    servingWeightGrams = null, // Will be filled when we get detailed nutrition
                    calories = branded.nf_calories,
                    protein = 0.0, // Placeholder - will get real values from detailed search
                    fat = 0.0,
                    carbs = 0.0,
                ))
            }
            
            // Add common foods
            instantResponse.common.take(5 - results.size).forEach { common ->
                results.add(Ingredient(
                    name = common.food_name,
                    servingSize = common.serving_qty ?: 1.0,
                    servingUnit = common.serving_unit ?: "serving",
                    servingWeightGrams = null,
                    calories = common.nf_calories,
                    protein = 0.0,
                    fat = 0.0,
                    carbs = 0.0,
                ))
            }
            
            return results
        }
    }

    fun searchFoodOptions(query: String): List<Ingredient> {
        // Try instant search first (better for specific items)
        val instantResults = searchFoodOptionsInstant(query)
        if (instantResults.isNotEmpty()) {
            return instantResults
        }
        
        // Fall back to natural nutrients search (current working method)
        val requestBody = """{"query": "$query"}""".toRequestBody(jsonMediaType)

        val request =
            Request
                .Builder()
                .url("https://trackapi.nutritionix.com/v2/natural/nutrients")
                .addHeader("x-app-id", appId)
                .addHeader("x-app-key", appKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                println("API Error: ${response.code} - ${response.message}")
                return emptyList()
            }

            val responseBody = response.body?.string() ?: return emptyList()
            val nutritionixResponse = gson.fromJson(responseBody, NutritionixResponse::class.java)

            return nutritionixResponse.foods.map { food ->
                Ingredient(
                    name = food.food_name,
                    servingSize = food.serving_qty,
                    servingUnit = food.serving_unit,
                    servingWeightGrams = food.serving_weight_grams,
                    calories = food.nf_calories,
                    protein = food.nf_protein,
                    fat = food.nf_total_fat,
                    carbs = food.nf_total_carbohydrate,
                )
            }
        }
    }

    fun searchFood(query: String): Ingredient? {
        val requestBody = """{"query": "$query"}""".toRequestBody(jsonMediaType)

        val request =
            Request
                .Builder()
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
                    servingWeightGrams = food.serving_weight_grams,
                    calories = food.nf_calories,
                    protein = food.nf_protein,
                    fat = food.nf_total_fat,
                    carbs = food.nf_total_carbohydrate,
                )
            }
        }
    }
}
