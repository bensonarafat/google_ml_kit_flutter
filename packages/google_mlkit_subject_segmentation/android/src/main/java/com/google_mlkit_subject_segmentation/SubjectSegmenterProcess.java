package com.google_mlkit_subject_segmentation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;


import androidx.annotation.NonNull;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions;
import com.google_mlkit_commons.InputImageConverter;

public class SubjectSegmenterProcess implements MethodChannel.MethodCallHandler {
    private static final String START = "vision#startSubjectSegmenter";
    private static final String CLOSE = "vision#closeSubjectSegmenter";

    private final Context context;

    private int imageWidth;
    private int imageHeight;

    private final Map<String, SubjectSegmenter> instances = new HashMap<>();

    public SubjectSegmenterProcess(Context context) {
        this.context = context;
    }
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        String method = call.method;


        switch (method) {
            case START:
                handleDetection(call, result);
                break;
            case CLOSE:
                closeDetector(call);
                result.success(null);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private SubjectSegmenter initialize(MethodCall call) {
        Boolean enableForegroundConfidenceMask = call.argument("enableForegroundConfidenceMask");
        Boolean enableForegroundBitmap = call.argument("enableForegroundBitmap");
        SubjectSegmenterOptions.Builder builder = new SubjectSegmenterOptions.Builder();

        if(Boolean.TRUE.equals(enableForegroundConfidenceMask)){
            builder.enableForegroundConfidenceMask();
        }
        if(Boolean.TRUE.equals(enableForegroundBitmap)){
            builder.enableForegroundBitmap();
        }

        SubjectSegmenterOptions options = builder.build();
        return SubjectSegmentation.getClient(options);
    }

    private void handleDetection(MethodCall call, MethodChannel.Result result){
        Map<String, Object> imageData = (Map<String, Object>) call.argument("imageData");
        InputImage inputImage = InputImageConverter.getInputImageFromData(imageData, context, result);
        if (inputImage == null) return;
        imageHeight = inputImage.getHeight();
        imageWidth = inputImage.getWidth();
        String id = call.argument("id");
        SubjectSegmenter subjectSegmenter = instances.get(id);
        if (subjectSegmenter == null) {
            subjectSegmenter = initialize(call);
            instances.put(id, subjectSegmenter);
        }

       subjectSegmenter.process(inputImage)
               .addOnSuccessListener( subjectSegmentationResult -> {
                   Map<String, Object> map = new HashMap<>();
                   FloatBuffer foregroundMask = subjectSegmentationResult.getForegroundConfidenceMask();
                   Bitmap foregroundBitmap = subjectSegmentationResult.getForegroundBitmap();
                   int[] colors = new int[imageWidth * imageHeight];
                   for (int i = 0; i < imageWidth* imageHeight; i++) {
                       if (foregroundMask.get() > 0.5f) {
                           colors[i] = Color.argb(128, 255, 0, 255);
                       }
                   }
                   result.success(map);

               }).addOnFailureListener( e -> result.error("Subject segmentation failed!", e.getMessage(), e) );
    }

    private void closeDetector(MethodCall call) {
        String id = call.argument("id");
        SubjectSegmenter subjectSegmenter = instances.get(id);
        if (subjectSegmenter == null) return;
        subjectSegmenter.close();
        instances.remove(id);
    }
}
