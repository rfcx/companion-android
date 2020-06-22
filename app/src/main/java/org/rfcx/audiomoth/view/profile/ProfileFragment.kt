package org.rfcx.audiomoth.view.profile


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
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
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleShowSnackbarResult(requestCode, resultCode, data)
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
            val intent = Intent(activity, FeedbackActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE)
        }

        logoutTextView.setOnClickListener {
            listener.onLogout()
        }
    }

    private fun handleShowSnackbarResult(requestCode: Int, resultCode: Int, intentData: Intent?) {
        if (requestCode != REQUEST_CODE || resultCode != RESULT_CODE || intentData == null) return

        view?.let {
            Snackbar.make(it, R.string.feedback_submitted, Snackbar.LENGTH_LONG)
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                .setAnchorView(R.id.createLocationButton).show()
        }
    }

    companion object {
        const val tag = "ProfileFragment"
        const val RESULT_CODE = 12
        const val REQUEST_CODE = 11

        fun newInstance(): ProfileFragment {
            return ProfileFragment()
        }
    }
}
