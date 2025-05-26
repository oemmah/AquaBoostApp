package com.example.aquaboost;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * HomeFragment - Main dashboard fragment displaying water intake analytics
 * This fragment shows daily intake tracking via line chart visualization,
 * today's intake summary, and monthly totals using Firebase Realtime Database
 */
public class HomeFragment extends Fragment {

    // =========================== UI COMPONENTS ===========================
    // Line chart component for visualizing daily water intake trends over time
    private LineChart intakeLineChart;

    // Text view displaying today's water intake in milliliters
    private TextView todayIntakeText;

    // Text view showing monthly cumulative water intake summary
    private TextView monthlySummaryText;

    /**
     * Required empty public constructor for Fragment instantiation
     * Fragment system uses this constructor when recreating fragments
     */
    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * FRAGMENT LIFECYCLE: View Creation
     * Creates and initializes the fragment's user interface
     * @param inflater Layout inflater for creating views from XML
     * @param container Parent view that fragment UI will be attached to
     * @param savedInstanceState Previously saved fragment state (if any)
     * @return Inflated view containing the fragment's UI
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the fragment layout from XML
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // INITIALIZE UI COMPONENTS: Connect Java objects to XML views
        intakeLineChart = view.findViewById(R.id.intakeLineChart);       // Chart for data visualization
        todayIntakeText = view.findViewById(R.id.todayIntakeText);       // Today's intake display
        monthlySummaryText = view.findViewById(R.id.monthlySummaryText); // Monthly total display

        // LOAD DATA: Fetch and display analytics data from Firebase
        loadAnalyticsData();

        return view;
    }

    /**
     * FIREBASE DATA LOADING AND ANALYTICS
     * Retrieves water intake data from Firebase Realtime Database,
     * processes it for visualization, and updates UI components
     */
    private void loadAnalyticsData() {
        // AUTHENTICATION CHECK: Ensure user is logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return; // Exit if no authenticated user

        // DATABASE REFERENCE: Point to user's intake data in Firebase
        // Database structure: /intake/{userId}/{date} -> intake_value
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("intake")
                .child(user.getUid());

        // SINGLE DATA FETCH: Get all intake data once (not real-time listener)
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // DATA PROCESSING VARIABLES
                List<Entry> entries = new ArrayList<>(); // Chart data points
                int total = 0;                          // Monthly cumulative total
                int index = 0;                          // Chart x-axis index counter

                // Get today's date for comparison (format: yyyy-MM-dd)
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                // ITERATE THROUGH FIREBASE DATA
                // Each child represents a date with its corresponding intake value
                for (DataSnapshot dateSnap : snapshot.getChildren()) {
                    String date = dateSnap.getKey();        // Date key (yyyy-MM-dd)
                    Integer value = dateSnap.getValue(Integer.class); // Intake value in ml

                    // Skip invalid data entries
                    if (value == null) continue;

                    // ADD TO CHART DATA: Create chart entry point
                    entries.add(new Entry(index++, value)); // x=index, y=intake_value

                    // ACCUMULATE MONTHLY TOTAL
                    total += value;

                    // CHECK FOR TODAY'S DATA: Update today's intake display
                    if (date.equals(today)) {
                        todayIntakeText.setText("Today's Intake: " + value + " ml");
                    }
                }

                // CHART STYLING AND CONFIGURATION
                LineDataSet dataSet = new LineDataSet(entries, "Daily Intake");

                // Visual styling for the line chart
                dataSet.setColor(Color.parseColor("#00BFFF"));         // Line color: DeepSkyBlue
                dataSet.setCircleColor(Color.parseColor("#1E90FF"));   // Data point color: DodgerBlue
                dataSet.setLineWidth(2f);                              // Line thickness
                dataSet.setCircleRadius(4f);                           // Data point size
                dataSet.setValueTextColor(Color.DKGRAY);               // Value label color
                dataSet.setValueTextSize(10f);                         // Value label text size

                // APPLY DATA TO CHART
                LineData lineData = new LineData(dataSet);
                intakeLineChart.setData(lineData);                     // Set chart data
                intakeLineChart.getDescription().setEnabled(false);    // Hide default description
                intakeLineChart.invalidate();                          // Refresh/redraw chart

                // UPDATE MONTHLY SUMMARY
                monthlySummaryText.setText("Monthly Total: " + total + " ml");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // ERROR HANDLING: Firebase database read operation failed
                // TODO: Implement proper error handling
                // - Show user-friendly error message
                // - Log error for debugging
                // - Possibly retry the operation
                // - Fall back to cached data if available
            }
        });
    }
}