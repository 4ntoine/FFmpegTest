package com.splinex.streaming;/*
 *				Intrinsyc Software International, Inc.
 *				Suite 380 - 885 Dunsmuir Street
 *				Vancouver, BC 
 *				Canada V6C 1N5
 *				Phone: (604) 801-6461
 *				Toll free: 1-800-474-7644
 *				Fax: (604) 801-6417
 *==============================================================================================
 * Author : Nina Sverdlenko <nsverdlenko@intrinsyc.com>
 * Date   : 6th January 2014
 * Version : 1.0
 */

import android.hardware.Camera;

interface CameraViewInterface {
    public void setCamera(Camera camera, int index, String name);

    public void setCamera(Camera camera);

    public void initPreview(int w, int h, boolean fStart);

    public void endPreview();
}
