/*
 * Copyright (c) 2014-2018 by The Monix Project Developers.
 * See the project homepage at: https://monix.io
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

package monix.eval

import monix.execution.Scheduler
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/** Safe `App` type that runs a [[Task]] action.
  *
  * Clients should implement `run`, `runl`, or `runc`.
  *
  * Also available for Scala.js, but without the ability
  * to take arguments and without the blocking in main.
  */
trait TaskApp {
  def run(args: Array[String]): Task[Unit] =
    runl(args.toList)

  def runl(args: List[String]): Task[Unit] =
    runc

  def runc: Task[Unit] = {
    // $COVERAGE-OFF$
    Task.now(())
    // $COVERAGE-ON$
  }

  /** Scheduler for executing the [[Task]] action.
    * Defaults to `global`, but can be overridden.
    */
  protected val scheduler: Coeval[Scheduler] =
    Coeval.evalOnce(Scheduler.global)

  /** [[monix.eval.Task.Options Options]] for executing the
    * [[Task]] action. The default value is defined in
    * [[monix.eval.Task.defaultOptions defaultOptions]],
    * but can be overridden.
    */
  protected val options: Coeval[Task.Options] =
    Coeval.evalOnce(Task.defaultOptions)

  final def main(args: Array[String]): Unit = {
    val f = run(args).runAsyncOpt(scheduler.value, options.value)
    Await.result(f, Duration.Inf)
  }
}
