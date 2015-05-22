package com.itextpdf.core.pdf.xobject;

import com.itextpdf.basics.PdfException;
import com.itextpdf.basics.image.Image;
import com.itextpdf.basics.image.RawImage;
import com.itextpdf.core.image.*;
import com.itextpdf.core.pdf.*;

public class PdfImageXObject extends PdfXObject {

    private float width;
    private float height;
    private boolean mask;
    private boolean softMask;

    public PdfImageXObject(PdfDocument document, Image image) throws PdfException {
        this(document, image, null);
    }

    public PdfImageXObject(PdfDocument document, Image image, PdfImageXObject imageMask) throws PdfException {
        this(createPdfStream(document, image, imageMask), document);
        mask = image.isMask();
        softMask = image.isSoftMask();
    }

    public PdfImageXObject(PdfStream pdfObject, PdfDocument document) throws PdfException {
        super(pdfObject, document);
    }

    public float getWidth() throws PdfException {
        if (!isFlushed())
            return getPdfObject().getAsNumber(PdfName.Width).getFloatValue();
        else
            return width;
    }

    public float getHeight() throws PdfException {
        if (!isFlushed())
            return getPdfObject().getAsNumber(PdfName.Height).getFloatValue();
        else
            return height;
    }

    @Override
    public void flush() throws PdfException {
        width = getPdfObject().getAsNumber(PdfName.Width).getFloatValue();
        height = getPdfObject().getAsNumber(PdfName.Height).getFloatValue();
        super.flush();
    }

    @Override
    public PdfImageXObject copy(PdfDocument document) throws PdfException {
        PdfImageXObject image = new PdfImageXObject((PdfStream) getPdfObject().copy(document), document);
        image.width = width;
        image.height = height;
        image.mask = mask;
        image.softMask = softMask;
        return image;
    }

    protected static PdfStream createPdfStream(PdfDocument document, Image image, PdfImageXObject imageMask) throws PdfException {

        PdfStream stream = new PdfStream(document);
        stream.put(PdfName.Type, PdfName.XObject);
        stream.put(PdfName.Subtype, PdfName.Image);
//TODO: return to this later
//        if (image.getLayer() != null)
//            put(PdfName.OC, image.getLayer().getRef());

        if (image.isMask() && (image.getBpc() == 1 || image.getBpc() > 0xff))
            stream.put(PdfName.ImageMask, PdfBoolean.PdfTrue);

        if (imageMask != null) {
            if (imageMask.softMask)
                stream.put(PdfName.SMask, imageMask.getPdfObject());
            else if (imageMask.mask)
                stream.put(PdfName.Mask, imageMask.getPdfObject());
        }

        if (image.isMask() && image.isInverted())
            stream.put(PdfName.Decode, new PdfArray(new float[]{1, 0}));
        if (image.isInterpolation())
            stream.put(PdfName.Interpolate, PdfBoolean.PdfTrue);
        // deal with transparency
        int[] transparency = image.getTransparency();
        if (transparency != null && !image.isMask() && imageMask == null) {
            PdfArray t = new PdfArray();
            for (int transparencyItem : transparency)
                t.add(new PdfNumber(transparencyItem));
            stream.put(PdfName.Mask, t);
        }

        switch (image.getOriginalType()) {
            case Image.GIF:
                GifImageHelper.processImage(image, stream);
                break;
            case Image.JPEG:
                JpegImageHelper.processImage(image, stream);
                break;
            case Image.JPEG2000:
                Jpeg2000ImageHelper.processImage(image, stream);
                break;
            case Image.JBIG2: //TODO JBIG2Globals caching?
                Jbig2ImageHelper.processImage(image, stream);
                break;
            case Image.PNG:
                PngImageHelper.processImage(image, stream);
                break;
            case Image.BMP:
                BmpImageHelper.processImage(image, stream);
                break;
            case Image.TIFF:
                TiffImageHelper.processImage(image, stream);
                break;
        }

        if (image.getOriginalType() == Image.RAW){
            RawImageHelper.updatePdfStream((RawImage)image, null, stream);
        }
        stream.put(PdfName.Width, new PdfNumber(image.getWidth()));
        stream.put(PdfName.Height, new PdfNumber(image.getHeight()));
        return stream;
    }
}
