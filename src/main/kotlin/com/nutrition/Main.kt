// src/main/kotlin/com/nutrition/Main.kt
package com.nutrition

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.nutrition.api.NutritionixClient
import com.nutrition.cli.CreateRecipeCommand
import com.nutrition.cli.DeleteRecipeCommand
import com.nutrition.cli.JournalCommand
import com.nutrition.cli.ListRecipesCommand
import com.nutrition.cli.RenameRecipeCommand
import com.nutrition.cli.ResetJournalCommand
import com.nutrition.cli.ShowRecipeCommand
import com.nutrition.database.DatabaseManager
import com.nutrition.mcp.MacroUtilMCPServer
import java.io.FileInputStream
import java.util.Properties

class NutritionApp : CliktCommand() {
    private val mcpServer by option("--mcp-server", help = "Run as MCP server").flag(default = false)
    
    override fun run() {
        if (mcpServer) {
            println("Starting MCP server mode...")
            val config = loadConfig()
            val databasePath = config.getProperty("database.path", "nutrition.db")
            val db = DatabaseManager(databasePath)
            val appId = config.getProperty("nutritionix.app.id")
            val appKey = config.getProperty("nutritionix.app.key")
            
            if (appId == null || appKey == null) {
                println("Error: Nutritionix API credentials not found in config.properties")
                return
            }
            
            val nutritionix = NutritionixClient(appId, appKey)
            val server = MacroUtilMCPServer(db, nutritionix)
            
            try {
                server.start()
            } finally {
                db.close()
            }
        }
    }
}

fun loadConfig(): Properties {
    val properties = Properties()
    try {
        FileInputStream("config.properties").use { input ->
            properties.load(input)
        }
    } catch (e: Exception) {
        println("Warning: Could not load config.properties. Using default values.")
        // Set defaults or throw exception
        properties.setProperty("nutritionix.app.id", "default-id")
        properties.setProperty("nutritionix.app.key", "default-key")
        properties.setProperty("database.path", "nutrition.db")
    }
    return properties
}

fun main(args: Array<String>) {
    val config = loadConfig()

    val databasePath = config.getProperty("database.path", "nutrition.db")
    val db = DatabaseManager(databasePath)

    val appId = config.getProperty("nutritionix.app.id")
    val appKey = config.getProperty("nutritionix.app.key")

    if (appId == null || appKey == null) {
        println("Error: Nutritionix API credentials not found in config.properties")
        return
    }

    val nutritionix = NutritionixClient(appId, appKey)

    try {
        NutritionApp()
            .subcommands(
                ListRecipesCommand(db),
                ShowRecipeCommand(db),
                CreateRecipeCommand(db, nutritionix),
                RenameRecipeCommand(db),
                DeleteRecipeCommand(db),
                JournalCommand(db, nutritionix),
                ResetJournalCommand(db),
            ).main(args)
    } finally {
        db.close()
    }
}
