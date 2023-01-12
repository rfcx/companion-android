package org.rfcx.companion.view.deployment.guardian.storage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_heatmap_audio_coverage.*
import kotlinx.android.synthetic.main.toolbar_default.*
import org.rfcx.companion.R
import org.rfcx.companion.entity.socket.response.GuardianArchived

class HeatmapAudioCoverageActivity : AppCompatActivity() {

    private val archivedHeatmapAdapter by lazy { ArchivedHeatmapAdapter() }

    private var archivedAudios = listOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_heatmap_audio_coverage)
        setupToolbar()
        getExtra()
        archivedHeatmap.apply {
            adapter = archivedHeatmapAdapter
            layoutManager = GridLayoutManager(context, 25)
        }

        val mock = arrayListOf<HeatmapItem>()
        for (i in 1..30) {
            mock.add(HeatmapItem.YAxis("$i"))
            for (j in 1..24) {
                val rand = (0..30).random()
                mock.add(HeatmapItem.Normal(rand))
            }
            if (i == 30) {
                val text = TextView(this).apply {
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    textSize = 12f
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
                            textSize = 12f
                        }
                        text2.text = "0$k"
                        hoursLayout.addView(text2)
                        val params2 = text2.layoutParams as LinearLayout.LayoutParams
                        params2.width = 0
                        params2.weight = 1f
                        text2.layoutParams = params2
                    } else {
                        val text3 = TextView(this).apply {
                            textAlignment = View.TEXT_ALIGNMENT_CENTER
                            textSize = 12f
                        }
                        text3.text = "$k"
                        hoursLayout.addView(text3)
                        val params3 = text3.layoutParams as LinearLayout.LayoutParams
                        params3.width = 0
                        params3.weight = 1f
                        text3.layoutParams = params3
                    }
                }
            }
        }

        archivedHeatmapAdapter.setData(mock)
    }

    private fun getExtra() {
        val parcel = intent?.extras?.getParcelableArray(EXTRA_ARCHIVED_AUDIO) ?: return
        archivedAudios = parcel.map { it as GuardianArchived }.map { archived -> archived.toListOfTimestamp() }.flatten().sorted()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Audio Coverage"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
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
