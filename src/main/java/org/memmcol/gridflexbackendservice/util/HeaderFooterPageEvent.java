package org.memmcol.gridflexbackendservice.util;

import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;

public class HeaderFooterPageEvent implements IEventHandler {
    private Document document;
    private PdfFont font;
    private String headerText;

    public HeaderFooterPageEvent(Document document, PdfFont font, String headerText) {
        this.document = document;
        this.font = font;
        this.headerText = headerText;
    }

    @Override
    public void handleEvent(Event event) {
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        PdfPage page = docEvent.getPage();
        PdfDocument pdfDoc = docEvent.getDocument();
        PdfCanvas canvas = new PdfCanvas(page);

        int pageNumber = pdfDoc.getPageNumber(page);
        Rectangle pageSize = page.getPageSize();

        // Add header text at the top of the page
        canvas.beginText();
        canvas.setFontAndSize(font, 6);
        canvas.moveText(pageSize.getLeft() + 30, pageSize.getTop() - 20); // Adjust the position of the header
        canvas.showText(headerText).setFontAndSize(font, 6);
        canvas.endText();

        // Add footer with page number at the bottom of each page
        canvas.beginText();
        canvas.setFontAndSize(font, 6);
        canvas.moveText((pageSize.getRight() + pageSize.getLeft()) / 2, pageSize.getBottom() + 20);
        canvas.showText("Page " + pageNumber).setFontAndSize(font, 6);
        canvas.endText();
        canvas.release();
    }
}



