package com.example.Billing.application.service;

import java.math.BigDecimal;

import com.example.Billing.application.service.interfaces.IPDFGenerationService;
import com.example.Billing.infrastructure.persistence.document.InvoiceDocument;
import com.example.Billing.infrastructure.persistence.repository.InvoiceRepository;
import com.example.Billing.shared.exception.BillingProcessingException;
import com.example.Billing.shared.exception.ResourceNotFoundException;
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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class PDFGenerationService implements IPDFGenerationService {

    private final InvoiceRepository invoiceRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    @Override
    public byte[] generateInvoicePDF(String invoiceId) {
        try {
            log.info("📝 Starting PDF generation for invoice: {}", invoiceId);

            // Fetch invoice from database
            InvoiceDocument invoice = invoiceRepository.findByInvoiceId(invoiceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

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

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ PDF generation failed for invoice {}: {}", invoiceId, e.getMessage(), e);
            throw new BillingProcessingException("Unable to generate PDF invoice at this time due to an internal rendering error.");
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
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER);
        doc.add(invoiceTitle);

        doc.add(new Paragraph(""));
    }

    private void addInvoiceDetails(Document doc, InvoiceDocument invoice, PdfFont boldFont, PdfFont font) throws Exception {
        Table detailsTable = new Table(new float[]{1, 1});
        detailsTable.setWidth(UnitValue.createPercentValue(100));

        // Left column - Invoice Info
        Cell leftCell = new Cell();
        leftCell.add(new Paragraph("Invoice Number:").setFont(boldFont));
        leftCell.add(new Paragraph(invoice.getInvoiceId()).setFont(font));
        leftCell.add(new Paragraph("Issue Date:").setFont(boldFont));
        leftCell.add(new Paragraph(invoice.getIssuedDate().atZone(ZoneOffset.UTC).toLocalDate().format(DATE_FORMATTER)).setFont(font));
        leftCell.add(new Paragraph("Due Date:").setFont(boldFont));
        leftCell.add(new Paragraph(invoice.getDueDate().atZone(ZoneOffset.UTC).toLocalDate().format(DATE_FORMATTER)).setFont(font));
        detailsTable.addCell(leftCell);

        // Right column - Customer Info
        Cell rightCell = new Cell();
        rightCell.add(new Paragraph("Bill To:").setFont(boldFont));
        rightCell.add(new Paragraph("User ID: " + invoice.getUserId()).setFont(font));
        rightCell.add(new Paragraph("Email: " + (invoice.getUserEmail() != null ? invoice.getUserEmail() : "N/A")).setFont(font));
        rightCell.add(new Paragraph("Billing Period:").setFont(boldFont));
        String period = invoice.getBillingPeriod().getStartDate().atZone(ZoneOffset.UTC).toLocalDate().format(DATE_FORMATTER) +
                " to " + invoice.getBillingPeriod().getEndDate().atZone(ZoneOffset.UTC).toLocalDate().format(DATE_FORMATTER);
        rightCell.add(new Paragraph(period).setFont(font));
        detailsTable.addCell(rightCell);

        doc.add(detailsTable);
        doc.add(new Paragraph(""));
    }

    private void addChargesTable(Document doc, InvoiceDocument invoice, PdfFont boldFont, PdfFont font) throws Exception {
        Table storageTable = new Table(new float[]{2, 1, 1, 1});
        storageTable.setWidth(UnitValue.createPercentValue(100));

        // Header
        storageTable.addHeaderCell(new Cell().add(new Paragraph("Storage Type").setFont(boldFont)));
        storageTable.addHeaderCell(new Cell().add(new Paragraph("Usage (GB-Days)").setFont(boldFont)));
        storageTable.addHeaderCell(new Cell().add(new Paragraph("Rate/GB-Day").setFont(boldFont)));
        storageTable.addHeaderCell(new Cell().add(new Paragraph("Amount").setFont(boldFont)));

        // Images row
        storageTable.addCell(new Paragraph("Gallery & Event Images").setFont(font));
        storageTable.addCell(new Paragraph(String.format("%.2f", invoice.getStorageMetrics().getImageGBDays())).setFont(font));
        storageTable.addCell(new Paragraph(String.format("$%.2f", invoice.getCharges().getStorageRate())).setFont(font));
        storageTable.addCell(new Paragraph(String.format("$%.2f", invoice.getStorageMetrics().getImageCost())).setFont(font));

        // Videos row
        storageTable.addCell(new Paragraph("Gallery & Event Videos").setFont(font));
        storageTable.addCell(new Paragraph(String.format("%.2f", invoice.getStorageMetrics().getVideoGBDays())).setFont(font));
        storageTable.addCell(new Paragraph(String.format("$%.2f", invoice.getCharges().getStorageRate())).setFont(font));
        storageTable.addCell(new Paragraph(String.format("$%.2f", invoice.getStorageMetrics().getVideoCost())).setFont(font));

        // Subtotal row
        storageTable.addCell(new Paragraph("Total Storage Usage").setFont(boldFont));
        storageTable.addCell(new Paragraph(String.format("%.2f", invoice.getStorageMetrics().getTotalGBDays())).setFont(boldFont));
        storageTable.addCell(new Paragraph(""));
        storageTable.addCell(new Paragraph(""));

        doc.add(storageTable);

        // Summary
        doc.add(new Paragraph(""));
        Table summaryTable = new Table(new float[]{1, 1});
        summaryTable.setWidth(UnitValue.createPercentValue(100));

        summaryTable.addCell(new Cell().add(new Paragraph("Subtotal:").setFont(boldFont)));
        summaryTable.addCell(new Cell().add(new Paragraph(String.format("$%.2f", invoice.getCharges().getSubtotal())).setFont(font)));

        summaryTable.addCell(new Cell().add(new Paragraph("Tax (" + String.format("%.0f%%", invoice.getCharges().getTaxRate().multiply(BigDecimal.valueOf(100))) + "):").setFont(boldFont)));
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
        String status = "PAID".equals(invoice.getStatus().name()) ?
                "✓ PAID on " + invoice.getPaidDate().atZone(ZoneOffset.UTC).toLocalDate().format(DATE_FORMATTER) :
                invoice.getStatus().name();

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
