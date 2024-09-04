import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.faloucomproucerto.R

data class Product(
    val imageUrl: String,
    val description: String,
    val price: Double,
    val quantity: Int
)

class CartAdapter(
    private val products: List<Product>,
    private val removeListener: (Product) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productImage: ImageView = view.findViewById(R.id.product_image)
        val productDescription: TextView = view.findViewById(R.id.product_description)
        val productPrice: TextView = view.findViewById(R.id.product_price)
        val productQuantity: TextView = view.findViewById(R.id.product_quantity)
        val removeButton: Button = view.findViewById(R.id.remove_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val product = products[position]

        // Exibe a imagem do produto usando uma biblioteca como Picasso ou Glide
        // Picasso.get().load(product.imageUrl).into(holder.productImage)

        holder.productDescription.text = product.description
        holder.productPrice.text = "Preço: R$ ${product.price}"
        holder.productQuantity.text = "Quantidade: ${product.quantity}"

        holder.removeButton.setOnClickListener {
            removeListener(product)
        }
    }

    override fun getItemCount(): Int = products.size
}
