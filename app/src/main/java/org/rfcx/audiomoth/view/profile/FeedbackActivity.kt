package org.rfcx.audiomoth.view.profile

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import java.io.File
import kotlinx.android.synthetic.main.activity_feedback.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.adapter.BaseListItem
import org.rfcx.audiomoth.repo.Firestore
import org.rfcx.audiomoth.util.*

class FeedbackActivity : AppCompatActivity() {
    private var imageFile: File? = null
    private val galleryPermissions by lazy { GalleryPermissions(this) }
    private val cameraPermissions by lazy { CameraPermissions(this) }
    private val feedbackImageAdapter by lazy { FeedbackImageAdapter() }
    private var pathListArray: List<String>? = null
    private var menuAll: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        setupToolbar()
        setTextFrom()
        setupFeedbackImages()

        feedbackEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (p0 != null) {
                    if (p0.isEmpty()) {
                        setEnableSendFeedbackView(false)
                    }
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                setEnableSendFeedbackView()
            }
        })
    }

    private fun setupFeedbackImages() {
        feedbackRecycler.apply {
            layoutManager = LinearLayoutManager(this@FeedbackActivity)
            adapter = feedbackImageAdapter
        }

        feedbackImageAdapter.onFeedbackImageAdapterClickListener =
            object : OnFeedbackImageAdapterClickListener {
                override fun pathListArray(path: ArrayList<BaseListItem>) {
                    val pathList = mutableListOf<String>()
                    path.forEach {
                        val itemImage = it as LocalImageItem
                        pathList.add(itemImage.localPath)
                    }
                    pathListArray = pathList
                }

                override fun onDeleteImageClick(position: Int) {
                    feedbackImageAdapter.removeAt(position)
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleGalleryResult(requestCode, resultCode, data)
        handleTakePhotoResult(requestCode, resultCode)
    }

    @SuppressLint("ResourceAsColor")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuAll = menu
        val inflater = menuInflater
        inflater.inflate(R.menu.feedback_menu, menu)
        setEnableSendFeedbackView(false)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> finish()
            R.id.camera -> takePhoto()
            R.id.gallery -> openGallery()
            R.id.sendFeedbackView -> sendFeedback()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setEnableSendFeedbackView(start: Boolean = true) {
        menuAll?.findItem(R.id.sendFeedbackView)?.isEnabled = start
        val itemSend = menuAll?.findItem(R.id.sendFeedbackView)?.icon
        val wrapDrawable = itemSend?.let { DrawableCompat.wrap(it) }

        if (wrapDrawable != null) {
            if (start) DrawableCompat.setTint(
                wrapDrawable,
                ContextCompat.getColor(this, R.color.colorPrimary)
            ) else DrawableCompat.setTint(
                wrapDrawable,
                ContextCompat.getColor(this, android.R.color.darker_gray)
            )
        }
    }

    private fun openGallery() {
        if (!galleryPermissions.allowed()) {
            imageFile = null
            galleryPermissions.check { }
        } else {
            startOpenGallery()
        }
    }

    private fun takePhoto() {
        if (!cameraPermissions.allowed()) {
            imageFile = null
            cameraPermissions.check { }
        } else {
            startTakePhoto()
        }
    }

    private fun startTakePhoto() {
        if (feedbackImageAdapter.getImageCount() < FeedbackImageAdapter.MAX_IMAGE_SIZE) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            imageFile = ImageUtils.createImageFile()
            if (imageFile != null) {
                val photoURI = imageFile?.let {
                    FileProvider.getUriForFile(this, ImageUtils.FILE_CONTENT_PROVIDER, it)
                }
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, ImageUtils.REQUEST_TAKE_PHOTO)
            }
        } else {
            Toast.makeText(this, R.string.maximum_number_of_attachments, Toast.LENGTH_LONG).show()
        }
    }

    private fun startOpenGallery() {
        if (feedbackImageAdapter.getImageCount() < FeedbackImageAdapter.MAX_IMAGE_SIZE) {
            val remainingImage =
                FeedbackImageAdapter.MAX_IMAGE_SIZE - feedbackImageAdapter.getImageCount()
            Matisse.from(this)
                .choose(MimeType.ofImage())
                .countable(true)
                .maxSelectable(remainingImage)
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                .thumbnailScale(0.85f)
                .imageEngine(GlideV4ImageEngine())
                .theme(R.style.Matisse_Dracula)
                .forResult(ImageUtils.REQUEST_GALLERY)
        } else {
            Toast.makeText(this, R.string.maximum_number_of_attachments, Toast.LENGTH_LONG).show()
        }
    }

    private fun handleTakePhotoResult(requestCode: Int, resultCode: Int) {
        if (requestCode != ImageUtils.REQUEST_TAKE_PHOTO) return

        if (resultCode == Activity.RESULT_OK) {
            imageFile?.let {
                val pathList = listOf(it.absolutePath)
                feedbackImageAdapter.addImages(pathList)
            }
        } else {
            // remove file image
            imageFile?.let {
                ImageFileUtils.removeFile(it)
                this.imageFile = null
            }
        }
    }

    private fun handleGalleryResult(requestCode: Int, resultCode: Int, intentData: Intent?) {
        if (requestCode != ImageUtils.REQUEST_GALLERY || resultCode != Activity.RESULT_OK || intentData == null) return

        val pathList = mutableListOf<String>()
        val results = Matisse.obtainResult(intentData)
        results.forEach {
            val imagePath = ImageFileUtils.findRealPath(this, it)
            imagePath?.let { path ->
                pathList.add(path)
            }
        }
        feedbackImageAdapter.addImages(pathList)
    }

    private fun sendFeedback() {
        val sendFeedbackView = findViewById<View>(R.id.sendFeedbackView)
        val contextView = findViewById<View>(R.id.content)
        val feedbackInput = feedbackEditText.text.toString()

        feedbackGroupView.visibility = View.GONE
        feedbackProgressBar.visibility = View.VISIBLE

        setEnableSendFeedbackView(false)
        sendFeedbackView.hideKeyboard()

        Firestore(this)
            .saveFeedback(feedbackInput, pathListArray) { success ->
            if (success) {
                val intent = Intent()
                setResult(ProfileFragment.RESULT_CODE, intent)
                finish()
            } else {
                feedbackGroupView.visibility = View.VISIBLE
                feedbackProgressBar.visibility = View.GONE

                Snackbar.make(
                    contextView,
                    R.string.feedback_submission_failed,
                    Snackbar.LENGTH_LONG
                ).setAction(R.string.snackbar_retry) { sendFeedback() }.show()
            }
        }
    }

    private fun View.hideKeyboard() = this.let {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun setTextFrom() {
        fromEmailTextView.text = getString(R.string.from, this.getEmailUser())
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.profile_send_feedback)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

interface OnFeedbackImageAdapterClickListener {
    fun onDeleteImageClick(position: Int)
    fun pathListArray(path: ArrayList<BaseListItem>)
}
