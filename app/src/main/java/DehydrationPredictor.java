import android.content.Context;
import android.content.res.AssetFileDescriptor;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class DehydrationPredictor {
    private Interpreter tflite;

    public DehydrationPredictor(Context context) throws IOException {
        tflite = new Interpreter(loadModelFile(context));
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd("dehydration_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public String predictRisk(float age, float weight, float height, float waterIntake, int activityLevel) {
        float[][] input = new float[1][5]; // assuming 5 inputs
        input[0][0] = age;
        input[0][1] = weight;
        input[0][2] = height;
        input[0][3] = waterIntake;
        input[0][4] = activityLevel;

        float[][] output = new float[1][1];
        tflite.run(input, output);

        float prediction = output[0][0];
        if (prediction < 0.33) return "Low Risk";
        else if (prediction < 0.66) return "Mild Risk";
        else return "High Risk";
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
        }
    }
}
