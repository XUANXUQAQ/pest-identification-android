package com.rjgc.api;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Rect;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.PyTorchAndroid;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CnnApi {
    private Module module;
    private static final float mThreshold = 0.30f; // score above which a detection is generated
    private static final int mNmsLimit = 15;
    private static final float imageSize = 640;


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

    public void init(AssetManager assetManager, String moduleName) {
        module = PyTorchAndroid.loadModuleFromAsset(assetManager, moduleName);
    }

    public Map<String, Integer> predict(Bitmap bitmap, Object[] processedImg) {
        HashMap<String, Integer> resultMap = new HashMap<>();
        // preparing input tensor
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                new float[]{0.0f, 0.0f, 0.0f}, new float[]{1.0f, 1.0f, 1.0f});

        // running the model
        IValue value = IValue.from(inputTensor);
        final IValue[] output = module.forward(value).toTuple();
        final Tensor outputTensor = output[0].toTensor();
        final float[] outputs = outputTensor.getDataAsFloatArray();
        float mImgScaleX = bitmap.getWidth() / imageSize;
        float mImgScaleY = bitmap.getHeight() / imageSize;
        float mIvScaleX = (bitmap.getWidth() > bitmap.getHeight() ? imageSize / bitmap.getWidth() : imageSize / bitmap.getHeight());
        float mIvScaleY = (bitmap.getHeight() > bitmap.getWidth() ? imageSize / bitmap.getHeight() : imageSize / bitmap.getWidth());
        final ArrayList<Result> results = outputsToNMSPredictions(outputs, mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY);
        for (Result each : results) {
            String code = Classes.classes[each.classIndex];
            if (resultMap.containsKey(code)) {
                Integer num = resultMap.get(code);
                num += 1;
                resultMap.put(code, num);
            } else {
                resultMap.put(code, 1);
            }
        }
        // todo 在bitmap上画框
        processedImg[0] = bitmap;
        if (resultMap.isEmpty()) {
            resultMap.put("error", 1);
        }
        return resultMap;
    }

    private static ArrayList<Result> outputsToNMSPredictions(float[] outputs, float imgScaleX, float imgScaleY, float ivScaleX, float ivScaleY) {
        ArrayList<Result> results = new ArrayList<>();
        int mOutputRow = 25200;
        int mOutputColumn = Classes.classes.length + 5;
        for (int i = 0; i < mOutputRow; i++) {
            try {
                if (outputs[i * mOutputColumn + 4] > mThreshold) {
                    float x = outputs[i * mOutputColumn];
                    float y = outputs[i * mOutputColumn + 1];
                    float w = outputs[i * mOutputColumn + 2];
                    float h = outputs[i * mOutputColumn + 3];

                    float left = imgScaleX * (x - w / 2);
                    float top = imgScaleY * (y - h / 2);
                    float right = imgScaleX * (x + w / 2);
                    float bottom = imgScaleY * (y + h / 2);

                    float max = outputs[i * mOutputColumn + 5];
                    int cls = 0;
                    for (int j = 0; j < mOutputColumn - 5; j++) {
                        if (outputs[i * mOutputColumn + 5 + j] > max) {
                            max = outputs[i * mOutputColumn + 5 + j];
                            cls = j;
                        }
                    }

                    Rect rect = new Rect((int) ((float) 0 + ivScaleX * left), (int) ((float) 0 + top * ivScaleY), (int) ((float) 0 + ivScaleX * right), (int) ((float) 0 + ivScaleY * bottom));
                    Result result = new Result(cls, outputs[i * mOutputColumn + 4], rect);
                    results.add(result);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
        return nonMaxSuppression(results);
    }

    private static ArrayList<Result> nonMaxSuppression(ArrayList<Result> boxes) {

        // Do an argsort on the confidence scores, from high to low.
        Collections.sort(boxes,
                (o1, o2) -> o1.score.compareTo(o2.score));

        ArrayList<Result> selected = new ArrayList<>();
        boolean[] active = new boolean[boxes.size()];
        Arrays.fill(active, true);
        int numActive = active.length;

        // The algorithm is simple: Start with the box that has the highest score.
        // Remove any remaining boxes that overlap it more than the given threshold
        // amount. If there are any boxes left (i.e. these did not overlap with any
        // previous boxes), then repeat this procedure, until no more boxes remain
        // or the limit has been reached.
        boolean done = false;
        for (int i=0; i<boxes.size() && !done; i++) {
            if (active[i]) {
                Result boxA = boxes.get(i);
                selected.add(boxA);
                if (selected.size() >= CnnApi.mNmsLimit) break;

                for (int j=i+1; j<boxes.size(); j++) {
                    if (active[j]) {
                        Result boxB = boxes.get(j);
                        if (IOU(boxA.rect, boxB.rect) > CnnApi.mThreshold) {
                            active[j] = false;
                            numActive -= 1;
                            if (numActive <= 0) {
                                done = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return selected;
    }

    private static float IOU(Rect a, Rect b) {
        float areaA = (a.right - a.left) * (a.bottom - a.top);
        if (areaA <= 0.0) return 0.0f;

        float areaB = (b.right - b.left) * (b.bottom - b.top);
        if (areaB <= 0.0) return 0.0f;

        float intersectionMinX = Math.max(a.left, b.left);
        float intersectionMinY = Math.max(a.top, b.top);
        float intersectionMaxX = Math.min(a.right, b.right);
        float intersectionMaxY = Math.min(a.bottom, b.bottom);
        float intersectionArea = Math.max(intersectionMaxY - intersectionMinY, 0) *
                Math.max(intersectionMaxX - intersectionMinX, 0);
        return intersectionArea / (areaA + areaB - intersectionArea);
    }

    static class Result {
        int classIndex;
        Float score;
        Rect rect;

        public Result(int cls, Float output, Rect rect) {
            this.classIndex = cls;
            this.score = output;
            this.rect = rect;
        }
    }
}