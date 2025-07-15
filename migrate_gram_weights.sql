-- Migration script to add serving_weight_grams column to existing database
-- Run this if you have an existing database without the gram weights column

ALTER TABLE ingredients ADD COLUMN serving_weight_grams REAL;

-- Update common ingredients with estimated gram weights
-- These are rough estimates for ingredients that commonly don't have gram weights from API

UPDATE ingredients SET serving_weight_grams = 85 WHERE serving_unit LIKE '%breast%' AND serving_weight_grams IS NULL;
UPDATE ingredients SET serving_weight_grams = 240 WHERE serving_unit LIKE '%cup%' AND serving_weight_grams IS NULL;
UPDATE ingredients SET serving_weight_grams = 28 WHERE serving_unit LIKE '%oz%' AND serving_weight_grams IS NULL;
UPDATE ingredients SET serving_weight_grams = 30 WHERE serving_unit LIKE '%slice%' AND serving_weight_grams IS NULL;
UPDATE ingredients SET serving_weight_grams = 150 WHERE serving_unit LIKE '%medium%' AND serving_weight_grams IS NULL;
UPDATE ingredients SET serving_weight_grams = 15 WHERE serving_unit LIKE '%tbsp%' AND serving_weight_grams IS NULL;
UPDATE ingredients SET serving_weight_grams = 5 WHERE serving_unit LIKE '%tsp%' AND serving_weight_grams IS NULL;

-- Display results
SELECT name, serving_size, serving_unit, serving_weight_grams FROM ingredients ORDER BY name;