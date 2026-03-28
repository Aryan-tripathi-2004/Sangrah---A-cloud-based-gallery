package com.example.Billing.application.service;

import com.example.Billing.infrastructure.persistence.document.InvoiceDocument;
import com.example.Billing.infrastructure.persistence.repository.InvoiceRepository;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class PDFGenerationService {

    private final InvoiceRepository invoiceRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    public byte[] generateInvoicePDF(String invoiceId) {
        try {
            log.info("📝 Starting PDF generation for invoice: {}", invoiceId);

            // Fetch invoice from database
            InvoiceDocument invoice = invoiceRepository.findByInvoiceId(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

            // Generate PDF in memory
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter pdfWriter = new PdfWriter(baos);
            PdfDocument pdfDocument = new PdfDocument(pdfWriter);
            Document document = new Document(pdfDocument);

            // Set default font
            PdfFont font = PdfFontFactory.createFont("Helvetica");
            PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");

            // Add header
            addInvoiceHeader(document, invoice, boldFont, font);

            // Add invoice details
            addInvoiceDetails(document, invoice, boldFont, font);

            // Add charges breakdown
            addChargesTable(document, invoice, boldFont, font);

            // Add footer
            addInvoiceFooter(document, invoice, font);

            document.close();

            byte[] pdfContent = baos.toByteArray();
            log.info("✅ PDF generated successfully: {} bytes", pdfContent.length);

            return pdfContent;

        } catch (Exception e) {
            log.error("❌ PDF generation failed for invoice {}: {}", invoiceId, e.getMessage(), e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    private void addInvoiceHeader(Document doc, InvoiceDocument invoice, PdfFont boldFont, PdfFont font) throws Exception {
        // Company name
        Paragraph title = new Paragraph("SANGRAH")
                .setFont(boldFont)
                .setFontSize(28)
                .setTextAlignment(TextAlignment.CENTER);
        doc.add(title);

        // Tagline
        Paragraph tagline = new Paragraph("Cloud Storage Platform")
                .setFont(font)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER);
        doc.add(tagline);

        // Empty line
        doc.add(new Paragraph(""));

        // Invoice title
        Paragraph invoiceTitle = new Paragraph("INVOICE")
                .setFont(boldFont)
                .setFontSize(16);
        doc.add(invoiceTitle);
    }

    private void addInvoiceDetails(Document doc, InvoiceDocument invoice, PdfFont boldFont, PdfFont font) throws Exception {
        // Invoice details table
        Table detailsTable = new Table(new float[]{1, 1});
        detailsTable.setWidth(UnitValue.createPercentValue(100));

        // Left column
        Cell leftCell = new Cell();
        leftCell.add(new Paragraph("Invoice Number:").setFont(boldFont).setFontSize(10));
        leftCell.add(new Paragraph(invoice.getInvoiceId()).setFont(font).setFontSize(9));
        leftCell.add(new Paragraph(""));
        leftCell.add(new Paragraph("Invoice Date:").setFont(boldFont).setFontSize(10));
        leftCell.add(new Paragraph(invoice.getIssuedDate().atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(DATE_FORMATTER)).setFont(font).setFontSize(9));
        leftCell.add(new Paragraph(""));
        leftCell.add(new Paragraph("Due Date:").setFont(boldFont).setFontSize(10));
        leftCell.add(new Paragraph(invoice.getDueDate().atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(DATE_FORMATTER)).setFont(font).setFontSize(9));

        // Right column
        Cell rightCell = new Cell();
        rightCell.add(new Paragraph("User ID:").setFont(boldFont).setFontSize(10));
        rightCell.add(new Paragraph(invoice.getUserId()).setFont(font).setFontSize(9));
        rightCell.add(new Paragraph(""));
        rightCell.add(new Paragraph("Billing Period:").setFont(boldFont).setFontSize(10));
        rightCell.add(new Paragraph(
                invoice.getBillingPeriod().getStartDate().atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(DATE_FORMATTER) +
                        " to " +
                        invoice.getBillingPeriod().getEndDate().atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(DATE_FORMATTER)
        ).setFont(font).setFontSize(9));

        detailsTable.addCell(leftCell);
        detailsTable.addCell(rightCell);
        doc.add(detailsTable);
    }

    private void addChargesTable(Document doc, InvoiceDocument invoice, PdfFont boldFont, PdfFont font) throws Exception {
        doc.add(new Paragraph(""));

        // Storage metrics
        Paragraph storageTitle = new Paragraph("Storage Usage")
                .setFont(boldFont)
                .setFontSize(12);
        doc.add(storageTitle);

        Table storageTable = new Table(new float[]{1, 1, 1, 1});
        storageTable.setWidth(UnitValue.createPercentValue(100));

        // Header row
        storageTable.addHeaderCell(new Cell().add(new Paragraph("Type").setFont(boldFont)));
        storageTable.addHeaderCell(new Cell().add(new Paragraph("GB-Days").setFont(boldFont)));
        storageTable.addHeaderCell(new Cell().add(new Paragraph("Rate").setFont(boldFont)));
        storageTable.addHeaderCell(new Cell().add(new Paragraph("Amount").setFont(boldFont)));

        // Data rows
        InvoiceDocument.StorageMetrics metrics = invoice.getStorageMetrics();
        storageTable.addCell(new Paragraph("Images").setFont(font));
        storageTable.addCell(new Paragraph(String.format("%.2f", metrics.getImageGBDays())).setFont(font));
        storageTable.addCell(new Paragraph(String.format("$%.4f", invoice.getCharges().getStorageRate())).setFont(font));
        storageTable.addCell(new Paragraph(String.format("$%.2f", metrics.getImageCost())).setFont(font));

        storageTable.addCell(new Paragraph("Videos").setFont(font));
        storageTable.addCell(new Paragraph(String.format("%.2f", metrics.getVideoGBDays())).setFont(font));
        storageTable.addCell(new Paragraph(String.format("$%.4f", invoice.getCharges().getStorageRate())).setFont(font));
        storageTable.addCell(new Paragraph(String.format("$%.2f", metrics.getVideoCost())).setFont(font));

        storageTable.addCell(new Paragraph(""));
        storageTable.addCell(new Paragraph(""));
        storageTable.addCell(new Paragraph(""));
        storageTable.addCell(new Paragraph(""));

        doc.add(storageTable);

        // Summary
        doc.add(new Paragraph(""));
        Table summaryTable = new Table(new float[]{1, 1});
        summaryTable.setWidth(UnitValue.createPercentValue(100));

        summaryTable.addCell(new Cell().add(new Paragraph("Subtotal:").setFont(boldFont)));
        summaryTable.addCell(new Cell().add(new Paragraph(String.format("$%.2f", invoice.getCharges().getSubtotal())).setFont(font)));

        summaryTable.addCell(new Cell().add(new Paragraph("Tax (" + String.format("%.0f%%", invoice.getCharges().getTaxRate() * 100) + "):").setFont(boldFont)));
        summaryTable.addCell(new Cell().add(new Paragraph(String.format("$%.2f", invoice.getCharges().getTax())).setFont(font)));

        Cell totalAmountCell = new Cell().add(new Paragraph("Total Amount:").setFont(boldFont));
        Cell totalCell = new Cell().add(new Paragraph(String.format("$%.2f", invoice.getCharges().getTotalAmount())).setFont(boldFont));
        summaryTable.addCell(totalAmountCell);
        summaryTable.addCell(totalCell);

        doc.add(summaryTable);
    }

    private void addInvoiceFooter(Document doc, InvoiceDocument invoice, PdfFont font) throws Exception {
        doc.add(new Paragraph(""));
        doc.add(new Paragraph(""));

        // Payment status
        String status = "PAID".equals(invoice.getStatus()) ?
                "✓ PAID on " + invoice.getPaidDate().atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(DATE_FORMATTER) :
                invoice.getStatus();

        Paragraph footerStatus = new Paragraph("Status: " + status)
                .setFont(font)
                .setFontSize(10);
        doc.add(footerStatus);

        // Footer text
        Paragraph footer = new Paragraph("Thank you for using Sangrah Cloud Storage!")
                .setFont(font)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER);
        doc.add(footer);

        Paragraph contact = new Paragraph("For support, visit www.sangrah.com or email support@sangrah.com")
                .setFont(font)
                .setFontSize(7)
                .setTextAlignment(TextAlignment.CENTER);
        doc.add(contact);
    }
}
