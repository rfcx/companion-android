package org.rfcx.companion.view.profile.locationgroup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_create_new_group.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.LocationGroups
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.entity.toLocationGroup
import org.rfcx.companion.localdb.LocationGroupDb
import org.rfcx.companion.service.LocationGroupSyncWorker
import org.rfcx.companion.util.Analytics
import org.rfcx.companion.util.RealmHelper
import org.rfcx.companion.util.randomLocationGroup
import org.rfcx.companion.view.detail.EditLocationActivity.Companion.EXTRA_LOCATION_GROUP
import kotlin.collections.ArrayList

class CreateNewGroupActivity : AppCompatActivity(), (ColorPickerItem, Int) -> Unit {
    private val realm by lazy { Realm.getInstance(RealmHelper.migrationConfig()) }

    private val locationGroupDb by lazy { LocationGroupDb(realm) }
    private val colorPickerAdapter by lazy { ColorPickerAdapter(this) }
    private var colorPickerState = ArrayList<ColorPickerItem>()
    private var colorPickerList = listOf<String>()
    private val analytics by lazy { Analytics(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_new_group)
        setupToolbar()
        window.decorView.rootView.viewTreeObserver.addOnGlobalLayoutListener { setOnFocusEditText() }

        colorPickerRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 5)
            adapter = colorPickerAdapter
        }

        colorPickerList = this.resources.getStringArray(R.array.group_color_picker).toList()

        colorPickerList.forEach {
            colorPickerState.add(ColorPickerItem(it, colorPickerList.first() == it))
        }
        colorPickerAdapter.items = colorPickerState

        saveButton.setOnClickListener {
            val color = colorPickerState.firstOrNull { it.selected }
            if (locationGroupEditText.text.isNullOrBlank() || color == null) {
                Toast.makeText(this, R.string.missing_name_group, Toast.LENGTH_LONG).show()
            } else {
                val group = LocationGroups(
                    name = locationGroupEditText.text.toString(),
                    color = color.color,
                    serverId = null
                )
                locationGroupDb.insertOrUpdateLocationGroup(group)
                LocationGroupSyncWorker.enqueue(this)

                analytics.trackSaveNewGroupEvent()

                val intent = Intent()
                intent.putExtra(EXTRA_LOCATION_GROUP, group.toLocationGroup())
                setResult(LocationGroupActivity.RESULT_OK, intent)
                finish()
            }
        }

        locationGroupEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val isGroupExisted = locationGroupDb.isExisted(s.toString())
                    saveButton.isEnabled = !isGroupExisted

                    if(isGroupExisted){
                        locationGroupTextInput.error = getString(R.string.group_already_exists)
                    } else {
                        locationGroupTextInput.error = null
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
    }
    override fun invoke(colorPickerItem: ColorPickerItem, position: Int) {
        colorPickerList.forEachIndexed { index, _ ->
            colorPickerState[index] = ColorPickerItem(colorPickerList[index], position == index)
        }
        colorPickerAdapter.items = colorPickerState
    }

    private fun setOnFocusEditText() {
        val screenHeight: Int = window.decorView.rootView?.height ?: 0
        val r = Rect()
        window.decorView.rootView?.getWindowVisibleDisplayFrame(r)
        val keypadHeight: Int = screenHeight - r.bottom
        if (keypadHeight > screenHeight * 0.15) {
            saveButton.visibility = View.GONE
        } else {
            if (saveButton != null) {
                saveButton.visibility = View.VISIBLE
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.create_new_group)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        fun startActivity(context: Context, screen: String = Screen.PROFILE.id) {
            val intent = Intent(context, CreateNewGroupActivity::class.java)
            intent.putExtra(LocationGroupActivity.EXTRA_SCREEN, screen)
            context.startActivity(intent)
        }

        fun startActivity(
            context: Context,
            screen: String = Screen.PROFILE.id,
            requestCode: Int
        ) {
            val intent = Intent(context, CreateNewGroupActivity::class.java)
            intent.putExtra(LocationGroupActivity.EXTRA_SCREEN, screen)
            (context as Activity).startActivityForResult(intent, requestCode)
        }
    }
}
