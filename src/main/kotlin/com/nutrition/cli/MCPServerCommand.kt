package com.nutrition.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.nutrition.api.NutritionixClient
import com.nutrition.database.DatabaseManager
import com.nutrition.mcp.MacroUtilMCPServer

class MCPServerCommand(
    private val db: DatabaseManager,
    private val nutritionix: NutritionixClient,
) : CliktCommand(name = "mcp-server", help = "Start MCP server for AI integration") {
    
    override fun run() {
        echo("Starting MCP server mode...")
        val server = MacroUtilMCPServer(db, nutritionix)
        server.start()
    }
}