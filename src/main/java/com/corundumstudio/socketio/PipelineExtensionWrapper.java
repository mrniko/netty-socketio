package com.corundumstudio.socketio;


import io.netty.channel.ChannelHandler;

/**
 * Copyright 2012 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class PipelineExtensionWrapper {

	public enum Mode {
		ADD_FIRST,
		ADD_BEFORE,
		ADD_AFTER,
		ADD_LAST;
	}

	private String name;
	private Mode mode;
	private String refName;
	private ChannelHandler handler;

	public PipelineExtensionWrapper(String name, Mode mode, String refN, ChannelHandler ch) {
		setName(name);
		setMode(mode);
		setRefName(refN);
		setHandler(ch);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {

		if (name == null || name.equalsIgnoreCase("")) {
			throw new NullPointerException("name cannot be null or empty");
		}

		this.name = name;
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public String getRefName() {
		return refName;
	}

	public void setRefName(String refN) {
		this.refName = refN;
	}

	public ChannelHandler getHandler() {
		return handler;
	}

	public void setHandler(ChannelHandler handler) {

		if (handler == null) {
			throw new NullPointerException("handler object cannot be null");
		}

		this.handler = handler;
	}
}
