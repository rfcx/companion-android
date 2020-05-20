package org.rfcx.audiomoth.view

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_login.*
import org.rfcx.audiomoth.R

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        signInButton.setOnClickListener {
            val email = loginEmailEditText.text.toString()
            val password = loginPasswordEditText.text.toString()
            it.hideKeyboard()

            if (validateInput(email, password)) {
                loading()
            }
        }
    }

    private fun loading(start: Boolean = true) {
        loginGroupView.visibility = if (start) View.GONE else View.VISIBLE
        loginProgressBar.visibility = if (start) View.VISIBLE else View.GONE
    }

    private fun validateInput(email: String?, password: String?): Boolean {
        if (email.isNullOrEmpty()) {
            loginEmailEditText.error = getString(R.string.pls_fill_email)
            return false
        } else if (password.isNullOrEmpty()) {
            loginPasswordEditText.error = getString(R.string.pls_fill_password)
            return false
        }
        return true
    }

    private fun View.hideKeyboard() = this.let {
        val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }
}
