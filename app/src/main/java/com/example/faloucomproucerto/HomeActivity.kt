package com.example.faloucomproucerto

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.faloucomproucerto.model.Product
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.ValueEventListener
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.Manifest;
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener
import com.example.faloucomproucerto.utils.Talks
import java.util.Locale

class HomeActivity : AppCompatActivity() , TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var buttonOpenCamera: Button
    private lateinit var buttonGoToCart: Button
    private lateinit var speechRecognizer: SpeechRecognizer
    private val talks = Talks()

    companion object {
        private const val REQUEST_MICROPHONE_PERMISSION = 1
        private const val KEYWORD_CART = "compras"
        private const val KEYWORD_BAR = "comprar"
        private const val RESTART_DELAY = 2000L
    }

    //temporario
    private lateinit var database: DatabaseReference
    private val db = FirebaseFirestore.getInstance()

    private var isProductDetailVisible = false // Flag para controlar a exibição da tela de detalhes
    private var isProductNotFoundMessageShown = false // Flag para controlar a exibição da mensagem de produto não encontrado



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Inicializa o scanner de código de barras
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        tts = TextToSpeech(this, this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                100
            )
        }

        // Executor para a câmera
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Inicializa elementos da UI
        previewView = findViewById(R.id.viewFinder)
        buttonOpenCamera = findViewById(R.id.buttonOpenCamera)
        buttonGoToCart = findViewById(R.id.buttonGoToAnotherPage)

        // Inicializa o PreviewView como invisível
        previewView.visibility = View.GONE

        // Configurar o botão para iniciar a câmera
        buttonOpenCamera.setOnClickListener {
            iniciarEscaneamento()
        }

        // Configurar o botão para abrir o carrinho
        buttonGoToCart.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
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
                            Toast.makeText(this@HomeActivity, "Problema de rede, tentando novamente...", Toast.LENGTH_SHORT).show()
                            Handler().postDelayed({ startListening() }, HomeActivity.RESTART_DELAY)
                        }
                        else -> {
                            Toast.makeText(this@HomeActivity, "Ocorreu um erro, tentando novamente...", Toast.LENGTH_SHORT).show()
                            Handler().postDelayed({ startListening() }, HomeActivity.RESTART_DELAY)
                        }
                    }
                }, HomeActivity.RESTART_DELAY)

            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.let {
                    if (it.isNotEmpty()) {
                        val recognizedText = it[0]
                        if (recognizedText.contains(KEYWORD_BAR, ignoreCase = true)) {
                            iniciarEscaneamento()
                        } else if (recognizedText.contains(KEYWORD_CART, ignoreCase = true)) {
                            speechRecognizer.destroy()
                            inCart()
                            return
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
    private fun onReturn() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun inCart() {
        val intent = Intent(this, CartActivity::class.java)
        startActivity(intent)
    }
    private fun iniciarEscaneamento() {
        buttonOpenCamera.visibility = View.GONE
        buttonGoToCart.visibility = View.GONE
        previewView.visibility = View.VISIBLE

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Cria uma pré-visualização
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // ImageAnalysis para processar as imagens de escaneamento
            val imageAnalysis = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImage(imageProxy)
                }
            }

            // Selecionar a câmera traseira
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Desvincula todas as câmeras antes de reanexar
                cameraProvider.unbindAll()

                // Vincular câmera à lifecycleOwner
                cameraProvider.bindToLifecycle(
                    this as LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e("HomeActivity", "Falha ao abrir a câmera: ${exc.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val codigoBarras = barcode.rawValue
                        if (codigoBarras != null && !isProductDetailVisible) {
                            buscarProdutoPorCodigoBarras(codigoBarras)
                            break
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("HomeActivity", "Erro ao processar a imagem: ${e.message}")
                    mostrarMensagem("Erro ao processar a imagem.")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun buscarProdutoPorCodigoBarras(codigoBarras: String) {
        val database = FirebaseDatabase.getInstance().reference
        database.child("produtos").orderByChild("codigoBarras").equalTo(codigoBarras)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (produtoSnapshot in snapshot.children) {
                            val produto = produtoSnapshot.getValue(Product::class.java)
                            exibirProduto(produto)
                        }
                    } else {
                        if (!isProductNotFoundMessageShown) {
                            mostrarMensagem("Produto não encontrado.")
                            isProductNotFoundMessageShown = true
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    mostrarMensagem("Erro ao acessar o banco de dados.")
                }
            })
    }

    private fun exibirProduto(produto: Product?) {
        produto?.let {
            val intent = Intent(this, ProductDetailActivity::class.java)
            intent.putExtra("product", it)
            startActivity(intent)

            isProductDetailVisible = true

            previewView.visibility = View.GONE

            buttonOpenCamera.visibility = View.VISIBLE
            buttonGoToCart.visibility = View.VISIBLE

            isProductNotFoundMessageShown = false
        }
    }

    private fun mostrarMensagem(mensagem: String) {
        AlertDialog.Builder(this)
            .setTitle("Aviso")
            .setMessage(mensagem)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                isProductNotFoundMessageShown = false
                finish()
                startActivity(Intent(this, HomeActivity::class.java))
            }
            .show()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        isProductDetailVisible = false
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

                speak(talks.converteFalas("Home"))
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

    override fun onPause() {
        super.onPause()
        tts?.stop()
        speechRecognizer.destroy()
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
