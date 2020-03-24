package org.rfcx.audiomoth.view.configure


import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_sync.*
import org.rfcx.audiomoth.R

class SyncFragment : Fragment() {

    lateinit var listener: ConfigureListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = (context as ConfigureListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sync, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var i = 0

        val handler = Handler()

        handler.postDelayed(object : Runnable {
            override fun run() {
                if (i != 100) {
                    i += 20
                    progressBarHorizontal.progress = i
                    percentSyncTextView.text = "$i %"
                    handler.postDelayed(this, 500)
                } else {
                    listener.openVerifySync()
                }
            }
        }, 0)
    }
}

