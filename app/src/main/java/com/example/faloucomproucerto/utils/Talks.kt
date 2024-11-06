package com.example.faloucomproucerto.utils

class Talks {

    fun converteFalas(fala: String): String {

        when("fala")
        {
            "Main" -> return "Bem vindo ao Falou Comprou! Nesta tela basta apenas falar 'Entrar', para começar suas compras!"
            "Home" -> return "Você está na tela de menu! Nesta tela você tem as opções ! comprar para ler o qrcode e compras para ir ao carrinho!"
            "Cart" -> return "Esta é a tela de carrinho, nela você poderá escolher a opções,  produtos para listar o produtos e seus preços, total para falar o preço total do carrinho, repitira para repitir a instrução e voltar para voltar a tela anterior!"
            else -> return "A"
        }

    }

}