package org.rfcx.companion.view.profile

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.opensooq.supernova.gligar.GligarPicker
import kotlinx.android.synthetic.main.activity_feedback.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.adapter.BaseListItem
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.entity.StatusEvent
import org.rfcx.companion.repo.Firestore
import org.rfcx.companion.util.*
import java.io.File

class FeedbackActivity : AppCompatActivity() {
    private var imageFile: File? = null
    private val galleryPermissions by lazy { GalleryPermissions(this) }
    private val cameraPermissions by lazy { CameraPermissions(this) }
    private val feedbackImageAdapter by lazy { FeedbackImageAdapter() }
    private var pathListArray: List<String>? = null
    private var menuAll: Menu? = null
    private val analytics by lazy { Analytics(this) }

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
        handleGligarPickerResult(requestCode, resultCode, data)
    }

    @SuppressLint("ResourceAsColor")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuAll = menu
        val inflater = menuInflater
        inflater.inflate(R.menu.feedback_menu, menu)
        setEnableSendFeedbackView(false)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.attachView -> {
                if (!cameraPermissions.allowed() || !galleryPermissions.allowed()) {
                    imageFile = null
                    if (!cameraPermissions.allowed()) cameraPermissions.check { }
                    if (!galleryPermissions.allowed()) galleryPermissions.check { }
                } else {
                    startOpenGligarPicker()
                }
            }
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
                ContextCompat.getColor(this, R.color.disableColor)
            )
        }
    }

    private fun startOpenGligarPicker() {
        if (feedbackImageAdapter.getImageCount() < FeedbackImageAdapter.MAX_IMAGE_SIZE) {
            val remainingImage =
                FeedbackImageAdapter.MAX_IMAGE_SIZE - feedbackImageAdapter.getImageCount()
            GligarPicker()
                .requestCode(ImageUtils.REQUEST_GALLERY)
                .limit(remainingImage)
                .withActivity(this)
                .show()
        } else {
            Toast.makeText(this, R.string.maximum_number_of_attachments, Toast.LENGTH_LONG).show()
        }
    }

    private fun handleGligarPickerResult(requestCode: Int, resultCode: Int, intentData: Intent?) {
        if (requestCode != ImageUtils.REQUEST_GALLERY || resultCode != Activity.RESULT_OK || intentData == null) return

        val pathList = mutableListOf<String>()
        val results = intentData.extras?.getStringArray(GligarPicker.IMAGES_RESULT)
        results?.forEach {
            pathList.add(it)
        }
        analytics.trackAddFeedbackImagesEvent()
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
                    analytics.trackSendFeedbackEvent(StatusEvent.SUCCESS.id)

                    if (pathListArray != null) {
                        analytics.trackAddFeedbackImagesEvent()
                    }

                    finish()
                } else {
                    feedbackGroupView.visibility = View.VISIBLE
                    feedbackProgressBar.visibility = View.GONE
                    analytics.trackSendFeedbackEvent(StatusEvent.FAILURE.id)

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

    override fun onResume() {
        super.onResume()
        analytics.trackScreen(Screen.FEEDBACK)
    }
}

interface OnFeedbackImageAdapterClickListener {
    fun onDeleteImageClick(position: Int)
    fun pathListArray(path: ArrayList<BaseListItem>)
}
