# Nutrition Tracker CLI & MCP Server

A practical Kotlin application for tracking nutrition information in recipes and daily food journaling. Built to solve the real problem of calculating calories and macros without the tedious manual work of maintaining spreadsheets. Now with AI integration through MCP server support.

## Why This Exists

I was tracking my nutrition using a spreadsheet, manually looking up ingredient data and calculating totals for each recipe. It worked, but updating it was a pain every time I wanted to try a new recipe or adjust portions. This CLI tool automates the tedious parts while keeping the flexibility I need.

**Built in 4 hours** to scratch my own itch - sometimes the best tools come from solving your own problems.

## What It Does

- **Recipe Management**: Create, view, and manage your recipes
- **Daily Food Journaling**: Track daily nutrition with date-based entries
- **Automatic Nutrition Lookup**: Integrates with Nutritionix API to get nutrition data automatically
- **Smart Calculations**: Handles fractional servings and calculates both absolute values and caloric percentages
- **Local Storage**: SQLite database keeps everything fast and offline-friendly
- **AI Integration**: MCP server enables natural language food tracking through AI assistants

## Quick Start

### CLI Mode
```bash
# Set up the database
./setup_database.sh

# Create your first recipe
./gradlew run --args="create"

# View a recipe with nutrition breakdown
./gradlew run --args="show 'Recipe Name'"

# Start food journaling
./gradlew run --args="journal"
```

### MCP Server Mode (AI Integration)
```bash
# Build the application
./gradlew build

# Start MCP server for AI integration
./build/install/macro-util/bin/macro-util mcp-server
```

## Core Features

### Recipe Management
Interactive process that guides you through:
- Adding ingredients (with automatic nutrition lookup)
- Specifying serving amounts (supports decimals like 0.5, 1.25)
- Calculating total nutrition automatically

### Food Journaling
- **Daily Tracking**: Record food intake with date-based entries
- **Recipe Integration**: Add recipes directly to journal
- **Flexible Dates**: Support for "today", "yesterday", or specific dates
- **Running Totals**: See cumulative nutrition throughout the day

### Nutrition Analysis
- **Macros**: Protein, fat, carbs with caloric percentages
- **Smart Percentages**: Based on caloric contribution (9 cal/g fat, 4 cal/g protein/carbs)
- **Flexible Servings**: Scale recipes up or down easily
- **Weight-Based Input**: Support for gram/ounce inputs with automatic conversion

### AI Integration (MCP Server)
- **Natural Language**: "I had oatmeal and eggs for breakfast"
- **All CLI Functions**: Complete access to recipes and journaling
- **Structured Data**: JSON responses for reliable integration
- **Stateless Operations**: Each request is independent

## Technical Details

- **Language**: Kotlin 2.2.0
- **Database**: SQLite for fast, local storage
- **API**: Nutritionix for ingredient data
- **CLI**: Clikt framework for clean command handling

## Commands

### Build & Test
```bash
./gradlew build
./gradlew test
```

### Recipe Operations
```bash
./gradlew run --args="create"                    # Create new recipe
./gradlew run --args="show 'Recipe Name'"        # View recipe nutrition
./gradlew run --args="list"                      # List all recipes
```

### Food Journaling
```bash
./gradlew run --args="journal"                   # Add to today's journal
./gradlew run --args="journal yesterday"         # Add to yesterday
./gradlew run --args="reset-journal"             # Clear today's journal
```

### MCP Server
```bash
./build/install/macro-util/bin/macro-util mcp-server  # Start MCP server
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
- **API**: External service integration (Nutritionix)
- **Core**: Business logic and calculations
- **CLI**: User interaction
- **MCP**: AI integration server

Built for maintainability and testing, because even quick solutions should be done right.

## MCP Integration

The MCP server provides 8 tools for AI integration:
- Recipe management (list, show, create)
- Ingredient search and nutrition lookup
- Daily food journaling with date support
- Journal summaries and reset functionality

Perfect for natural language food tracking: *"I had a protein shake and oatmeal for breakfast"* → structured nutrition data.