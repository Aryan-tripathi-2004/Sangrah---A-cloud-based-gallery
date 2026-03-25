package com.example.Billing.service;

import com.example.Billing.model.Invoice;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class PDFGenerationService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    /**
     * Generates a PDF invoice for the given {@link Invoice} and returns the
     * content as a byte array so that it can be streamed to the caller.
     *
     * Uses {@code com.itextpdf.kernel.font.PdfFont} and
     * {@code com.itextpdf.kernel.font.PdfFontFactory} from the iText 8 kernel
     * dependency.
     */
    public byte[] generateInvoicePdf(Invoice invoice) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDoc = new PdfDocument(writer);

        try (Document document = new Document(pdfDoc)) {
            PdfFont boldFont = PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

            // Title
            document.add(new Paragraph("INVOICE")
                    .setFont(boldFont)
                    .setFontSize(24));

            document.add(new Paragraph(" "));

            // Invoice details table
            Table detailsTable = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                    .useAllAvailableWidth();

            addTableRow(detailsTable, "Invoice Number:", invoice.getInvoiceNumber(),
                    boldFont, regularFont);
            addTableRow(detailsTable, "Customer Email:", invoice.getUserEmail(),
                    boldFont, regularFont);
            addTableRow(detailsTable, "Status:", invoice.getStatus().name(),
                    boldFont, regularFont);
            addTableRow(detailsTable, "Amount:",
                    String.format("%s %.2f", invoice.getCurrency().toUpperCase(),
                            invoice.getAmount()),
                    boldFont, regularFont);
            addTableRow(detailsTable, "Description:",
                    invoice.getDescription() != null ? invoice.getDescription() : "-",
                    boldFont, regularFont);
            addTableRow(detailsTable, "Created At:",
                    invoice.getCreatedAt() != null
                            ? invoice.getCreatedAt().format(DATE_FORMATTER) : "-",
                    boldFont, regularFont);
            if (invoice.getPaidAt() != null) {
                addTableRow(detailsTable, "Paid At:",
                        invoice.getPaidAt().format(DATE_FORMATTER),
                        boldFont, regularFont);
            }
            if (invoice.getStripePaymentIntentId() != null) {
                addTableRow(detailsTable, "Payment Reference:",
                        invoice.getStripePaymentIntentId(),
                        boldFont, regularFont);
            }

            document.add(detailsTable);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Thank you for using Sangrah!")
                    .setFont(regularFont)
                    .setFontSize(10));
        }

        return outputStream.toByteArray();
    }

    private void addTableRow(Table table, String label, String value,
                              PdfFont boldFont, PdfFont regularFont) {
        table.addCell(new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(label).setFont(boldFont).setFontSize(11))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        table.addCell(new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(value).setFont(regularFont).setFontSize(11))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
    }
}
