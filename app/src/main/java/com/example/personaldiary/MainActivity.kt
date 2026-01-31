package com.example.personaldiary

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var tvEmpty: android.widget.TextView
    private lateinit var dbHelper: DiaryDatabaseHelper
    private lateinit var adapter: DiaryAdapter
    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = DiaryDatabaseHelper(this)
        recyclerView = findViewById(R.id.recyclerView)
        fabAdd = findViewById(R.id.fabAdd)
        tvEmpty = findViewById(R.id.tvEmpty)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DiaryAdapter(emptyList()) { entry ->
            showEditDialog(entry)
        }
        recyclerView.adapter = adapter

        loadEntries()

        fabAdd.setOnClickListener {
            showAddDialog()
        }
    }

    private fun loadEntries() {
        val entries = mutableListOf<DiaryEntry>()
        var cursor: android.database.Cursor? = null

        try {
            cursor = dbHelper.getAllEntries()
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(DiaryDatabaseHelper.COLUMN_ID))
                    val title = cursor.getString(cursor.getColumnIndexOrThrow(DiaryDatabaseHelper.COLUMN_TITLE))
                    val content = cursor.getString(cursor.getColumnIndexOrThrow(DiaryDatabaseHelper.COLUMN_CONTENT))

                    val imagePath = cursor.getString(cursor.getColumnIndexOrThrow(DiaryDatabaseHelper.COLUMN_IMAGE_PATH))
                        ?.takeIf { it.isNotBlank() }

                    val createdAt = cursor.getString(cursor.getColumnIndexOrThrow(DiaryDatabaseHelper.COLUMN_CREATED_AT))

                    entries.add(DiaryEntry(id, title, content, imagePath, createdAt))
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка загрузки: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            cursor?.close()
        }

        adapter.updateData(entries)
        tvEmpty.visibility = if (entries.isEmpty()) android.widget.TextView.VISIBLE else android.widget.TextView.GONE
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_entry, null)
        val etTitle = dialogView.findViewById<android.widget.EditText>(R.id.etTitle)
        val etContent = dialogView.findViewById<android.widget.EditText>(R.id.etContent)
        val btnSelectImage = dialogView.findViewById<android.widget.Button>(R.id.btnSelectImage)
        val ivPreview = dialogView.findViewById<android.widget.ImageView>(R.id.ivPreview)

        selectedImageUri = null
        ivPreview.visibility = android.widget.ImageView.GONE

        btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        AlertDialog.Builder(this)
            .setTitle("Новая запись")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val title = etTitle.text.toString().trim()
                val content = etContent.text.toString().trim()

                if (title.isNotEmpty() && content.isNotEmpty()) {
                    val imagePath = selectedImageUri?.let { saveImage(it) }
                    val id = dbHelper.addEntry(title, content, imagePath)

                    if (id != -1L) {
                        Toast.makeText(this, "Запись сохранена", Toast.LENGTH_SHORT).show()
                        loadEntries() // ← Здесь был краш из-за NULL в базе
                    } else {
                        Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditDialog(entry: DiaryEntry) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_entry, null)
        val etTitle = dialogView.findViewById<android.widget.EditText>(R.id.etTitle)
        val etContent = dialogView.findViewById<android.widget.EditText>(R.id.etContent)
        val btnSelectImage = dialogView.findViewById<android.widget.Button>(R.id.btnSelectImage)
        val ivPreview = dialogView.findViewById<android.widget.ImageView>(R.id.ivPreview)

        etTitle.setText(entry.title)
        etContent.setText(entry.content)

        if (!entry.imagePath.isNullOrBlank() && File(entry.imagePath).exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(entry.imagePath)
                if (bitmap != null) {
                    ivPreview.setImageBitmap(bitmap)
                    ivPreview.visibility = android.widget.ImageView.VISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        AlertDialog.Builder(this)
            .setTitle("Редактировать запись")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val title = etTitle.text.toString().trim()
                val content = etContent.text.toString().trim()

                if (title.isNotEmpty() && content.isNotEmpty()) {
                    val imagePath = selectedImageUri?.let { saveImage(it) } ?: entry.imagePath
                    val rows = dbHelper.updateEntry(entry.id, title, content, imagePath)

                    if (rows > 0) {
                        Toast.makeText(this, "Запись обновлена", Toast.LENGTH_SHORT).show()
                        loadEntries()
                    } else {
                        Toast.makeText(this, "Ошибка обновления", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Удалить") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("Удаление")
                    .setMessage("Удалить эту запись?")
                    .setPositiveButton("Да") { _, _ ->
                        val rows = dbHelper.deleteEntry(entry.id)
                        if (rows > 0) {
                            Toast.makeText(this, "Запись удалена", Toast.LENGTH_SHORT).show()
                            loadEntries()
                        }
                    }
                    .setNegativeButton("Нет", null)
                    .show()
            }
            .setNeutralButton("Отмена", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            Toast.makeText(this, "Изображение выбрано", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImage(uri: Uri): String? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "DIARY_$timeStamp.jpg"
            val storageDir = File(filesDir, "diary_images")

            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }

            val imageFile = File(storageDir, imageFileName)
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(imageFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            imageFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка сохранения изображения", Toast.LENGTH_SHORT).show()
            null //  null при ошибке
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}