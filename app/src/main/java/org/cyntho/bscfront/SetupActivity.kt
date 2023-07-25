package org.cyntho.bscfront

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.runBlocking
import org.cyntho.bscfront.databinding.ActivitySetupBinding
import org.cyntho.bscfront.exceptions.AuthException
import org.cyntho.bscfront.net.KotlinMqtt
import java.io.File
import java.util.concurrent.TimeoutException

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding

    private var dataCA = ""
    private var dataKey = ""
    private var dataPem = ""

    private var mode = 0
    private var tested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
        editor.putString("runtime_username", "")
        editor.putString("runtime_password", "")
        editor.apply()

        // Instantly forward?
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("setup_complete", false)){
            startActivity(Intent(applicationContext, LoginActivity::class.java))
            finish()
        }

        val intentOpenFile = Intent(Intent.ACTION_GET_CONTENT)

        val btnSelectCaPem = binding.btnSelectCaPem
        intentOpenFile.type = "*/*"

        binding.txtServerIp.setOnFocusChangeListener { _, _ -> revertTestStatus() }
        binding.txtClientId.setOnFocusChangeListener { _, _ -> revertTestStatus() }
        binding.txtClientName.setOnFocusChangeListener { _, _ -> revertTestStatus() }

        binding.btnNext.setBackgroundColor(resources.getColor(androidx.appcompat.R.color.material_blue_grey_800, theme))

        btnSelectCaPem.setOnClickListener {
            mode = 0
            resultLauncher.launch(Intent.createChooser(intentOpenFile, "Select a file"))
        }

        val btnSelectClientKey = binding.btnSelectClientKey
        btnSelectClientKey.setOnClickListener {
            mode = 1
            resultLauncher.launch(Intent.createChooser(intentOpenFile, "Select a file"))
        }

        val btnSelectClientPem = binding.btnSelectClientPem
        btnSelectClientPem.setOnClickListener {
            mode = 2
            resultLauncher.launch(Intent.createChooser(intentOpenFile, "Select a file"))
        }

        val txtClientId = binding.txtClientId

        val btnNext = binding.btnNext
        btnNext.setOnClickListener {
            if (tested){
                val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()

                editor.putString("connection_client_id", binding.txtClientId.text.toString())
                editor.putString("connection_server_ip", binding.txtServerIp.text.toString())
                editor.putBoolean("setup_complete", true)
                editor.apply()

                startActivity(Intent(applicationContext, LoginActivity::class.java))
                finish()

            } else {
                if (dataCA != "" && dataKey != "" && dataPem != "" && txtClientId.text.toString() != ""){
                    writeToFile(dataCA, File(applicationContext.filesDir, "certs/ca.pem"))
                    writeToFile(dataKey, File(applicationContext.filesDir, "certs/client.key"))
                    writeToFile(dataPem, File(applicationContext.filesDir, "certs/client.pem"))

                    runBlocking {
                        try {
                            if (KotlinMqtt.testConnection(applicationContext, binding.txtServerIp.text.toString(), binding.txtClientName.text.toString(), binding.txtClientPassword.text.toString())){
                                tested = true
                                btnNext.text = resources.getString(R.string.txt_setup_complete)
                                btnNext.setBackgroundColor(resources.getColor(R.color.background_status, theme))
                            } else {
                                Snackbar.make(binding.btnNext, resources.getString(R.string.msg_err_connection_failed), Snackbar.LENGTH_LONG).setAction("CLOSE") {}.show()
                            }
                        } catch (auth: AuthException){
                            Snackbar.make(binding.btnNext, "Username or password wrong", Snackbar.LENGTH_LONG).setAction("CLOSE") {}.show()
                        } catch (timout: TimeoutException){
                            Snackbar.make(binding.btnNext, "Unable to connect to the server", Snackbar.LENGTH_LONG).setAction("CLOSE") {}.show()
                        } catch (any: Exception){
                            Snackbar.make(binding.btnNext, "Unknown error occurred", Snackbar.LENGTH_LONG).setAction("CLOSE") {}.show()
                        }
                    }

                } else {
                    Snackbar.make(binding.btnNext, resources.getString(R.string.msg_err_missing_data), Snackbar.LENGTH_LONG).setAction("CLOSE") {}.show()
                }
            }
        }

    }

    private fun revertTestStatus(){
        tested = false
        binding.btnNext.text = resources.getString(R.string.txt_test_connection)
        binding.btnNext.setBackgroundColor(resources.getColor(androidx.appcompat.R.color.material_blue_grey_800, theme))
    }


    private fun writeToFile(data: String, output: File) : Boolean{
        try {
            val writer = output.bufferedWriter()
            writer.write(data)
            writer.flush()
            writer.close()
            return true
        } catch (any: Exception){
            println(any.message)
        }
        return false
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK){
            try {
                revertTestStatus()
                val selection = it.data?.data
                if (selection != null){
                    val file: File?
                    val builder: StringBuilder = StringBuilder()
                    contentResolver.openInputStream(selection)?.bufferedReader()?.forEachLine { line -> builder.append(line + "\n") }
                    when (mode){
                        0 -> {
                            file = File(applicationContext.filesDir, "certs/ca.pem")
                            binding.txtCaPemVerify.text = file.absolutePath.replace(applicationContext.filesDir.absolutePath, "")
                            dataCA = builder.toString()
                        }
                        1 -> {
                            file = File(applicationContext.filesDir, "certs/client.key")
                            binding.txtClientKeyVerify.text = file.absolutePath.replace(applicationContext.filesDir.absolutePath, "")
                            dataKey = builder.toString()
                        }
                        2 -> {
                            file = File(applicationContext.filesDir, "certs/client.pem")
                            binding.txtClientPemVerify.text = file.absolutePath.replace(applicationContext.filesDir.absolutePath, "")
                            dataPem = builder.toString()
                        }
                    }
                }

            } catch (any: Exception){
                any.printStackTrace()
            }
        }
    }

}