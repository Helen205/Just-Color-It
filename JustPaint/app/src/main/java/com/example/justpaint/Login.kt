package com.example.justpaint

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity(),View.OnClickListener {

    lateinit var etloginemail: EditText
    lateinit var tvregister: TextView
    lateinit var btnlogin: Button
    lateinit var etloginpassword: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etloginemail = findViewById(R.id.et_loginemail)
        tvregister = findViewById(R.id.tv_register)
        btnlogin = findViewById(R.id.btn_login)
        etloginpassword = findViewById(R.id.et_loginpassword)

        btnlogin.setOnClickListener(this)
        tvregister.setOnClickListener(this)
    }

    override fun onClick(view : View?){
        if(view != null){
            when(view.id){
                R.id.btn_login -> {
                    loginRegisteredUser()
                }
                R.id.tv_register ->{
                    val intent = Intent(this@Login,KayitOl::class.java)
                    startActivity(intent)
                }
            }
        }
    }

    private fun validateLoginDetails():Boolean{
        return when{
            TextUtils.isEmpty(etloginemail.text.toString().trim{ it <= ' '}) -> {
               Toast.makeText(this,"Lütfen e-maili giriniz",Toast.LENGTH_SHORT)
                   .show()
                false
            }
            TextUtils.isEmpty(etloginpassword.text.toString().trim{ it <= ' '}) -> {
                Toast.makeText(this,"Lütfen şifre giriniz.",Toast.LENGTH_SHORT)
                    .show()
                false
            }
            else ->{
                true
            }
        }
    }
    private fun loginRegisteredUser() {

        if(validateLoginDetails()) {
            val email = etloginemail.text.toString().trim() { it <= ' ' }
            val password = etloginpassword.text.toString().trim() { it <= ' ' }
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val intent = Intent(this@Login, MainActivity::class.java)
                        startActivity(intent)
                        Toast.makeText(this, "Giriş başarılı.", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(
                            this,
                            task.exception!!.message.toString(),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
        }
    }
}
