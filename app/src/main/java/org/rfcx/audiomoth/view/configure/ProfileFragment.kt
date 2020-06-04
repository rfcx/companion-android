package org.rfcx.audiomoth.view.configure


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_profile.*
import org.rfcx.audiomoth.BuildConfig
import org.rfcx.audiomoth.MainActivityListener
import org.rfcx.audiomoth.R
import org.rfcx.audiomoth.util.getDefaultSiteName
import org.rfcx.audiomoth.util.getUserNickname

class ProfileFragment : Fragment() {
    lateinit var listener: MainActivityListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = (context as MainActivityListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userNameTextView.text = context.getUserNickname()
        userLocationTextView.text = context?.getDefaultSiteName()
        versionTextView.text = getString(
            R.string.version_app,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE.toString()
        )

        feedbackTextView.setOnClickListener {
            Toast.makeText(context, "Feedback!", Toast.LENGTH_SHORT).show()
        }

        logoutTextView.setOnClickListener {
            listener.onLogout()
        }
    }

    companion object {
        const val tag = "ProfileFragment"
        fun newInstance(): ProfileFragment {
            return ProfileFragment()
        }
    }
}
