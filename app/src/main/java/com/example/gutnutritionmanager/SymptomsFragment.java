package com.example.gutnutritionmanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

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

        return view;
    }

    private String normalizeSymptomName(String symptomName) {
        return symptomName.trim().toLowerCase();
    }


    private void logSymptom() {

        String symptomName = symptomNameEditText.getText().toString().trim();
        if (symptomName.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter a symptom name", Toast.LENGTH_SHORT).show();
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


    }
}