package org.rfcx.audiomoth.view.profile.locationgroup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.activity_create_new_group.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.audiomoth.R

class CreateNewGroupActivity : AppCompatActivity() {
    private val colorPickerAdapter by lazy { ColorPickerAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_new_group)
        setupToolbar()

        colorPickerRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 5)
            adapter = colorPickerAdapter
        }

        colorPickerAdapter.items = arrayListOf(
            "#ff7575",
            "#ffcd00",
            "#ba4d4d",
            "#31984f",
            "#528aff",
            "#ff7575",
            "#ffcd00",
            "#ba4d4d",
            "#31984f",
            "#528aff",
            "#ff7575",
            "#ffcd00",
            "#ba4d4d",
            "#31984f",
            "#528aff",
            "#ff7575",
            "#ffcd00",
            "#ba4d4d",
            "#31984f",
            "#528aff"
        )
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
        fun startActivity(context: Context) {
            val intent = Intent(context, CreateNewGroupActivity::class.java)
            context.startActivity(intent)
        }
    }
}
