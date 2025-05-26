package com.example.aquaboost;

// Imports required Android and Firebase libraries
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IntakePageFragment extends Fragment {

    // UI component declarations
    private EditText ageInput, heightInput, weightInput;
    private RadioGroup sexGroup;
    private Spinner activitySpinner;
    private TextView bmiResultText, intakeAmountText;
    private ProgressBar intakeProgressBar;
    private Button addWaterButton, resetButton, setGoalButton;
    private CardView hydrationCard;

    // Variables to hold water intake data
    private int intakeAmount = 0;        // Amount of water consumed
    private int dailyGoal = 2000;        // Default daily water intake goal

    // Firebase variables
    private DatabaseReference dbRef;     // Reference to Firebase Realtime Database
    private FirebaseUser currentUser;    // Logged-in user

    // Default constructor
    public IntakePageFragment() {
    }

    // Factory method to create new instance with parameters (currently unused)
    public static IntakePageFragment newInstance(String param1, String param2) {
        IntakePageFragment fragment = new IntakePageFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_intake_page, container, false);

        // Initialize UI elements
        ageInput = view.findViewById(R.id.ageInput);
        heightInput = view.findViewById(R.id.heightInput);
        weightInput = view.findViewById(R.id.weightInput);
        sexGroup = view.findViewById(R.id.sexGroup);
        activitySpinner = view.findViewById(R.id.activitySpinner);
        bmiResultText = view.findViewById(R.id.bmiResultText);
        intakeAmountText = view.findViewById(R.id.intakeAmountText);
        intakeProgressBar = view.findViewById(R.id.intakeProgressBar);
        addWaterButton = view.findViewById(R.id.addWaterButton);
        resetButton = view.findViewById(R.id.resetButton);
        setGoalButton = view.findViewById(R.id.setGoalButton);
        hydrationCard = view.findViewById(R.id.hydrationCard);

        // Initially hide the hydration result card
        hydrationCard.setVisibility(View.GONE);

        // Set up Firebase references
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        dbRef = FirebaseDatabase.getInstance().getReference("intake");

        // Populate the activity level spinner with options from XML
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(), R.array.activity_types, android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activitySpinner.setAdapter(adapter);

        // Set button click actions
        setGoalButton.setOnClickListener(v -> calculateGoal());

        addWaterButton.setOnClickListener(v -> {
            // Increment intake and update UI and Firebase
            intakeAmount += 250;
            if (intakeAmount > dailyGoal) intakeAmount = dailyGoal;
            updateUI();
            saveIntakeToFirebase();
        });

        resetButton.setOnClickListener(v -> {
            // Reset intake and update UI and Firebase
            intakeAmount = 0;
            updateUI();
            saveIntakeToFirebase();
        });

        // Set initial UI state
        updateUI();
        return view;
    }

    // Method to calculate daily water intake goal and BMI
    private void calculateGoal() {
        // Read input values
        String ageStr = ageInput.getText().toString();
        String heightStr = heightInput.getText().toString();
        String weightStr = weightInput.getText().toString();

        // Validate all inputs are provided
        if (ageStr.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty() ||
                sexGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse and calculate BMI
        int age = Integer.parseInt(ageStr);
        double height = Double.parseDouble(heightStr) / 100.0; // convert cm to meters
        double weight = Double.parseDouble(weightStr);
        boolean isMale = sexGroup.getCheckedRadioButtonId() == R.id.maleOption;

        double bmi = weight / (height * height);
        bmiResultText.setText(String.format("BMI: %.1f", bmi));

        // Set base water intake goal based on sex and age
        dailyGoal = isMale ? (age < 18 ? 2100 : 3000) : (age < 18 ? 1800 : 2200);

        // Adjust goal based on activity level
        String activity = activitySpinner.getSelectedItem().toString();
        if (activity.equalsIgnoreCase("Active")) {
            dailyGoal += 300;
        } else if (activity.equalsIgnoreCase("Very Active")) {
            dailyGoal += 600;
        }

        Toast.makeText(getContext(), "Daily goal set: " + dailyGoal + " ml", Toast.LENGTH_SHORT).show();

        // Reset intake and update display
        intakeAmount = 0;
        updateUI();

        // Make hydration summary visible
        hydrationCard.setVisibility(View.VISIBLE);
    }

    // Updates the UI elements like progress bar and intake text
    private void updateUI() {
        intakeAmountText.setText(intakeAmount + " ml");
        intakeProgressBar.setMax(dailyGoal);
        intakeProgressBar.setProgress(intakeAmount);
    }

    // Saves current water intake to Firebase for the logged-in user
    private void saveIntakeToFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Format current date for tracking daily intake
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());

        // Save intake value to Firebase database
        dbRef.child(currentUser.getUid()).child(currentDate).setValue(intakeAmount)
                .addOnSuccessListener(unused -> {
                    Log.d("FIREBASE", "Saved: " + intakeAmount);
                    Toast.makeText(getContext(), "Saved to Firebase", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE", "Save failed", e);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
