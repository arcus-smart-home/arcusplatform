/*
 * Copyright 2019 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iris.oculus.widget;


import java.awt.Color;
import java.awt.Component;
import java.awt.Label;
import java.awt.Point;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.Popup;
import javax.swing.PopupFactory;

/**
 * Created by jlyman on 9/8/15.
 */
public class Toast {

    private final Component component;
    private Point   location;
    private final String  message;
    private long duration; //in millisecond

    public Toast(Component comp, Point toastLocation, String msg, long forDuration) {
        this.component = comp;
        this.location = toastLocation;
        this.message = msg;
        this.duration = forDuration;

        if(this.component != null)
        {

            if(this.location == null)
            {
                this.location = component.getLocationOnScreen();
            }

            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    Popup view = null;
                    try
                    {
                        Label tip = new Label(message);
                        tip.setForeground(Color.black);
                        tip.setBackground(Color.white);
                        view = PopupFactory.getSharedInstance().getPopup(component, tip , location.x + 30, location.y + component.getHeight() + 5);
                        view.show();
                        Thread.sleep(duration);
                    } catch (InterruptedException ex)
                    {
                        Logger.getLogger(Toast.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    finally
                    {
                        view.hide();
                    }
                }
            }).start();
        }
    }



    public static void showToast(Component component, String message)
    {
        new Toast(component, null, message, 2000/*Default 2 Sec*/);
    }

    public static void showToast(Component component, String message, Point location, long forDuration)
    {
        new Toast(component, location, message, forDuration);
    }
}

