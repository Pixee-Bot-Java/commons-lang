/* Copyright 2023 Norconex Inc.
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
package com.norconex.commons.lang.flow.module;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonToken;
import com.norconex.commons.lang.flow.AnyOf;
import com.norconex.commons.lang.flow.FlowCondition;
import com.norconex.commons.lang.flow.module.FlowDeserializer.FlowDeserContext;

class AnyOfHandler<T> implements StatementHandler<AnyOf<T>> {

    @SuppressWarnings("unchecked")
    @Override
    public AnyOf<T> read(FlowDeserContext ctx) throws IOException {

        var p = ctx.getParser();
        var anyOf = new AnyOf<T>();
        FlowUtil.logOpen(ctx, p.getCurrentName());
        p.nextToken(); // <-- START_OBJECT

        while ((p.nextToken()) != JsonToken.END_OBJECT) { // <-- FIELD_NAME
            var fieldName = p.getCurrentName();
            if (!Statement.isAnyOf(fieldName,
                    Statement.CONDITION, Statement.ALLOF, Statement.ANYOF)) {
                throw new IOException("""
                        Only <condition>, <allOf>, and <anyOf> are \
                        permitted as direct child elements of <if> and
                        <ifNot>. Got instead: "%s"
                        """.formatted(fieldName));
            }
            anyOf.add((FlowCondition<T>)
                    Statement.of(fieldName).handler().read(ctx));
        }

        FlowUtil.logClose(ctx, p.getCurrentName());
        return anyOf;
    }

    @Override
    public void write() throws IOException {
        //TODO
    }
}
