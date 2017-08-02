/*
 * Copyright (c) 2014-2017 by The Monix Project Developers.
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

package monix.reactive.observables

import monix.execution.cancelables.MultiAssignmentCancelable
import monix.reactive.Observable
import monix.reactive.observers.Subscriber

/** A `StackedObservable` is an [[Observable]] type used in operators that
  * end up being used in loops and that need to be memory safe
  * (e.g. `++`, `suspend`).
  *
  * This is achieved with the same trick used in the [[monix.eval.Task]]
  * implementation. The problem with recursive operators is that they
  * are leaving work behind due to the need to return a `Cancelable`
  * and thus leak memory.
  */
trait ChainedObservable[+A] extends Observable[A] {
  /** Alternative subscription method, that gets injected a stacked
    * cancelable, in order to preserve memory safety.
    */
  def unsafeSubscribeFn(conn: MultiAssignmentCancelable, subscriber: Subscriber[A]): Unit
}

object ChainedObservable {
  /** Function that checks if the `source` is a `ChainedObservable`,
    * subscribing to it by injecting the provided `conn` if it is,
    * otherwise it subscribes
    */
  def subscribe[A](source: Observable[A], conn: MultiAssignmentCancelable, out: Subscriber[A]): Unit =
    out.scheduler.executeTrampolined { () =>
      source match {
        case _: ChainedObservable[_] =>
          source.asInstanceOf[ChainedObservable[A]].unsafeSubscribeFn(conn, out)
        case _ =>
          conn := source.unsafeSubscribeFn(out)
      }
    }
}