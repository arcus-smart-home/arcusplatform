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
package com.iris.agent.util;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.exec.ExecService;

import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.schedulers.Schedulers;

public final class RxIris {
   private static final Logger log = LoggerFactory.getLogger(RxIris.class);

   public static final Scheduler io = Schedulers.from(ExecService.io());
   public static final Scheduler background = Schedulers.from(ExecService.backgroundIo());
   public static final Scheduler periodic = Schedulers.from(ExecService.periodic());

   private RxIris() {
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   public static rx.functions.Func1<Object,Boolean> isnull() {
      return IsNull.INSTANCE;
   }

   public static rx.functions.Func1<Object,Boolean> notnull() {
      return NotNull.INSTANCE;
   }

   private static enum IsNull implements rx.functions.Func1<Object,Boolean> {
      INSTANCE;

      @Override
      public Boolean call(@Nullable Object obj) {
         return obj == null;
      }
   }

   private static enum NotNull implements rx.functions.Func1<Object,Boolean> {
      INSTANCE;

      @Override
      public Boolean call(@Nullable Object obj) {
         return obj != null;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   public static <T> rx.functions.Func1<Throwable,? extends T> retrn(@Nullable final T value) {
      return new rx.functions.Func1<Throwable, T>() {
         @Nullable
         @Override
         public T call(@Nullable Throwable th) {
            return value;
         }
      };
   }

   public static <T> Always<T> always(T value) {
      return new Always<T>(Observable.just(value));
   }

   public static <T> Always<T> always(Observable<T> obs) {
      return new Always<T>(obs);
   }

   public static <T> Then<T> then(Observable<T> obs) {
      return new Then<T>(obs);
   }

   public static <T> After<T> after(Observable<?> obs) {
      return new After<T>(obs);
   }

   public static final class Always<T> extends Operator<T,Object> {
      private final Observable<T> obs;

      public Always(Observable<T> obs) {
         this.obs = obs;
      }

      @Override
      public rx.Subscriber<? super Object> run(final rx.Subscriber<? super T> subscriber) {
         return new rx.Subscriber<Object>() {
            @Override
            public void onNext(@Nullable Object next) {
            }

            @Override
            public void onError(@Nullable Throwable cause) {
               if (!subscriber.isUnsubscribed()) {
                  obs.subscribe(subscriber);
               }
            }

            @Override
            public void onCompleted() {
               if (!subscriber.isUnsubscribed()) {
                  obs.subscribe(subscriber);
               }
            }
         };
      }
   }

   public static final class Then<T> extends Operator<T,Object> {
      private final Observable<T> obs;

      public Then(Observable<T> obs) {
         this.obs = obs;
      }

      @Override
      public rx.Subscriber<? super Object> run(final rx.Subscriber<? super T> subscriber) {
         return new rx.Subscriber<Object>() {
            @Override
            public void onNext(@Nullable Object next) {
            }

            @Override
            public void onError(@Nullable Throwable cause) {
               if (!subscriber.isUnsubscribed()) {
                  subscriber.onError(cause);
               }
            }

            @Override
            public void onCompleted() {
               if (!subscriber.isUnsubscribed()) {
                  obs.subscribe(subscriber);
               }
            }
         };
      }
   }

   public static final class After<T> extends Operator<T,T> {
      private final Observable<?> after;

      public After(Observable<?> after) {
         this.after = after;
      }

      @Override
      public rx.Subscriber<? super T> run(final rx.Subscriber<? super T> subscriber) {
         return new rx.Subscriber<T>() {
            @Override
            public void onNext(@Nullable T next) {
               if (!subscriber.isUnsubscribed()) {
                  subscriber.onNext(next);
               }
            }

            @Override
            public void onError(@Nullable Throwable cause) {
               if (!subscriber.isUnsubscribed()) {
                  subscriber.onError(cause);
               }
            }

            @Override
            public void onCompleted() {
               if (!subscriber.isUnsubscribed()) {
                  after.subscribe(new rx.Observer<Object>() {
                     @Override
                     public void onNext(@Nullable Object value) {
                        // Ignore events sent by after
                     }

                     @Override
                     public void onError(@Nullable Throwable cause) {
                        if (!subscriber.isUnsubscribed()) {
                           subscriber.onError(cause);
                        }
                     }

                     @Override
                     public void onCompleted() {
                        if (!subscriber.isUnsubscribed()) {
                           subscriber.onCompleted();
                        }
                     }
                  });
               }
            }
         };
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Error producing operators
   /////////////////////////////////////////////////////////////////////////////

   public static <T> Observable.Operator<Boolean,T> equalToOrError(T value, final String header) {
      return equalToOrError(value, new rx.functions.Func1<T,Throwable>() {
         @Override
         public Throwable call(@Nullable T value) {
            return new Exception(header + value);
         }
      });
   }

   public static <T> Observable.Operator<Boolean,T> equalToOrError(final T value, final rx.functions.Func1<T,Throwable> error) {
      return new Operator<Boolean,T>() {
         @Override
         public rx.Subscriber<? super T> run(final rx.Subscriber<? super Boolean> subscriber) {
            return new rx.Subscriber<T>() {
               @Override
               public void onNext(@Nullable T next) {
                  if (!subscriber.isUnsubscribed()) {
                     if (value.equals(next)) {
                        subscriber.onNext(true);
                     } else {
                        subscriber.onError(error.call(next));
                     }
                  }
               }

               @Override
               public void onError(@Nullable Throwable cause) {
                  if (!subscriber.isUnsubscribed()) {
                     subscriber.onError(cause);
                  }
               }

               @Override
               public void onCompleted() {
                  if (!subscriber.isUnsubscribed()) {
                     subscriber.onCompleted();
                  }
               }
            };
         }
      };
   }

   public static <T> Error<T> errorWhen(final String header, final rx.functions.Func1<T,Boolean> cond) {
      return errorWhen(cond, new rx.functions.Func1<T,Throwable>() {
         @Override
         public Throwable call(@Nullable T value) {
            return new Exception(header + value);
         }
      });
   }

   public static <T> Error<T> errorWhen(final rx.functions.Func1<T,Boolean> cond, final String header) {
      return errorWhen(cond, new rx.functions.Func1<T,Throwable>() {
         @Override
         public Throwable call(@Nullable T value) {
            return new Exception(header + value);
         }
      });
   }

   public static <T> Error<T> errorWhen(final rx.functions.Func1<T,Boolean> cond, final rx.functions.Func1<T,Throwable> error) {
      return new Error<T>(cond, error);
   }

   public static final class Error<T> extends Operator<T,T> {
      private final rx.functions.Func1<T,Boolean> cond;
      private final rx.functions.Func1<T,Throwable> error;

      public Error(rx.functions.Func1<T, Boolean> cond, rx.functions.Func1<T, Throwable> error) {
         this.cond = cond;
         this.error = error;
      }

      @Override
      public rx.Subscriber<? super T> run(final rx.Subscriber<? super T> subscriber) {
         return new rx.Subscriber<T>() {
            @Override
            public void onNext(@Nullable T next) {
               if (!subscriber.isUnsubscribed()) {
                  if (cond.call(next)) {
                     subscriber.onError(error.call(next));
                  } else {
                     subscriber.onNext(next);
                  }
               }
            }

            @Override
            public void onError(@Nullable Throwable cause) {
               if (!subscriber.isUnsubscribed()) {
                  subscriber.onError(cause);
               }
            }

            @Override
            public void onCompleted() {
               if (!subscriber.isUnsubscribed()) {
                  subscriber.onCompleted();
               }
            }
         };
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Equality operators
   /////////////////////////////////////////////////////////////////////////////

   public static <T> EqualTo<T> equalTo(@Nullable T value) {
      return new EqualTo<T>(value);
   }

   public static final class EqualTo<T> extends Operator<Boolean,T> {
      private final @Nullable T value;

      public EqualTo(@Nullable T value) {
         this.value = value;
      }

      @Override
      public rx.Subscriber<? super T> run(final rx.Subscriber<? super Boolean> subscriber) {
         return new rx.Subscriber<T>() {
            @Override
            public void onNext(@Nullable T next) {
               if (!subscriber.isUnsubscribed()) {
                  if (value != null) {
                     subscriber.onNext(value.equals(next));
                  } else {
                     subscriber.onNext(next == null);
                  }
               }
            }

            @Override
            public void onError(@Nullable Throwable cause) {
               if (!subscriber.isUnsubscribed()) {
                  subscriber.onError(cause);
               }
            }

            @Override
            public void onCompleted() {
               if (!subscriber.isUnsubscribed()) {
                  subscriber.onCompleted();
               }
            }
         };
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Repeat logic
   /////////////////////////////////////////////////////////////////////////////

   public static Repeat repeat(long attempts, long delay, TimeUnit units) {
      return repeat(attempts, delay, units, -1, TimeUnit.NANOSECONDS);
   }

   public static Repeat repeat(long delay, TimeUnit units, long duration, TimeUnit durationUnits) {
      return repeat(Long.MAX_VALUE, delay, units, duration, durationUnits);
   }

   public static Repeat repeat(long delay, TimeUnit units, long duration, TimeUnit durationUnits, @Nullable AtomicBoolean alive) {
      return repeat(Long.MAX_VALUE, delay, units, duration, durationUnits, alive);
   }

   public static Repeat repeat(long attempts, long delay, TimeUnit units, long duration, TimeUnit durationUnits) {
      return repeat(attempts, delay, units, duration, durationUnits, null);
   }

   public static Repeat repeat(long attempts, long delay, TimeUnit units, long duration, TimeUnit durationUnits, @Nullable AtomicBoolean alive) {
      return new RepeatBuilder()
         .attempts(attempts)
         .initial(delay, units)
         .delay(delay, units)
         .random((long)(0.10*TimeUnit.NANOSECONDS.convert(delay,units)), TimeUnit.NANOSECONDS)
         .factor(1.0)
         .duration(duration, durationUnits)
         .aliveOn(alive)
         .build();
   }

   public static Repeat repeat(long attempts, double factor, long delay, TimeUnit units) {
      return repeat(attempts, factor, delay, units, -1, TimeUnit.NANOSECONDS);
   }

   public static Repeat repeat(long attempts, double factor, long delay, TimeUnit units, long duration, TimeUnit durationUnits) {
      return new RepeatBuilder()
         .attempts(attempts)
         .initial(delay, units)
         .delay(delay, units)
         .random((long)(0.10*TimeUnit.NANOSECONDS.convert(delay,units)), TimeUnit.NANOSECONDS)
         .factor(factor)
         .duration(duration, durationUnits)
         .build();
   }

   public static RepeatBuilder repeat() {
      return new RepeatBuilder();
   }

   public static final class Repeat extends AbstractRetry<Void> {
      private final long initialDelay;
      private final long standardDelay;
      private final double factor;

      public Repeat(long maxAttempts, long initialDelay, long standardDelay, long randomDelay, long maxDelay, double factor, long duration, @Nullable AtomicBoolean alive) {
         super(maxAttempts, randomDelay, maxDelay, duration, alive);
         this.initialDelay = initialDelay;
         this.standardDelay = standardDelay;
         this.factor = factor;
      }

      @Override
      Observable<?> finish(@Nullable Void value) {
         return Observable.empty();
      }

      @Override
      long getDelay(long attempt) {
         return (attempt == 1) ? initialDelay : (long)(standardDelay*Math.pow(factor, attempt-2));
      }
   }

   public static class RepeatBuilder extends AbstractRetryBuilder<Repeat> {
      @Override
      public Repeat build() {
         return new Repeat(maxAttempts, initial, delay, random, max, factor, duration, alive);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Backoff and retry logic
   /////////////////////////////////////////////////////////////////////////////

   public static Retry retry(long attempts, long delay, TimeUnit units) {
      return retry(attempts, delay, units, -1, TimeUnit.NANOSECONDS);
   }

   public static Retry retry(long delay, TimeUnit units, long duration, TimeUnit durationUnits) {
      return retry(Long.MAX_VALUE, delay, units, duration, durationUnits);
   }

   public static Retry retry(long delay, TimeUnit units, long duration, TimeUnit durationUnits, @Nullable AtomicBoolean alive) {
      return retry(Long.MAX_VALUE, delay, units, duration, durationUnits, alive);
   }

   public static Retry retry(long attempts, long delay, TimeUnit units, long duration, TimeUnit durationUnits) {
      return retry(attempts, delay, units, duration, durationUnits, null);
   }

   public static Retry retry(long attempts, long delay, TimeUnit units, long duration, TimeUnit durationUnits, @Nullable AtomicBoolean alive) {
      return new RetryBuilder()
         .attempts(attempts)
         .initial(delay, units)
         .delay(delay, units)
         .random((long)(0.10*TimeUnit.NANOSECONDS.convert(delay,units)), TimeUnit.NANOSECONDS)
         .factor(1.0)
         .duration(duration, durationUnits)
         .aliveOn(alive)
         .build();
   }

   public static Retry retry(long attempts, double factor, long delay, TimeUnit units) {
      return retry(attempts, factor, delay, units, -1, TimeUnit.NANOSECONDS);
   }

   public static Retry retry(long attempts, double factor, long delay, TimeUnit units, long duration, TimeUnit durationUnits) {
      return new RetryBuilder()
         .attempts(attempts)
         .initial(delay, units)
         .delay(delay, units)
         .random((long)(0.10*TimeUnit.NANOSECONDS.convert(delay,units)), TimeUnit.NANOSECONDS)
         .factor(factor)
         .duration(duration, durationUnits)
         .build();
   }

   public static BackoffRetry retry(Backoff backoff) {
      return new BackoffRetry(backoff, null);
   }

   public static BackoffRetry retry(Backoff backoff, AtomicBoolean isAlive) {
      return new BackoffRetry(backoff, isAlive);
   }

   public static RetryBuilder retry() {
      return new RetryBuilder();
   }

   public static final class Retry extends AbstractRetry<Throwable> {
      private final long initialDelay;
      private final long standardDelay;
      private final double factor;

      public Retry(long maxAttempts, long initialDelay, long standardDelay, long randomDelay, long maxDelay, double factor, long duration, @Nullable AtomicBoolean alive) {
         super(maxAttempts, randomDelay, maxDelay, duration, alive);
         this.initialDelay = initialDelay;
         this.standardDelay = standardDelay;
         this.factor = factor;
      }

      @Override
      Observable<?> finish(@Nullable Throwable error) {
         return Observable.error(error);
      }

      @Override
      long getDelay(long attempt) {
         return (attempt == 1) ? initialDelay : (long)(standardDelay*Math.pow(factor, attempt-2));
      }
   }

   public static final class BackoffRetry extends AbstractRetry<Throwable> {
      private final Backoff backoff;

      public BackoffRetry(Backoff backoff, @Nullable AtomicBoolean alive) {
         super(Long.MAX_VALUE, 0L, Long.MAX_VALUE, -1L, alive);
         this.backoff = backoff;
      }

      @Override
      Observable<?> finish(@Nullable Throwable error) {
         return Observable.error(error);
      }

      @Override
      long getDelay(long attempt) {
         return backoff.nextDelay(TimeUnit.NANOSECONDS);
      }

      @Override
      protected long getTotalDelay(long attempt) {
         return backoff.nextDelay(TimeUnit.NANOSECONDS);
      }
   }

   public static class RetryBuilder extends AbstractRetryBuilder<Retry> {
      @Override
      public Retry build() {
         return new Retry(maxAttempts, initial, delay, random, max, factor, duration, alive);
      }
   }

   public static abstract class AbstractRetryBuilder<T> {
      protected long maxAttempts = Long.MAX_VALUE;
      protected long initial = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
      protected long delay = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
      protected long random = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
      protected long max = TimeUnit.NANOSECONDS.convert(120, TimeUnit.SECONDS);
      protected @Nullable AtomicBoolean alive = null;
      protected long duration = -1L;
      protected double factor = 2.0;

      public AbstractRetryBuilder<T> attempts(long maxAttempts) {
         this.maxAttempts = maxAttempts;
         return this;
      }

      public AbstractRetryBuilder<T> initial(long delay, TimeUnit unit) {
         this.initial = TimeUnit.NANOSECONDS.convert(delay, unit);
         return this;
      }

      public AbstractRetryBuilder<T> delay(long delay, TimeUnit unit) {
         this.delay = TimeUnit.NANOSECONDS.convert(delay, unit);
         return this;
      }

      public AbstractRetryBuilder<T> random(long delay, TimeUnit unit) {
         this.random = TimeUnit.NANOSECONDS.convert(delay, unit);
         return this;
      }

      public AbstractRetryBuilder<T> max(long delay, TimeUnit unit) {
         this.max = TimeUnit.NANOSECONDS.convert(delay, unit);
         return this;
      }

      public AbstractRetryBuilder<T> factor(double factor) {
         this.factor = factor;
         return this;
      }

      public AbstractRetryBuilder<T> duration(long duration, TimeUnit unit) {
         this.duration = TimeUnit.NANOSECONDS.convert(duration, unit);
         return this;
      }

      public AbstractRetryBuilder<T> aliveOn(@Nullable AtomicBoolean alive) {
         this.alive = alive;
         return this;
      }

      public abstract T build();
   }

   private static abstract class AbstractRetry<T> extends Func1<Observable<? extends T>, Observable<?>> {
      private final @Nullable AtomicBoolean alive;
      private final long randomDelay;
      private final long maxDelay;
      private final long maxAttempts;
      private final long duration;
      private long retryCount;

      public AbstractRetry(long maxAttempts, long randomDelay, long maxDelay, long duration, @Nullable AtomicBoolean alive) {
         this.maxAttempts = maxAttempts;
         this.randomDelay = randomDelay;
         this.maxDelay = maxDelay;
         this.duration = duration;
         this.alive = alive;
         this.retryCount = 0;
      }

      @Override
      public Observable<?> run(Observable<? extends T> attempts) {
         final AtomicLong endTime = new AtomicLong(-1L);
         return attempts.doOnSubscribe(new Action0() {
            @Override
            public void run() {
               long end = (duration >= 0) ? (System.nanoTime() + duration) : -1L;
               if (end >= 0) {
                  endTime.compareAndSet(-1L, end);
               }
            }
         }).flatMap(new rx.functions.Func1<T, Observable<?>>() {
            @Override
            public Observable<?> call(@Nullable T value) {
               long time = System.nanoTime();
               long end = endTime.get();
               long rem = end - time;
               AtomicBoolean alv = alive;
               if (++retryCount < maxAttempts && (end < 0 || rem > 0) && (alv == null || alv.get())) {
                  log.trace("reattempt retry={}, max={}, time={}, end={}, rem={}", retryCount, maxAttempts, time, end, rem);

                  // When this Observable calls onNext, the original
                  // Observable will be retried (i.e. re-subscribed).
                  return Observable.timer(getTotalDelay(retryCount), TimeUnit.NANOSECONDS);
               }

               // Max retries or max duration hit. Just pass the error along.
               log.trace("finishing retry={}, max={}, time={}, end={}, rem={}, alive={}", retryCount, maxAttempts, time, end, rem, alive);
               return finish(value);
            }
         });
      }

      protected long getTotalDelay(long attempt) {
         long delay = getDelay(attempt);
         long random = (randomDelay == 0) ? 0 : ThreadLocalRandom.current().nextLong(randomDelay);
         long total = delay + random;
         total = (total > maxDelay) ? (maxDelay - random + randomDelay) : total;
         return total;
      }

      abstract Observable<?> finish(@Nullable T value);
      abstract long getDelay(long attempt);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Logging operators
   /////////////////////////////////////////////////////////////////////////////

   private static enum LogLevel {
      TRACE, DEBUG, INFO, WARN, ERROR
   }

   private static <T> void dolog(final LogLevel level, final Logger log, final String format, Object... value) {
      switch (level) {
      case TRACE:
         if (log.isTraceEnabled())
            log.trace(format, value);
         break;

      case DEBUG:
         if (log.isDebugEnabled())
            log.debug(format, value);
         break;

      case INFO:
         if (log.isInfoEnabled())
            log.info(format, value);
         break;

      case WARN:
         if (log.isWarnEnabled())
            log.warn(format, value);
         break;

      case ERROR:
         if (log.isErrorEnabled())
            log.error(format, value);
         break;

      default:
         break;
      }
   }

   private static <T> Observable.Operator<T,T> logger(final LogLevel level, final Logger log, final String format) {
      return new Operator<T,T>() {
         @Override
         public rx.Subscriber<? super T> run(final rx.Subscriber<? super T> s) {
            return new rx.Subscriber<T>(s) {
               @Override
               public void onNext(@Nullable T value) {
                  if (!s.isUnsubscribed()) {
                     dolog(level, log, format, value);
                     s.onNext(value);
                  }
               }

               @Override
               public void onCompleted() {
                  if (!s.isUnsubscribed()) {
                     s.onCompleted();
                  }
               }

               @Override
               public void onError(@Nullable Throwable e) {
                  if (!s.isUnsubscribed()) {
                     s.onError(e);
                  }
               }
            };
         }
      };
   }

   private static rx.Observer<Object> loggingObserver(final LogLevel level, final Logger log, final String format) {
      return new rx.Observer<Object>() {
         @Override
         public void onNext(@Nullable Object value) {
            dolog(level, log, format, value);
         }

         @Override
         public void onCompleted() {
            dolog(level, log, format, "complete");
         }

         @Override
         public void onError(@Nullable Throwable e) {
            dolog(level, log, format, e);
         }
      };
   }

   public static <T> Observable.Operator<T,T> trace(final Logger log, final String format) {
      return logger(LogLevel.TRACE, log, format);
   }

   public static <T> Observable.Operator<T,T> debug(final Logger log, final String format) {
      return logger(LogLevel.DEBUG, log, format);
   }

   public static <T> Observable.Operator<T,T> info(final Logger log, final String format) {
      return logger(LogLevel.INFO, log, format);
   }

   public static <T> Observable.Operator<T,T> warn(final Logger log, final String format) {
      return logger(LogLevel.WARN, log, format);
   }

   public static <T> Observable.Operator<T,T> error(final Logger log, final String format) {
      return logger(LogLevel.ERROR, log, format);
   }

   public static <T> Subscription traceObserver(Observable<T> ob, final Logger log, final String format) {
      return ob.subscribe(loggingObserver(LogLevel.TRACE, log, format));
   }

   public static <T> Subscription debugObserver(Observable<T> ob, final Logger log, final String format) {
      return ob.subscribe(loggingObserver(LogLevel.DEBUG, log, format));
   }

   public static <T> Subscription infoObserver(Observable<T> ob, final Logger log, final String format) {
      return ob.subscribe(loggingObserver(LogLevel.INFO, log, format));
   }

   public static <T> Subscription warnObserver(Observable<T> ob, final Logger log, final String format) {
      return ob.subscribe(loggingObserver(LogLevel.WARN, log, format));
   }

   public static <T> Subscription errorObserver(Observable<T> ob, final Logger log, final String format) {
      return ob.subscribe(loggingObserver(LogLevel.ERROR, log, format));
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utility functions and types
   /////////////////////////////////////////////////////////////////////////////

   public static final class Tuple2<T1,T2> {
      @Nullable
      public final T1 fst;

      @Nullable
      public final T2 snd;

      public Tuple2(@Nullable T1 fst, @Nullable T2 snd) {
         this.fst = fst;
         this.snd = snd;
      }
   }

   public static final class Tuple3<T1,T2,T3> {
      @Nullable
      public final T1 fst;

      @Nullable
      public final T2 snd;

      @Nullable
      public final T3 thr;

      public Tuple3(@Nullable T1 fst, @Nullable T2 snd, @Nullable T3 thr) {
         this.fst = fst;
         this.snd = snd;
         this.thr = thr;
      }
   }

   @SuppressWarnings("unchecked")
   public static <T1,T2> rx.functions.Func2<T1,T2,Tuple2<T1,T2>> mkTuple2() {
      return (rx.functions.Func2<T1,T2,Tuple2<T1,T2>>)(rx.functions.Func2<?,?,?>)MkTuple2.INSTANCE;
   }

   @SuppressWarnings("unchecked")
   public static <T1,T2,T3> rx.functions.Func3<T1,T2,T3,Tuple3<T1,T2,T3>> mkTuple3() {
      return (rx.functions.Func3<T1,T2,T3,Tuple3<T1,T2,T3>>)(rx.functions.Func3<?,?,?,?>)MkTuple3.INSTANCE;
   }

   public static enum MkTuple2 implements rx.functions.Func2<Object,Object,Tuple2<Object,Object>> {
      INSTANCE;

      @Override
      public Tuple2<Object,Object> call(@Nullable Object v1, @Nullable Object v2) {
         return new Tuple2<Object,Object>(v1,v2);
      }
   }

   public static enum MkTuple3 implements rx.functions.Func3<Object,Object,Object,Tuple3<Object,Object,Object>> {
      INSTANCE;

      @Override
      public Tuple3<Object,Object,Object> call(@Nullable Object v1, @Nullable Object v2, @Nullable Object v3) {
         return new Tuple3<Object,Object,Object>(v1,v2,v3);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // RxJava wrappers for @Nullable annotation.
   /////////////////////////////////////////////////////////////////////////////

   public static abstract class Action0 implements rx.functions.Action0 {
      @Override
      public void call() {
         run();
      }

      public abstract void run();
   }

   public static abstract class Action1<T1> implements rx.functions.Action1<T1> {
      @Override
      public void call(@Nullable T1 v1) {
         if (v1 == null) {
            throw new NullPointerException();
         }

         run(v1);
      }

      public abstract void run(T1 v1);
   }

   public static abstract class Action2<T1,T2> implements rx.functions.Action2<T1,T2> {
      @Override
      public void call(@Nullable T1 v1, @Nullable T2 v2) {
         if (v1 == null || v2 == null) {
            throw new NullPointerException();
         }

         run(v1,v2);
      }

      public abstract void run(T1 v1, T2 v2);
   }

   public static abstract class Func0<R> implements rx.functions.Func0<R> {
      @Override
      public R call() {
         return run();
      }

      public abstract R run();
   }

   public static abstract class Func1<T1,R> implements rx.functions.Func1<T1,R> {
      @Override
      public R call(@Nullable T1 v1) {
         if (v1 == null) {
            throw new NullPointerException();
         }

         return run(v1);
      }

      public abstract R run(T1 v1);
   }

   public static abstract class Func2<T1,T2,R> implements rx.functions.Func2<T1,T2,R> {
      @Override
      public R call(@Nullable T1 v1, @Nullable T2 v2) {
         if (v1 == null || v2 == null)  {
            throw new NullPointerException();
         }

         return run(v1,v2);
      }

      public abstract R run(T1 v1, T2 v2);
   }

   public static abstract class Operator<T,R> implements Observable.Operator<T,R> {
      @Override
      public rx.Subscriber<? super R> call(@Nullable rx.Subscriber<? super T> s) {
         if (s == null) {
            throw new NullPointerException();
         }

         return run(s);
      }

      public abstract rx.Subscriber<? super R> run(final rx.Subscriber<? super T> s);
   }

   public static abstract class Transformer<T,R> implements Observable.Transformer<T,R> {
      @Override
      public Observable<R> call(@Nullable Observable<T> obs) {
         if (obs == null) {
            throw new NullPointerException();
         }

         return run(obs);
      }

      public abstract Observable<R> run(Observable<T> obs);
   }

   public static abstract class Observer<T> implements rx.Observer<T> {
      @Override
      public void onNext(@Nullable T t) {
         if (t != null) {
            processNext(t);
         }
      }

      @Override
      public void onError(@Nullable Throwable e) {
         if (e != null) {
            processError(e);
         }
      }

      @Override
      public void onCompleted() {
         processCompleted();
      }

      public abstract void processNext(T t);
      public abstract void processError(Throwable e);
      public abstract void processCompleted();
   }

   public static abstract class EmptyObserver<T> extends Observer<T> {
      @Override
      public void processNext(T t) {
      }

      @Override
      public void processError(Throwable e) {
      }

      @Override
      public void processCompleted() {
      }
   }

   public static abstract class Subscriber<T> extends rx.Subscriber<T> {
      @Override
      public void onNext(@Nullable T t) {
         if (t != null) {
            processNext(t);
         }
      }

      @Override
      public void onError(@Nullable Throwable e) {
         if (e != null) {
            processError(e);
         }
      }

      @Override
      public void onCompleted() {
         processCompleted();
      }

      public abstract void processNext(T t);
      public abstract void processError(Throwable e);
      public abstract void processCompleted();
   }

   public static abstract class DelegateSubscriber<F,T> extends rx.Subscriber<F> {
      protected final rx.Subscriber<? super T> delegate;

      public DelegateSubscriber(rx.Subscriber<? super T> delegate) {
         this.delegate = delegate;
      }

      @Override
      public void onNext(@Nullable F value) {
         if (value != null && !delegate.isUnsubscribed()) {
            T tvalue = transform(value);
            if (tvalue != null) {
               delegate.onNext(transform(value));
            }
         }
      }

      @Override
      public void onError(@Nullable Throwable e) {
         if (!delegate.isUnsubscribed()) {
            delegate.onError(e);
         }
      }

      @Override
      public void onCompleted() {
         if (!delegate.isUnsubscribed()) {
            delegate.onCompleted();
         }
      }

      @Nullable
      public abstract T transform(F from);
   }

   public static class SwallowCompleteObserver<T> implements rx.Observer<T> {
      private final rx.Observer<? super T> delegate;

      public SwallowCompleteObserver(rx.Observer<? super T> delegate) {
         this.delegate = delegate;
      }

      @Override
      public void onCompleted() {
      }

      @Override
      public void onError(@Nullable Throwable e) {
      }

      @Override
      public void onNext(@Nullable T t) {
         delegate.onNext(t);
      }
   }

   public static final rx.Observer<Object> SWALLOW_ALL = new rx.Observer<Object>() {
      @Override
      public void onCompleted() {
      }

      @Override
      public void onError(@Nullable Throwable e) {
         if (e != null) {
            if (log.isTraceEnabled()) {
               log.trace("swallowed observable error: {}", e.getMessage(), e);
            } else {
               log.trace("swallowed observable error: {}", e.getMessage());
            }
         }
      }

      @Override
      public void onNext(@Nullable Object t) {
      }
   };

   public static abstract class OnSubscribe<T> implements Observable.OnSubscribe<T> {
      @Override
      public void call(@Nullable rx.Subscriber<? super T> sub) {
         if (sub == null) {
            return;
         }

         run(sub);
      }

      public abstract void run(rx.Subscriber<? super T> sub);
   }
}

