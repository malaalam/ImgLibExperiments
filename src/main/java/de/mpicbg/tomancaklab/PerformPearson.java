package de.mpicbg.tomancaklab;

import ij.ImagePlus;
import io.scif.img.IO;
import net.imagej.ImageJ;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;
import org.scijava.Context;
import org.scijava.command.CommandService;

public class PerformPearson<T extends RealType<T>> {
    {


        final Img<T> imgCropped = (Img<T>) IO.openImgs("/home/manan/Desktop/02_Repositories/12_HomePage/minuteskeeper/images/2019-09-30/Slice303_Cropped_8bit.png").get(0);
        final Img<T> imgOriginal = (Img<T>) IO.openImgs("/home/manan/Desktop/02_Repositories/12_HomePage/minuteskeeper/images/2019-09-30/Slice303_Original_8bit.png").get(0);
        ImagePlus imgPlusCropped = ImageJFunctions.wrap(imgCropped, "cropped");
        int height = imgPlusCropped.getHeight();
        int width=imgPlusCropped.getWidth();
        calculateMaxPearson(imgCropped, imgOriginal, width, height);
    }

    private void calculateMaxPearson(Img<T> imgCropped, Img<T> imgOriginal, int width, int height) {
        ImgFactory imgFactory = new ArrayImgFactory<>(new DoubleType());
        Img imgOriginalCopy = imgFactory.create(imgOriginal);

        Cursor<DoubleType> imgOriginalCursor = imgOriginalCopy.localizingCursor();
        double pearsonOpt = -1;
        int xOpt=0;
        int yOpt=0;
        while(imgOriginalCursor.hasNext()) {
            imgOriginalCursor.fwd();

            long[] min = {imgOriginalCursor.getIntPosition(0), imgOriginalCursor.getIntPosition(1)};
            long[] max = {imgOriginalCursor.getIntPosition(0) + width - 1, imgOriginalCursor.getIntPosition(1) + height - 1};
            if (max[0] < imgOriginalCopy.dimension(0) && max[1] < imgOriginalCopy.dimension(1)) {
                RandomAccessibleInterval view = Views.interval(imgOriginal, min, max);
                Views.extendZero(view);
                RandomAccessibleInterval<T> viewOriginal = Views.interval(imgOriginal, view);
                ImagePlus viewOriginalImgPlus = ImageJFunctions.wrap(viewOriginal, "cropped");
                Img<T> viewOriginalImage = (Img<T>) ImageJFunctions.wrap(viewOriginalImgPlus);
                double pearsonTemp = calculatePearson(viewOriginalImage, imgCropped);
                if (pearsonTemp > pearsonOpt) {
                    pearsonOpt = pearsonTemp;

                    xOpt = imgOriginalCursor.getIntPosition(0);
                    yOpt = imgOriginalCursor.getIntPosition(1);
                    System.out.println(" x= " + xOpt + " y= " + yOpt + " pearsonOpt= " + pearsonOpt);
                }
            }
        }
    }

    private double calculatePearson(Img<T> viewOriginalImage, Img<T> imgCropped) {
        double EX=averageImage(viewOriginalImage);
        double EY=averageImage(imgCropped);
        Img <T> XY=multiplyImages(viewOriginalImage, imgCropped);
        Img <T> X2=multiplyImages(viewOriginalImage, viewOriginalImage);
        Img <T> Y2=multiplyImages(imgCropped, imgCropped);
        double EXY=averageImage(XY);
        double EX2=averageImage(X2);
        double EY2=averageImage(Y2);
        double pearson = (EXY - EX * EY) / Math.sqrt(EX2 - Math.pow(EX, 2)) / Math.sqrt(EY2 - Math.pow(EY, 2));
        return pearson;
    }

    private Img<T> multiplyImages(Img<T> X, Img<T> Y){
        ImgFactory imgFactory = new ArrayImgFactory<>(new DoubleType());
        Img Z=imgFactory.create(X);

        Cursor<DoubleType> ZCursor = Z.localizingCursor();
        RandomAccess<T> XRandomAccess = X.randomAccess();
        RandomAccess<T> YRandomAccess = Y.randomAccess();


        while(ZCursor.hasNext()) {
            ZCursor.fwd();
            XRandomAccess.setPosition(ZCursor);
            YRandomAccess.setPosition(ZCursor);
            ZCursor.get().set(XRandomAccess.get().getRealDouble()*YRandomAccess.get().getRealDouble());
        }

        return Z;
    }




    private double averageImage(Img<T> image) {
        Cursor<DoubleType> imageCursor = (Cursor<DoubleType>) image.localizingCursor();
        RandomAccess<T> sourceOneRandomAccess = image.randomAccess();
        int countPixel=0;
        double sumIntensity=0.0;
        while (imageCursor.hasNext()) {
            imageCursor.fwd();

            sourceOneRandomAccess.setPosition(imageCursor);
            sumIntensity+=sourceOneRandomAccess.get().getRealDouble();
            countPixel+=1;
        }
        return sumIntensity/countPixel;
    }


    public static void main(String[] args) {

        CommandService cs;
        ImageJ imageJ = new ImageJ();
        Context ctx = imageJ.context();
        cs = ctx.getService(CommandService.class);
        new PerformPearson();


    }

}
