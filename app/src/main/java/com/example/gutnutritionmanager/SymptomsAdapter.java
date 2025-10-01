package com.example.gutnutritionmanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

// Change SymptomsAdapter to use LogEntryWithSymptoms
public class SymptomsAdapter extends RecyclerView.Adapter<SymptomsAdapter.ViewHolder> {

    private List<LogEntryWithSymptoms> entriesWithSymptoms;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

    public SymptomsAdapter(List<LogEntryWithSymptoms> entriesWithSymptoms) {
        this.entriesWithSymptoms = entriesWithSymptoms;
    }

    public void updateData(List<LogEntryWithSymptoms> entriesWithSymptoms) {
        this.entriesWithSymptoms = entriesWithSymptoms;
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
        LogEntryWithSymptoms entry = entriesWithSymptoms.get(position);

        holder.symptomName.setText(entry.symptomName);
        holder.symptomSeverity.setText("Severity: " + entry.symptomSeverity + "/5");
        holder.symptomTime.setText(dateFormat.format(entry.logEntry.timestamp));

        // Set severity color
        setSeverityColor(holder, entry.symptomSeverity);
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
        return entriesWithSymptoms != null ? entriesWithSymptoms.size() : 0;
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