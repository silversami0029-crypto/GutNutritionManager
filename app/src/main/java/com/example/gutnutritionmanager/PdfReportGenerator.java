package com.example.gutnutritionmanager;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfReportGenerator {

    private Context context;

    public PdfReportGenerator(Context context) {
        this.context = context;
    }
    public File generateDoctorReport(String reportText, String period) {
        try {
            // Create PDF document using Android's PdfDocument
            PdfDocument document = new PdfDocument();

            // Create page info for A4 size (595 x 842 points)
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);

            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            Paint titlePaint = new Paint();

            // Set up paints
            paint.setColor(Color.BLACK);
            paint.setTextSize(10);
            paint.setAntiAlias(true);

            titlePaint.setColor(Color.BLACK);
            titlePaint.setTextSize(14);
            titlePaint.setFakeBoldText(true);
            titlePaint.setAntiAlias(true);

            // Draw title
            String title = "Gut Nutrition Manager - Clinical Report";
            canvas.drawText(title, 50, 50, titlePaint);
            canvas.drawText("Period: " + period, 50, 70, paint);

            // Draw generation date
            String date = "Generated: " + new java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault()).format(new java.util.Date());
            canvas.drawText(date, 50, 90, paint);

            // Draw report text with proper line wrapping
            int yPosition = 120;
            int margin = 50;
            int pageWidth = 595 - (2 * margin);

            String[] lines = reportText.split("\n");

            for (String line : lines) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    yPosition += 5; // Small gap for empty lines
                    continue;
                }

                // Check if this line contains bullet points with emojis (mood lines)
                boolean isMoodLine = line.trim().startsWith("•") &&
                        (line.contains("😊") || line.contains("😐") ||
                                line.contains("😰") || line.contains("😟") ||
                                line.contains("😴"));

                // Handle line wrapping
                if (paint.measureText(line) > pageWidth && !isMoodLine) {
                    // Split long lines (but not mood lines)
                    List<String> wrappedLines = wrapText(line, paint, pageWidth);
                    for (String wrappedLine : wrappedLines) {
                        if (yPosition > 800) { // New page needed
                            document.finishPage(page);
                            pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                            page = document.startPage(pageInfo);
                            canvas = page.getCanvas();
                            yPosition = 50;

                            // Redraw title on new page
                            canvas.drawText(title + " (cont.)", 50, 50, titlePaint);
                        }
                        canvas.drawText(wrappedLine, margin, yPosition, paint);
                        yPosition += 15;
                    }
                } else {
                    // For mood lines or short lines, don't wrap
                    if (yPosition > 800) { // New page needed
                        document.finishPage(page);
                        pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                        page = document.startPage(pageInfo);
                        canvas = page.getCanvas();
                        yPosition = 50;

                        // Redraw title on new page
                        canvas.drawText(title + " (cont.)", 50, 50, titlePaint);
                    }

                    // Draw the line as-is (mood lines will keep their emojis intact)
                    canvas.drawText(line, margin, yPosition, paint);
                    yPosition += 15;
                }
            }

            document.finishPage(page);

            // Save the document
            String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
            String fileName = "Doctor_Report_" + timeStamp + ".pdf";

            File appSpecificDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (appSpecificDir == null) {
                appSpecificDir = context.getFilesDir();
            }

            File file = new File(appSpecificDir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();

            Log.d("PDFGenerator", "PDF created successfully: " + file.getAbsolutePath());
            return file;

        } catch (Exception e) {
            Log.e("PDFGenerator", "Error creating PDF: " + e.getMessage(), e);
            return null;
        }
    }
    // Method to generate PDF from pre-generated report text
    /*public File generateDoctorReport(String reportText, String period) {
        try {
            // Create PDF document using Android's PdfDocument
            PdfDocument document = new PdfDocument();

            // Create page info for A4 size (595 x 842 points)
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);

            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            Paint titlePaint = new Paint();

            // Set up paints
            paint.setColor(Color.BLACK);
            paint.setTextSize(10);
            paint.setAntiAlias(true);

            titlePaint.setColor(Color.BLACK);
            titlePaint.setTextSize(14);
            titlePaint.setFakeBoldText(true);
            titlePaint.setAntiAlias(true);

            // Draw title
            String title = "Gut Nutrition Manager - Clinical Report";
            canvas.drawText(title, 50, 50, titlePaint);
            canvas.drawText("Period: " + period, 50, 70, paint);

            // Draw generation date
            String date = "Generated: " + new java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault()).format(new java.util.Date());
            canvas.drawText(date, 50, 90, paint);

            // Draw report text with proper line wrapping
            int yPosition = 120;
            int margin = 50;
            int pageWidth = 595 - (2 * margin);

            String[] lines = reportText.split("\n");

            for (String line : lines) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    yPosition += 5; // Small gap for empty lines
                    continue;
                }

                // Handle line wrapping for long text
                if (paint.measureText(line) > pageWidth) {
                    // Split long lines
                    List<String> wrappedLines = wrapText(line, paint, pageWidth);
                    for (String wrappedLine : wrappedLines) {

                        if (yPosition > 800) { // New page needed
                            document.finishPage(page);
                            pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                            page = document.startPage(pageInfo);
                            canvas = page.getCanvas();
                            yPosition = 50;

                            // Redraw title on new page
                            canvas.drawText(title + " (cont.)", 50, 50, titlePaint);
                        }
                        canvas.drawText(wrappedLine, margin, yPosition, paint);
                        yPosition += 15;
                    }
                } else {
                    if (yPosition > 800) { // New page needed
                        document.finishPage(page);
                        pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                        page = document.startPage(pageInfo);
                        canvas = page.getCanvas();
                        yPosition = 50;

                        // Redraw title on new page
                        canvas.drawText(title + " (cont.)", 50, 50, titlePaint);
                    }
                    canvas.drawText(line, margin, yPosition, paint);
                    yPosition += 15;
                }
            }

            document.finishPage(page);

            // Save the document
            String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
            String fileName = "Doctor_Report_" + timeStamp + ".pdf";

            File appSpecificDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (appSpecificDir == null) {
                appSpecificDir = context.getFilesDir();
            }

            File file = new File(appSpecificDir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();

            Log.d("PDFGenerator", "PDF created successfully: " + file.getAbsolutePath());
            return file;

        } catch (Exception e) {
            Log.e("PDFGenerator", "Error creating PDF: " + e.getMessage(), e);
            return null;
        }
    }*/

    // Helper method for text wrapping
    private List<String> wrapText(String text, Paint paint, int maxWidth) {
        List<String> lines = new ArrayList<>();

        // Handle very long words
        if (paint.measureText(text) <= maxWidth) {
            lines.add(text);
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;

            if (paint.measureText(testLine) < maxWidth) {
                currentLine.append(currentLine.length() == 0 ? word : " " + word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    // Single word is too long - break it
                    while (paint.measureText(word) > maxWidth) {
                        int breakIndex = word.length() * maxWidth / (int)paint.measureText(word);
                        lines.add(word.substring(0, breakIndex));
                        word = word.substring(breakIndex);
                    }
                    if (!word.isEmpty()) {
                        currentLine.append(word);
                    }
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }


}