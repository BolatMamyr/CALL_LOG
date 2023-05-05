package com.example.calllog

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.CallLog
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.calllog.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val requestCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideKeyboardWhenFull()
        getResults()
        setButtonClickListeners()

        if (isPermissionDenied()) {
            showPermissionAlertDialog()
        }
    }

    private fun setButtonClickListeners() {
        binding.apply {
            btnClear.setOnClickListener {
                etPhoneNumber.setText("")
                hideResults()
            }
            btnExit.setOnClickListener {
                finish()
            }
        }
    }

    private fun hideResults() {
        binding.apply {
            txtInSummary.visibility = View.INVISIBLE
            txtInResults.visibility = View.INVISIBLE
            txtOutSummary.visibility = View.INVISIBLE
            txtOutResults.visibility = View.INVISIBLE
        }
    }

    private fun getResults() {
        binding.apply {
            btnShowResults.setOnClickListener {
                val phoneNumber = "+" + etPhoneNumber.text.toString()
                if (isValidPhoneNumber(phoneNumber)) {
                    val incomingResults = getIncomingCallsDurationForLastThreeDays(phoneNumber)
                    val outgoingResult = getAllOutgoingCallsDuration(phoneNumber)
                    binding.txtInResults.text = incomingResults
                    binding.txtOutResults.text = outgoingResult

                    showResults()
                } else {
                    setEditTextError()
                }
            }
        }
    }

    private fun showResults() {
        binding.apply {
            txtInSummary.visibility = View.VISIBLE
            txtInResults.visibility = View.VISIBLE
            txtOutSummary.visibility = View.VISIBLE
            txtOutResults.visibility = View.VISIBLE
        }
    }

    private fun setEditTextError() {
        binding.apply {
            etPhoneNumber.error = getString(R.string.error_enter_correct_phone_number)
            etPhoneNumber.clearFocus()
        }
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean = phoneNumber.length >= 11

    // hide keyborad when EditText text length is equal to 11
    private fun hideKeyboardWhenFull() {
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager


        binding.etPhoneNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(text: Editable?) {
                text?.let {
                    if (text.length == 11) {
                        inputMethodManager.hideSoftInputFromWindow(
                            binding.etPhoneNumber.windowToken,
                            0
                        )
                    }
                }
            }
        })
    }

    private fun isPermissionDenied(): Boolean =
        (ContextCompat.checkSelfPermission(this, PERMISSION_READ_CALL_LOG)
                == PackageManager.PERMISSION_DENIED)

    private fun requestPermissions() {
        if (isPermissionDenied()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(PERMISSION_READ_CALL_LOG),
                requestCode
            )
        }
    }

    private fun showPermissionAlertDialog() {
        AlertDialog.Builder(this).apply {
            setTitle(getString(R.string.rationale_give_all_permissions_to_use_app))
            setPositiveButton("Предоставить") { _, _ ->
                requestPermissions()
            }
            setCancelable(false)
            show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // GRANTED
            } else {
                showPermissionAlertDialog()
            }
        }
    }

    private fun getAllOutgoingCallsDuration(phoneNumber: String): String {
        var totalDuration = 0L
        val cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            "${CallLog.Calls.TYPE} = ${CallLog.Calls.OUTGOING_TYPE} AND ${CallLog.Calls.NUMBER} = '$phoneNumber'",
            null,
            null
        )

        if (cursor != null && cursor.moveToFirst()) {
            val durationColumnIndex = cursor.getColumnIndex(CallLog.Calls.DURATION)
            do {
                val duration = cursor.getLong(durationColumnIndex)
                totalDuration += duration
            } while (cursor.moveToNext())

            cursor.close()
        }

        return formatDuration(totalDuration)
    }

    private fun getIncomingCallsDurationForLastThreeDays(phoneNumber: String): String {
        var totalDuration = 0L
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -3)
        val date = calendar.timeInMillis
        val cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            CallLog.Calls.TYPE + " = " + CallLog.Calls.INCOMING_TYPE + " AND " + CallLog.Calls.NUMBER + " = '" + phoneNumber + "' AND " + CallLog.Calls.DATE + " > " + date,
            null,
            null
        )

        if (cursor != null && cursor.moveToFirst()) {
            val durationColumnIndex = cursor.getColumnIndex(CallLog.Calls.DURATION)

            do {
                val duration = cursor.getLong(durationColumnIndex)
                totalDuration += duration
            } while (cursor.moveToNext())

            cursor.close()
        }

        return formatDuration(totalDuration)
    }

    private fun formatDuration(duration: Long): String {
        val hours = duration / 3600
        val minutes = (duration % 3600) / 60
        val seconds = duration % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    companion object {
        const val PERMISSION_READ_CALL_LOG = android.Manifest.permission.READ_CALL_LOG
    }
}