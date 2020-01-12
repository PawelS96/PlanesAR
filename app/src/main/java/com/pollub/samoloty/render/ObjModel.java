package com.pollub.samoloty.render;

import android.content.res.AssetManager;
import android.util.Log;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class ObjModel extends Model {

    private static final String TAG = "ObjectLoader";
    private static final int BUFFER_READER_SIZE = 65536;
    private static final boolean ENABLE_LOGGING = true;

    private int numVerts = 0;

    private ByteBuffer vBuffer, nBuffer, tBuffer;

    @NotNull
    @Override
    public Buffer getVertices() {
        return vBuffer;
    }

    @NotNull
    @Override
    public Buffer getTexCoords() {
        return tBuffer;
    }

   // @Override
    //public Buffer getNormals() {
      //  return nBuffer;
   // }

   /* @Override
    public Buffer getIndices() {
        return null;
    }
*/
    @Override
    public int getVertexCount() {
        return numVerts;
    }

    public void loadModel(AssetManager assetManager, String filename) throws IOException{

        long time1 = System.currentTimeMillis();

        InputStream is = assetManager.open(filename);;
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        try {
            ArrayList<Float> vlist = new ArrayList<>();
            ArrayList<Float> tlist = new ArrayList<>();
            ArrayList<Float> nlist = new ArrayList<>();
            ArrayList<Face> fplist = new ArrayList<>();

            int numTexCoords = 0;
            int numNormals = 0;
            int numFaces = 0;

            String str;
            String[] tmp;

            reader = new BufferedReader(new InputStreamReader(is), BUFFER_READER_SIZE);

            while ((str = reader.readLine()) != null) {

                // Replace double spaces. Some files may have it. Ex. files from 3ds max.
             //   str = str.replace("  ", " ");
                tmp = str.split(" ");

                if (tmp[0].equalsIgnoreCase("v")) {
                    for (int i = 1; i < 4; i++) {
                        vlist.add(Float.parseFloat(tmp[i]));
                    }
                    numVerts++;
                }

                else if (tmp[0].equalsIgnoreCase("vn")) {
                    for (int i = 1; i < 4; i++) {
                        nlist.add(Float.parseFloat(tmp[i]));
                    }
                    numNormals++;
                }

                else if (tmp[0].equalsIgnoreCase("vt")) {
                    for (int i = 1; i < 3; i++) {
                        tlist.add(Float.parseFloat(tmp[i]));
                    }
                    numTexCoords++;
                }

                else if (tmp[0].equalsIgnoreCase("f")) {

                    String[] ftmp;
                    int facex;
                    int facey;
                    int facez;

                    for (int i = 1; i < 4; i++) {
                        ftmp = tmp[i].split("/");

                        facex = faceStringToInt(ftmp, 0) - 1;
                        facey = faceStringToInt(ftmp, 1) - 1;
                        facez = faceStringToInt(ftmp, 2) - 1;

                        fplist.add(new Face(facex, facey, facez));
                    }

                    numFaces++;

                    if (tmp.length > 4 && !tmp[4].equals("")) {

                        for (int i = 1; i < 5; i++) {
                            ftmp = tmp[i].split("/");

                            if (i == 1 || i == 3) {
                                facex = faceStringToInt(ftmp, 0) - 1;
                                facey = faceStringToInt(ftmp, 1) - 1;
                                facez = faceStringToInt(ftmp, 2) - 1;
                                fplist.add(new Face(facex, facey, facez));
                            } else if (i == 2) {
                                String[] gtmp = tmp[4].split("/");
                                facex = faceStringToInt(gtmp, 0) - 1;
                                facey = faceStringToInt(gtmp, 1) - 1;
                                facez = faceStringToInt(gtmp, 2) - 1;
                                fplist.add(new Face(facex, facey, facez));
                            }
                        }

                        numFaces++;
                    }
                }
            }

            if (ENABLE_LOGGING) {
                Log.d(TAG, "Vertices: " + numVerts);
                Log.d(TAG, "Normals: " + numNormals);
                Log.d(TAG, "TextureCoords: " + numTexCoords);
                Log.d(TAG, "Faces: " + numFaces);
            }

            // Each float takes 4 bytes
            int fplistSize = fplist.size();
            vBuffer = ByteBuffer.allocateDirect(fplistSize * 4 * 3);
            vBuffer.order(ByteOrder.nativeOrder());

            tBuffer = ByteBuffer.allocateDirect(fplistSize * 4 * 2);
            tBuffer.order(ByteOrder.nativeOrder());

            nBuffer = ByteBuffer.allocateDirect(fplistSize * 4 * 3);
            nBuffer.order(ByteOrder.nativeOrder());

            int vlistSize = vlist.size();
            int nlistSize = nlist.size();
            int tlistSize = tlist.size();

            int finalNumTexCoords = numTexCoords;
            int finalNumNormals = numNormals;

            for (int j = 0; j < fplistSize; j++) {

                Face face = fplist.get(j);

                int x = (int)face.fx * 3;

                vBuffer.putFloat(vlist.get(fixedIndex(vlistSize, x, numVerts)));
                vBuffer.putFloat(vlist.get(fixedIndex(vlistSize, x + 1, numVerts)));
                vBuffer.putFloat(vlist.get(fixedIndex(vlistSize, x + 2, numVerts)));

                int y = face.fy * 2;

                tBuffer.putFloat(tlist.get(fixedIndex(tlistSize, y, finalNumTexCoords)));
                tBuffer.putFloat(tlist.get(fixedIndex(tlistSize, y + 1, finalNumTexCoords)));

                int z = face.fz * 3;

                nBuffer.putFloat(nlist.get(fixedIndex(nlistSize, z, finalNumNormals)));
                nBuffer.putFloat(nlist.get(fixedIndex(nlistSize, z + 1, finalNumNormals)));
                nBuffer.putFloat(nlist.get(fixedIndex(nlistSize, z + 2, finalNumNormals)));

        };
            vBuffer.rewind();
            tBuffer.rewind();
            nBuffer.rewind();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.d("LoadingTime", Long.toString(System.currentTimeMillis() - time1));
    }

    private static int fixedIndex(int listSize, int index, int total) {
        if (index >= listSize || index < 0) {
            return total - 1;
        } else {
            return index;
        }
    }

    private static int faceStringToInt(String[] number, int index) {
        int result;

        try {
            result = Integer.parseInt(number[index]);
        } catch (Exception e) {
            result = 0;
        }

        return result;
    }

    private static class Face {
         long fx;
         int fy;
         int fz;

         Face(long fx, int fy, int fz) {
            this.fx = fx;
            this.fy = fy;
            this.fz = fz;
        }
    }
}
