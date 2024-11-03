package com.example.faloucomproucerto

import com.example.faloucomproucerto.R
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
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.app.AlertDialog
import android.content.Intent

class CartActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CartAdapter
    private val cartProducts = mutableListOf<Product>()
    private lateinit var totalPriceTextView: TextView
    private lateinit var finalizeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.item_spacing)
        recyclerView = findViewById(R.id.cart_recycler_view)
        recyclerView.addItemDecoration(SpaceItemDecoration(spacingInPixels))
        recyclerView.layoutManager = LinearLayoutManager(this)

        totalPriceTextView = findViewById(R.id.total_price)
        finalizeButton = findViewById(R.id.finalize_button)

        adapter = CartAdapter(cartProducts,
            { product -> removeFromCart(product) },
            { product -> increaseQuantity(product) },
            { product -> decreaseQuantity(product) }
        )
        recyclerView.adapter = adapter

        loadCartProducts()

        // Configurar o clique no botão "Finalizar Compra"
        finalizeButton.setOnClickListener {
            finalizePurchase()
        }
    }

    private fun finalizePurchase() {
        // Crie um AlertDialog para confirmar a finalização da compra
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Finalizar Compra")
        builder.setMessage("Você realmente deseja finalizar a compra?")

        builder.setPositiveButton("Sim") { dialog, which ->
            // Se o usuário clicar em "Sim", finalize a compra
            val totalPrice = cartProducts.sumOf { it.preco * it.quantidade }
            val database = FirebaseDatabase.getInstance().reference
            database.child("carrinho").removeValue()
                .addOnSuccessListener {
                    cartProducts.clear() // Limpa a lista local
                    adapter.notifyDataSetChanged() // Atualiza o RecyclerView
                    updateTotalPrice() // Atualiza o valor total para zero

                    // Exibe uma mensagem de confirmação e retorna à home
                    Toast.makeText(this, "Compra finalizada no valor: R$ %.2f".format(totalPrice), Toast.LENGTH_SHORT).show()

                    // Retorna para a tela inicial ou atividade desejada
                    val intent = Intent(this, MainActivity::class.java) // Substitua MainActivity pela sua tela inicial
                    startActivity(intent)
                    finish() // Finaliza a CartActivity
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao finalizar a compra", Toast.LENGTH_SHORT).show()
                }
        }

        builder.setNegativeButton("Não") { dialog, which ->
            dialog.dismiss() // Apenas fecha o diálogo
        }

        // Cria e exibe o diálogo
        val dialog = builder.create()
        dialog.show()
    }

    private fun loadCartProducts() {
        val database = FirebaseDatabase.getInstance().reference
        database.child("carrinho").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cartProducts.clear()
                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    if (product != null) {
                        cartProducts.add(product)
                    }
                }
                adapter.notifyDataSetChanged()
                updateTotalPrice()
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
                cartProducts.remove(product)
                adapter.notifyDataSetChanged()
                updateTotalPrice()
            }
    }

    private fun increaseQuantity(product: Product) {
        val position = cartProducts.indexOf(product)
        product.quantidade++
        updateCartProduct(product)
        adapter.notifyItemChanged(position)
        updateTotalPrice()
    }

    private fun decreaseQuantity(product: Product) {
        val position = cartProducts.indexOf(product)
        if (product.quantidade > 1) {
            product.quantidade--
            updateCartProduct(product)
            adapter.notifyItemChanged(position)
            updateTotalPrice()
        } else if (product.quantidade == 1) {
            removeFromCart(product)
        }
    }

    private fun updateCartProduct(product: Product) {
        val database = FirebaseDatabase.getInstance().reference
        database.child("carrinho").child(product.id).setValue(product)
            .addOnSuccessListener {
                // Não é necessário chamar notifyDataSetChanged()
            }
    }

    private fun updateTotalPrice() {
        val totalPrice = cartProducts.sumOf { it.preco * it.quantidade }
        totalPriceTextView.text = "Total: R$ %.2f".format(totalPrice)
    }
}
