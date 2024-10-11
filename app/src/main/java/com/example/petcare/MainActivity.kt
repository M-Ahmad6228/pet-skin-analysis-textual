package com.example.petcare

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.google.gson.Gson
import com.opencsv.CSVReader
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


class MainActivity : ComponentActivity() {
    private lateinit var inputText: EditText
    private lateinit var predictButton: Button
    private lateinit var resultText: TextView
    private lateinit var tflite: Interpreter
    private lateinit var tokenizer: Map<String, Int>
    private lateinit var diseaseInfoList: List<DiseaseInfo> // Declare without initialization
    val py: PyObject = Python.getInstance().getModule("labelencoder")




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputText = findViewById(R.id.input_text)
        predictButton = findViewById(R.id.predict_button)
        resultText = findViewById(R.id.result_text)


        try {
            tflite = Interpreter(loadModelFile("pet_model.tflite"))
            tokenizer = loadTokenizer(this)  // Load tokenizer here
            diseaseInfoList = loadDiseaseData("pet_skin_diseases.csv")
            val labels = diseaseInfoList.map { it.disease }
            py.callAttr("fit_encoder", labels)


        } catch (e: IOException) {
            Toast.makeText(
                this,
                "Error loading model or tokenizer: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        predictButton.setOnClickListener {
            val userInput = inputText.text.toString()

            if (userInput.isNotBlank()) {
                val prediction = predictDisease(userInput)
                resultText.text = prediction
            } else {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadModelFile(fileName: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(fileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // Function to load the tokenizer from the assets folder
    fun loadTokenizer(context: Context): Map<String, Int> {
        return try {
            // Reading the JSON file from the assets folder
            val tokenizerJson =
                context.assets.open("tokenizer_pet.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(tokenizerJson)

            // Getting the 'config' object from the JSON
            val config = jsonObject.getJSONObject("config")

            // Ensure 'word_index' exists in the config
            if (!config.has("word_index")) {
                throw IllegalArgumentException("Tokenizer JSON does not contain 'word_index'")
            }

            // Extract the 'word_index' from the config
            val wordIndex = config.getJSONObject("word_index")
            val tokenizerMap = mutableMapOf<String, Int>()

            // Iterating through the keys of word_index to populate the tokenizer map
            val keys = wordIndex.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                tokenizerMap[key] = wordIndex.getInt(key)
            }

            // Returning the tokenizer map
            tokenizerMap
        } catch (e: Exception) {
            Log.e("TokenizerError", "Error loading tokenizer: ${e.localizedMessage}")
            emptyMap()
        }
    }

    private fun loadDiseaseData(fileName: String): List<DiseaseInfo> {
        val diseaseInfoList = mutableListOf<DiseaseInfo>()
        try {
            val inputStream = assets.open(fileName)
            val reader = CSVReader(InputStreamReader(inputStream))
            reader.readNext() // Skip header line if present
            var line: Array<String>?
            while (reader.readNext().also { line = it } != null) {
                if (line!!.size >= 5) {
                    diseaseInfoList.add(
                        DiseaseInfo(
                            line!![0],
                            line!![1],
                            line!![2],
                            line!![3],
                            line!![4]
                        )
                    )
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return diseaseInfoList

//        val diseaseInfoList = mutableListOf<DiseaseInfo>()
//        try {
//            val inputStream = assets.open(fileName)
//            val reader = BufferedReader(InputStreamReader(inputStream))
//            reader.readLine() // Skip header line if present
//            reader.forEachLine { line ->
//                val parts = line.split(",")
//                if (parts.size >= 4) {
//                    diseaseInfoList.add(DiseaseInfo(parts[0], parts[1], parts[2], parts[3], parts[4]))
//                }
//            }
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//        return diseaseInfoList
    }

    private fun predictDisease(behavior: String): String {

        // Convert behavior text to sequences and pad them
        val seq = textsToSequences(behavior)
        val padded = padSequences(seq)

        if (padded == null) {
            return "Error padding sequences."
        }

        val predictions = Array(1) { FloatArray(diseaseInfoList.size) }
        try {
            // Run the model
            tflite.run(padded, predictions)
        } catch (e: Exception) {
            return "Error running model: ${e.message}"
        }

        // Debug: Log the raw predictions
        Log.d("PredictDisease", "Raw Predictions: ${predictions[0].contentToString()}")

        // Get the predicted class index with the highest score
        val predictedClassIndex = predictions[0].indices.maxByOrNull { predictions[0][it] } ?: -1

        // Decode the predicted class using the Python function
        return try {
            val decodedLabels: PyObject = py.callAttr("decode_labels", arrayOf(predictedClassIndex))
            val resultString = decodedLabels.toString()
            // Find the predicted disease based on the decoded label
            val predictedDisease = diseaseInfoList.find { it.disease.equals(resultString, true) }

            predictedDisease?.let {
                "Predicted Disease: ${it.disease}\n" +
                        "Symptoms: ${it.symptoms}\n" +
                        "Treatment: ${it.treatment}\n" +
                        "Prevention: ${it.prevention}\n" +
                        "Danger: ${it.danger}"
            } ?: "Disease not found."
        } catch (e: Exception) {
            Log.e("PredictDisease", "Error decoding label: ${e.message}")
            "Error decoding label: ${e.message}"
        }
    }


    // Convert the behavior text to a sequence of integers using the tokenizer
    private fun textsToSequences(text: String): List<Int> {
        val words = text.split(" ")
        return words.map { tokenizer[it] ?: 0 }
    }

    // Pad the sequences to the required max length
    private fun padSequences(sequences: List<Int>): ByteBuffer? {
        val inputTensorShape = tflite.getInputTensor(0).shape()  // Get the expected input shape
        val inputLength =
            inputTensorShape[1]  // Typically, the second dimension is the sequence length
        val dataType = tflite.getInputTensor(0).dataType()

        if (sequences.isEmpty()) return null

        // Allocate a ByteBuffer with the correct size based on input tensor type
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputLength).order(ByteOrder.nativeOrder())

        // Pad the input sequences or truncate if necessary
        for (i in 0 until inputLength) {
            val value =
                if (i < sequences.size) sequences[i] else 0  // Pad with zeros if the input is shorter
            byteBuffer.putInt(value)  // Assuming the model expects int32 inputs
        }

        byteBuffer.rewind()
        return byteBuffer
    }


    // Data class for the disease info
    data class DiseaseInfo(
        val symptoms: String,
        val disease: String,
        val treatment: String,
        val prevention: String,
        val danger: String
    )
}
