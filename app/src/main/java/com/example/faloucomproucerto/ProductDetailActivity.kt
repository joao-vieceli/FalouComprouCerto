package com.example.faloucomproucerto

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton // Importação correta
import com.example.faloucomproucerto.model.Product
import com.example.faloucomproucerto.utils.Talks
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import java.util.Locale

class ProductDetailActivity : AppCompatActivity() , TextToSpeech.OnInitListener {

    private lateinit var productImage: ImageView
    private lateinit var productName: TextView
    private lateinit var productPrice: TextView
    private lateinit var buttonAdd: AppCompatImageButton // Alterado para AppCompatImageButton
    private lateinit var buttonDecline: AppCompatImageButton // Alterado para AppCompatImageButton
    private var nome = ""

    private var tts: TextToSpeech? = null
    private lateinit var speechRecognizer: SpeechRecognizer

    companion object {
        private const val REQUEST_MICROPHONE_PERMISSION = 1
        private const val RESTART_DELAY = 2000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        // Inicialização dos elementos da UI
        productImage = findViewById(R.id.product_image)
        productName = findViewById(R.id.product_name)
        productPrice = findViewById(R.id.product_price)
        buttonAdd = findViewById(R.id.button_add)
        buttonDecline = findViewById(R.id.button_decline)
        tts = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        // Recebe o produto da Intent
        val product = intent.getSerializableExtra("product") as? Product
        if (product != null) {
            // Exibe os dados do produto
            productName.text = product.nome
            productPrice.text = "R$ ${product.preco}"

            nome = product.nome
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


        speechRecognizer.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
            }

            override fun onBeginningOfSpeech() {
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int)
            {
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
                            Toast.makeText(this@ProductDetailActivity, "Problema de rede, tentando novamente...", Toast.LENGTH_SHORT).show()
                            Handler().postDelayed({ startListening() }, RESTART_DELAY)
                        }
                        else -> {
                            Toast.makeText(this@ProductDetailActivity, "Ocorreu um erro, tentando novamente...", Toast.LENGTH_SHORT).show()
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
                        if (recognizedText.contains("aceitar", ignoreCase = true)) {
                            speechRecognizer.destroy()
                            product?.let { nonNullProduct ->
                                addToCart(nonNullProduct)
                            }

                        } else if (recognizedText.contains("recusar", ignoreCase = true)) {
                            speechRecognizer.destroy()
                            onReturn()
                            return
                        } else if (recognizedText.contains("voltar", ignoreCase = true)) {
                            speechRecognizer.destroy()
                            onReturn()
                            return
                        }else{

                            restartListening()
                        }
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

    }

    private fun onReturn() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onPause() {
        super.onPause()
        tts?.stop()
        speechRecognizer.destroy()
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

                speak("Produto selecionado " + nome + " fale aceitar para aceitar o produto ou recusar para recusalo")
            }
        } else {
            println("Falha ao inicializar o TextToSpeech")
        }

    }

    private fun speak(text: String) {
        val utteranceId = System.currentTimeMillis().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
            }

            override fun onDone(utteranceId: String?) {
                runOnUiThread {
                    startListening()
                }
            }

            override fun onError(utteranceId: String?) {
                println("Erro na fala")
            }
        })
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
}
