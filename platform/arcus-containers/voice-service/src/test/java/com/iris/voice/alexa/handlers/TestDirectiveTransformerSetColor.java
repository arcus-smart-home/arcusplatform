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
package com.iris.voice.alexa.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.alexa.AlexaInterfaces;
import com.iris.alexa.error.AlexaErrors;
import com.iris.alexa.error.AlexaException;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ColorCapability;
import com.iris.messages.capability.LightCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.service.AlexaService;
import com.iris.messages.type.AlexaColor;
import com.iris.voice.alexa.AlexaConfig;

public class TestDirectiveTransformerSetColor {

   private Model colorModel;
   private AlexaConfig config;

   @Before
   public void setup() {
      colorModel = new SimpleModel();
      colorModel.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(SwitchCapability.NAMESPACE, ColorCapability.NAMESPACE, LightCapability.NAMESPACE));

      config = new AlexaConfig();
   }

   @Test
   public void testSetMissingArgumentArgsNull() {
      try {
         MessageBody req = request(null);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, colorModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INVALID_DIRECTIVE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetMissingArgumentColorNull() {
      try {
         MessageBody req = builder().withArguments(ImmutableMap.of()).build();
         DirectiveTransformer.transformerFor(req).txfmRequest(req, colorModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INVALID_DIRECTIVE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetMissingArgumentHue() {
      try {
         AlexaColor c = new AlexaColor();
         c.setSaturation(1.0);
         MessageBody req = request(c);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, colorModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INVALID_DIRECTIVE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetHueOutOfRangeHigh() {
      try {
         AlexaColor c = new AlexaColor();
         c.setSaturation(1.0);
         c.setHue(360.1);
         MessageBody req = request(c);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, colorModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_VALUE_OUT_OF_RANGE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetHueOutOfRangeLow() {
      try {
         AlexaColor c = new AlexaColor();
         c.setSaturation(1.0);
         c.setHue(-0.1);
         MessageBody req = request(c);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, colorModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_VALUE_OUT_OF_RANGE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetMissingArgumentSaturation() {
      try {
         AlexaColor c = new AlexaColor();
         c.setHue(120.0);
         MessageBody req = request(c);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, colorModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INVALID_DIRECTIVE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetSaturationOutOfRangeHigh() {
      try {
         AlexaColor c = new AlexaColor();
         c.setSaturation(1.1);
         c.setHue(120.0);
         MessageBody req = request(c);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, colorModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_VALUE_OUT_OF_RANGE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetSaturationOutOfRangeLow() {
      try {
         AlexaColor c = new AlexaColor();
         c.setSaturation(-0.1);
         c.setHue(120.0);
         MessageBody req = request(c);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, colorModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_VALUE_OUT_OF_RANGE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetCurrentHueUnknown() {
      colorModel.setAttribute(ColorCapability.ATTR_SATURATION, 100);
      try {
         AlexaColor c = new AlexaColor();
         c.setHue(120.0);
         c.setSaturation(1.0);
         MessageBody req = request(c);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, colorModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INTERNAL_ERROR, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetCurrentSaturationUnknown() {
      colorModel.setAttribute(ColorCapability.ATTR_HUE, 120);
      try {
         AlexaColor c = new AlexaColor();
         c.setHue(120.0);
         c.setSaturation(1.0);
         MessageBody req = request(c);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, colorModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INTERNAL_ERROR, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetExistingColorTurnsOn() {
      colorModel.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF);
      colorModel.setAttribute(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLOR);
      colorModel.setAttribute(ColorCapability.ATTR_SATURATION, 0);
      colorModel.setAttribute(ColorCapability.ATTR_HUE, 100);

      AlexaColor c = new AlexaColor();
      c.setSaturation(0.0);
      c.setHue(100.0);

      MessageBody req = request(c);

      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, colorModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(1, body.getAttributes().size());
      assertEquals(SwitchCapability.STATE_ON, SwitchCapability.getState(body));
   }

   @Test
   public void testSetExistingColorTurnsOnAndChangesColorMode() {
      colorModel.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF);
      colorModel.setAttribute(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLORTEMP);
      colorModel.setAttribute(ColorCapability.ATTR_SATURATION, 0);
      colorModel.setAttribute(ColorCapability.ATTR_HUE, 100);

      AlexaColor c = new AlexaColor();
      c.setSaturation(0.0);
      c.setHue(100.0);

      MessageBody req = request(c);

      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, colorModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(2, body.getAttributes().size());
      assertEquals(SwitchCapability.STATE_ON, SwitchCapability.getState(body));
      assertEquals(LightCapability.COLORMODE_COLOR, LightCapability.getColormode(body));
   }

   @Test
   public void testSetExistingColor() {
      colorModel.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      colorModel.setAttribute(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLOR);
      colorModel.setAttribute(ColorCapability.ATTR_SATURATION, 0);
      colorModel.setAttribute(ColorCapability.ATTR_HUE, 100);

      AlexaColor c = new AlexaColor();
      c.setSaturation(0.0);
      c.setHue(100.0);

      MessageBody req = request(c);

      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, colorModel, config);
      assertFalse(optBody.isPresent());
   }

   @Test
   public void testSetColorHueOnlyChanges() {
      colorModel.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      colorModel.setAttribute(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLOR);
      colorModel.setAttribute(ColorCapability.ATTR_SATURATION, 0);
      colorModel.setAttribute(ColorCapability.ATTR_HUE, 100);

      AlexaColor c = new AlexaColor();
      c.setSaturation(0.0);
      c.setHue(120.0);

      MessageBody req = request(c);

      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, colorModel, config);
      MessageBody body = optBody.get();
      assertEquals(1, body.getAttributes().size());
      assertEquals(120, ColorCapability.getHue(body).intValue());
   }

   @Test
   public void testSetColorSatOnlyChanges() {
      colorModel.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      colorModel.setAttribute(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLOR);
      colorModel.setAttribute(ColorCapability.ATTR_SATURATION, 0);
      colorModel.setAttribute(ColorCapability.ATTR_HUE, 100);

      AlexaColor c = new AlexaColor();
      c.setSaturation(1.0);
      c.setHue(100.0);

      MessageBody req = request(c);

      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, colorModel, config);
      MessageBody body = optBody.get();
      assertEquals(1, body.getAttributes().size());
      assertEquals(100, ColorCapability.getSaturation(body).intValue());
   }

   @Test
   public void testSetColorChanges() {
      colorModel.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      colorModel.setAttribute(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLOR);
      colorModel.setAttribute(ColorCapability.ATTR_SATURATION, 0);
      colorModel.setAttribute(ColorCapability.ATTR_HUE, 100);

      AlexaColor c = new AlexaColor();
      c.setSaturation(1.0);
      c.setHue(120.0);

      MessageBody req = request(c);

      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, colorModel, config);
      MessageBody body = optBody.get();
      assertEquals(2, body.getAttributes().size());
      assertEquals(100, ColorCapability.getSaturation(body).intValue());
      assertEquals(120, ColorCapability.getHue(body).intValue());
   }

   private MessageBody request(AlexaColor color) {
      AlexaService.ExecuteRequest.Builder builder = builder();

      if(color != null) {
         builder.withArguments(ImmutableMap.of(AlexaInterfaces.ColorController.PROP_COLOR, color.toMap()));
      }

      return builder.build();
   }

   private AlexaService.ExecuteRequest.Builder builder() {
      return AlexaService.ExecuteRequest.builder()
         .withDirective(AlexaInterfaces.ColorController.REQUEST_SETCOLOR);
   }

}

