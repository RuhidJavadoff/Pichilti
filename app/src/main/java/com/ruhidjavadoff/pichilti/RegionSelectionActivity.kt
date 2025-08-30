package com.ruhidjavadoff.pichilti

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ruhidjavadoff.pichilti.databinding.ActivityRegionSelectionBinding

class RegionSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegionSelectionBinding
    private var isRegionActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegionSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val regions = resources.getStringArray(R.array.regions_array)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, regions)
        binding.regionAutoComplete.setAdapter(adapter)

        binding.regionAutoComplete.setOnItemClickListener { parent, view, position, id ->
            val selectedRegion = parent.getItemAtPosition(position).toString()
            if (selectedRegion == "Azerbaijan") {
                isRegionActive = true
            } else {
                isRegionActive = false
                Toast.makeText(this, getString(R.string.coming_soon), Toast.LENGTH_SHORT).show()
                binding.regionAutoComplete.text.clear()
            }
        }

        binding.continueButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()

            if (username.isBlank()) {
                Toast.makeText(this, getString(R.string.please_enter_username), Toast.LENGTH_SHORT).show()
            } else if (!isRegionActive) {
                Toast.makeText(this, getString(R.string.please_select_azerbaijan), Toast.LENGTH_SHORT).show()
            } else {
                // YENİ MƏNTİQ BURADADIR
                // 1. İstifadəçi adını yoxlayıb rolu təyin edirik
                val userRole = if (username.equals("admin", ignoreCase = true)) {
                    UserRole.ADMIN
                } else {
                    UserRole.USER
                }

                // 2. Ana Səhifəyə keçid edirik və rolu da özümüzlə aparırıq
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("USER_ROLE", userRole.name) // Rolu "ADMIN" və ya "USER" mətni kimi göndəririk
                intent.putExtra("USERNAME", username) // Gələcəkdə istifadə üçün adı da göndərək
                startActivity(intent)
            }
        }

        binding.sendRequestTextView.setOnClickListener {
            Toast.makeText(this, getString(R.string.coming_soon), Toast.LENGTH_SHORT).show()
        }
    }
}