package com.rjgc.api;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.FileInputStream;
import java.io.IOException;

public class CnnApi {

    private Module module;

    public static volatile CnnApi instance;

    private CnnApi() {
    }

    public static CnnApi getInstance() {
        if (instance == null) {
            synchronized (CnnApi.class) {
                if (instance == null) {
                    instance = new CnnApi();
                }
            }
        }
        return instance;
    }

    public void init(String modelPath) {
        module = Module.load(modelPath);
    }

    public String predict(String image) {
        Bitmap bitmap;
        try (FileInputStream fis = new FileInputStream(image)) {
            bitmap = BitmapFactory.decodeStream(fis);
            // preparing input tensor
            final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                    TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);

            // running the model
            final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();

            // getting tensor content as java array of floats
            final float[] scores = outputTensor.getDataAsFloatArray();

            // searching for the index with maximum score
            float maxScore = -Float.MAX_VALUE;
            int maxScoreIdx = -1;
            for (int i = 0; i < scores.length; i++) {
                if (scores[i] > maxScore) {
                    maxScore = scores[i];
                    maxScoreIdx = i;
                }
            }
            return Classes.classes[maxScoreIdx];
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
