package com.example.objectdetectionapp.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import coil.load
import coil.transform.CircleCropTransformation
import com.example.objectdetectionapp.R
import com.example.objectdetectionapp.databinding.FragmentImageHelperBinding
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

open class ImageHelperFragment : Fragment() {

    private lateinit var fragmentHelperImageHelperBinding: FragmentImageHelperBinding
    private var imgFromStore: String? = null
    private var selectedImage: Uri? = null
    private var picturePath: String? = null
    lateinit var bitmapImage: Bitmap
    lateinit var photoFile: File
    private var fileUri: Uri? = null
    private val CODE_IMAGE_CAPTURE = 1001

    private lateinit var imageLabeler: ImageLabeler


    private var getContent = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Handle the returned Uri
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data = result.data!!
            selectedImage = data.data
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = requireActivity().contentResolver.query(
                selectedImage!!,
                filePathColumn,
                null,
                null,
                null
            )
            cursor!!.moveToFirst()
            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            picturePath = cursor.getString(columnIndex)
            cursor.close()
            bitmapImage = BitmapFactory.decodeFile(picturePath)
            fragmentHelperImageHelperBinding.ivImage.setImageBitmap(
                bitmapImage
            )
            runClassification(bitmapImage)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        fragmentHelperImageHelperBinding =
            FragmentImageHelperBinding.inflate(layoutInflater, container, false)

        val localModel = LocalModel.Builder().setAssetFilePath("model_flowers.tflite").build()
        val customImageLabelerOptions = CustomImageLabelerOptions.Builder(localModel)
            .setConfidenceThreshold(0.5f)
            .setMaxResultCount(5)
            .build()
        imageLabeler = ImageLabeling.getClient(customImageLabelerOptions)

        setDefaultValue()
        readDataStore()
        return fragmentHelperImageHelperBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getPhotoFromGallery()
        onStartCamera()

    }

    private fun onStartCamera() {
        fragmentHelperImageHelperBinding.btnStartCamera.setOnClickListener {
            cameraCheckPermission()
//        //create a file to share camera
//        photoFile = createPhotoFile()
//        fileUri =
//            FileProvider.getUriForFile(requireContext(), "com.iago.fileprovider", photoFile)
//
//        //create an intent
//
//        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
//
//        //startActivityForResult
//        //startActivityForResult(intent, 0)
//        getContent.launch(intent)
        }
    }

    private fun cameraCheckPermission() {
        Dexter.withContext(
            requireActivity()
        )
            .withPermissions(
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ).withListener(
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {

                        report?.let {
                            if (report.areAllPermissionsGranted()) {
                                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                startActivityForResult(intent, CODE_IMAGE_CAPTURE)
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: MutableList<PermissionRequest>?,
                        p1: PermissionToken?
                    ) {
                        AlertDialog.Builder(requireContext())
                            .setMessage(
                                "It look like you have turned of permission"
                                        + "requied for this future. it can be enable under app setting!"
                            )

                            .setPositiveButton("Setting") { _, _ ->
                                try {
                                    val intent =
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    val uri =
                                        Uri.fromParts("package", requireContext().packageName, null)
                                    intent.data = uri
                                    startActivity(intent)

                                } catch (e: ActivityNotFoundException) {
                                    e.printStackTrace()
                                }
                            }
                            .setNegativeButton("Cancel") { dialog, _ ->
                                run {
                                    dialog.dismiss()
                                }
                            }.show()
                    }
                }
            ).onSameThread().check()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            when(requestCode){
                CODE_IMAGE_CAPTURE ->{
                    val bitmap = data?.extras?.get("data") as Bitmap
                    fragmentHelperImageHelperBinding.ivImage.load(bitmap){
                        crossfade(true)
                        crossfade(1000)
                    }
                    runClassification(bitmap)
                }
            }
        }
    }


    private fun createPhotoFile(): File {
        val photoFileDir = File(
            context?.getExternalFilesDir(
                Environment.DIRECTORY_PICTURES
            ), "ML_IMAGE_HELPER"
        )
        if (photoFileDir.exists()) {
            photoFileDir.mkdir()
        }
        val name = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return File(photoFileDir.path + File.separator + name)
    }

    private fun getPhotoFromGallery() {
        fragmentHelperImageHelperBinding.btnPickImage.setOnClickListener {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(
                        requireContext(),
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                        0
                    )
                } else {

                    val cameraIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    getContent.launch(cameraIntent)
                    writeToDataStore()

                }
            }
        }
    }

    private fun setDefaultValue() {
        if (imgFromStore == null) {
            fragmentHelperImageHelperBinding.ivImage.setImageResource(
                R.drawable.default_image
            )
        } else {
            fragmentHelperImageHelperBinding.ivImage.setImageBitmap(
                decodeBase64(
                    imgFromStore
                )
            )

        }
    }

    private fun readDataStore() {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        imgFromStore = sharedPrefs.getString("profileImg", null)
    }

    // Write image From Shared Preference
    private fun writeToDataStore() {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        val editor = sharedPrefs.edit()
        if (picturePath != null) {
            editor.putString(
                "profileImg",
                encodeToBase64(BitmapFactory.decodeFile(picturePath))
            )
        }

        editor.apply()
    }

    // decode string to bitmap
    private fun decodeBase64(input: String?): Bitmap? {
        val decodedByte = Base64.decode(input, 0)
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
    }

    // method for bitmap to base64
    private fun encodeToBase64(image: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
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
                    fragmentHelperImageHelperBinding.tvImage.text = builder.toString()
                } else {
                    fragmentHelperImageHelperBinding.tvImage.text = "Could not classify"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), e.toString(), Toast.LENGTH_LONG).show()
            }
    }
}