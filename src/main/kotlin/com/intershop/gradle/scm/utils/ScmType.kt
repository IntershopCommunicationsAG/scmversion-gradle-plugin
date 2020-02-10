/*
 * Copyright 2020 Intershop Communications AG.
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

/**
 * <p>This is an enumuration of all supported SCM of this plugin.</p>
 * <p><b>git</b> - Git
 * <p><b>svn</b> - Subversion
 * <p><b>file</b> - This is used for unknown SCMs or not stored projects.
 */
enum class ScmType {
    GIT ,
    FILE ;
}
