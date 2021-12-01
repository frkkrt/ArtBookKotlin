package com.furkankurt.artbookkotlin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.furkankurt.artbookkotlin.databinding.ActivityDetailsBinding
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.jar.Manifest

class DetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailsBinding

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var permissonLauncher: ActivityResultLauncher<String>

    var selectedBitmap : Bitmap?=null

    private lateinit var database:SQLiteDatabase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityDetailsBinding.inflate(layoutInflater)
        val view=binding.root
        setContentView(view)
        //Soyut Sınıf
        database=this.openOrCreateDatabase("Arts",Context.MODE_PRIVATE,null)
        registerLauncher()

        val intent=intent
        val info=intent.getStringExtra("info")
        if(info.equals("new"))
        {
            binding.artnameText.setText("")
            binding.artistnameText2.setText("")
            binding.yearnameText.setText("")
            binding.save.visibility=View.VISIBLE
            binding.imageView.setImageResource(R.drawable.resim)
        }
        else {
            binding.save.visibility = View.INVISIBLE
            val selectedId = intent.getIntExtra("id", 1)
            val cursor =
                database.rawQuery("SELECT * FROM arts WHERE id=?", arrayOf(selectedId.toString()))

            val artNameIx = cursor.getColumnIndex("artname")
            val artListNameIx = cursor.getColumnIndex("artistname")
            val yearIx = cursor.getColumnIndex("year")
            val imageIx=cursor.getColumnIndex("image")
            while(cursor.moveToNext())
            {
                binding.artistnameText2.setText(cursor.getString(artListNameIx))
                binding.artnameText.setText(cursor.getString(artNameIx))
                binding.yearnameText.setText(cursor.getString(yearIx))

                val byteArray=cursor.getBlob(imageIx)
                val bitmap =BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                binding.imageView.setImageBitmap(bitmap)
            }
            cursor.close()
        }

    }
    fun saveClick(view:View)
    {
        val artName=binding.artnameText.text.toString()
        val artistName=binding.artistnameText2.text.toString()
        val year=binding.yearnameText.text.toString()
        if(selectedBitmap!=null)
        {
            val smallBitmap=makeSmallerBitmap(selectedBitmap!!,300)
            val outputStream=ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray=outputStream.toByteArray()
            try{
                //val database=this.openOrCreateDatabase("Arts",Context.MODE_PRIVATE,null)
                database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY,artname VARCHAR,artistname VARCHAR,year VARCHAR,image BLOB)")
                val sqlString="INSERT INTO arts(artname,artistname,year,image) VALUES(? , ?, ?, ?)"
                //SQL STRİNGİNİ BURAYA VEREBİLİYORUZ.? KISMI DOLDURMUŞ OLUYORUZ.
                val statement=database.compileStatement(sqlString)
                statement.bindString(1,artName)
                statement.bindString(2,artistName)
                statement.bindString(3,year)
                statement.bindBlob(4,byteArray)
                statement.execute()

                val intent=Intent(this,MainActivity::class.java)
                //ARKADA ne kadar activity varsa kapatıyoruz.
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)


            }
            catch (e:Exception){

            }

        }
    }
    private fun makeSmallerBitmap(image:Bitmap,maximumSize:Int):Bitmap{
        var width=image.width
        var height=image.height

        val bitmapRatio:Double=width.toDouble() / height.toDouble()
        if(bitmapRatio>1)
        {
            //landscape
            width=maximumSize
            val scaledHeigh=width/bitmapRatio
            height=scaledHeigh.toInt()
        }
        else
        {
            //portrait
            height=maximumSize
            val scaledWidth=height*bitmapRatio
            width=scaledWidth.toInt()

        }

        return Bitmap.createScaledBitmap(image,width,height,true)
    }

    fun selectimage(view:View)
    {
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)
        {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,android.Manifest.permission.READ_EXTERNAL_STORAGE))
            {
                //rationale (İzin alma mantığını kullanıcıya gösterim mi)
                Snackbar.make(view,"Permission Needed For Gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",View.OnClickListener {
                    //request permission
                    permissonLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }).show()
            }
            else
            {
                //Request Permission
                permissonLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        else
        {
            //Galeriye Gitme Kodu
            val intentToGallery=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            //intent
            activityResultLauncher.launch(intentToGallery)
        }

    }
    private fun registerLauncher()
    {
        activityResultLauncher=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if(result.resultCode== RESULT_OK)
            {
                val intentFromResult=result.data
                if(intentFromResult!=null)
                {
                    val imageData=intentFromResult.data
                    //binding.imageView.setImageURI(imageData)
                    if(imageData!=null)
                    {
                        try
                        {
                            if(Build.VERSION.SDK_INT>=28)
                            {
                                val source =ImageDecoder.createSource(this.contentResolver,imageData)
                                selectedBitmap=ImageDecoder.decodeBitmap(source)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }
                            else
                            {
                                selectedBitmap=MediaStore.Images.Media.getBitmap(contentResolver,imageData)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }


                        }
                        catch (e:Exception)
                        {
                            e.printStackTrace()
                        }

                    }

                }
            }
        }
        permissonLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if(result)
            {
                //permission granted
                val intentToGallery=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
            else
            {
                Toast.makeText(this,"Permission Needed",Toast.LENGTH_LONG).show()
                //permission denied
            }
        }
    }
}