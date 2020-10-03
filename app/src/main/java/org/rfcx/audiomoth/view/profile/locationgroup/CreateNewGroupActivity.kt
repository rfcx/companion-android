package org.rfcx.audiomoth.view.profile.locationgroup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.activity_create_new_group.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.audiomoth.R

class CreateNewGroupActivity : AppCompatActivity(), (ColorPickerItem, Int) -> Unit {
    private val colorPickerAdapter by lazy { ColorPickerAdapter(this) }
    private var colorPickerState = ArrayList<ColorPickerItem>()
    private val colorPickerList = arrayListOf(
        "#ff7575",
        "#ffcd00",
        "#ba4d4d",
        "#31984f",
        "#9d9d9d",
        "#f6402c",
        "#eb1460",
        "#9c1ab1",
        "#6633b9",
        "#3d4db7",
        "#46af4a",
        "#129788",
        "#8cfffb",
        "#00bbd5",
        "#00a6f6",
        "#88c440",
        "#ccdd1e",
        "#ff9800",
        "#7a5547",
        "#5e7c8b"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_new_group)
        setupToolbar()

        colorPickerRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 5)
            adapter = colorPickerAdapter
        }



        colorPickerList.forEach {
            colorPickerState.add(ColorPickerItem(it, colorPickerList.first() == it))
        }
        colorPickerAdapter.items = colorPickerState

    }

    override fun invoke(colorPickerItem: ColorPickerItem, position: Int) {
        colorPickerList.forEachIndexed { index, _ ->
            colorPickerState[index] = ColorPickerItem(colorPickerList[index], position == index)
        }
        colorPickerAdapter.items = colorPickerState
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
