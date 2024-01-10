package com.example.last;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import android.media.ThumbnailUtils;
import android.net.Uri;

import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.last.ml.PaddyTf;

import org.tensorflow.lite.DataType;

import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;


import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class Second extends AppCompatActivity {
    Button selectBtn,captureBtn,predictBtn;
    ImageView imageView;
    TextView result;
    Bitmap bitmap;

    int imageSize = 224;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        //permission
        getPermission();


        selectBtn = findViewById(R.id.selectBtn);
        predictBtn = findViewById(R.id.predictBtn);
        captureBtn = findViewById(R.id.captureBtn);
        result = findViewById(R.id.result);
        imageView = findViewById(R.id.imageView);

        selectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 10);
            }
        });
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 12);
            }
        });
        predictBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {



                    PaddyTf model = PaddyTf.newInstance(getApplicationContext());

                    // Creates inputs for reference.
                    TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
                    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
                    byteBuffer.order(ByteOrder.nativeOrder());

                    int[] intValues = new int[imageSize * imageSize];
                    bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

                    int pixel = 0;
                    // Iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
                    for (int i = 0; i < imageSize; i++) {
                        for (int j = 0; j < imageSize; j++) {
                            int val = intValues[pixel++]; // RGB
                            byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 1));
                            byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 1));
                            byteBuffer.putFloat((val & 0xFF) * (1.f / 1));
                        }
                    }

                    inputFeature0.loadBuffer(byteBuffer);

                    String[] classes = {"bacterial_leaf_blight", "brown_spot", "healthy","leaf_blast","leaf_scald","narrow_brown_spot"};
                    String[] classDescriptions = {"To prevent bacterial leaf blight in paddy leaves, ensure proper field sanitation by removing infected plant debris and use resistant varieties or apply appropriate fungicides as recommended by agricultural experts",
                            "To prevent brown spot in paddy leaves, maintain proper water management to avoid waterlogged conditions and apply recommended fungicides at the early stages of the disease as advised by agricultural experts.",
                            "To maintain healthy paddy leaves, practice good crop rotation and avoid continuous rice cultivation in the same field. Additionally, implement proper nutrient management and follow recommended fertilization practices to ensure optimal plant health.",
                            "To prevent leaf blast in paddy leaves, use resistant varieties, implement proper water management techniques to avoid prolonged leaf wetness, and apply fungicides at the recommended stages as per agricultural guidelines.",
                            "To prevent leaf scald in paddy leaves, avoid excessive nitrogen application, provide adequate spacing between plants for better air circulation, and promptly remove and destroy infected plant material to minimize disease spread.",
                            "To prevent narrow brown spot in paddy leaves, practice crop rotation and avoid continuous rice cultivation in the same field. Additionally, maintain proper water management by ensuring adequate drainage to minimize the risk of the disease."};

                    // Runs model inference and gets result.
                    PaddyTf.Outputs outputs = model.process(inputFeature0);
                    TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                    float[] confidences = outputFeature0.getFloatArray();

                    // ...

                    StringBuilder confidenceText = new StringBuilder();

                    for (int i = 0; i < confidences.length; i++) {
                        String className = classes[i];
                        float confidence = confidences[i];
                        float confidencePercentage = confidence * 100;
                        String formattedConfidence = String.format("%.2f%%", confidencePercentage); // Format confidence as percentage

                        if (confidence >= 0.85) {
                            confidenceText.append(className).append(": ").append(formattedConfidence).append("\n");
                        }
                    }

// ...


                    // Find the index of the class with the highest confidence.
                    int maxPos = 0;
                    float maxConfidence = 0;
                    for (int i = 0; i < confidences.length; i++) {
                        if (confidences[i] > maxConfidence) {
                            maxConfidence = confidences[i];
                            maxPos = i;
                        }
                    }




                    String prediction = "Prediction: " + classes[maxPos];
                    String precaution = "Precaution: " + classDescriptions[maxPos];

                    if (maxConfidence < 0.85) {
                        prediction = "Unfortunately, the model was unable to accurately detect the type of paddy leaf based on the provided image. Please ensure that the image is clear and representative of a paddy leaf, and try again.";
                        precaution = "";
                        confidenceText = new StringBuilder(); // Reset confidenceText to empty
                    }

                    result.setText(prediction + "\n" + precaution);

                    if (maxConfidence >= 0.85) {
                        result.append("\n\nConfidence:\n" + confidenceText.toString());
                    }


                    // Release model resources if no longer used.
                    model.close();
                } catch (IOException e) {
                    // TODO Handle the exception
                }
            }
        });


    }








    void getPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(Second.this,new String[]{Manifest.permission.CAMERA},11);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode==11){
            if (grantResults.length>0){
                if (grantResults[0] !=PackageManager.PERMISSION_GRANTED){
                    this.getPermission();

                }

            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==10){
            if(data!=null){
                Uri uri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),uri);
                    int dimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
                    bitmap = ThumbnailUtils.extractThumbnail(bitmap,dimension,dimension);
                    bitmap = Bitmap.createScaledBitmap(bitmap,224,224,true);
                    imageView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else if(requestCode==12){
            bitmap = (Bitmap) data.getExtras().get("data");
            int dimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
            bitmap = ThumbnailUtils.extractThumbnail(bitmap,dimension,dimension);
            bitmap = Bitmap.createScaledBitmap(bitmap,224,224,true);
            imageView.setImageBitmap(bitmap);

        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}