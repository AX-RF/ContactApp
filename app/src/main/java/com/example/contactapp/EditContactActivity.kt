package com.example.contactapp

import android.Manifest
import android.app.Activity
import android.content.ContentProviderOperation
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class EditContactActivity : AppCompatActivity() {

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var etEmail: EditText
    private lateinit var imgContactPhoto: ImageView
    private lateinit var btnSelectPhoto: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private var contactId: Long = -1
    private var photoUri: String? = null

    private val PICK_IMAGE_REQUEST = 1
    private val WRITE_CONTACTS_PERMISSION_CODE = 103

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_contact)

        // Initialize views
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        etEmail = findViewById(R.id.etEmail)
        imgContactPhoto = findViewById(R.id.imgContactPhoto)
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        // Get contact data from intent
        contactId = intent.getLongExtra("CONTACT_ID", -1)
        etFirstName.setText(intent.getStringExtra("FIRST_NAME"))
        etLastName.setText(intent.getStringExtra("LAST_NAME"))
        etPhoneNumber.setText(intent.getStringExtra("PHONE_NUMBER"))
        etEmail.setText(intent.getStringExtra("EMAIL"))
        photoUri = intent.getStringExtra("PHOTO_URI")

        // Display existing photo if available
        photoUri?.let {
            imgContactPhoto.setImageURI(Uri.parse(it))
        }

        // Set up click listeners
        btnSelectPhoto.setOnClickListener {
            openImagePicker()
        }

        btnSave.setOnClickListener {
            saveContact()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri = data.data
            selectedImageUri?.let {
                imgContactPhoto.setImageURI(it)
                photoUri = it.toString()
            }
        }
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

        if (requestCode == WRITE_CONTACTS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveContact()
            } else {
                Toast.makeText(this, "Write permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveContact() {
        // Check permission first
        if (!checkWriteContactPermission()) {
            requestWriteContactPermission()
            return
        }

        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val phoneNumber = etPhoneNumber.text.toString().trim()
        val email = etEmail.text.toString().trim()

        // Validation
        if (firstName.isEmpty()) {
            etFirstName.error = "First name is required"
            return
        }

        if (phoneNumber.isEmpty()) {
            etPhoneNumber.error = "Phone number is required"
            return
        }

        try {
            // Update contact in system contacts
            val fullName = "$firstName $lastName".trim()

            // Create batch operations to update the contact
            val operations = ArrayList<ContentProviderOperation>()

            // Get the raw contact ID for this contact
            val rawContactId = getRawContactId(contactId)

            if (rawContactId != null) {
                // Update display name
                operations.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            ContactsContract.Data.RAW_CONTACT_ID + "=? AND " +
                                    ContactsContract.Data.MIMETYPE + "=?",
                            arrayOf(
                                rawContactId.toString(),
                                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                            )
                        )
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, fullName)
                        .build()
                )

                // Update phone number
                operations.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            ContactsContract.Data.RAW_CONTACT_ID + "=? AND " +
                                    ContactsContract.Data.MIMETYPE + "=?",
                            arrayOf(
                                rawContactId.toString(),
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                            )
                        )
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                        .build()
                )

                // Update email if provided
                if (email.isNotEmpty()) {
                    operations.add(
                        ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection(
                                ContactsContract.Data.RAW_CONTACT_ID + "=? AND " +
                                        ContactsContract.Data.MIMETYPE + "=?",
                                arrayOf(
                                    rawContactId.toString(),
                                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                                )
                            )
                            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                            .build()
                    )
                }

                // Apply batch update
                contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)

                Toast.makeText(this, "Contact updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to find contact", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error updating contact: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun getRawContactId(contactId: Long): Long? {
        var rawContactId: Long? = null
        val cursor = contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            ContactsContract.RawContacts.CONTACT_ID + "=?",
            arrayOf(contactId.toString()),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                rawContactId = it.getLong(0)
            }
        }

        return rawContactId
    }
}