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
package org.ng12306.ngsql.parser.ast.stmt.ddl;

import com.alibaba.cobar.parser.ast.expression.primary.Identifier;
import com.alibaba.cobar.parser.ast.stmt.SQLStatement;
import com.alibaba.cobar.parser.visitor.SQLASTVisitor;

/**
 * 
* [添加说明]
* @author: <a href="mailto:lvbomr@gmail.com">lvbo</a>
* @date: 2013-5-26 上午11:54:47
* @version: 1.0
 */
public class DescTableStatement implements SQLStatement {
    private final Identifier table;

    public DescTableStatement(Identifier table) {
        if (table == null) throw new IllegalArgumentException("table is null for desc table");
        this.table = table;
    }

    public Identifier getTable() {
        return table;
    }

    @Override
    public void accept(SQLASTVisitor visitor) {
        visitor.visit(this);
    }
}