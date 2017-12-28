/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.roogle.simple.stetho.inspector.protocol.module;

import android.content.Context;

/**
 * Replaces {@link DatabaseDriver} to enforce that the generic type must
 * extend {@link DatabaseDescriptor}.
 */
public abstract class DatabaseDriver<DESC extends DatabaseDescriptor>
		extends BaseDatabaseDriver<DESC> {
	public DatabaseDriver(Context context) {
		super(context);
	}
}
