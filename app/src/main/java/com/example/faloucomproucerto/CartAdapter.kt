package com.example.faloucomproucerto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class CartAdapter(
    private val products: List<Product>,
    private val onRemoveClick: (Product) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val product = products[position]
        holder.bind(product, onRemoveClick)
    }

    override fun getItemCount(): Int = products.size

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val decreaseButton: ImageButton = itemView.findViewById(R.id.decrease_button)
        private val increaseButton: ImageButton = itemView.findViewById(R.id.increase_button)
        private val productName: TextView = itemView.findViewById(R.id.product_name)
        private val productPrice: TextView = itemView.findViewById(R.id.product_price)
        private val productImage: ImageView = itemView.findViewById(R.id.product_image)
        private val removeButton: ImageButton = itemView.findViewById(R.id.remove_button)

        fun bind(product: Product, onRemoveClick: (Product) -> Unit) {
            productName.text = product.nome
            productPrice.text = String.format("R$ %.2f", product.preco)

            // Load product image using Picasso with error handling
            if (product.imageUrl.isNotEmpty()) {
                Picasso.get()
                    .load(product.imageUrl)
                    .error(R.drawable.produtos) // Set default image in case of error
                    .into(productImage)
            } else {
                productImage.setImageResource(R.drawable.produtos) // Default image
            }

            // Set onClick listener for the remove button
            removeButton.setOnClickListener {
                onRemoveClick(product)
            }

            // Placeholder logic for increase/decrease buttons (if needed)
            // decreaseButton.setOnClickListener { /* logic for decreasing quantity */ }
            // increaseButton.setOnClickListener { /* logic for increasing quantity */ }
        }
    }
}
