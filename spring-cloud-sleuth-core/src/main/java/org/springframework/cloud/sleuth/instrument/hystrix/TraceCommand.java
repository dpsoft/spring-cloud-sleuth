/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.instrument.hystrix;

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.trace.SpanContextHolder;

import com.netflix.hystrix.HystrixCommand;

/**
 * Abstraction over {@code HystrixCommand} that wraps command execution with Trace setting
 *
 * @see HystrixCommand
 * @see Tracer
 *
 * @author Tomasz Nurkiewicz, 4financeIT
 * @author Marcin Grzejszczak, 4financeIT
 * @author Spencer Gibb
 */
public abstract class TraceCommand<R> extends HystrixCommand<R> {

	private final Tracer tracer;
	private final Span parentSpan;

	protected TraceCommand(Tracer tracer, Setter setter) {
		super(setter);
		this.tracer = tracer;
		this.parentSpan = tracer.getCurrentSpan();
	}

	@Override
	protected R run() throws Exception {
		enforceThatHystrixThreadIsNotPollutedByPreviousTraces();
		Span span = this.tracer.joinTrace(getCommandKey().name(), this.parentSpan);
		try {
			return doRun();
		}
		finally {
			this.tracer.close(span);
		}
	}

	// TODO: Do more analysis why this is not removed properly
	private void enforceThatHystrixThreadIsNotPollutedByPreviousTraces() {
		SpanContextHolder.removeCurrentSpan();
	}

	public abstract R doRun() throws Exception;
}
