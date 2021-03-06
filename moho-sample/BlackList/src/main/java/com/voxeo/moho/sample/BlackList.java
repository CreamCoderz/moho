/**
 * Copyright 2010 Voxeo Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho.sample;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.media.mscontrol.join.Joinable;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.State;
import com.voxeo.moho.event.AcceptableEvent.Reason;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.TextToSpeechResource;

/**
 * Black list(incoming call screening) example.
 */
public class BlackList implements Application {

  Map<String, List<String>> _blacklists = new HashMap<String, List<String>>();

  OutputCommand _prompt = new OutputCommand(new TextToSpeechResource("Please wait while we connect you."));

  @Override
  public void destroy() {
  }

  @Override
  public void init(final ApplicationContext ctx) {
    try {
      InputStream in = new FileInputStream(ctx.getServletContext().getRealPath("WEB-INF/blacklist.txt"));

      final Properties p = new Properties();
      p.load(in);
      final Enumeration<?> e = p.propertyNames();
      while (e.hasMoreElements()) {
        final String key = (String) e.nextElement();
        final String values = p.getProperty(key);
        if (values != null && values.length() > 0) {
          final List<String> list = new ArrayList<String>();
          for (final String value : values.split(",")) {
            list.add(value.trim());
          }
          _blacklists.put(key, list);
        }
      }
    }
    catch (final Exception e) {
      // ignore the exception, as there is no black list.
      System.out.println("Can't get blackList File");
      e.printStackTrace();
    }

    System.out.println("BlackList: " + _blacklists);
  }

  @State
  public void handleIncomingCall(final IncomingCall call) throws Exception {
    final Endpoint caller = call.getInvitor();
    final CallableEndpoint callee = call.getInvitee();
    final List<String> blacklist = _blacklists.get(callee.getName());
    if (blacklist != null && blacklist.contains(caller.getName())) {
      call.reject(Reason.FORBIDEN);
      return;
    }

    call.answer();
    call.output(_prompt).get();
    call.join(callee, JoinType.DIRECT, Joinable.Direction.DUPLEX).get();
  }
}