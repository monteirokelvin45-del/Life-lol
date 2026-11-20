package com.lifeos.core.domain.algorithm

import java.util.Locale

data class ParsedQuickTransaction(
    val amount: Double,
    val type: String, // "EXPENSE" | "INCOME"
    val category: String,
    val paymentMethod: String,
    val note: String
)

/**
 * Parser PLN ultrarrápido (<1ms) para registro financeiro em 2 segundos por voz ou texto:
 * Exemplo: `"Almoço 38 débito"`, `"Uber 250 mpesa"`, `"Salário 65000 transferência"`.
 */
object QuickFinanceNlpParser {

    private val numberRegex = Regex("""(\d+(?:[.,]\d{1,2})?)""")

    fun parse(rawInput: String): ParsedQuickTransaction? {
        val cleaned = rawInput.trim()
        if (cleaned.isBlank()) return null

        val match = numberRegex.find(cleaned) ?: return null
        val amount = match.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        val lower = cleaned.lowercase(Locale.ROOT)

        val paymentMethod = when {
            "mpesa" in lower || "m-pesa" in lower -> "M-Pesa"
            "emola" in lower || "e-mola" in lower -> "e-Mola"
            "pix" in lower -> "PIX"
            "débito" in lower || "debito" in lower -> "Débito"
            "crédito" in lower || "credito" in lower || "cartão" in lower || "cartao" in lower -> "Cartão"
            "dinheiro" in lower || "numerário" in lower || "numerario" in lower -> "Numerário"
            "transf" in lower -> "Transferência"
            else -> "Débito"
        }

        val isIncome = listOf("salário", "salario", "receita", "freela", "bônus", "bonus", "entrada", "dividendo")
            .any { it in lower }

        val category = when {
            isIncome -> "Receita & Renda"
            listOf("almoço", "almoco", "jantar", "lanche", "café", "cafe", "ifood", "comida", "mercado", "restaurante", "padaria").any { it in lower } -> "Alimentação"
            listOf("uber", "táxi", "taxi", "combustível", "combustivel", "gasolina", "chapa", "transporte").any { it in lower } -> "Transporte"
            listOf("aluguel", "renda", "luz", "água", "agua", "internet", "energia", "casa", "moradia").any { it in lower } -> "Moradia"
            listOf("livro", "curso", "faculdade", "concurso", "educação", "educacao").any { it in lower } -> "Livros & Educação"
            listOf("whey", "creatina", "academia", "farmácia", "farmacia", "saúde", "saude", "proteína").any { it in lower } -> "Nutrição & Saúde"
            else -> "Geral"
        }

        val strippedNote = cleaned
            .replace(match.value, "")
            .replace(Regex("""(?i)\b(m-?pesa|e-?mola|pix|d[ée]bito|cr[ée]dito|cart[ãa]o|reais|mt|mzn|r\$)\b"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .ifBlank { category }

        return ParsedQuickTransaction(
            amount = amount,
            type = if (isIncome) "INCOME" else "EXPENSE",
            category = category,
            paymentMethod = paymentMethod,
            note = strippedNote
        )
    }
}
