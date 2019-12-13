/*===============================================================================
Copyright (c) 2016,2018 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.pollub.samoloty.render;

import java.nio.Buffer;

/**
 * The Model utility class is used to render various 3D objects and stores
 * all the information required for rendering.
 */

public abstract class Model {

    public abstract Buffer getVertices();

    public abstract Buffer getTexCoords();

    public abstract Buffer getNormals();

    public abstract Buffer getIndices();

    public abstract int getNumObjectVertex();
}
