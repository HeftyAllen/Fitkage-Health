// FinishWorkoutDialog.kt
package com.example.fitkagehealth

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button

class FinishWorkoutDialog(
    context: Context,
    private val onConfirm: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_finish_workout)

        findViewById<Button>(R.id.finishButton)?.setOnClickListener {
            onConfirm()
            dismiss()
        }

        findViewById<Button>(R.id.cancelButton)?.setOnClickListener {
            dismiss()
        }
    }
}
