package com.example.faloucomproucerto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton // Importação correta
import com.example.faloucomproucerto.model.Product
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var productImage: ImageView
    private lateinit var productName: TextView
    private lateinit var productPrice: TextView
    private lateinit var buttonAdd: AppCompatImageButton // Alterado para AppCompatImageButton
    private lateinit var buttonDecline: AppCompatImageButton // Alterado para AppCompatImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        // Inicialização dos elementos da UI
        productImage = findViewById(R.id.product_image)
        productName = findViewById(R.id.product_name)
        productPrice = findViewById(R.id.product_price)
        buttonAdd = findViewById(R.id.button_add)
        buttonDecline = findViewById(R.id.button_decline)

        // Recebe o produto da Intent
        val product = intent.getSerializableExtra("product") as? Product
        if (product != null) {
            // Exibe os dados do produto
            productName.text = product.nome
            productPrice.text = "R$ ${product.preco}"

            // Carrega a imagem usando Picasso
            if (product.imageUrl.isNotEmpty()) {
                Picasso.get()
                    .load(product.imageUrl)
                    .error(R.drawable.produtos)  // Imagem padrão caso ocorra erro ao carregar
                    .into(productImage)
            } else {
                productImage.setImageResource(R.drawable.produtos) // Imagem padrão se não houver URL
            }
        } else {
            Log.e("ProductDetailActivity", "Produto não encontrado na Intent.")
            finish() // Se não encontrar, fecha a atividade
        }

        // Configura o clique nos botões
        buttonAdd.setOnClickListener {
            product?.let { nonNullProduct ->
                addToCart(nonNullProduct)
                finish() // Volta para a MainActivity
            } ?: run {
                Toast.makeText(this, "Produto não encontrado.", Toast.LENGTH_SHORT).show()
            }
        }

        buttonDecline.setOnClickListener {
            // Mostra uma mensagem de confirmação ou feedback
            Toast.makeText(this, "Produto recusado.", Toast.LENGTH_SHORT).show()

            // Iniciar a HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP // Limpa as atividades anteriores
            startActivity(intent)
            finish() // Fecha a ProductDetailActivity
        }
    }

    private fun addToCart(product: Product) {
        val database = FirebaseDatabase.getInstance().reference
        database.child("carrinho").child(product.id).setValue(product)
            .addOnSuccessListener {
                Toast.makeText(this, "Produto adicionado ao carrinho", Toast.LENGTH_SHORT).show()
                goToHomeActivity()  // Navega de volta para HomeActivity
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao adicionar ao carrinho", Toast.LENGTH_SHORT).show()
            }
    }

    private fun goToHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }
}
