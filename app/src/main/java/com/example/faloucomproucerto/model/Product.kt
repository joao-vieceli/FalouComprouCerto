package com.example.faloucomproucerto.model

import java.io.Serializable
import java.text.NumberFormat
import java.util.Locale

data class Product(
    val id: String = "",
    val nome: String = "",
    val preco: Double = 0.0,
    val imageUrl: String = "",
    val codigoBarras: String = "",
    var quantidade: Int = 1 // Adiciona a propriedade quantidade
) : Serializable {

    init {
        require(quantidade >= 0) { "A quantidade não pode ser negativa." }
    }

    // Formata o preço para exibição
    fun getFormattedPrice(): String {
        val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return format.format(preco)
    }

    fun incrementarQuantidade() {
        quantidade++
    }

    fun decrementarQuantidade() {
        if (quantidade > 0) quantidade-- // Previne que a quantidade fique negativa
    }

    override fun toString(): String {
        return "Product(id='$id', nome='$nome', preco=$preco, imageUrl='$imageUrl', codigoBarras='$codigoBarras', quantidade=$quantidade)"
    }
}
