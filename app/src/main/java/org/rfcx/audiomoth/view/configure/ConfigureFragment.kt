package org.rfcx.audiomoth.view.configure


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_configure.*
import org.rfcx.audiomoth.R

class ConfigureFragment : Fragment() {

    private val sampleRateList = arrayOf(8, 16, 32, 48, 96, 192, 256, 384)
    private val listItems = arrayOf("8 (default)", "16", "32", "48", "96", "192", "256", "384")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_configure, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sampleRateLayout.setOnClickListener {
            val mBuilder = context?.let { it1 -> AlertDialog.Builder(it1) }

            val str: String = sampleRateValueTextView.text.trim().toString()
            var index = 0

            for (i in listItems.indices) {
                if(listItems[i] == str.split(" ")[0]) {
                    index = i
                }
            }

            if (mBuilder != null) {
                mBuilder.setTitle("Choose sample rate (kHz)")

                mBuilder.setSingleChoiceItems(listItems, index) { dialogInterface, i ->
                    sampleRateValueTextView.text = "${sampleRateList[i]} kHz"
                    dialogInterface.dismiss()
                }
                mBuilder.setNeutralButton("Cancel") { dialog, which ->
                    dialog.cancel()
                }

                val mDialog = mBuilder.create()
                mDialog.show()
            }
        }
    }
}
