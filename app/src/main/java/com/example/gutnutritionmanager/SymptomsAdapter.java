package com.example.gutnutritionmanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Change SymptomsAdapter to use LogEntryWithSymptoms
public class SymptomsAdapter extends RecyclerView.Adapter<SymptomsAdapter.ViewHolder> {

    private List<NormalizedSymptom> normalizedSymptoms;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

    // New data class for normalized symptoms
    public static class NormalizedSymptom {
        public String normalizedName;
        public String displayName;
        public int totalOccurrences;
        public double averageSeverity;
        public List<LogEntryWithSymptoms> originalEntries;

        public NormalizedSymptom(String normalizedName, List<LogEntryWithSymptoms> entries) {
            this.normalizedName = normalizedName;
            this.originalEntries = entries;
            this.totalOccurrences = entries.size();

            // Calculate average severity
            this.averageSeverity = entries.stream()
                    .mapToInt(e -> e.symptomSeverity)
                    .average()
                    .orElse(0.0);

            // Create display name with proper capitalization
            this.displayName = normalizedName.substring(0, 1).toUpperCase() + normalizedName.substring(1);
        }
    }

    public SymptomsAdapter(List<NormalizedSymptom> normalizedSymptoms) {
        this.normalizedSymptoms = normalizedSymptoms;
    }

    public void updateData(List<LogEntryWithSymptoms> entriesWithSymptoms) {
        // Normalize and group symptoms
        Map<String, List<LogEntryWithSymptoms>> groupedSymptoms = new HashMap<>();

        for (LogEntryWithSymptoms entry : entriesWithSymptoms) {
            if (entry.symptomName != null && !entry.symptomName.isEmpty()) {
                String normalizedName = entry.symptomName.trim().toLowerCase();
                groupedSymptoms.computeIfAbsent(normalizedName, k -> new ArrayList<>()).add(entry);
            }
        }

        // Convert to NormalizedSymptom list
        this.normalizedSymptoms = new ArrayList<>();
        for (Map.Entry<String, List<LogEntryWithSymptoms>> entry : groupedSymptoms.entrySet()) {
            normalizedSymptoms.add(new NormalizedSymptom(entry.getKey(), entry.getValue()));
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_symptom, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NormalizedSymptom normalizedSymptom = normalizedSymptoms.get(position);

        holder.symptomName.setText(normalizedSymptom.displayName + " (" + normalizedSymptom.totalOccurrences + " times)");
        holder.symptomSeverity.setText("Avg severity: " + String.format("%.1f", normalizedSymptom.averageSeverity) + "/5");

        // Show most recent time or range
        if (!normalizedSymptom.originalEntries.isEmpty()) {
            LogEntryWithSymptoms mostRecent = normalizedSymptom.originalEntries.get(0);
            holder.symptomTime.setText("Recent: " + dateFormat.format(mostRecent.logEntry.timestamp));
        }

        // Set color based on average severity
        setSeverityColor(holder, (int) Math.round(normalizedSymptom.averageSeverity));
    }

    private void setSeverityColor(ViewHolder holder, int severity) {
        int color;
        switch (severity) {
            case 1: color = 0xFF4CAF50; break;
            case 2: color = 0xFF8BC34A; break;
            case 3: color = 0xFFFFC107; break;
            case 4: color = 0xFFFF9800; break;
            case 5: color = 0xFFF44336; break;
            default: color = 0xFF757575;
        }
        holder.symptomSeverity.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return normalizedSymptoms != null ? normalizedSymptoms.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView symptomName, symptomSeverity, symptomTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            symptomName = itemView.findViewById(R.id.symptomName);
            symptomSeverity = itemView.findViewById(R.id.symptomSeverity);
            symptomTime = itemView.findViewById(R.id.symptomTime);
        }
    }
}