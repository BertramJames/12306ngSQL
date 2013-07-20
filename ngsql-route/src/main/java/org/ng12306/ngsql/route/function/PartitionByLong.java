/*
 * Copyright 2012-2013 NgSql Group.
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
package org.ng12306.ngsql.route.function;

import java.util.List;
import java.util.Map;

import org.ng12306.ngsql.parser.ast.expression.Expression;
import org.ng12306.ngsql.parser.ast.expression.primary.function.FunctionExpression;

/*-
 * 支持字符类型的ID，按照字符数值的区间分区，实现单维路由
 * @author:Fredric
 * @date: 2013-5-17
 */
public class PartitionByLong extends PartitionFunction{
    public PartitionByLong(String functionName, List<Expression> arguments) {
        super(functionName, arguments);
    }

    @Override
    public Object evaluationInternal(Map<? extends Object, ? extends Object> parameters) {
        Object arg = arguments.get(0).evaluation(parameters);
        if (arg == null) {
            throw new IllegalArgumentException("partition key is null ");
        } else if (arg == UNEVALUATABLE) {
            throw new IllegalArgumentException("argument is UNEVALUATABLE");
        }
        Number key;
        if (arg instanceof Number) {
            key = (Number) arg;
        } else if (arg instanceof String) {
            key = Long.parseLong((String) arg);
        } else {
            throw new IllegalArgumentException("unsupported data type for partition key: " + arg.getClass());
        }
        return partitionIndex(key.longValue());
    }

    @Override
    public FunctionExpression constructFunction(List<Expression> arguments) {
        if (arguments == null || arguments.size() != 1)
            throw new IllegalArgumentException("function "
                                               + getFunctionName()
                                               + " must have 1 argument but is "
                                               + arguments);
        PartitionByLong partitionFunc = new PartitionByLong(functionName, arguments);
        partitionFunc.count = count;
        partitionFunc.length = length;
        return partitionFunc;
    }
}
