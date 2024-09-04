package com.example.faloucomproucerto

import CartAdapter
import Product
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CartActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CartAdapter
    private val cartProducts = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = CartAdapter(cartProducts) { product ->
            removeFromCart(product)
        }
        recyclerView.adapter = adapter

        // Carregar produtos do carrinho
        loadCartProducts()
    }

    private fun loadCartProducts() {
        // Código para carregar os produtos do carrinho
    }

    private fun removeFromCart(product: Product) {
        // Código para remover o produto do carrinho
        cartProducts.remove(product)
        adapter.notifyDataSetChanged()
    }
}
