package com.example.faloucomproucerto

import com.example.faloucomproucerto.utils.SpaceItemDecoration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.faloucomproucerto.adapter.CartAdapter
import com.example.faloucomproucerto.model.Product
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CartActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CartAdapter
    private val cartProducts = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        val spacingInPixels = resources.getDimensionPixelSize((R.dimen.item_spacing))

        recyclerView = findViewById(R.id.cart_recycler_view)
        recyclerView.addItemDecoration(SpaceItemDecoration(spacingInPixels))

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = CartAdapter(cartProducts) { product ->
            removeFromCart(product)
        }
        recyclerView.adapter = adapter

        // Carregar produtos do carrinho
        loadCartProducts()
    }

    private fun loadCartProducts() {
        val database = FirebaseDatabase.getInstance().reference
        database.child("carrinho").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cartProducts.clear() // Limpa a lista antes de carregar os novos produtos
                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    if (product != null) {
                        cartProducts.add(product) // Adiciona cada produto à lista
                    }
                }
                adapter.notifyDataSetChanged() // Notifica o adaptador sobre a mudança
            }

            override fun onCancelled(error: DatabaseError) {
                // Tratamento de erro, se necessário
            }
        })
    }

    private fun removeFromCart(product: Product) {
        val database = FirebaseDatabase.getInstance().reference
        database.child("carrinho").child(product.id).removeValue()
            .addOnSuccessListener {
                // Remover da lista local após sucesso no banco de dados
                cartProducts.remove(product)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                // Tratar falha, se necessário
            }
    }
}
