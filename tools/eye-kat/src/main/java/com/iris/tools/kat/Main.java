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
package com.iris.tools.kat;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.CommaParameterSplitter;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HostAndPort;
import com.iris.io.json.JSON;
import com.iris.kafka.KafkaStream;
import com.iris.tools.kat.message.Entry;
import com.iris.tools.kat.output.FormattedMessageWriter;
import com.iris.tools.kat.output.JsonMessageWriter;
import com.iris.tools.kat.output.MessageWriter;
import com.iris.util.ByteUtil;
import com.iris.util.IrisCollections;
import com.iris.util.ThreadPoolBuilder;

public class Main {


   public static class Arguments {
      @Parameter(
            names = { "-t", "--topics" },
            description = "Topics to connect to, all must currently be at the same broker",
            arity = 1,
            required = true
      )
      private String topics;
      @Parameter(
            names = { "-s", "--start" },
            description = "Start time as 'earliest', 'latest', 'YYYY-MM-DDTHH:MM:SS' or '+/-#d#h#m#s'.  For dates all later parts are optional, so 2016-12-02 would be valid to start at midnight on the December 2nd",
            arity = 1,
            converter = StartTimeConverter.class,
            required = false
      )
      private Instant start = Instant.ofEpochMilli(kafka.api.OffsetRequest.EarliestTime());
      @Parameter(
            names = { "-e", "--end" },
            description = "End time as 'now', 'YYYY-MM-DDTHH:MM:SS' or '+/-#d#h#m#s'.",
            arity = 1,
            converter = EndTimeConverter.class,
            required = false
      )
      private Instant end = Instant.MAX; // sometime a bit after the heat-death of the universe
      @Parameter(
            names = { "-b", "--broker" },
            description = "Broker to connect to, default: kafka.eyeris:9092",
            arity = 1,
            required = false
      )
      private String broker = "kafka.eyeris:9092";
      @Parameter(
            names = { "--broker-overrides" },
            description = "Comma separate list of brokers to use instead of those reported by Kafka, this is generally used in port forwarding situations",
            arity = 1,
            required = false
      )
      private String brokerOverrides = null;
      @Parameter(
            names = { "-p", "--places" },
            description = "ID of a place(s) to include, only messages with this place header will be included",
            arity = 1,
            splitter = CommaParameterSplitter.class,
            required = false
      )
      private List<String> places = ImmutableList.of();
      @Parameter(
            names = { "-a", "--addresses" },
            description = "Address(es) to search for, this may be wildcarded, and source, destination and actor headers will be searched. May be specified multiple times.",
            arity = 1,
            splitter = CommaParameterSplitter.class,
            required = false
      )
      private List<String> addresses = ImmutableList.of();
      @Parameter(
            names = { "-f", "--format" },
            description = "Output format: summary or json",
            arity = 1,
            required = false
      )
      private OutputFormat format = OutputFormat.SUMMARY;
      
   }

   public static void main(String [] args) throws Exception {
      Arguments arguments = new Arguments();
      new JCommander(arguments, args);
      
      ExecutorService executor =
            new ThreadPoolBuilder()
               .withNameFormat("topic-reader-%d")
               .build();
      Map<String, Future<?>> results = new LinkedHashMap<>();
      String [] topics = arguments.topics.split("\\s*,\\s*");
      for(String topic: IrisCollections.setOf(topics)) {
         Future<?> result = executor.submit(() -> run(topic, arguments));
         results.put(topic, result);
      }
      boolean success = true;
      for(Map.Entry<String, Future<?>> result: results.entrySet()) {
         try {
            result.getValue().get();
         }
         catch(Exception e) {
            System.err.println("Error streaming from topic: " + result.getKey());
            e.printStackTrace();
            success = false;
         }
      }
      System.exit(success ? 0 : -1);
   }
   
   private static void run(String topic, Arguments arguments) {
      KafkaStream.Builder<Integer, ? extends Entry> builder =
            KafkaStream
               .builder()
               .withClientId("eye-kat")
               .withTopic(topic)
               .withBroker(HostAndPort.fromString(arguments.broker))
               .deserializeByteKeys(ByteUtil::bytesToInt)
               .deserializeStringValues(Entry.getDeserializerForTopic(topic))
               ;
         if(arguments.start != null) {
            builder
               // get in the ballpark
               .scanBefore(arguments.start)
               // get exact
               .scanValues((message) -> message != null ? message.getTimestamp() : null, arguments.start);
         }
         else {
            builder.scanToLatest();
         }
         if(arguments.brokerOverrides != null) {
            String [] brokers = arguments.brokerOverrides.split("\\s*,\\s*");
            List<HostAndPort> overrides = new ArrayList<>(brokers.length);
            for(String broker: brokers) {
               overrides.add(HostAndPort.fromString(broker));
            }
            builder.withBrokerOverrides(overrides);
         }
         
         Predicate<Entry> filter = buildPredicate(arguments);
         KafkaStream<Integer, ? extends Entry> stream =
            builder
               .build()
               .whileValuesMatch((message) -> message.getTimestamp().isBefore(arguments.end))
               .filterValues((e) -> filter.apply(e));
         
         BlockingQueue<Entry> messages = new ArrayBlockingQueue<>(100);
         Thread t = new Thread("output-writer") {

            /* (non-Javadoc)
             * @see java.lang.Thread#run()
             */
            @Override
            public void run() {
               try(MessageWriter writer = arguments.format.create(new PrintWriter(System.out))) {
                  while(true) {
                     Entry message = messages.take();
                     try {
                         writer.write(message);
                      }
                      catch(IOException e) {
                         e.printStackTrace();
                      }
                  }
               }
               catch(InterruptedException e) {
                  // expected, done
               }
            }
            
         };
         t.start();
         
         try {
            stream.forEach((partition, message) -> {
               try {
                  messages.put(message);
               }
               catch(InterruptedException e) {
                  e.printStackTrace();
               }
            });
            while(!messages.isEmpty()) {
               Thread.sleep(100);
            }
            t.interrupt();
            t.join();
         }
         catch(InterruptedException e) {
            
         }
   }

   private static Predicate<Entry> buildPredicate(Arguments arguments) {
      return Predicates.and(matchingPlaces(arguments.places), matchingAddresses(arguments.addresses));
   }

   private static Predicate<Entry> matchingPlaces(List<String> places) {
      return or(places, EntryPredicates::matchesPlace);
   }
   
   private static Predicate<Entry> matchingAddresses(List<String> addresses) {
      return or(addresses, EntryPredicates::matchesAddress);
   }

   private static Predicate<Entry> or(List<String> queries, Function<String, Predicate<Entry>> transform) {
      Preconditions.checkNotNull(transform);
      if(queries == null || queries.isEmpty()) {
         return Predicates.alwaysTrue();
      }
      List<Predicate<Entry>> predicates = new ArrayList<>(queries.size());
      for(String query: queries) {
         Predicate<Entry> p = transform.apply(query);
         if(p != null) {
            predicates.add(p);
         }
      }
      if(predicates.isEmpty()) {
         return Predicates.alwaysTrue();
      }
      else if(predicates.size() == 1) {
         return predicates.get(0);
      }
      else {
         return Predicates.or(predicates);
      }
   }
   
   private static enum OutputFormat {
      SUMMARY,
      JSON;
      
      public MessageWriter create(Writer writer) {
         switch(this) {
         case SUMMARY:
            return new FormattedMessageWriter(writer);
         case JSON:
            return new JsonMessageWriter(writer);
         default:
            throw new UnsupportedOperationException("Not yet defined");
         }
         
      }
   }
}

