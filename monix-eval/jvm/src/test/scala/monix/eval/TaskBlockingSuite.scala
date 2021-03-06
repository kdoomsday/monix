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

import minitest.SimpleTestSuite
import monix.execution.Scheduler.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._

object TaskBlockingSuite extends SimpleTestSuite {
  test("blocking should work") {
    val source1 = Task(100)
    val source2 = Task(200).onErrorHandleWith { case e: Exception => Task.raiseError(e) }

    val derived = source1.map { x =>
      val r = Await.result(source2.runAsync, 10.seconds)
      r + x
    }

    val result = Await.result(derived.runAsync, 10.seconds)
    assertEquals(result, 300)
  }
}
