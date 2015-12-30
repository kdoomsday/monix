/*
 * Copyright (c) 2014-2015 by its authors. Some rights reserved.
 * See the project homepage at: http://www.monix.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package monix.concurrent

import java.util.concurrent.TimeoutException
import monix.internal.concurrent.{PromiseCompleteWithRunnable, PromiseFailureRunnable, RunnableAction}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

object FutureUtils {
  /** Utility that returns a new Future that either completes with
    * the original Future's result or with a TimeoutException in case
    * the maximum wait time was exceeded.
    *
    * @param atMost specifies the maximum wait time until the future is
    *               terminated with a TimeoutException
    *
    * @param s is the Scheduler, needed for completing our internal promise
    *
    * @return a new future that will either complete with the result of our
    *         source or fail in case the timeout is reached.
    */
  def timeout[T](source: Future[T], atMost: FiniteDuration)(implicit s: Scheduler): Future[T] = {
    val err = new TimeoutException
    val promise = Promise[T]()
    val task = s.scheduleOnce(atMost.length, atMost.unit, PromiseFailureRunnable(promise, err))

    source.onComplete { case r =>
      // canceling task to prevent waisted CPU resources and memory leaks
      // if the task has been executed already, this has no effect
      task.cancel()
      promise.tryComplete(r)
    }

    promise.future
  }

  /** Utility that returns a new Future that either completes with
    * the original Future's result or after the timeout specified by
    * `atMost` it tries to complete with the given `fallback`.
    * Whatever `Future` finishes first after the timeout, will win.
    *
    * @param atMost specifies the maximum wait time until the future is
    *               terminated with a TimeoutException
    *
    * @param fallback the fallback future that gets triggered after timeout
    *
    * @param s is the Scheduler, needed for completing our internal promise
    *
    * @return a new future that will either complete with the result of our
    *         source or with the fallback in case the timeout is reached
    */
  def timeout[T](source: Future[T], atMost: FiniteDuration, fallback: => Future[T])
    (implicit s: Scheduler): Future[T] = {

    val promise = Promise[T]()
    val task = s.scheduleOnce(atMost.length, atMost.unit,
      PromiseCompleteWithRunnable(promise, fallback))

    source.onComplete { case r =>
      // canceling task to prevent waisted CPU resources and memory leaks
      // if the task has been executed already, this has no effect
      task.cancel()
      promise.tryComplete(r)
    }

    promise.future
  }

  /** Utility that lifts a `Future[T]` into a `Future[Try[T]]`, just because
    * it is useful sometimes.
    */
  def liftTry[T](source: Future[T])(implicit ec: ExecutionContext): Future[Try[T]] = {
    if (source.isCompleted) {
      Future.successful(source.value.get)
    }
    else {
      val p = Promise[Try[T]]()
      source.onComplete { case result => p.success(result) }
      p.future
    }
  }

  /** Creates a future that completes with the specified `result`, but only
    * after the specified `delay`.
    */
  def delayedResult[T](delay: FiniteDuration)(result: => T)(implicit s: Scheduler): Future[T] = {
    val p = Promise[T]()
    s.scheduleOnce(delay.length, delay.unit, RunnableAction(p.complete(Try(result))))
    p.future
  }
}