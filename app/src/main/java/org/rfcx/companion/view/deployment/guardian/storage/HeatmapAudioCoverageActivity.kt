package org.rfcx.companion.view.deployment.guardian.storage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_heatmap_audio_coverage.*
import kotlinx.android.synthetic.main.toolbar_default.*
import kotlinx.coroutines.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.socket.response.GuardianArchived
import org.rfcx.companion.util.audiocoverage.AudioCoverageUtils
import org.rfcx.companion.view.dialog.MonthYearPickerDialog
import kotlin.coroutines.CoroutineContext

class HeatmapAudioCoverageActivity :
    AppCompatActivity(), MonthYearPickerDialog.OnPickListener, CoroutineScope {

    private lateinit var job: Job

    // context for io thread
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val archivedHeatmapAdapter by lazy { ArchivedHeatmapAdapter() }

    private var archivedAudios = listOf<Long>()
    private var archivedAudioStructure = JsonObject()
    private var availableYearMonths = hashMapOf<Int, List<Int>>()
    private var selectedMonth = 0
    private var selectedYear = 1995

    private var menuAll: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_heatmap_audio_coverage)

        job = Job()

        setupToolbar()
        getExtra()
        val latestMonthYear = AudioCoverageUtils.getLatestMonthYear(archivedAudios)
        selectedMonth = latestMonthYear.first
        selectedYear = latestMonthYear.second
        availableYearMonths = AudioCoverageUtils.getAvailableMonths(archivedAudioStructure)
        archivedHeatmap.apply {
            adapter = archivedHeatmapAdapter
            layoutManager = GridLayoutManager(context, 25)
        }
        addHoursItem()
        getData(archivedAudioStructure, selectedMonth, selectedYear)
    }

    private fun getExtra() {
        val parcel = intent?.extras?.getParcelableArray(EXTRA_ARCHIVED_AUDIO) ?: return
        archivedAudios =
            parcel.map { it as GuardianArchived }.map { archived -> archived.toListOfTimestamp() }
                .flatten().sorted()
        archivedAudioStructure = AudioCoverageUtils.toDateTimeStructure(archivedAudios)
    }

    private fun addHoursItem() {
        val text = TextView(this).apply {
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            textSize = 8f
        }
        hoursLayout.addView(text)
        val params = text.layoutParams as LinearLayout.LayoutParams
        params.width = 0
        params.weight = 1f
        text.layoutParams = params
        for (k in 0..23) {
            if (k < 10) {
                val text2 = TextView(this).apply {
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    textSize = 8f
                }
                text2.text = getString(R.string.hour_less_than_10, k)
                hoursLayout.addView(text2)
                val params2 = text2.layoutParams as LinearLayout.LayoutParams
                params2.width = 0
                params2.weight = 1f
                text2.layoutParams = params2
            } else {
                val text3 = TextView(this).apply {
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    textSize = 8f
                }
                text3.text = k.toString()
                hoursLayout.addView(text3)
                val params3 = text3.layoutParams as LinearLayout.LayoutParams
                params3.width = 0
                params3.weight = 1f
                text3.layoutParams = params3
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.audio_coverage)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuAll = menu
        val inflater = menuInflater
        inflater.inflate(R.menu.month_year_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.picker -> {
                MonthYearPickerDialog.newInstance(
                    System.currentTimeMillis(),
                    selectedMonth,
                    selectedYear,
                    availableYearMonths,
                    this
                ).show(supportFragmentManager, MonthYearPickerDialog::class.java.name)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPick(month: Int, year: Int) {
        selectedMonth = month
        selectedYear = year
        getData(archivedAudioStructure, selectedMonth, selectedYear)
    }

    private fun getData(obj: JsonObject, month: Int, year: Int) {
        launch {
            withContext(Dispatchers.Main) {
                showLoading()
            }
            val items = AudioCoverageUtils.filterByMonthYear(obj, month, year)
            val months = arrayOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            withContext(Dispatchers.Main) {
                archivedDate.text = getString(R.string.coverage_on, months[month], year)
                archivedHeatmapAdapter.setData(items)
                hideLoading()
            }
        }
    }

    fun showLoading() {
        coverageLoading.visibility = View.VISIBLE
        archivedDate.visibility = View.GONE
        archivedHeatmap.visibility = View.GONE
        hoursLayout.visibility = View.GONE
    }

    fun hideLoading() {
        coverageLoading.visibility = View.GONE
        archivedDate.visibility = View.VISIBLE
        archivedHeatmap.visibility = View.VISIBLE
        hoursLayout.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    companion object {

        private const val EXTRA_ARCHIVED_AUDIO = "EXTRA_ARCHIVED_AUDIO"

        fun startActivity(context: Context, archivedAudios: List<GuardianArchived>) {
            val intent = Intent(context, HeatmapAudioCoverageActivity::class.java)
            intent.putExtra(EXTRA_ARCHIVED_AUDIO, archivedAudios.toTypedArray())
            context.startActivity(intent)
        }
    }
}
