# Nutrition Tracker CLI Project

## Project Overview
A Kotlin CLI application for tracking nutrition information for recipes. The app uses SQLite for persistence and integrates with the Nutritionix API for automatic ingredient lookup. Built with a clean architecture separating concerns across models, database, API, core logic, and CLI layers.

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

# Show a recipe
./gradlew run --args="show 'Recipe Name'"

# Create a new recipe
./gradlew run --args="create"

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
   - id, name (unique), serving_size, serving_unit
   - calories, protein, fat, carbs

2. **recipes**: Recipe names
   - id, name (unique)

3. **recipe_ingredients**: Junction table
   - recipe_id, ingredient_id, servings

### Conventions
- Case-insensitive name matching for user convenience
- Decimal servings allowed (e.g., 0.5, 1.5)
- All nutrition values stored as REAL (double)

## API Integration

### Nutritionix API
- Endpoint: `https://trackapi.nutritionix.com/v2/natural/nutrients`
- Authentication: x-app-id and x-app-key headers
- Request format: `{"query": "ingredient name"}`
- Caches results locally to minimize API calls

### Error Handling
- Log API errors with status code and message
- Return null on failure (don't crash)
- Prompt user for alternative ingredient names

## Nutrition Calculations
- **Caloric values**: 9 cal/g fat, 4 cal/g protein, 4 cal/g carbs
- **Percentages**: Calculate based on caloric contribution, not weight
- **Aggregation**: Sum nutrition values × servings for each ingredient
- **Display**: Show both absolute values and percentage of calories

## Testing Approach
- Unit tests for NutritionCalculator logic
- Integration tests for database operations
- Mock NutritionixClient for API tests
- CLI commands tested with test databases

## Common Tasks

## Important Notes
- **Credentials**: Never commit config.properties with real API keys (oops, Nick may have already done this, but can be fixed later)
- **Database**: The .db file should be git-ignored
- **Servings**: Can be fractional (0.5, 1.25, etc.)
- **Names**: Ingredient/recipe names are case-insensitive
- **Whitespace**: Run ktlint for consistent formatting

## Future Enhancements (not implemented)
- Food journaling
- Web interface
