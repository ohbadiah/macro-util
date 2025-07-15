package com.nutrition.database

import com.nutrition.models.*
import java.sql.Connection
import java.sql.DriverManager

class DatabaseManager(
    databasePath: String = "nutrition.db",
) {
    private val connection: Connection = DriverManager.getConnection("jdbc:sqlite:$databasePath")

    init {
        createTables()
    }

    private fun createTables() {
        val sql = """
            CREATE TABLE IF NOT EXISTS ingredients (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                serving_size REAL NOT NULL,
                serving_unit TEXT NOT NULL,
                serving_weight_grams REAL,
                calories REAL NOT NULL,
                protein REAL NOT NULL,
                fat REAL NOT NULL,
                carbs REAL NOT NULL
            );

            CREATE TABLE IF NOT EXISTS recipes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL
            );

            CREATE TABLE IF NOT EXISTS recipe_ingredients (
                recipe_id INTEGER NOT NULL,
                ingredient_id INTEGER NOT NULL,
                servings REAL NOT NULL,
                FOREIGN KEY (recipe_id) REFERENCES recipes(id),
                FOREIGN KEY (ingredient_id) REFERENCES ingredients(id),
                PRIMARY KEY (recipe_id, ingredient_id)
            );
        """

        connection.createStatement().use { statement ->
            sql.split(";").filter { it.isNotBlank() }.forEach { query ->
                statement.execute(query.trim())
            }
        }
    }

    fun getIngredient(name: String): Ingredient? {
        val sql = "SELECT * FROM ingredients WHERE LOWER(name) = LOWER(?)"
        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, name)
            val rs = statement.executeQuery()
            return if (rs.next()) {
                Ingredient(
                    id = rs.getInt("id"),
                    name = rs.getString("name"),
                    servingSize = rs.getDouble("serving_size"),
                    servingUnit = rs.getString("serving_unit"),
                    servingWeightGrams = rs.getObject("serving_weight_grams") as Double?,
                    calories = rs.getDouble("calories"),
                    protein = rs.getDouble("protein"),
                    fat = rs.getDouble("fat"),
                    carbs = rs.getDouble("carbs"),
                )
            } else {
                null
            }
        }
    }

    fun saveIngredient(ingredient: Ingredient): Ingredient {
        // Check if ingredient already exists
        val existing = getIngredient(ingredient.name)
        if (existing != null) {
            // Return existing ingredient instead of trying to insert duplicate
            return existing
        }
        
        val sql = """
            INSERT INTO ingredients (name, serving_size, serving_unit, serving_weight_grams, calories, protein, fat, carbs)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """
        connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS).use { statement ->
            statement.setString(1, ingredient.name)
            statement.setDouble(2, ingredient.servingSize)
            statement.setString(3, ingredient.servingUnit)
            if (ingredient.servingWeightGrams != null) {
                statement.setDouble(4, ingredient.servingWeightGrams)
            } else {
                statement.setNull(4, java.sql.Types.REAL)
            }
            statement.setDouble(5, ingredient.calories)
            statement.setDouble(6, ingredient.protein)
            statement.setDouble(7, ingredient.fat)
            statement.setDouble(8, ingredient.carbs)
            statement.executeUpdate()

            val generatedKeys = statement.generatedKeys
            generatedKeys.next()
            return ingredient.copy(id = generatedKeys.getInt(1))
        }
    }

    fun saveRecipe(recipe: Recipe): Recipe {
        val sql = "INSERT INTO recipes (name) VALUES (?)"
        connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS).use { statement ->
            statement.setString(1, recipe.name)
            statement.executeUpdate()

            val generatedKeys = statement.generatedKeys
            generatedKeys.next()
            val recipeId = generatedKeys.getInt(1)

            recipe.ingredients.forEach { ri ->
                saveRecipeIngredient(recipeId, ri.ingredient.id, ri.servings)
            }

            return recipe.copy(id = recipeId)
        }
    }

    private fun saveRecipeIngredient(
        recipeId: Int,
        ingredientId: Int,
        servings: Double,
    ) {
        val sql = "INSERT INTO recipe_ingredients (recipe_id, ingredient_id, servings) VALUES (?, ?, ?)"
        connection.prepareStatement(sql).use { statement ->
            statement.setInt(1, recipeId)
            statement.setInt(2, ingredientId)
            statement.setDouble(3, servings)
            statement.executeUpdate()
        }
    }

    fun getRecipe(name: String): Recipe? {
        val sql = "SELECT * FROM recipes WHERE LOWER(name) = LOWER(?)"
        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, name)
            val rs = statement.executeQuery()
            return if (rs.next()) {
                val recipeId = rs.getInt("id")
                val recipeName = rs.getString("name")
                val ingredients = getRecipeIngredients(recipeId)
                Recipe(id = recipeId, name = recipeName, ingredients = ingredients)
            } else {
                null
            }
        }
    }

    private fun getRecipeIngredients(recipeId: Int): List<RecipeIngredient> {
        val sql = """
            SELECT i.*, ri.servings
            FROM recipe_ingredients ri
            JOIN ingredients i ON ri.ingredient_id = i.id
            WHERE ri.recipe_id = ?
        """
        connection.prepareStatement(sql).use { statement ->
            statement.setInt(1, recipeId)
            val rs = statement.executeQuery()
            val ingredients = mutableListOf<RecipeIngredient>()
            while (rs.next()) {
                val ingredient =
                    Ingredient(
                        id = rs.getInt("id"),
                        name = rs.getString("name"),
                        servingSize = rs.getDouble("serving_size"),
                        servingUnit = rs.getString("serving_unit"),
                        servingWeightGrams = rs.getObject("serving_weight_grams") as Double?,
                        calories = rs.getDouble("calories"),
                        protein = rs.getDouble("protein"),
                        fat = rs.getDouble("fat"),
                        carbs = rs.getDouble("carbs"),
                    )
                ingredients.add(RecipeIngredient(ingredient, rs.getDouble("servings")))
            }
            return ingredients
        }
    }

    fun close() {
        connection.close()
    }
}
