package com.example.faloucomproucerto


import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.Manifest
import android.os.Handler

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat

import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.faloucomproucerto.utils.Talks

import java.util.Locale


class MainActivity : AppCompatActivity() , TextToSpeech.OnInitListener
{
    private var tts: TextToSpeech? = null
    private lateinit var speechRecognizer: SpeechRecognizer
    private val talks = Talks()

    companion object {
        private const val REQUEST_MICROPHONE_PERMISSION = 1
        private const val KEYWORD = "entrar"
        private const val RESTART_DELAY = 2000L
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Variaveis
        val btnEntrar: Button = findViewById(R.id.btnEntrar)
        tts = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_MICROPHONE_PERMISSION)
        }

        btnEntrar.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
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
                            Toast.makeText(this@MainActivity, "Problema de rede, tentando novamente...", Toast.LENGTH_SHORT).show()
                            Handler().postDelayed({ startListening() }, RESTART_DELAY)
                        }
                        else -> {
                            Toast.makeText(this@MainActivity, "Ocorreu um erro, tentando novamente...", Toast.LENGTH_SHORT).show()
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
                        if (recognizedText.contains(KEYWORD, ignoreCase = true)) {
                            //speechRecognizer.stopListening()
                            speechRecognizer.destroy()
                            inEntrar()
                            return
                        } else if (recognizedText.contains("voltar", ignoreCase = true)) {

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

    private fun inEntrar() {
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

                speak(talks.converteFalas("Main"))
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

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer.destroy()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MICROPHONE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissão de microfone concedida!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissão de microfone negada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        tts?.stop()
        speechRecognizer.destroy()
    }

    override fun onResume() {
        super.onResume()
        onInit(0)
        //recreate()
    }

}