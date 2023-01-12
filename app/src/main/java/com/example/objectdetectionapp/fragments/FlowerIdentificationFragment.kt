package com.example.objectdetectionapp.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.objectdetectionapp.databinding.FragmentFlowerIdenditicationBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler


class FlowerIdentificationFragment : Fragment() {

    private lateinit var fragmentFlowerIdentificationBinding: FragmentFlowerIdenditicationBinding
    private lateinit var imageLabeler: ImageLabeler
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        fragmentFlowerIdentificationBinding =
            FragmentFlowerIdenditicationBinding.inflate(layoutInflater, container, false)
        return fragmentFlowerIdentificationBinding.root
    }


    private fun runClassification(bitmap: Bitmap) {
        val inputImage: InputImage = InputImage.fromBitmap(bitmap, 0)
        imageLabeler.process(inputImage)
            .addOnSuccessListener { imageLabels ->
                if (imageLabels.size > 0) {
                    val builder: StringBuilder = StringBuilder()
                    for (label in imageLabels) {
                        builder.append(label.text)
                            .append(" : ")
                            .append(label.confidence)
                            .append("\n")

                        Toast.makeText(requireContext(), label.text, Toast.LENGTH_LONG).show()
                    }
//                    Toast.makeText(requireContext(), builder.toString(), Toast.LENGTH_LONG).show()
                    fragmentFlowerIdentificationBinding.tvImage.text = builder.toString()
                } else {
                    fragmentFlowerIdentificationBinding.tvImage.text = "Could not classify"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), e.toString(), Toast.LENGTH_LONG).show()
            }
    }

}

