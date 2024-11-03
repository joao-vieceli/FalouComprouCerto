package com.example.faloucomproucerto.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.faloucomproucerto.model.Product
import com.example.faloucomproucerto.R
import com.squareup.picasso.Picasso

class CartAdapter(
    private val products: MutableList<Product>,
    private val onRemoveClick: (Product) -> Unit,
    private val onIncreaseQuantity: (Product) -> Unit,
    private val onDecreaseQuantity: (Product) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val product = products[position]
        holder.bind(product, onRemoveClick, onIncreaseQuantity, onDecreaseQuantity)
    }

    override fun getItemCount(): Int = products.size

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productName: TextView = itemView.findViewById(R.id.product_name)
        private val productPrice: TextView = itemView.findViewById(R.id.product_price)
        private val productImage: ImageView = itemView.findViewById(R.id.product_image)
        private val removeButton: ImageButton = itemView.findViewById(R.id.remove_button)
        private val decreaseButton: ImageButton = itemView.findViewById(R.id.decrease_button)
        private val increaseButton: ImageButton = itemView.findViewById(R.id.increase_button)
        private val quantityTextView: TextView = itemView.findViewById(R.id.product_quantity)

        fun bind(product: Product, onRemoveClick: (Product) -> Unit, onIncreaseQuantity: (Product) -> Unit, onDecreaseQuantity: (Product) -> Unit) {
            productName.text = product.nome
            updatePrice(product)

            // Carrega a imagem do produto
            if (product.imageUrl.isNotEmpty()) {
                Picasso.get()
                    .load(product.imageUrl)
                    .error(R.drawable.produtos)
                    .into(productImage)
            } else {
                productImage.setImageResource(R.drawable.produtos)
            }

            // Listener do botão de remover
            removeButton.setOnClickListener { onRemoveClick(product) }

            // Listener para o botão de diminuir
            decreaseButton.setOnClickListener {
                if (product.quantidade > 1) { // Previne que a quantidade fique menor que 1
                    onDecreaseQuantity(product) // Notifica a mudança
                    updatePrice(product) // Atualiza o preço
                }
            }

            // Listener para o botão de aumentar
            increaseButton.setOnClickListener {
                onIncreaseQuantity(product) // Notifica a mudança
                updatePrice(product) // Atualiza o preço
            }
        }

        private fun updatePrice(product: Product) {
            // Atualiza o preço total
            val totalPrice = product.preco * product.quantidade
            productPrice.text = String.format("R$ %.2f", totalPrice)
            quantityTextView.text = product.quantidade.toString() // Atualiza a quantidade exibida
        }
    }
}
