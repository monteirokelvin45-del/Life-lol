package com.lifeos.core.ai.schema

/**
 * JSON Schema Draft-07 estrito exigido pelo Pilar 5.1 para ingestão documental estruturada.
 */
object LifeOsExtractionSchema {

    const val STRICT_JSON_SCHEMA: String = """
{
  "${'$'}schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "detected_habits": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "name": { "type": "string" },
          "category": { "type": "string", "enum": ["HEALTH", "STUDY", "FINANCE", "MIND"] },
          "hex_color": { "type": "string" },
          "target_days_mask": { "type": "integer" }
        },
        "required": ["name", "category", "hex_color", "target_days_mask"]
      }
    },
    "detected_workouts": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "name": { "type": "string" },
          "day_of_week": { "type": "integer", "minimum": 1, "maximum": 7 },
          "exercises": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "target_sets": { "type": "integer" },
                "target_reps": { "type": "integer" },
                "rest_seconds": { "type": "integer" }
              },
              "required": ["name", "target_sets", "target_reps", "rest_seconds"]
            }
          }
        },
        "required": ["name", "day_of_week", "exercises"]
      }
    },
    "detected_studies": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "subject": { "type": "string" },
          "topic": { "type": "string" }
        },
        "required": ["subject", "topic"]
      }
    },
    "detected_finances": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "amount": { "type": "number" },
          "type": { "type": "string", "enum": ["INCOME", "EXPENSE"] },
          "category": { "type": "string" }
        },
        "required": ["amount", "type", "category"]
      }
    }
  },
  "required": ["detected_habits", "detected_workouts", "detected_studies", "detected_finances"]
}
"""

    fun buildSystemPrompt(): String = """
Você é o motor de extração estruturada do Life OS.
Converta o documento bruto do usuário em um objeto JSON estritamente aderente ao schema abaixo.
Regras:
1. Retorne EXCLUSIVAMENTE JSON válido, sem markdown nem texto adicional.
2. `target_days_mask` usa bitmask: Seg=1, Ter=2, Qua=4, Qui=8, Sex=16, Sab=32, Dom=64 (Todos=127).
3. `hex_color` usa a paleta OLED: #10E05B (HEALTH), #8B5CF6 (STUDY), #00E5FF (FINANCE), #FF4800 (MIND).
4. `day_of_week` varia de 1 (Segunda) a 7 (Domingo).
Schema:
$STRICT_JSON_SCHEMA
""".trimIndent()
}
