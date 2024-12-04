package com.example.faloucomproucerto.utils

class Talks {

    fun converteFalas(fala: String): String {

        when(fala)
        {
            "Main" -> return "Bem vindo ao Falou Comprou!"
            "Home" -> return "Você está na tela de menu!"
            "Cart" -> return "Esta é a tela de carrinho!"
            else -> return "A"
        }

    }

}