#!/bin/bash

# Create database using SQLite CLI
sqlite3 nutrition.db < create_database.sql

echo "Database created successfully!"
