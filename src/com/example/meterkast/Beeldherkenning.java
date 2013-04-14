package com.example.meterkast;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;

import android.os.AsyncTask;

import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Arjen Swellengrebel
 */
public class Beeldherkenning extends AsyncTask<Object, Object, Object> {
    private File fileName; // The path to the picture we're working with

    // The picture and the view it should be printed to
    private Bitmap bitmap = null;
    private ImageView imageView1;

    // The suggestions and the views they should be set in
    private int suggest1 = -1, suggest2 = -1;
    private EditText editText1, editText2;

    // A picture containing all ten numbers for comparing.
    private Bitmap exampleNums;
    private Bitmap testBMP = null; // This can be set whenever an intermediate step needs to be shown (for testing).

    private final int LEFT = 0, TOP = 1, RIGHT = 2, BOTTOM = 3;

    // Some numbers being used throughout the process:
    // STEP 1, detecting number shapes.
    private int imageWidth = 480, imageHeight = 640; // The size we need for the working image.
    private int blurEdgeDetOriginal = 0, blurEdgeDetBlurred = 9; // The amount of blurring for edge detection.
    private int numEdgeThreshold = 35; // the threshold crossed at the edge of a number.

    // STEP 2: finding where the "boxes" with numbers are located.
    private int blurBoxLocations = 60;
    private int thresholdBoxLocations = 30;

    // STEP 3: finding where the separate numbers are located.
    private int blurNumberPlaces = 13;
    private int thresholdNumberPlaces = 40;

    private int checkEveryXLines = 7; // The number of lines that are glossed over for expediency.

    private double numberRatioMin = 0.25, numberRatioMax = 1.5;
    private double boxRatioMin = 2, boxRatioMax = 10;

    private int compareHeight = 14;
    private int compareWidth = 10;

    /**
     * The main task that is called with onExecute(). This only preps the data; the recognition is mostly done by readNumbers().
     *
     * @param Object { ImageView, TextView, TextView, File, Bitmap }
     */
    @Override
    protected Object doInBackground(Object... params) {
        // Store the parameters
        this.imageView1 = (ImageView) params[0];
        this.editText1 = (EditText) params[1];
        this.editText2 = (EditText) params[2];
        this.fileName = (File) params[3];
        this.exampleNums = (Bitmap) params[4];

        if (this.fileName.exists()) {
            // Store the image in this.bitmap (onPostExecute will print the content of that variable to the screen)
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                this.bitmap = BitmapFactory.decodeFile(this.fileName.getAbsolutePath(), options);
            } catch (OutOfMemoryError oomERR) {
                // oomERR.printStackTrace();
                System.gc(); // Clean system resources and try again.

                try {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 8;
                    this.bitmap = BitmapFactory.decodeFile(this.fileName.getAbsolutePath(), options);
                } catch (OutOfMemoryError oomERR2) {
                    // oomERR2.printStackTrace();
                }
            }

            if (this.bitmap.getHeight() < this.bitmap.getWidth()) {
                // Picture is in landscape mode. We rotate it now
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                this.bitmap = Bitmap.createBitmap(this.bitmap, 0, 0, this.bitmap.getWidth(), this.bitmap.getHeight(), matrix, true);
            }

            // Resize to a more respectable number of pixels.
            this.bitmap = Bitmap.createScaledBitmap(this.bitmap, this.imageWidth, this.imageHeight, false);
            this.testBMP = Bitmap.createBitmap(this.bitmap); // Initialize the testview as the main view

            // Do the image recognition.
            readNumbers();

            this.bitmap = Bitmap.createBitmap(this.testBMP);
        }

        return null; // I don't really know what I'm supposed to return here.
    }

    private void readNumbers() {
        Bitmap step1result;
        Bitmap step2result;
        Bitmap blurred = Bitmap.createBitmap(this.bitmap);

        // Step 1, edge detection.
        step1result = blurHorizontal(this.bitmap, this.blurEdgeDetOriginal, this.checkEveryXLines);
        blurred = blurHorizontal(blurred, this.blurEdgeDetBlurred, this.checkEveryXLines);
        step1result = subtractImages(step1result, blurred, this.checkEveryXLines);
        step1result = enforceThreshold(step1result, this.numEdgeThreshold, this.checkEveryXLines);

        // Step 2, find the number's boxes.
        step2result = blurHorizontal(step1result, this.blurBoxLocations, this.checkEveryXLines);
        step2result = enforceThreshold(step2result, this.thresholdBoxLocations, this.checkEveryXLines);

        int[][] boxblobs = detectBoxesFromBlobs(step2result, this.checkEveryXLines);
        int[] suggestions = new int[boxblobs.length];

        for (int i = 0; i < boxblobs.length; i++) {
            this.testBMP = drawSquare(this.testBMP, boxblobs[i][this.LEFT], boxblobs[i][this.TOP], boxblobs[i][this.RIGHT],
                    boxblobs[i][this.BOTTOM], Color.RED);

            publishProgress();

            if (legalBoxPlace(boxblobs[i][this.LEFT], boxblobs[i][this.TOP], boxblobs[i][this.RIGHT], boxblobs[i][this.BOTTOM])) {
                this.testBMP = drawSquare(this.testBMP, boxblobs[i][this.LEFT], boxblobs[i][this.TOP], boxblobs[i][this.RIGHT],
                        boxblobs[i][this.BOTTOM], Color.GREEN);

                publishProgress();

                int suggestion = findNumbers(this.bitmap, boxblobs[i][this.LEFT], boxblobs[i][this.TOP], boxblobs[i][this.RIGHT],
                        boxblobs[i][this.BOTTOM]);

                suggestions[i] = suggestion;
            }
        }

        if (suggestions.length > 0) {
            this.suggest1 = suggestions[0];
        }

        if (suggestions.length > 1) {
            this.suggest2 = suggestions[1];
        }
    }

    private int findNumbers(Bitmap image, int left, int top, int right, int bottom) {
        Bitmap boxEdges = blurHorizontal(image, this.blurEdgeDetOriginal, left, top, right, bottom, 1);
        Bitmap boxBlurred = blurHorizontal(image, this.blurEdgeDetBlurred, left, top, right, bottom, 1);
        boxEdges = subtractImages(boxEdges, boxBlurred, 1);
        boxEdges = enforceThreshold(boxEdges, this.numEdgeThreshold, left, top, right, bottom, 1);

        Bitmap blobs = blurHorVert(boxEdges, this.blurNumberPlaces, left, top, right, bottom, 1);
        blobs = enforceThreshold(blobs, this.thresholdNumberPlaces, left, top, right, bottom, 1);

        int[][] numberboxes = detectBoxesFromBlobs(blobs, left, top, right, bottom, 1);
        List<Integer> found = new ArrayList<Integer>();

        for (int n = 0; n < numberboxes.length; n++) {
            this.testBMP = drawSquare(this.testBMP, numberboxes[n][this.LEFT], numberboxes[n][this.TOP], numberboxes[n][this.RIGHT],
                    numberboxes[n][this.BOTTOM], Color.RED);
            this.testBMP = drawCrosshair(this.testBMP, numberboxes[n][this.LEFT], numberboxes[n][this.TOP],
                    numberboxes[n][this.RIGHT], numberboxes[n][this.BOTTOM], Color.RED);

            publishProgress();

            if (legalNumberPlace(numberboxes[n][this.LEFT], numberboxes[n][this.TOP], numberboxes[n][this.RIGHT],
                        numberboxes[n][this.BOTTOM])) {
                this.testBMP = drawSquare(this.testBMP, numberboxes[n][this.LEFT], numberboxes[n][this.TOP],
                        numberboxes[n][this.RIGHT], numberboxes[n][this.BOTTOM], Color.CYAN);
                this.testBMP = drawCrosshair(this.testBMP, numberboxes[n][this.LEFT], numberboxes[n][this.TOP],
                        numberboxes[n][this.RIGHT], numberboxes[n][this.BOTTOM], Color.CYAN);

                publishProgress();

                Bitmap number = Bitmap.createBitmap(boxEdges, numberboxes[n][this.LEFT], numberboxes[n][this.TOP],
                        numberboxes[n][this.RIGHT] - numberboxes[n][this.LEFT],
                        numberboxes[n][this.BOTTOM] - numberboxes[n][this.TOP]);
                found.add(Integer.valueOf(this.compareNumber(number)));
            }
        }

        return makeDecimalFromNumbers(found);
    }

    private boolean legalNumberPlace(int left, int top, int right, int bottom) {
        boolean positiveSize = left < right && top < bottom;
        double width = right - left;
        double height = bottom - top;
        double ratio = width / height;
        boolean normalProportions = ratio < this.numberRatioMax && ratio > this.numberRatioMin;

        return positiveSize && normalProportions;
    }

    private boolean legalBoxPlace(int left, int top, int right, int bottom) {
        boolean positiveSize = left < right && top < bottom;
        double width = right - left;
        double height = bottom - top;
        double ratio = width / height;
        boolean normalProportions = ratio < this.boxRatioMax && ratio > this.boxRatioMin;
        // The boxes themselves have a ratio of about 4 < r < 5.

        return positiveSize && normalProportions;
    }

    private Bitmap blurHorVert(Bitmap image, int radius, int left, int top, int right, int bottom, int skiplines) {
        Bitmap result = blurHorizontal(image, radius / 8, left, top, right, bottom, skiplines);
        result = blurHorizontal(result, radius / 8, left, top, right, bottom, skiplines);
        result = blurHorizontal(result, radius / 4, left, top, right, bottom, skiplines);

        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        result = Bitmap.createBitmap(result, 0, 0, result.getWidth(), result.getHeight(), matrix, true);

        result = blurHorizontal(result, radius / 8, result.getWidth() - bottom, left, result.getWidth() - top, right, skiplines);
        result = blurHorizontal(result, radius / 8, result.getWidth() - bottom, left, result.getWidth() - top, right, skiplines);
        result = blurHorizontal(result, radius / 4, result.getWidth() - bottom, left, result.getWidth() - top, right, skiplines);

        matrix = new Matrix();
        matrix.postRotate(-90);
        result = Bitmap.createBitmap(result, 0, 0, result.getWidth(), result.getHeight(), matrix, true);

        return result;
    }

    private int[][] detectBoxesFromBlobs(Bitmap blobs, int i) {
        return detectBoxesFromBlobs(blobs, 0, 0, blobs.getWidth(), blobs.getHeight(), i);
    }

    /**
     * A picture of some blobs was entered; now, create an array containing the {left, top, right, bottom} sides of all blobs in
     * the picture.
     *
     * @param  image
     * @param  skiplines
     *
     * @return
     */
    private int[][] detectBoxesFromBlobs(Bitmap image, int left, int top, int right, int bottom, int skiplines) {
        // The color of the current and previous pixel.
        int thiscolor = 0, remembercolor = 0;

        // The left and right coordinates of all horizontal white lines in the picture.
        List<Point> leftbounds = new ArrayList<Point>(), rightbounds = new ArrayList<Point>();

        // On each row, find the white lines and store their left and right sides.
        for (int y = top; y < bottom; y += skiplines) {
            remembercolor = 0;

            for (int x = left; x < right; x += 1) {
                thiscolor = getColorValue(image.getPixel(x, y));

                if (thiscolor != remembercolor) {
                    // This is a change in color, so I found an edge
                    if (thiscolor == 255) { // Left bound found; this color is white, previous was black.
                        leftbounds.add(new Point(x, y));
                    } else { // Right boundary found.
                        rightbounds.add(new Point(x - 1, y));
                    }
                }

                remembercolor = thiscolor; // Update the "previous color"
            }

            // If the right side of the image is reached but there is no right edge to match the last left edge
            if (rightbounds.size() < leftbounds.size()) {
                // Right edge of the last-found line is the right side of the image.
                rightbounds.add(new Point(right - 1, y));
            }
        }

        List<Point> blobUpperLeft = new ArrayList<Point>(), blobLowerRight = new ArrayList<Point>();

        // For each line, I'll add it to an existing blob or make it a new blob.
        for (int line = 0; line < leftbounds.size(); line++) {
            int lineleftx = leftbounds.get(line).x;
            int linerightx = rightbounds.get(line).x;
            int liney = leftbounds.get(line).y;

            // If not part of a blob
            boolean partOfaBlob = false;

            for (int blob = 0; blob < blobUpperLeft.size(); blob++) {
                int blobleftx = blobUpperLeft.get(blob).x;
                int blobrightx = blobLowerRight.get(blob).x;
                int bloblowesty = blobLowerRight.get(blob).y;

                if ((((bloblowesty + skiplines) == liney) || (bloblowesty == liney)) && !(linerightx < blobleftx)
                        && !(lineleftx > blobrightx)) {
                    // add it to that blob
                    int newupleftx = blobleftx, newlowerrightx = blobrightx, newuplefty, newlowerrighty;

                    if (lineleftx < blobleftx) {
                        // Update left side
                        newupleftx = lineleftx;
                    }

                    if (linerightx > blobrightx) {
                        // update right side
                        newlowerrightx = linerightx;
                    }

                    // Update bottom
                    newlowerrighty = liney;
                    newuplefty = blobUpperLeft.get(blob).y;

                    // Pass the blob those values.
                    blobUpperLeft.set(blob, new Point(newupleftx, newuplefty));
                    blobLowerRight.set(blob, new Point(newlowerrightx, newlowerrighty));

                    partOfaBlob = true;

                    break;
                }
            }

            if (!partOfaBlob) { // If no blob was found that's a part of this line

                // make it a new blob.
                blobUpperLeft.add(leftbounds.get(line));
                blobLowerRight.add(rightbounds.get(line));
            }
        }

        int[][] resultingblobs = new int[blobLowerRight.size()][4];

        // Turn the blobs into an array {left, top, right, bottom}

        for (int blob = 0; blob < blobUpperLeft.size(); blob++) {
            resultingblobs[blob][this.LEFT] = blobUpperLeft.get(blob).x;
            resultingblobs[blob][this.TOP] = blobUpperLeft.get(blob).y - (skiplines - 1);
            resultingblobs[blob][this.RIGHT] = blobLowerRight.get(blob).x;
            resultingblobs[blob][this.BOTTOM] = blobLowerRight.get(blob).y + (skiplines - 1);
        }

        // BLOBLOBLOBLOBLOBLOBLOBLOBLOBLOBLOBLOBLOBLOBLOBLOBLOBLOBLOBLOB
        return resultingblobs;
    }

    private Bitmap drawSquare(Bitmap step3result, int left, int top, int right, int bottom, int color) {
        Bitmap result = Bitmap.createBitmap(step3result);

        for (int x = left; x < right; x++) {
            result.setPixel(x, top, color); // Draw the upper side
            result.setPixel(x, bottom, color); // And the lower side
        }

        for (int y = top; y < bottom; y++) {
            result.setPixel(left, y, color); // Draw the left side
            result.setPixel(right, y, color); // And the right side.
        }

        return result;
    }

    private Bitmap enforceThreshold(Bitmap image, int threshold, int skiplines) {
        return enforceThreshold(image, threshold, 0, 0, image.getWidth(), image.getHeight(), skiplines);
    }

    private Bitmap enforceThreshold(Bitmap image, int threshold, int left, int top, int right, int bottom, int skiplines) {
        Bitmap result = Bitmap.createBitmap(image);

        for (int y = top; y < bottom; y += skiplines) {
            for (int x = left; x < right; x += 1) {
                if (getColorValue(image.getPixel(x, y)) >= threshold) {
                    result.setPixel(x, y, Color.WHITE);
                } else {
                    result.setPixel(x, y, Color.BLACK);
                }
            }
        }

        return result;
    }

    private int makeDecimalFromNumbers(List<Integer> found) {
        int result = 0;

        // Significance of the first place in the number is 10 ^ ( the amount of numbers - 1 )
        // (significance of the last bit is 1, not 10)
        int significance = (int) Math.pow(10, found.size() - 1);

        for (int item : found) {
            result += item * significance;
            significance /= 10;
        }

        return result;
    }

    @Override
    protected void onPostExecute(Object obj) {
        // Put the image to the screen
        if (this.bitmap != null && this.imageView1 != null) {
            this.imageView1.setImageBitmap(this.bitmap);
        }

        // Put the first suggestion to the screen
        if (this.editText1 != null) {
            this.editText1.setText(Integer.toString(this.suggest1));
        }

        // And put the second suggestion to the screen
        if (this.editText2 != null) {
            this.editText2.setText(Integer.toString(this.suggest2));
        }
    }

    @Override
    protected void onProgressUpdate(Object... values) {
        super.onProgressUpdate(values);

        if (this.testBMP != null && this.imageView1 != null) {
            this.imageView1.setImageBitmap(this.testBMP);
        }
    }

    private Bitmap blurHorizontal(Bitmap source, int radius, int checkLines) {
        return blurHorizontal(source, radius, 0, 0, source.getWidth(), source.getHeight(), checkLines);
    }

    private Bitmap blurHorizontal(Bitmap source, int radius, int left, int top, int right, int bottom, int checkLines) {
        Bitmap dest = Bitmap.createBitmap(source);

        for (int y = top; y < bottom; y += checkLines) {
            int total = 0;

            // Process entire window for first pixel
            for (int kx = left - radius; kx <= left + radius; kx += 1) {
                total += getColorValue(getPixelInBounds(source, kx, y));
            }

            dest.setPixel(left, y, makeColorGrey(total / (radius * 2 + 1)));

            // Subsequent pixels just update window total
            for (int x = left + 1; x < right; x += 1) {
                // Subtract pixel leaving window
                total -= getColorValue(getPixelInBounds(source, x - radius - 1, y));

                // Add pixel entering window
                total += getColorValue(getPixelInBounds(source, x + radius, y));

                dest.setPixel(x, y, makeColorGrey(total / (radius * 2 + 1)));
            }
        }

        return dest;
    }

    private int getPixelInBounds(Bitmap source, int i, int y) {
        return getPixelInBounds(source, 0, 0, source.getWidth(), source.getHeight(), i, y);
    }

    private Bitmap subtractImages(Bitmap original, Bitmap blurred, int checkLines) {
        return subtractImages(original, blurred, 0, 0, original.getWidth(), original.getHeight(), checkLines);
    }

    private Bitmap subtractImages(Bitmap original, Bitmap blurred, int left, int top, int right, int bottom, int skipLines) {
        Bitmap edges = Bitmap.createBitmap(original);

        for (int y = top; y < bottom; y += skipLines) {
            for (int x = left; x < right; x += 1) {
                int clr = getColorValue(original.getPixel(x, y)) - getColorValue(blurred.getPixel(x, y));

                edges.setPixel(x, y, makeColorGrey(clr));
            }
        }

        return edges;
    }

    private int getPixelInBounds(Bitmap bmp, int left, int top, int right, int bottom, int x, int y) {
        int nx = x;
        int ny = y;

        if (x < left) {
            nx = left;
        } else if (x >= right) {
            nx = right - 1;
        }

        if (y < top) {
            ny = top;
        } else if (y >= bottom) {
            ny = bottom - 1;
        }

        return bmp.getPixel(nx, ny);
    }

    /**
     * Gives the average of the red, green and blue components of a color.
     *
     * @param  color - the Color (Android.Graphics.Color) we want to know the value of.
     *
     * @return value - an Integer ranging from 0 to 255 inclusive, denoting the brightness value of the Color.
     */
    public int getColorValue(int color) {
        int avg = (Color.red(color) + Color.green(color) + Color.blue(color)) / 3;

        return avg;
    }

    /**
     * Gives a shade of grey with an given intensity.
     *
     * @param  value - a value between 0 and 255, inclusive. If the value is lower than 0, the function will return pure black (as
     *               if 0 was given) and if it's 256 or higher, pure white is returned.
     *
     * @return the numeric representation of the specific shade of grey.
     */
    public int makeColorGrey(int value) {
        int v;

        if (value < 0) {
            v = 0;
        } else if (value > 255) {
            v = 255;
        } else {
            v = value;
        }

        return Color.rgb(v, v, v);
    }

    private int compareNumber(Bitmap input) {
        Bitmap toGuess = Bitmap.createScaledBitmap(input, this.compareWidth, this.compareHeight, false);

        int[] candidates = findCandidates(toGuess); // Find a list of numbers (hopefully shortened) that this one could be

        /* need to scale examplenums down because it's bigger than its source file. Where the program gets the idea that this is
         * okay I do not know.
         */
        Bitmap numbers = Bitmap.createScaledBitmap(this.exampleNums, this.compareWidth * 10, this.compareHeight, false);
        Bitmap exampl;
        int smallestDiff = this.compareWidth * this.compareHeight * 256;
        int bestGuess = 0;

        for (int n : candidates) {
            int diff = 0;
            exampl = Bitmap.createBitmap(numbers, (n * this.compareWidth), 0, this.compareWidth, this.compareHeight);

            Bitmap show = Bitmap.createBitmap(numbers, (n * this.compareWidth), 0, this.compareWidth, this.compareHeight);

            for (int y = 0; y < this.compareHeight; y++) {
                for (int x = 0; x < this.compareWidth; x++) {
                    int toGuessCLR = getColorValue(toGuess.getPixel(x, y));
                    int examplCLR = getColorValue(exampl.getPixel(x, y));

                    int pixelsDifference = toGuessCLR - examplCLR; // Compare the number to the example

                    if (pixelsDifference < 0) {
                        pixelsDifference = 0;
                    }

                    diff += pixelsDifference;

                    show.setPixel(x, y, makeColorGrey(toGuessCLR));
                }
            }

            if (diff < smallestDiff) {
                smallestDiff = diff; // This is a better candidate than the previously found one, so update this.
                bestGuess = n;
            }
        }

        return bestGuess;
    }

    private Bitmap drawCrosshair(Bitmap step3result, int left, int top, int right, int bottom, int color) {
        Bitmap result = Bitmap.createBitmap(step3result);

        int height = bottom - top;
        int width = right - left;
        double upperCheckLineDouble = height / 3.0;
        double lowerCheckLineDouble = upperCheckLineDouble * 2.0;
        double middleCheckLineDouble = width / 2.0;
        int upperCheckLine = top + (int) upperCheckLineDouble, lowerCheckLine = top + (int) lowerCheckLineDouble;
        int middleCheckLine = left + (int) middleCheckLineDouble;

        for (int x = left; x < right; x++) {
            result.setPixel(x, upperCheckLine, color); // Draw the upper side
            result.setPixel(x, lowerCheckLine, color); // And the lower side
        }

        for (int y = top; y < bottom; y++) {
            result.setPixel(middleCheckLine, y, color);
        }

        return result;
    }

    /**
     * Generates a list of possible numbers based on the edges detected in the picture.
     *
     * @param  toGuess
     *
     * @return
     */
    private int[] findCandidates(Bitmap toGuess) {
        Bitmap edgeyGuess = Bitmap.createBitmap(toGuess);

        edgeyGuess = blurHorizontal(edgeyGuess, 2, 1);
        edgeyGuess = subtractImages(toGuess, edgeyGuess, 1); // Subtract blurred from original for edge detection

        // Do we see edges in the left-up corner, left-down corner, etc
        boolean LUedge = false, LDedge = false, RUedge = false, RDedge = false;
        boolean MUedge = false, MMedge = false, MDedge = false;
        double upperCheckLineDouble = this.compareHeight / 3.0;
        double lowerCheckLineDouble = upperCheckLineDouble * 2.0;
        double middleCheckLineDouble = this.compareWidth / 2.0;
        int upperCheckLine = (int) upperCheckLineDouble, lowerCheckLine = (int) lowerCheckLineDouble;
        int middleCheckLine = (int) middleCheckLineDouble;

        // For the left side of the image
        for (int x = 0; x < edgeyGuess.getWidth() / 2; x += 1) {
            if (getColorValue(edgeyGuess.getPixel(x, upperCheckLine)) > this.numEdgeThreshold) {
                LUedge = true; // Edge was found on pixel-row 4
            }

            if (getColorValue(edgeyGuess.getPixel(x, lowerCheckLine)) > this.numEdgeThreshold) {
                LDedge = true; // Edge was found on pixel-row 9
            }
        }

        // Same for the right side of the image
        for (int x = edgeyGuess.getWidth() / 2; x < edgeyGuess.getWidth(); x += 1) {
            if (getColorValue(edgeyGuess.getPixel(x, upperCheckLine)) > this.numEdgeThreshold) {
                RUedge = true;
            }

            if (getColorValue(edgeyGuess.getPixel(x, lowerCheckLine)) > this.numEdgeThreshold) {
                RDedge = true;
            }
        }

        for (int y = 0; y < edgeyGuess.getHeight() / 3; y += 1) {
            if (getColorValue(edgeyGuess.getPixel(middleCheckLine, y)) > this.numEdgeThreshold) {
                MUedge = true;
            }
        }

        for (int y = edgeyGuess.getHeight() / 3; y < (edgeyGuess.getHeight() / 3) * 2; y += 1) {
            if (getColorValue(edgeyGuess.getPixel(middleCheckLine, y)) > this.numEdgeThreshold) {
                MMedge = true;
            }
        }

        for (int y = (edgeyGuess.getHeight() / 3) * 2; y < edgeyGuess.getHeight(); y += 1) {
            if (getColorValue(edgeyGuess.getPixel(middleCheckLine, y)) > this.numEdgeThreshold) {
                MDedge = true;
            }
        }

        List<Integer> resultList = numberEdgeLogic(LUedge, RUedge, LDedge, RDedge, MUedge, MMedge, MDedge);

        int[] result = new int[resultList.size()];

        for (int i = 0; i < resultList.size(); i++) {
            result[i] = resultList.get(i).intValue();
        }

        return result;
    }

    private List<Integer> numberEdgeLogic(boolean LUedge, boolean RUedge, boolean LDedge, boolean RDedge, boolean MUedge,
        boolean MMedge, boolean MDedge) {
        List<Integer> resultList = new ArrayList<Integer>();

        if (LUedge && LDedge && RUedge && RDedge && !MMedge && MUedge && MDedge) {
            resultList.add(Integer.valueOf(0));
        }

        if ((LUedge && LDedge) || (RUedge && RDedge)) { // 1: Corners immediately below each other have edges.
            resultList.add(Integer.valueOf(1));
        }

        if (MUedge && RUedge && MDedge && ((MMedge && LDedge) || RDedge)) { // 2
            resultList.add(Integer.valueOf(2));
        }

        if (RDedge && RUedge && MUedge && MDedge) { // 3
            resultList.add(Integer.valueOf(3));
        }

        if (!RUedge && !MMedge && RDedge && LDedge && LUedge && MUedge && MDedge) { // 4
            resultList.add(Integer.valueOf(4));
        }

        if (LUedge && RDedge && !RUedge && MMedge && MUedge && MDedge) { // 5: RU corner does not have an edge! Only one for a definitely not.
            resultList.add(Integer.valueOf(5));
        }

        if (LUedge && LDedge && RDedge && MMedge && MUedge && MDedge) { // 6: All but the RU are sure
            resultList.add(Integer.valueOf(6));
        }

        if (((LDedge && MMedge) || (RDedge && MDedge)) && RUedge && MUedge) { // done.
            resultList.add(Integer.valueOf(7));
        }

        if (LUedge && LDedge && RUedge && RDedge && MMedge && MUedge && MDedge) {
            resultList.add(Integer.valueOf(8));
        }

        if (LUedge && RUedge && LDedge && MMedge && MUedge && MDedge) { // 9: Only lower left corner is unsure.
            resultList.add(Integer.valueOf(9));
        }

        return resultList;
    }
}
