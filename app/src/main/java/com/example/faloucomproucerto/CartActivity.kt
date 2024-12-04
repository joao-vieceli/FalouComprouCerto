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
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.app.AlertDialog
import android.content.Intent
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.ImageButton
import com.example.faloucomproucerto.utils.Talks
import java.util.Locale

class CartActivity : AppCompatActivity() , TextToSpeech.OnInitListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CartAdapter
    private val cartProducts = mutableListOf<Product>()
    private lateinit var totalPriceTextView: TextView
    private lateinit var finalizeButton: Button
    private lateinit var talkButton: ImageButton
    private var tts: TextToSpeech? = null
    private val talks = Talks()
    private lateinit var speechRecognizer: SpeechRecognizer

    companion object {
        private const val REQUEST_MICROPHONE_PERMISSION = 1
        private const val KEYWORD_PRODUCTS = "produtos"
        private const val KEYWORD_REPEAT = "repetir"
        private const val KEYWORD_TOTAL = "total"
        private const val KEYWORD_REMOVER = "remover"
        private const val KEYWORD_FINALIZAR = "finalizar"
        private const val RESTART_DELAY = 2000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.item_spacing)
        recyclerView = findViewById(R.id.cart_recycler_view)
        recyclerView.addItemDecoration(SpaceItemDecoration(spacingInPixels))
        recyclerView.layoutManager = LinearLayoutManager(this)

        totalPriceTextView = findViewById(R.id.total_price)
        finalizeButton = findViewById(R.id.finalize_button)
        talkButton = findViewById(R.id.talk_button);
        tts = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        adapter = CartAdapter(cartProducts,
            { product -> removeFromCart(product) },
            { product -> increaseQuantity(product) },
            { product -> decreaseQuantity(product) }
        )
        recyclerView.adapter = adapter

        loadCartProducts()

        finalizeButton.setOnClickListener {
            finalizePurchase()
        }

        talkButton.setOnClickListener {
            talkForAllProductsInCar()
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
            }

            override fun onBeginningOfSpeech() {
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                println("Erro no reconhecimento de fala: $error")

                Handler().postDelayed({
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> {
                            startListening()
                        }

                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                            startListening()
                        }

                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                        SpeechRecognizer.ERROR_NETWORK -> {
                            Toast.makeText(
                                this@CartActivity,
                                "Problema de rede, tentando novamente...",
                                Toast.LENGTH_SHORT
                            ).show()
                            Handler().postDelayed({ startListening() }, RESTART_DELAY)
                        }

                        else -> {
                            Toast.makeText(
                                this@CartActivity,
                                "Ocorreu um erro, tentando novamente...",
                                Toast.LENGTH_SHORT
                            ).show()
                            Handler().postDelayed({ startListening() }, RESTART_DELAY)
                        }
                    }
                }, RESTART_DELAY)

            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.let {
                    if (it.isNotEmpty()) {
                        val recognizedText = it[0]
                        if (recognizedText.contains(KEYWORD_PRODUCTS, ignoreCase = true)) {
                            talkForAllProductsInCar()
                        } else if (recognizedText.contains(KEYWORD_REPEAT, ignoreCase = true)) {
                            speak(talks.converteFalas("Cart"))

                        } else if (recognizedText.contains(KEYWORD_TOTAL, ignoreCase = true)) {
                            valorTotal()

                        } else if (recognizedText.contains(KEYWORD_REMOVER, ignoreCase = true)) {
                            listAllProductsForRemove()

                        } else if (recognizedText.contains("sim finalizar", ignoreCase = true)) {
                            finalizarDontMessage()

                        } else if (recognizedText.contains(KEYWORD_FINALIZAR, ignoreCase = true)) {
                            speak("Você tem certeza que deseja finalizar a compra? Se sim, fale 'sim finalizar' se não fale 'não finalizar'")

                        }  else if (recognizedText.contains("número", ignoreCase = true)) {
                            val resultText = recognizedText.toString()

                            val regex = Regex("(?<=número\\s)(\\d+|um|dois|três|quatro|cinco|seis|sete|oito|nove|dez)", RegexOption.IGNORE_CASE)
                            val matchResult = regex.find(resultText)

                            val matchResultValue = matchResult?.value?.lowercase()
                            val number = when (matchResultValue) {
                                "um" -> 1
                                "dois" -> 2
                                "três" -> 3
                                "quatro" -> 4
                                "cinco" -> 5
                                "seis" -> 6
                                "sete" -> 7
                                "oito" -> 8
                                "nove" -> 9
                                else -> matchResult?.value?.toInt()
                            }
                            removeForId(number.toString())

                        } else if (recognizedText.contains("voltar", ignoreCase = true)) {
                            speechRecognizer.destroy()
                            onReturn()
                            return

                        } else {
                            restartListening()
                        }
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun removeForId(id:String){
        val database = FirebaseDatabase.getInstance().reference

        database.child("carrinho").child(id).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val product = snapshot.getValue(Product::class.java)
                    if (product != null) {
                        removeFromCart(product)
                        speak("Produto " + product.nome + " foi removido!")
                    }
                } else {
                }
            }
            .addOnFailureListener { exception ->
            }
    }

    private fun listAllProductsForRemove() {
        var textProduct = "Quais dos produtos deseja remover?";
        val database = FirebaseDatabase.getInstance().reference
        database.child("carrinho").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    if (product != null) {
                        textProduct += " Código " + product.id + " nome " + product.nome + " valor de " + product.preco + " reais , "
                    }
                }
                adapter.notifyDataSetChanged()
                updateTotalPrice()

                speak(textProduct + ", diga 'número + o código do produto'")
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun finalizarDontMessage()
    {
            // Se o usuário clicar em "Sim", finalize a compra
            val totalPriceCart = cartProducts.sumOf { it.preco * it.quantidade }
            val database = FirebaseDatabase.getInstance().reference
            database.child("carrinho").removeValue()
            .addOnSuccessListener()
            {
                cartProducts.clear()
                adapter.notifyDataSetChanged()
                updateTotalPrice()

                speak("Compra finalizada no valor de " + totalPriceCart + "reais")

            }
            .addOnFailureListener()
            {
                Toast.makeText(this, "Erro ao finalizar a compra", Toast.LENGTH_SHORT).show()
            }

    }
    private fun valorTotal()
    {
        var textProduct = "Valor total: ";

        val totalPrice = cartProducts.sumOf { it.preco * it.quantidade }

        speak(textProduct + totalPrice + " reais")

    }

    private fun onReturn() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
    }

    private fun restartListening() {
        speechRecognizer.stopListening()
        startListening()
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale algo")

        speechRecognizer.startListening(intent)
    }

    private fun talkForAllProductsInCar() {
        var textProduct = "Lista de produtos: ";
        val database = FirebaseDatabase.getInstance().reference
        database.child("carrinho").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    if (product != null) {
                        textProduct +=  "" + product.quantidade + " " + product.nome + " valor de " + product.quantidade * product.preco + " reais , "
                    }
                }
                adapter.notifyDataSetChanged()
                updateTotalPrice()

                speak(textProduct)
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
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

                    speak("Compra finalizada no valor: R$ %.2f".format(totalPrice));

                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao finalizar a compra", Toast.LENGTH_SHORT).show()
                }
        }

        builder.setNegativeButton("Não") { dialog, which ->
            dialog.dismiss()
        }

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
            }
    }

    private fun updateTotalPrice() {
        val totalPrice = cartProducts.sumOf { it.preco * it.quantidade }
        totalPriceTextView.text = "Total: R$ %.2f".format(totalPrice)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("pt", "BR"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                println("Idioma não suportado")
            } else {
                tts?.setSpeechRate(1.1f)
                val voices = tts?.voices

                voices?.firstOrNull { voice ->
                    voice.locale == Locale("pt", "BR") && !voice.name.contains("pt-br-x-ptd-local")
                }?.let { newVoice ->
                    tts?.voice = newVoice
                }

                speak(talks.converteFalas("Cart"))
            }
        } else {
            println("Falha ao inicializar o TextToSpeech")
        }
    }

    private fun inMain()
    {
        val intent = Intent(this, MainActivity::class.java) // Substitua MainActivity pela sua tela inicial
        startActivity(intent)
        finish()
    }

    private fun speak(text: String) {
        val utteranceId = System.currentTimeMillis().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
            }

            override fun onDone(utteranceId: String?) {
                runOnUiThread {
                    if(text.contains("Compra finalizada"))
                    {
                        inMain()
                    }

                    startListening()

                }
            }

            override fun onError(utteranceId: String?) {
                println("Erro na fala")
            }
        })
    }

    override fun onPause() {
        super.onPause()
        tts?.stop()
        speechRecognizer.destroy()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer.destroy()
        super.onDestroy()
    }
}
