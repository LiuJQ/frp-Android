package io.github.acedroidx.frp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class ConfigActivity : AppCompatActivity() {
    private val configName = "config.ini"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        val saveConfigButton = findViewById<Button>(R.id.saveConfigButton)
        saveConfigButton.setOnClickListener { saveConfig();finish() }
        val dontSaveConfigButton = findViewById<Button>(R.id.dontSaveConfigButton)
        dontSaveConfigButton.setOnClickListener { finish() }

        readConfig()
    }

    private fun readConfig() {
        val files: Array<String> = this.fileList()
        val configEditText = findViewById<EditText>(R.id.configEditText)
        if (files.contains(configName)) {
            val mReader = this.openFileInput(configName).bufferedReader()
            val mRespBuff = StringBuffer()
            val buff = CharArray(1024)
            var ch = 0
            while (mReader.read(buff).also { ch = it } != -1) {
                mRespBuff.append(buff, 0, ch)
            }
            mReader.close()
            configEditText.setText(mRespBuff.toString())
        } else {
            configEditText.setText("")
        }
    }

    private fun saveConfig() {
        val configEditText = findViewById<EditText>(R.id.configEditText)
        this.openFileOutput(configName, Context.MODE_PRIVATE).use {
            it.write(configEditText.text.toString().toByteArray())
//            Log.d("adx",configEditText.text.toString())
        }
    }
}