/*
 * Copyright 2015 Intershop Communications AG.
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
 *  limitations under the License.
 */
package com.intershop.gradle.scm.utils

import groovy.transform.CompileStatic

/**
 * SCM exception
 */
@CompileStatic
class ScmException extends RuntimeException {

    /**
     * Constructs a {@code ScmException} instance with no error message.
     */
    public ScmException() {
        super()
    }

    /**
     * Constructs a {@code ScmException} instance with an error message.
     *
     * @param message the error message
     */
    public ScmException(String message) {
        super(message)
    }

    /**
     * Constructs a {@code ParseException} instance with an error message.
     *
     * @param message the error message
     */
    public ScmException(String message, Throwable cause) {
        super(message, cause)
    }

    /**
     * Returns the string representation of this exception.
     *
     * @return the string representation of this exception
     */
    @Override
    public String toString() {
        Throwable cause = getCause()
        String msg = getMessage()
        if (msg != null) {
            msg += ((cause != null) ? " (" + cause.toString() + ")" : "")
            return msg
        }
        return ((cause != null) ? cause.toString() : "")
    }
}
