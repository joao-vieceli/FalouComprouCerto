package com.example.faloucomproucerto

import android.content.Intent
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HomeActivity : AppCompatActivity() {

    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var buttonOpenCamera: Button
    private lateinit var buttonGoToCart: Button

    private var isProductDetailVisible = false // Flag para controlar a exibição da tela de detalhes
    private var isProductNotFoundMessageShown = false // Flag para controlar a exibição da mensagem de produto não encontrado

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Inicializa o scanner de código de barras
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(com.google.mlkit.vision.barcode.Barcode.FORMAT_ALL_FORMATS)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

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
                        if (codigoBarras != null && !isProductDetailVisible) { // Verifica a flag
                            buscarProdutoPorCodigoBarras(codigoBarras)
                            break // Para evitar processamento adicional
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("HomeActivity", "Erro ao processar a imagem: ${e.message}")
                    mostrarMensagem("Erro ao processar a imagem.")
                }
                .addOnCompleteListener {
                    imageProxy.close() // Fechar o imageProxy após o processamento
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
                            isProductNotFoundMessageShown = true // Define a flag como verdadeira
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
            intent.putExtra("product", it) // Passar o produto como Serializable
            startActivity(intent)

            // Define a flag como verdadeira para indicar que a tela de detalhes foi aberta
            isProductDetailVisible = true

            // Oculta o PreviewView quando o produto é exibido
            previewView.visibility = View.GONE

            // Reexibe os botões quando um produto é exibido
            buttonOpenCamera.visibility = View.VISIBLE
            buttonGoToCart.visibility = View.VISIBLE

            // Reseta a flag da mensagem de produto não encontrado
            isProductNotFoundMessageShown = false
        }
    }

    private fun mostrarMensagem(mensagem: String) {
        AlertDialog.Builder(this)
            .setTitle("Aviso")
            .setMessage(mensagem)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss() // Fecha o diálogo
                // Retorna para a HomeActivity
                isProductNotFoundMessageShown = false // Reseta a flag
                finish() // Fecha a atividade atual
                startActivity(Intent(this, HomeActivity::class.java)) // Reabre a HomeActivity
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown() // Encerrar o executor da câmera ao destruir a Activity
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Redefina a flag quando a tela de detalhes do produto for fechada
        isProductDetailVisible = false
    }
}
