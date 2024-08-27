package com.pratik.whatbytes

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentProviderOperation
import android.content.OperationApplicationException
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.RemoteException
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.PhoneLookup
import android.provider.ContactsContract.RawContacts
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class MainActivity : AppCompatActivity() {



    lateinit var total_contact_db : TextView
    lateinit var total_contact_pb : TextView
    lateinit var total_contact_added : TextView
    lateinit var database: FirebaseDatabase
    lateinit var prog : TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        total_contact_db = findViewById(R.id.tc)
        total_contact_pb = findViewById(R.id.cp)
        total_contact_added = findViewById(R.id.ca)
        prog = findViewById(R.id.prog)
        FirebaseApp.initializeApp(this);
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_CONTACTS),
                101
            )
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                101
            )
        }

    }

    fun sync(view: View) {
        database =  Firebase.database
        val databaseReference = database.reference.child("phonebook")

        var fetchedCount = 0
        var addedCount = 0
        var notAddedCount = 0

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val contactNumber = snapshot.key
                    val contactName = snapshot.value as String

                    if (contactNumber != null) {
                        fetchedCount++

                        if (!contactExists(this@MainActivity,contactNumber)) {
                            addc(contactName,contactNumber )
                            addedCount++
                            total_contact_added.text="Number of contacts added: $addedCount"

                        } else {
                            notAddedCount++
                        }
                        prog.text = "${((addedCount+notAddedCount)/dataSnapshot.children.count())*100}%"
                    }
                }

                total_contact_added.text="Number of contacts added: $addedCount"
                total_contact_db.text = "Number of contacts fetched: $fetchedCount"
                total_contact_pb.text = "Number of contacts already present: $notAddedCount"

            }

            override fun onCancelled(databaseError: DatabaseError) {
                println("Failed to read data from Firebase: ${databaseError.message}")
            }
        })
    }
    fun contactExists(_activity: Activity, number: String?): Boolean {
        return if (number != null) {
            val lookupUri: Uri =
                Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            val mPhoneNumberProjection =
                arrayOf(PhoneLookup._ID, PhoneLookup.NUMBER, PhoneLookup.DISPLAY_NAME)
            val cur: Cursor? =
                _activity.contentResolver.query(lookupUri, mPhoneNumberProjection, null, null, null)
            cur.use { cur ->
                if (cur != null) {
                    if (cur.moveToFirst()) {
                        return true
                    }
                }
            }
            false
        } else {
            false
        }
    }
    fun addc(name : String, Number : String){

        val ops = ArrayList<ContentProviderOperation>()
        val rawContactInsertIndex: Int = ops.size

        ops.add(
            ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, null)
                .withValue(RawContacts.ACCOUNT_NAME, null).build()
        )
        ops.add(
            ContentProviderOperation
                .newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME, name) // Name of the person
                .build()
        )
        ops.add(
            ContentProviderOperation
                .newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(
                    ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex
                )
                .withValue(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                .withValue(Phone.NUMBER, Number) // Number of the person
                .withValue(Phone.TYPE, Phone.TYPE_MOBILE).build()
        ) // Type of mobile number

        try {
            val res = contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        } catch (e: RemoteException) {
            // error
        } catch (e: OperationApplicationException) {
            // error
        }
    }
    @SuppressLint("Range")
    fun deleteContact(ctx: Activity, phone: String?, name: String?): Boolean {
        val contactUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone))
        val cur: Cursor? = ctx.contentResolver.query(contactUri, null, null, null, null)
        try {
            if (cur != null) {
                if (cur.moveToFirst()) {
                    do {
                        if (cur.getString(cur.getColumnIndex(PhoneLookup.DISPLAY_NAME))
                                .equals(name, ignoreCase = true)
                        ) {
                            val lookupKey =
                                cur.getString(cur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY))
                            val uri = Uri.withAppendedPath(
                                ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                                lookupKey
                            )
                            ctx.contentResolver.delete(uri, null, null)
                            return true
                        }
                    } while (cur.moveToNext())
                }
            }
        } catch (e: Exception) {
            println(e.stackTrace)
        } finally {
            cur?.close()
        }
        return false
    }

    fun dell(view: View) {
        database =  Firebase.database

        val databaseReference = database.reference.child("phonebook")

        var fetchedCount = 0
        var addedCount = 0
        var notAddedCount = 0

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val contactNumber = snapshot.key
                    val contactName = snapshot.value as String

                    if (contactNumber != null) {
                        fetchedCount++

                        if (contactExists(this@MainActivity,contactNumber)) {
                            deleteContact(this@MainActivity,contactNumber,contactName )
                            addedCount++
                            total_contact_added.text="Number of contacts deleted: $addedCount"

                        } else {
                            notAddedCount++
                        }
                        prog.text = "${((addedCount+notAddedCount)/dataSnapshot.children.count())*100}%"

                    }
                }
                total_contact_added.text="Number of contacts deleted: $addedCount"

                total_contact_db.text = "Number of contacts fetched: $fetchedCount"
                total_contact_pb.text = ""

            }

            override fun onCancelled(databaseError: DatabaseError) {
                println("Failed to read data from Firebase: ${databaseError.message}")
            }
        })
    }

}