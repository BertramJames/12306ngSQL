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
package org.ng12306.ngsql.parser.ast.stmt.dal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ng12306.ngsql.parser.ast.expression.Expression;
import org.ng12306.ngsql.parser.ast.fragment.Limit;
import org.ng12306.ngsql.parser.visitor.SQLASTVisitor;


/**
 * 
* [添加说明]
* @author: <a href="mailto:lvbomr@gmail.com">lvbo</a>
* @date: 2013-5-26 上午11:47:36
* @version: 1.0
 */
public class ShowProfile extends DALShowStatement {
    /** enum name must equals to real sql while ' ' is replaced with '_' */
    public static enum Type {
        ALL,
        BLOCK_IO,
        CONTEXT_SWITCHES,
        CPU,
        IPC,
        MEMORY,
        PAGE_FAULTS,
        SOURCE,
        SWAPS
    }

    private final List<Type> types;
    private final Expression forQuery;
    private final Limit limit;

    public ShowProfile(List<Type> types, Expression forQuery, Limit limit) {
        if (types == null || types.isEmpty()) {
            this.types = Collections.emptyList();
        } else if (types instanceof ArrayList) {
            this.types = types;
        } else {
            this.types = new ArrayList<ShowProfile.Type>(types);
        }
        this.forQuery = forQuery;
        this.limit = limit;
    }

    /**
     * @return never null
     */
    public List<Type> getTypes() {
        return types;
    }

    public Expression getForQuery() {
        return forQuery;
    }

    public Limit getLimit() {
        return limit;
    }

    @Override
    public void accept(SQLASTVisitor visitor) {
        visitor.visit(this);
    }
}
