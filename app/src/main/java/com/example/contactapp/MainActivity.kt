package com.example.contactapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var searchBar: EditText
    private lateinit var themeToggle: ImageView
    private lateinit var fabAddContact: FloatingActionButton

    private val contactsList = mutableListOf<Contact>()
    private val filteredContactsList = mutableListOf<Contact>()

    companion object {
        private const val READ_CONTACT_PERMISSION_CODE = 100
        private const val CALL_PHONE_PERMISSION_CODE = 101
        private const val WRITE_CONTACTS_PERMISSION_CODE = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load saved theme preference
        loadThemePreference()

        setContentView(R.layout.activity_main)

        // Initialize views
        recyclerView = findViewById(R.id.rvContacts)
        searchBar = findViewById(R.id.searchBar)
        themeToggle = findViewById(R.id.ivThemeToggle)
        fabAddContact = findViewById(R.id.fabAddContact)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Setup theme toggle
        setupThemeToggle()

        // Setup search functionality
        setupSearchBar()

        // Setup FAB click listener
        fabAddContact.setOnClickListener {
            try {
                val intent = Intent(this, AddContactActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }

        // Check and request READ_CONTACTS permission
        if (checkReadContactPermission()) {
            loadContacts()
        } else {
            requestReadContactPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload contacts when returning to this activity
        if (checkReadContactPermission()) {
            loadContacts()
        }
    }

    private fun setupThemeToggle() {
        updateThemeIcon()

        themeToggle.setOnClickListener {
            val currentMode = AppCompatDelegate.getDefaultNightMode()
            val newMode = if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.MODE_NIGHT_NO
            } else {
                AppCompatDelegate.MODE_NIGHT_YES
            }

            // Save preference
            saveThemePreference(newMode)

            // Apply theme
            AppCompatDelegate.setDefaultNightMode(newMode)
        }
    }

    private fun updateThemeIcon() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        themeToggle.setImageResource(
            if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
                R.drawable.ic_light_mode
            } else {
                R.drawable.ic_dark_mode
            }
        )
    }

    private fun saveThemePreference(mode: Int) {
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        prefs.edit().putInt("theme_mode", mode).apply()
    }

    private fun loadThemePreference() {
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val savedMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedMode)
    }

    private fun setupSearchBar() {
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterContacts(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterContacts(query: String) {
        filteredContactsList.clear()

        if (query.isEmpty()) {
            filteredContactsList.addAll(contactsList)
        } else {
            val searchQuery = query.lowercase()
            filteredContactsList.addAll(
                contactsList.filter { contact ->
                    contact.firstName.lowercase().contains(searchQuery) ||
                            contact.lastName.lowercase().contains(searchQuery) ||
                            contact.phoneNumber.contains(searchQuery)
                }
            )
        }

        contactAdapter.notifyDataSetChanged()
    }

    private fun checkReadContactPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestReadContactPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_CONTACTS),
            READ_CONTACT_PERMISSION_CODE
        )
    }

    private fun checkCallPhonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCallPhonePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CALL_PHONE),
            CALL_PHONE_PERMISSION_CODE
        )
    }

    private fun checkWriteContactPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestWriteContactPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_CONTACTS),
            WRITE_CONTACTS_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            READ_CONTACT_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadContacts()
                } else {
                    Toast.makeText(
                        this,
                        "Permission denied. Cannot access contacts.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            WRITE_CONTACTS_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Write permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Write permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            requestReadContactPermission()
            return
        }

        contactsList.clear()

        // Query to get contacts with phone numbers
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

            while (it.moveToNext()) {
                val contactId = it.getLong(idIndex)
                val displayName = it.getString(nameIndex) ?: ""
                val phoneNumber = it.getString(numberIndex) ?: ""
                val photoUri = it.getString(photoIndex)

                // Split name into first and last name
                val nameParts = displayName.split(" ", limit = 2)
                val firstName = nameParts.getOrNull(0) ?: ""
                val lastName = nameParts.getOrNull(1) ?: ""

                // Get email for this contact (optional - can be slow for many contacts)
                val email = getEmailForContact(contactId)

                // Create Contact object with the ID
                val contact = Contact(
                    id = contactId,
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumber = phoneNumber,
                    email = email,
                    photoUri = photoUri
                )

                contactsList.add(contact)
            }
        }

        // Update filtered list and RecyclerView
        filteredContactsList.clear()
        filteredContactsList.addAll(contactsList)

        // Initialize adapter if not already done
        if (!::contactAdapter.isInitialized) {
            contactAdapter = ContactAdapter(
                contacts = filteredContactsList,
                onClick = { contact ->
                    showContactDetails(contact)
                },
                onLongClick = { contact ->
                    showContactOptions(contact)
                }
            )
            recyclerView.adapter = contactAdapter
        } else {
            contactAdapter.notifyDataSetChanged()
        }
    }

    private fun getEmailForContact(contactId: Long): String? {
        var email: String? = null
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
            arrayOf(contactId.toString()),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val emailIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                email = it.getString(emailIndex)
            }
        }

        return email
    }

    private fun showContactDetails(contact: Contact) {
        val details = buildString {
            append("Name: ${contact.firstName} ${contact.lastName}\n")
            append("Phone: ${contact.phoneNumber}\n")
            if (!contact.email.isNullOrEmpty()) {
                append("Email: ${contact.email}\n")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Contact Details")
            .setMessage(details)
            .setPositiveButton("Call") { _, _ ->
                callContact(contact.phoneNumber)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun callContact(phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            Toast.makeText(this, "Cannot call: Phone number is missing.", Toast.LENGTH_SHORT).show()
            return
        }

        if (checkCallPhonePermission()) {
            try {
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
                startActivity(intent)
            } catch (e: SecurityException) {
                Toast.makeText(this, "Call permission error. Please check app settings.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Could not initiate call: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            requestCallPhonePermission()
        }
    }

    private fun showContactOptions(contact: Contact) {
        val options = arrayOf("Edit", "Delete", "Call")

        AlertDialog.Builder(this)
            .setTitle("Contact Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Edit - Open system contact editor
                        openSystemContactEditor(contact)
                    }
                    1 -> {
                        // Delete
                        confirmDeleteContact(contact)
                    }
                    2 -> {
                        // Call
                        callContact(contact.phoneNumber)
                    }
                }
            }
            .create()
            .show()
    }

    private fun openSystemContactEditor(contact: Contact) {
        try {
            // Check if WRITE_CONTACTS permission is granted
            if (!checkWriteContactPermission()) {
                requestWriteContactPermission()
                Toast.makeText(this, "Write permission required to edit contacts", Toast.LENGTH_SHORT).show()
                return
            }

            // Open custom EditContactActivity
            val intent = Intent(this, EditContactActivity::class.java).apply {
                putExtra("CONTACT_ID", contact.id)
                putExtra("FIRST_NAME", contact.firstName)
                putExtra("LAST_NAME", contact.lastName)
                putExtra("PHONE_NUMBER", contact.phoneNumber)
                putExtra("EMAIL", contact.email)
                putExtra("PHOTO_URI", contact.photoUri)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open contact editor: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun confirmDeleteContact(contact: Contact) {
        AlertDialog.Builder(this)
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete ${contact.firstName} ${contact.lastName}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteContact(contact)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteContact(contact: Contact) {
        try {
            // Check if WRITE_CONTACTS permission is granted
            if (!checkWriteContactPermission()) {
                requestWriteContactPermission()
                Toast.makeText(this, "Write permission required to delete contacts", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_URI,
                contact.id.toString()
            )

            val deleted = contentResolver.delete(uri, null, null)

            if (deleted > 0) {
                Toast.makeText(this, "Contact deleted successfully", Toast.LENGTH_SHORT).show()
                loadContacts() // Refresh the list
            } else {
                Toast.makeText(this, "Failed to delete contact", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error deleting contact: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}