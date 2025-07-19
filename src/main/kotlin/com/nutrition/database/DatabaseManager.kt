package com.nutrition.database

import com.nutrition.models.*
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate

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
                name TEXT UNIQUE NOT NULL,
                servings REAL NOT NULL DEFAULT 1
            );

            CREATE TABLE IF NOT EXISTS recipe_ingredients (
                recipe_id INTEGER NOT NULL,
                ingredient_id INTEGER NOT NULL,
                servings REAL NOT NULL,
                FOREIGN KEY (recipe_id) REFERENCES recipes(id),
                FOREIGN KEY (ingredient_id) REFERENCES ingredients(id),
                PRIMARY KEY (recipe_id, ingredient_id)
            );

            CREATE TABLE IF NOT EXISTS food_journals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT UNIQUE NOT NULL
            );

            CREATE TABLE IF NOT EXISTS journal_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                journal_id INTEGER NOT NULL,
                entry_type TEXT NOT NULL,
                recipe_id INTEGER,
                ingredient_id INTEGER,
                servings REAL NOT NULL,
                name TEXT NOT NULL,
                calories REAL NOT NULL,
                protein REAL NOT NULL,
                fat REAL NOT NULL,
                carbs REAL NOT NULL,
                FOREIGN KEY (journal_id) REFERENCES food_journals(id),
                FOREIGN KEY (recipe_id) REFERENCES recipes(id),
                FOREIGN KEY (ingredient_id) REFERENCES ingredients(id)
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
        val sql = "INSERT INTO recipes (name, servings) VALUES (?, ?)"
        connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS).use { statement ->
            statement.setString(1, recipe.name)
            statement.setDouble(2, recipe.servings)
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
                val servings = rs.getDouble("servings")
                val ingredients = getRecipeIngredients(recipeId)
                Recipe(id = recipeId, name = recipeName, servings = servings, ingredients = ingredients)
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

    fun getAllRecipes(): List<Recipe> {
        val sql = "SELECT * FROM recipes ORDER BY name"
        connection.prepareStatement(sql).use { statement ->
            val rs = statement.executeQuery()
            val recipes = mutableListOf<Recipe>()
            while (rs.next()) {
                val recipeId = rs.getInt("id")
                val recipeName = rs.getString("name")
                val servings = rs.getDouble("servings")
                val ingredients = getRecipeIngredients(recipeId)
                recipes.add(Recipe(id = recipeId, name = recipeName, servings = servings, ingredients = ingredients))
            }
            return recipes
        }
    }

    fun renameRecipe(oldName: String, newName: String): Boolean {
        val sql = "UPDATE recipes SET name = ? WHERE LOWER(name) = LOWER(?)"
        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, newName)
            statement.setString(2, oldName)
            return statement.executeUpdate() > 0
        }
    }

    fun deleteRecipe(name: String): Boolean {
        // First get the recipe ID
        val recipe = getRecipe(name) ?: return false
        
        // Delete recipe ingredients first (foreign key constraint)
        val deleteIngredientsSQL = "DELETE FROM recipe_ingredients WHERE recipe_id = ?"
        connection.prepareStatement(deleteIngredientsSQL).use { statement ->
            statement.setInt(1, recipe.id)
            statement.executeUpdate()
        }
        
        // Then delete the recipe
        val deleteRecipeSQL = "DELETE FROM recipes WHERE id = ?"
        connection.prepareStatement(deleteRecipeSQL).use { statement ->
            statement.setInt(1, recipe.id)
            return statement.executeUpdate() > 0
        }
    }

    fun saveJournal(journal: FoodJournal): FoodJournal {
        // First, create or get the journal for the date
        val dateString = journal.date.toString()
        val journalId = getOrCreateJournalId(dateString)
        
        // Save each entry
        journal.entries.forEach { entry ->
            saveJournalEntry(journalId, entry)
        }
        
        return journal.copy(id = journalId)
    }
    
    private fun getOrCreateJournalId(dateString: String): Int {
        // Try to get existing journal
        val selectSql = "SELECT id FROM food_journals WHERE date = ?"
        connection.prepareStatement(selectSql).use { statement ->
            statement.setString(1, dateString)
            val rs = statement.executeQuery()
            if (rs.next()) {
                return rs.getInt("id")
            }
        }
        
        // Create new journal
        val insertSql = "INSERT INTO food_journals (date) VALUES (?)"
        connection.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS).use { statement ->
            statement.setString(1, dateString)
            statement.executeUpdate()
            val generatedKeys = statement.generatedKeys
            generatedKeys.next()
            return generatedKeys.getInt(1)
        }
    }
    
    private fun saveJournalEntry(journalId: Int, entry: JournalEntry) {
        val sql = """
            INSERT INTO journal_entries 
            (journal_id, entry_type, recipe_id, ingredient_id, servings, name, calories, protein, fat, carbs)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
        connection.prepareStatement(sql).use { statement ->
            statement.setInt(1, journalId)
            statement.setString(2, entry.type.name)
            
            // Set recipe_id or ingredient_id based on type (for future reference)
            if (entry.type == EntryType.RECIPE) {
                statement.setNull(3, java.sql.Types.INTEGER)
                statement.setNull(4, java.sql.Types.INTEGER)
            } else {
                statement.setNull(3, java.sql.Types.INTEGER)
                statement.setNull(4, java.sql.Types.INTEGER)
            }
            
            statement.setDouble(5, entry.servings)
            statement.setString(6, entry.name)
            statement.setDouble(7, entry.calories)
            statement.setDouble(8, entry.protein)
            statement.setDouble(9, entry.fat)
            statement.setDouble(10, entry.carbs)
            statement.executeUpdate()
        }
    }
    
    fun getJournalByDate(date: LocalDate): FoodJournal? {
        val dateString = date.toString()
        val sql = "SELECT id FROM food_journals WHERE date = ?"
        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, dateString)
            val rs = statement.executeQuery()
            if (rs.next()) {
                val journalId = rs.getInt("id")
                val entries = getJournalEntries(journalId)
                return FoodJournal(id = journalId, date = date, entries = entries)
            }
        }
        return null
    }
    
    private fun getJournalEntries(journalId: Int): List<JournalEntry> {
        val sql = """
            SELECT id, entry_type, servings, name, calories, protein, fat, carbs
            FROM journal_entries 
            WHERE journal_id = ?
            ORDER BY id
        """
        connection.prepareStatement(sql).use { statement ->
            statement.setInt(1, journalId)
            val rs = statement.executeQuery()
            val entries = mutableListOf<JournalEntry>()
            while (rs.next()) {
                entries.add(JournalEntry(
                    id = rs.getInt("id"),
                    type = EntryType.valueOf(rs.getString("entry_type")),
                    name = rs.getString("name"),
                    servings = rs.getDouble("servings"),
                    calories = rs.getDouble("calories"),
                    protein = rs.getDouble("protein"),
                    fat = rs.getDouble("fat"),
                    carbs = rs.getDouble("carbs"),
                ))
            }
            return entries
        }
    }
    
    fun addJournalEntry(date: LocalDate, entry: JournalEntry) {
        val dateString = date.toString()
        val journalId = getOrCreateJournalId(dateString)
        saveJournalEntry(journalId, entry)
    }

    fun resetJournal(date: LocalDate): Boolean {
        val dateString = date.toString()
        val sql = "SELECT id FROM food_journals WHERE date = ?"
        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, dateString)
            val rs = statement.executeQuery()
            if (rs.next()) {
                val journalId = rs.getInt("id")
                
                // Delete all entries for this journal
                val deleteEntriesSQL = "DELETE FROM journal_entries WHERE journal_id = ?"
                connection.prepareStatement(deleteEntriesSQL).use { deleteStatement ->
                    deleteStatement.setInt(1, journalId)
                    deleteStatement.executeUpdate()
                }
                
                // Optionally delete the journal record itself if no entries remain
                val deleteJournalSQL = "DELETE FROM food_journals WHERE id = ?"
                connection.prepareStatement(deleteJournalSQL).use { deleteStatement ->
                    deleteStatement.setInt(1, journalId)
                    return deleteStatement.executeUpdate() > 0
                }
            }
        }
        return false // No journal found for this date
    }

    fun close() {
        connection.close()
    }
}
