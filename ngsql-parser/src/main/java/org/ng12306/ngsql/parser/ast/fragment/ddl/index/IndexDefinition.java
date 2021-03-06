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
package org.ng12306.ngsql.parser.ast.fragment.ddl.index;

import java.util.Collections;
import java.util.List;

import org.ng12306.ngsql.parser.ast.ASTNode;
import org.ng12306.ngsql.parser.visitor.SQLASTVisitor;


/**
 * 
* [添加说明]
* @author: <a href="mailto:lvbomr@gmail.com">lvbo</a>
* @date: 2013-5-26 上午11:31:42
* @version: 1.0
 */
public class IndexDefinition implements ASTNode {
    public static enum IndexType {
        BTREE,
        HASH
    }

    private final IndexType indexType;
    private final List<IndexColumnName> columns;
    private final List<IndexOption> options;

    @SuppressWarnings("unchecked")
    public IndexDefinition(IndexType indexType, List<IndexColumnName> columns, List<IndexOption> options) {
        this.indexType = indexType;
        if (columns == null || columns.isEmpty()) throw new IllegalArgumentException("columns is null or empty");
        this.columns = columns;
        this.options = (List<IndexOption>) (options == null || options.isEmpty() ? Collections.emptyList() : options);
    }

    public IndexType getIndexType() {
        return indexType;
    }

    /**
     * @return never null
     */
    public List<IndexColumnName> getColumns() {
        return columns;
    }

    /**
     * @return never null
     */
    public List<IndexOption> getOptions() {
        return options;
    }

    @Override
    public void accept(SQLASTVisitor visitor) {
        // QS_TODO

    }

}
