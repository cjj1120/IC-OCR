package com.kuhan.textrecognition.utils

import android.graphics.Point
import android.graphics.Rect
import android.util.Log
import com.google.gson.GsonBuilder
import com.kuhan.textrecognition.App
import java.io.File
import com.google.mlkit.vision.text.Text as MLKitText

class MLKitUtils {
    fun getTextBlock(text: MLKitText): Text {
        val textBlocks = text.textBlocks.map { block ->
            val lines = block.lines.map { line ->
                val elements = line.elements.map { element ->
                    Element(element.text,
                        element.boundingBox,
                        element.cornerPoints,
                        element.recognizedLanguage)
                }
                Line(line.text,
                    line.boundingBox,
                    line.cornerPoints,
                    line.recognizedLanguage,
                    elements)

            }
            TextBlock(block.text,
                block.boundingBox,
                block.cornerPoints,
                block.recognizedLanguage,
                lines)
        }
        return Text(textBlocks, text.text)
    }

    fun getTextFile(text: MLKitText): File? {
        return try {
            val parsedText = MLKitUtils().getTextBlock(text)
            //MY TODO: Extract relevant info, and print out !!


//            Log.e("NAME-TAG", parsedText.textBlocks.get(4).lines[1].string)
//            Log.e("TAG", parsedText.textBlocks.toString())
//            Log.e("TAG", parsedText.textBlocks.javaClass.name) //THIS IS ARRAY LIST
//            Log.e("TAG", parsedText.text.javaClass.name) //THIS IS STRING
            var extractedText = ""
            val symbols = "0123456789"
            var icBoundingBoxNum = 0;

            Log.e("test",  parsedText.textBlocks.size.toString())
            for (i in 1..6) {
                extractedText = parsedText.textBlocks.get(i).string
//                Log.e("test", i.toString())
//                Log.e("test", extractedText)
                val x = extractedText.contains("[0-9]".toRegex());
                if ((extractedText.contains("-")) && x ){
                    icBoundingBoxNum = i
                }
            }

            Log.e("IC-FINAL-TAG", parsedText.textBlocks.get(icBoundingBoxNum).string)
            // if len of string < 5, icBoundingBoxNum + 1


            var nameBoundingBoxNum = 0;
            nameBoundingBoxNum = icBoundingBoxNum +1
            for (i in 1..parsedText.textBlocks.size) {
                if (  parsedText.textBlocks.get(nameBoundingBoxNum).string.length < 7){
                    nameBoundingBoxNum+=1
                    }
            }

            Log.e("NAME-FINAL-TAG", parsedText.textBlocks.get(nameBoundingBoxNum).string)



            val file = File.createTempFile("MLKitTextOutput", ".json", App.context.cacheDir)
            val json = GsonBuilder().setPrettyPrinting().create().toJson(parsedText)
            file.writeText(json)
            file
        } catch (ex: Throwable) {
            null
        }
    }

    data class Text(
        var textBlocks: List<TextBlock>,
        val text: String,
    )

    // for reference
    open class TextBase(
        val string: String?,
        val boundingBox: Rect?,
        val cornerPoints: Array<Point?>?,
        val recognizedLanguage: String?,
    )

    class Element(
        val string: String,
        val boundingBox: Rect?,
        val cornerPoints: Array<Point?>?,
        val recognizedLanguage: String?,
    )

    class Line(
        val string: String,
        val boundingBox: Rect?,
        val cornerPoints: Array<Point?>?,
        val recognizedLanguage: String?,
        val elements: List<Element>,
    )

    class TextBlock(
        val string: String,
        val boundingBox: Rect?,
        val cornerPoints: Array<Point?>?,
        val recognizedLanguage: String?,
        val lines: List<Line>,
    )
}