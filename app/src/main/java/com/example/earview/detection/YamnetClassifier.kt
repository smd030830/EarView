package com.example.earview.detection

import android.content.Context
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Runs the bundled YAMNet TensorFlow Lite model (`assets/yamnet.tflite`) on the device.
 *
 * The model is Google's "yamnet/classification/tflite/1" (Apache 2.0): 0.975 s of 16 kHz audio in
 * (15600 floats in [-1, 1]), 521 class scores in [0, 1] out. Nothing here touches the network.
 */
class YamnetClassifier private constructor(private val interpreter: Interpreter) : SoundClassifier {
  override val windowSize: Int = interpreter.getInputTensor(0).numElements()

  private val classCount = interpreter.getOutputTensor(0).numElements()
  private val input = newBuffer(windowSize)
  private val output = newBuffer(classCount)

  init {
    check(interpreter.getInputTensor(0).dataType() == DataType.FLOAT32) { "The model must take float audio" }
    check(interpreter.getOutputTensor(0).dataType() == DataType.FLOAT32) { "The model must give float scores" }
    check(classCount == YAMNET_CLASS_COUNT) { "The model gives $classCount scores, expected $YAMNET_CLASS_COUNT" }
  }

  override fun classify(window: FloatArray): FloatArray {
    require(window.size == windowSize) { "Expected $windowSize samples but got ${window.size}" }
    input.clear()
    input.asFloatBuffer().put(window)
    output.clear()
    interpreter.run(input, output)
    output.rewind()
    return FloatArray(classCount).also { output.asFloatBuffer().get(it) }
  }

  override fun close() {
    interpreter.close()
  }

  companion object {
    private const val MODEL_ASSET = "yamnet.tflite"

    /** Loads the model from the app's assets. Fails if it is missing or is not the expected model. */
    fun load(context: Context): YamnetClassifier {
      val bytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }
      val model = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
      model.put(bytes).rewind()
      val interpreter = Interpreter(model, Interpreter.Options().setNumThreads(INFERENCE_THREADS))
      return try {
        YamnetClassifier(interpreter)
      } catch (e: RuntimeException) {
        interpreter.close()
        throw e
      }
    }

    private const val INFERENCE_THREADS = 2

    private fun newBuffer(floatCount: Int): ByteBuffer = ByteBuffer.allocateDirect(floatCount * Float.SIZE_BYTES).order(ByteOrder.nativeOrder())
  }
}
