package com.example.justpaint

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    var customProgressDialog: Dialog? = null


    val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val imageBackground: ImageView = findViewById(R.id.iv_background)

                imageBackground.setImageURI(result.data?.data)
            }
        }

    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
                    //check the permission name and perform the specific operation
                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this,
                            "Permission granted for reading storage",
                            Toast.LENGTH_LONG
                        )
                            .show()

                        val pickIntent =
                            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(pickIntent)

                    } else if (permissionName == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                        //check the permission name and perform the specific operation
                        Toast.makeText(
                            this,
                            "Permission granted for writing storage",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                } else {
                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this,
                            "Permission denied for reading storage",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    } else if (permissionName == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this,
                            "Permission denied for writing storage",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawingView)
        drawingView?.setSizeForBrush(20f)

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_action_buttons)

        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.ic_undo
            )
        )

        val ibBrush: ImageButton = findViewById(R.id.ib_brush)
        ibBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        val ibGallery: ImageButton = findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener {
            requestStoragePermission()
        }

        val ibUndo: ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener {
            drawingView?.onClickUndo()
        }

        val ibRedo: ImageButton = findViewById(R.id.ib_redo)
        ibRedo.setOnClickListener {
            drawingView?.onClickRedo()
        }

        val ibPicker: ImageButton = findViewById(R.id.ib_picker)
        ibPicker.setOnClickListener{
            ColorPickerDialog.Builder(this)
                .setTitle("ColorPicker Dialog")
                .setPreferenceName("MyColorPickerDialog")
                .setPositiveButton(getString(R.string.confirm),
                    ColorEnvelopeListener { envelope, fromUser -> drawingView?.setColor(envelope.hexCode) })
                .setNegativeButton(
                    getString(R.string.cancel)
                ) { dialogInterface, i -> dialogInterface.dismiss() }
                .attachAlphaSlideBar(true) // the default value is true.
                .attachBrightnessSlideBar(true) // the default value is true.
                .setBottomSpace(12) // set a bottom space between the last slidebar and buttons.
                .show()

        }

        val ibSave: ImageButton = findViewById(R.id.ib_save)
        ibSave.setOnClickListener {
            showProgressDialog()
            if (isReadStorageAllowed()) {
                lifecycleScope.launch {
                    val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)

                    saveBitmapFile(getBitMapFromView(flDrawingView))
                }
            }
        }
    }

    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")
        val btnSmall = brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        btnSmall.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val btnMedium: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        btnMedium.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val btnLarge = brushDialog.findViewById<ImageButton>(R.id.ib_large_brush)
        btnLarge.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun paintClicked(view: View) {
        if (view !== mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag
            drawingView?.setColor(colorTag.toString())

            mImageButtonCurrentPaint = view
        }
    }

    private fun showRationaleDialog(
        title: String,
        message: String,
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun isReadStorageAllowed(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldShowRequestPermissionRationale(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            showRationaleDialog(
                " Permission Demo requires storage access",
                "Storage cannot be used because Storage access is denied"
            )
        } else {
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun getBitMapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitMap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitMap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitMap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(
                        externalCacheDir?.absoluteFile.toString() + File.separator + "DrawingApp_" +
                                System.currentTimeMillis() / 1000 + ".png"
                    )

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread {
                        cancelProgressDialog()
                        if (result.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File saved successfully: $result",
                                Toast.LENGTH_SHORT
                            ).show()
                            shareImage(result)
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun showProgressDialog() {
        customProgressDialog = Dialog(this@MainActivity)

        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        customProgressDialog?.show()
    }

    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result: String) {
        MediaScannerConnection.scanFile(this, arrayOf(result), null){
                path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "share"))
        }
    }
}