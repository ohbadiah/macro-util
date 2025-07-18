# Nutrition Tracker CLI Project

## Project Overview
A Kotlin CLI application for comprehensive nutrition tracking including recipes and daily food journaling. The app uses SQLite for persistence and integrates with the Nutritionix API for automatic ingredient lookup with enhanced search capabilities. Built with a clean architecture separating concerns across models, database, API, core logic, and CLI layers.

## Tech Stack
- **Language**: Kotlin 2.2.0
- **Build Tool**: Gradle with Kotlin DSL
- **Database**: SQLite (via xerial JDBC)
- **HTTP Client**: OkHttp 4.11.0
- **JSON**: Gson 2.10.1
- **CLI Framework**: Clikt 4.2.0
- **JVM Target**: Java 24

## Project Structure
```
nutrition-tracker/
├── build.gradle.kts              # Gradle build configuration
├── create_database.sql           # SQL schema definition
├── setup_database.sh            # Database initialization script
├── config.properties            # API credentials (git-ignored)
├── nutrition.db                 # SQLite database (git-ignored)
└── src/main/kotlin/com/nutrition/
    ├── Main.kt                  # Entry point with config loading
    ├── models/
    │   └── Models.kt           # Data classes for domain objects
    ├── database/
    │   └── DatabaseManager.kt  # SQLite operations
    ├── api/
    │   └── NutritionixClient.kt # External API integration
    ├── core/
    │   └── NutritionCalculator.kt # Business logic
    └── cli/
        └── Commands.kt         # CLI command implementations
```

## Commands

### Build & Run
```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Recipe Commands
./gradlew run --args="create"                    # Create new recipe
./gradlew run --args="show 'Recipe Name'"        # Show recipe nutrition
./gradlew run --args="list"                      # List all recipes
./gradlew run --args="rename 'Old' 'New'"        # Rename recipe
./gradlew run --args="delete 'Recipe Name'"      # Delete recipe

# Food Journal Commands
./gradlew run --args="journal"                   # Add to today's journal
./gradlew run --args="journal 2025-07-15"        # Add to specific date
./gradlew run --args="journal yesterday"         # Add to yesterday
./gradlew run --args="show-journal"              # Show today's journal
./gradlew run --args="show-journal 2025-07-15"   # Show specific date journal
./gradlew run --args="reset-journal"             # Clear today's journal
./gradlew run --args="reset-journal 2025-07-15"  # Clear specific date journal

# Clean build artifacts
./gradlew clean
```

### Database Setup
```bash
# Create database with schema
./setup_database.sh

# Or manually with SQLite
sqlite3 nutrition.db < create_database.sql
```

## Code Style & Conventions

### Kotlin Style
- Use 4 spaces for indentation
- Follow Kotlin official coding conventions
- Prefer data classes for DTOs
- Use meaningful variable names (except for simple loops: i, j, k)
- Group imports by package

### Architecture Guidelines
- **Models**: Pure data classes with no logic
- **Database**: All SQL operations isolated in DatabaseManager
- **API**: External service calls in dedicated client classes
- **Core**: Business logic separate from I/O
- **CLI**: Command handling and user interaction

### Error Handling
- Return null for "not found" scenarios (ingredients, recipes)
- Print user-friendly error messages for API failures
- Use try-catch with resources (`.use {}`) for connections
- Always close database connections properly

## Database Schema

### Tables
1. **ingredients**: Nutrition data per serving
   - id, name (unique), serving_size, serving_unit, serving_weight_grams
   - calories, protein, fat, carbs

2. **recipes**: Recipe names
   - id, name (unique)

3. **recipe_ingredients**: Junction table
   - recipe_id, ingredient_id, servings

4. **food_journals**: Daily nutrition tracking
   - id, date (unique)

5. **journal_entries**: Individual food entries per day
   - id, journal_id, entry_type ('recipe' or 'ingredient')
   - recipe_id, ingredient_id, servings

### Conventions
- Case-insensitive name matching for user convenience
- Decimal servings allowed (e.g., 0.5, 1.5)
- Weight-based serving input supported (e.g., 150g, 5oz)
- All nutrition values stored as REAL (double)
- Journal entries track both recipes and individual ingredients

## API Integration

### Nutritionix API
- Endpoint: `https://trackapi.nutritionix.com/v2/natural/nutrients`
- Authentication: x-app-id and x-app-key headers
- Request format: `{"query": "ingredient name"}`
- Enhanced search: Returns up to 15 results prioritizing USDA/generic foods
- Hybrid search: Shows common foods first, then branded alternatives
- Caches results locally to minimize API calls

### Error Handling
- Log API errors with status code and message
- Return null on failure (don't crash)
- "None of these" option when search results don't match
- Prompt user for alternative ingredient names

## Nutrition Calculations
- **Caloric values**: 9 cal/g fat, 4 cal/g protein, 4 cal/g carbs
- **Percentages**: Calculate based on caloric contribution, not weight
- **Aggregation**: Sum nutrition values × servings for each ingredient
- **Weight conversion**: Automatic conversion from weight inputs (g, oz) to servings
- **Display**: Show both absolute values and percentage of calories
- **Daily totals**: Aggregate nutrition across all journal entries for a date

## Testing Approach
- Unit tests for NutritionCalculator logic
- Integration tests for database operations
- Mock NutritionixClient for API tests
- CLI commands tested with test databases

## Common Tasks

## Important Notes
- **Credentials**: Never commit config.properties with real API keys (oops, Nick may have already done this, but can be fixed later)
- **Database**: The .db file should be git-ignored
- **Servings**: Can be fractional (0.5, 1.25, etc.) or weight-based (150g, 5oz)
- **Names**: Ingredient/recipe names are case-insensitive
- **Whitespace**: Run ktlint for consistent formatting
- **Journal dates**: Support flexible formats (today, yesterday, YYYY-MM-DD)

## Key Features Implemented
- **Recipe Management**: Full CRUD operations (create, show, list, rename, delete)
- **Food Journaling**: Daily nutrition tracking with date-based entries
- **Enhanced Search**: Improved ingredient lookup with USDA food prioritization
- **Weight-Based Input**: Support for gram/ounce inputs with automatic conversion
- **Daily Summaries**: Complete nutrition breakdowns for journal entries
- **MCP Server**: Model Context Protocol server for AI integration

## MCP Server Usage

The application now includes an MCP (Model Context Protocol) server that provides AI-friendly access to all nutrition tracking functionality.

### Starting the MCP Server
```bash
# Build the application
./gradlew build

# Start in MCP server mode
./build/install/macro-util/bin/macro-util mcp-server
```

### Available MCP Tools

The MCP server exposes the following tools for AI integration:

1. **list_recipes()** - List all available recipes
2. **show_recipe(recipe_name)** - Display recipe nutrition information
3. **create_recipe(name, ingredients)** - Create new recipe with ingredients
4. **search_ingredient(query)** - Search Nutritionix API for ingredients
5. **add_ingredient_to_journal(food_name, servings, date?)** - Add ingredient to journal
6. **add_recipe_to_journal(recipe_name, servings, date?)** - Add recipe to journal
7. **get_journal_summary(date?)** - Get nutrition summary for a date
8. **reset_journal(date?)** - Clear journal entries for a date

### Using with Claude

Once the MCP server is running, you can use natural language to interact with your nutrition data:

- "I had a banana blueberry protein shake and some oatmeal for breakfast"
- "Show me yesterday's nutrition summary"
- "Create a recipe for my morning smoothie with banana, protein powder, and almond milk"
- "Search for chicken breast nutrition information"

The MCP server provides structured, reliable access to all nutrition tracking functionality while maintaining the same business logic as the CLI.

### Dependencies

The MCP server uses:
- `org.jetbrains.kotlinx:kotlinx-mcp-sdk:0.1.0` from JetBrains Space
- Standard I/O transport for communication
- JSON serialization for tool parameters and responses

## Future Enhancements (not implemented)
- Web interface
- Export functionality
- Nutrition goal tracking
