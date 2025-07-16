# Nutrition Tracker CLI

A practical Kotlin CLI application for tracking nutrition information in recipes. Built to solve the real problem of calculating calories and macros without the tedious manual work of maintaining spreadsheets.

## Why This Exists

I was tracking my nutrition using a spreadsheet, manually looking up ingredient data and calculating totals for each recipe. It worked, but updating it was a pain every time I wanted to try a new recipe or adjust portions. This CLI tool automates the tedious parts while keeping the flexibility I need.

**Built in 4 hours** to scratch my own itch - sometimes the best tools come from solving your own problems.

## What It Does

- **Recipe Management**: Create, view, and manage your recipes
- **Automatic Nutrition Lookup**: Integrates with Nutritionix API to get nutrition data automatically
- **Smart Calculations**: Handles fractional servings and calculates both absolute values and caloric percentages
- **Local Storage**: SQLite database keeps everything fast and offline-friendly

## Quick Start

```bash
# Set up the database
./setup_database.sh

# Create your first recipe
./gradlew run --args="create"

# View a recipe with nutrition breakdown
./gradlew run --args="show 'Recipe Name'"
```

## Core Features

### Recipe Creation
Interactive process that guides you through:
- Adding ingredients (with automatic nutrition lookup)
- Specifying serving amounts (supports decimals like 0.5, 1.25)
- Calculating total nutrition automatically

### Nutrition Analysis
- **Macros**: Protein, fat, carbs with caloric percentages
- **Smart Percentages**: Based on caloric contribution (9 cal/g fat, 4 cal/g protein/carbs)
- **Flexible Servings**: Scale recipes up or down easily

### Ingredient Database
- Caches nutrition data locally to minimize API calls
- Case-insensitive searching for convenience
- Handles common ingredient variations

## Technical Details

- **Language**: Kotlin 2.2.0
- **Database**: SQLite for fast, local storage
- **API**: Nutritionix for ingredient data
- **CLI**: Clikt framework for clean command handling

## Commands

```bash
# Build and test
./gradlew build
./gradlew test

# Recipe operations
./gradlew run --args="create"                    # Create new recipe
./gradlew run --args="show 'Recipe Name'"        # View recipe nutrition
./gradlew run --args="list"                      # List all recipes
```

## Configuration

Add your Nutritionix API credentials to `config.properties`:
```
nutritionix.app.id=your_app_id
nutritionix.app.key=your_app_key
```

## Architecture

Clean separation of concerns across layers:
- **Models**: Data structures
- **Database**: SQLite operations
- **API**: External service integration
- **Core**: Business logic and calculations
- **CLI**: User interaction

Built for maintainability and testing, because even quick solutions should be done right.