package org.rfcx.companion.view.profile

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_profile.*
import org.rfcx.companion.BuildConfig
import org.rfcx.companion.MainActivityListener
import org.rfcx.companion.R
import org.rfcx.companion.entity.Screen
import org.rfcx.companion.entity.Theme
import org.rfcx.companion.util.*
import org.rfcx.companion.util.Preferences.Companion.DISPLAY_THEME
import org.rfcx.companion.view.profile.coordinates.CoordinatesActivity
import org.rfcx.companion.view.profile.guardiansoftware.GuardianSoftwareActivity
import org.rfcx.companion.view.profile.locationgroup.ProjectActivity
import org.rfcx.companion.view.profile.offlinemap.OfflineMapActivity

class ProfileFragment : Fragment() {
    lateinit var listener: MainActivityListener
    private val analytics by lazy { context?.let { Analytics(it) } }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = (context as MainActivityListener)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleShowSnackbarResult(requestCode, resultCode, data)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val preferences = context?.let { it1 -> Preferences.getInstance(it1) }
        val themeOption = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this.resources.getStringArray(R.array.theme_more_than_9)
        } else {
            this.resources.getStringArray(R.array.theme_less_than_10)
        }

        userNameTextView.text = context.getUserNickname()
        userLocationTextView.text = context?.getDefaultSiteName()
        versionTextView.text = getString(
            R.string.version_app,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE.toString()
        )
        formatCoordinatesTextView.text = context?.getCoordinatesFormat()
        themeSelectTextView.text = preferences?.getString(DISPLAY_THEME, themeOption[1])

        feedbackTextView.setOnClickListener {
            val intent = Intent(activity, FeedbackActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE)
        }

        logoutTextView.setOnClickListener {
            listener.onLogout()
        }

        offlineMapTextView.setOnClickListener {
            context?.let { it1 -> OfflineMapActivity.startActivity(it1) }
        }

        softwareTextView.setOnClickListener {
            context?.let { it1 -> GuardianSoftwareActivity.startActivity(it1) }
        }

        coordinatesLinearLayout.setOnClickListener {
            context?.let { it1 -> CoordinatesActivity.startActivity(it1) }
        }

        darkThemeLinearLayout.setOnClickListener {
            val builder = context?.let { it1 -> AlertDialog.Builder(it1, R.style.DialogCustom) }
            val selectedRadioItem =
                themeOption.indexOf(preferences?.getString(DISPLAY_THEME, themeOption[1]))

            if (builder != null) {
                builder.setTitle(getString(R.string.theme))

                builder.setSingleChoiceItems(
                    themeOption, selectedRadioItem,
                    DialogInterface.OnClickListener { dialog, which ->
                        when (themeOption[which]) {
                            themeOption[0] -> {
                                analytics?.trackChangeThemeEvent(Theme.MODE_LIGHT.id)
                                preferences?.putString(DISPLAY_THEME, themeOption[0])
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                            }
                            themeOption[1] -> {
                                analytics?.trackChangeThemeEvent(Theme.MODE_NIGHT.id)
                                preferences?.putString(DISPLAY_THEME, themeOption[1])
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                            }
                            themeOption[2] -> {
                                analytics?.trackChangeThemeEvent(Theme.SYSTEM_DEFAULT.id)
                                preferences?.putString(DISPLAY_THEME, themeOption[2])
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                            }
                        }
                        themeSelectTextView.text = themeOption[which]
                        dialog.dismiss()
                    }
                )
                builder.setPositiveButton(getString(R.string.cancel)) { dialog, which ->
                    dialog.dismiss()
                }
                builder.show()
            }
            locationGroupLinearLayout.setOnClickListener {
                context?.let { it1 -> ProjectActivity.startActivity(it1) }
            }
        }

        locationGroupLinearLayout.setOnClickListener {
            context?.let { it1 -> ProjectActivity.startActivity(it1) }
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

    override fun onResume() {
        super.onResume()
        formatCoordinatesTextView.text = context?.getCoordinatesFormat()
        analytics?.trackScreen(Screen.PROFILE)
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
