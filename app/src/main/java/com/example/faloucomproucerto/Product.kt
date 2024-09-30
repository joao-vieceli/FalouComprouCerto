package com.example.faloucomproucerto

import java.io.Serializable

data class Product(
    val id: String = "",
    val nome: String = "",
    val preco: Double = 0.0,
    val imageUrl: String = "",
    val codigoBarras: String = ""
) : Serializable
