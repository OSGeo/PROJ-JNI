/*
 * Copyright © 2019 Agency for Data Supply and Efficiency
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
#include <proj.h>
#include "org_kortforsyningen_proj_Context.h"


/** \brief Allocates a PJ_CONTEXT for using PROJ in a multi-threads environment.
 *
 * Each thread should have its own PJ_CONTEXT instance.
 *
 * @param  env         The JNI environment.
 * @param  caller      The class from which this method has been invoked.
 * @return The address of the new PJ_CONTEXT structure, or 0 in case of failure.
 */
JNIEXPORT jlong JNICALL Java_org_kortforsyningen_proj_Context_create(JNIEnv *env, jclass caller) {
    PJ_CONTEXT *ctx = proj_context_create();
    return (jlong) ctx;
}


/** \brief Releases a PJ_CONTEXT.
 *
 * @param  env     The JNI environment.
 * @param  caller  The class from which this method has been invoked.
 * @param  ctx     The address of the PJ_CONTEXT to release.
 */
JNIEXPORT void JNICALL Java_org_kortforsyningen_proj_Context_destroy(JNIEnv *env, jclass caller, jlong ctxPtr) {
    PJ_CONTEXT *ctx = (PJ_CONTEXT*) ctxPtr;
    proj_context_destroy(ctx);
}
