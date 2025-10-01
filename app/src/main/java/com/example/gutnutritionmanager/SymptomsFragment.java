package com.example.gutnutritionmanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import java.util.Date;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

public class SymptomsFragment extends Fragment {
    private NutritionViewModel viewModel;
    private EditText symptomNameEditText;
    private RadioGroup severityRadioGroup;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_symptoms, container, false);

        // Initialize ViewModel
        NutritionViewModelFactory factory = new NutritionViewModelFactory(requireActivity().getApplication());
        viewModel = new ViewModelProvider(requireActivity(), factory).get(NutritionViewModel.class);

        symptomNameEditText = view.findViewById(R.id.symptomNameEditText);
        severityRadioGroup = view.findViewById(R.id.severityRadioGroup);
        Button logSymptomButton = view.findViewById(R.id.logSymptomButton);

        logSymptomButton.setOnClickListener(v -> logSymptom());

        //enable quick symptoms selection
        setupQuickSymptoms(view);

        return view;
    }
    private void showQuickSelectionToast(String symptomName) {
        Toast.makeText(requireContext(), "Selected: " + symptomName, Toast.LENGTH_SHORT).show();
    }
    private String normalizeSymptomName(String symptomName) {
        return symptomName.trim().toLowerCase();
    }
    private void setSymptom(String symptomName, int defaultSeverity) {
        // Set the symptom name in EditText
        EditText symptomEditText = getView().findViewById(R.id.symptomNameEditText);
        symptomEditText.setText(symptomName);

        // Set the default severity in RadioGroup
        setSeverityLevel(defaultSeverity);

        // Optional: Show a quick confirmation
        showQuickSelectionToast(symptomName);
    }

    private void setSeverityLevel(int severity) {
        RadioGroup severityGroup = getView().findViewById(R.id.severityRadioGroup);

        switch (severity) {
            case 1:
                severityGroup.check(R.id.severity1);
                break;
            case 2:
                severityGroup.check(R.id.severity2);
                break;
            case 3:
                severityGroup.check(R.id.severity3);
                break;
            case 4:
                severityGroup.check(R.id.severity4);
                break;
            case 5:
                severityGroup.check(R.id.severity5);
                break;
            default:
                severityGroup.check(R.id.severity3); // Default to medium
        }
    }

    private void setupQuickSymptoms(View view) {
        // Get references to all quick symptom buttons
        Button btnBloating = view.findViewById(R.id.btnBloating);
        Button btnPain = view.findViewById(R.id.btnPain);
        Button btnGas = view.findViewById(R.id.btnGas);
        Button btnDiarrhea = view.findViewById(R.id.btnDiarrhea);
        Button btnConstipation = view.findViewById(R.id.btnConstipation);
        Button btnNausea = view.findViewById(R.id.btnNausea);

        // Set click listeners for each button
        btnBloating.setOnClickListener(v -> setSymptom("Bloating", 3));
        btnPain.setOnClickListener(v -> setSymptom("Abdominal Pain", 3));
        btnGas.setOnClickListener(v -> setSymptom("Gas", 2));
        btnDiarrhea.setOnClickListener(v -> setSymptom("Diarrhea", 4));
        btnConstipation.setOnClickListener(v -> setSymptom("Constipation", 3));
        btnNausea.setOnClickListener(v -> setSymptom("Nausea", 3));
    }

    private void logSymptom() {
        String symptomName = symptomNameEditText.getText().toString().trim();

        if (symptomName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a symptom", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected severity
        int selectedId = severityRadioGroup.getCheckedRadioButtonId();
        int severity = getSeverityFromRadioGroup(selectedId);

        // Create a log entry first (required for the foreign key relationship)
        LogEntry logEntry = new LogEntry();
        logEntry.timestamp = new Date(); // Using Date as in your LogEntry class
        logEntry.foods = "Symptom: " + symptomName; // Store symptom info in foods field

        // Insert log entry and then symptom
        viewModel.insertLogEntryWithSymptom(logEntry, symptomName, severity);

        //DEBUG: Check if symptoms are saved
        viewModel.debugSymptoms();

        // Clear form
        symptomNameEditText.setText("");
        severityRadioGroup.check(R.id.severity3);

        Toast.makeText(requireContext(), "Symptom logged successfully", Toast.LENGTH_SHORT).show();
    }

    private int getSeverityFromRadioGroup(int selectedId) {
        if (selectedId == R.id.severity1) return 1;
        else if (selectedId == R.id.severity2) return 2;
        else if (selectedId == R.id.severity3) return 3;
        else if (selectedId == R.id.severity4) return 4;
        else if (selectedId == R.id.severity5) return 5;
        else return 3; // default to medium
    }
    /*
    private void logSymptom() {
        String symptomName = symptomNameEditText.getText().toString().trim();
        if (symptomName.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter a symptom ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected severity
        int selectedId = severityRadioGroup.getCheckedRadioButtonId();
        final int severity;
        if (selectedId == R.id.severity1) severity = 1;
        else if (selectedId == R.id.severity2) severity = 2;
        else if (selectedId == R.id.severity3) severity = 3;
        else if (selectedId == R.id.severity4) severity = 4;
        else if (selectedId == R.id.severity5) severity = 5;
        else severity = 3; // default

        // Get the most recent log entry ID
        viewModel.getAllEntries().observe(getViewLifecycleOwner(), new Observer<List<LogEntry>>() {
            @Override
            public void onChanged(List<LogEntry> logEntries) {
                if (logEntries != null && !logEntries.isEmpty()) {
                    int lastEntryId = logEntries.get(0).id;
                    String normalizedSymptom = normalizeSymptomName(symptomName);
                    Symptom newSymptom = new Symptom(lastEntryId, normalizedSymptom, severity);
                    //Symptom newSymptom = new Symptom(lastEntryId, symptomName, severity);
                    viewModel.insertSymptom(newSymptom);
                    Toast.makeText(getActivity(), "Symptom '" + symptomName + "' logged!", Toast.LENGTH_SHORT).show();
                    symptomNameEditText.setText("");
                    // Use it like this:


                } else {
                    Toast.makeText(getActivity(), "Log a food entry first!", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }*/


}